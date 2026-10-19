package com.bl4ckswordsman.nekoyume.core.audio.service

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngine
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [PlaybackServiceController] coordinating between
 * UI callers, the headless [AudioLoopEngine], and the background [NekoyumePlaybackService]
 * via Media3's [MediaController] and [SessionToken] API (FR-012).
 */
@Singleton
class PlaybackServiceControllerImpl(
    private val context: Context,
    private val audioEngine: AudioLoopEngine,
    private val mixPersistence: MixStatePersistence?,
    private val sessionToken: SessionToken?,
    private val scope: CoroutineScope
) : PlaybackServiceController {

    @Inject
    constructor(
        context: Context,
        audioEngine: AudioLoopEngine,
        mixPersistence: MixStatePersistence,
        sessionToken: SessionToken
    ) : this(
        context = context,
        audioEngine = audioEngine,
        mixPersistence = mixPersistence,
        sessionToken = sessionToken,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    )

    constructor(
        context: Context,
        audioEngine: AudioLoopEngine,
        scope: CoroutineScope
    ) : this(
        context = context,
        audioEngine = audioEngine,
        mixPersistence = null,
        sessionToken = null,
        scope = scope
    )

    constructor(
        context: Context,
        audioEngine: AudioLoopEngine,
        mixPersistence: MixStatePersistence?,
        scope: CoroutineScope
    ) : this(
        context = context,
        audioEngine = audioEngine,
        mixPersistence = mixPersistence,
        sessionToken = null,
        scope = scope
    )

    private val _serviceState = MutableStateFlow(ServiceState())
    override val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _mixState = MutableStateFlow(AudioMixState())
    override val mixState: StateFlow<AudioMixState> = _mixState.asStateFlow()

    private var mediaController: MediaController? = null

    init {
        // Connect via MediaController and SessionToken per FR-012
        try {
            val token = sessionToken ?: SessionToken(context, ComponentName(context, NekoyumePlaybackService::class.java))
            val controllerFuture = MediaController.Builder(context, token).buildAsync()
            controllerFuture.addListener({
                try {
                    mediaController = controllerFuture.get()
                } catch (_: Throwable) {
                    // Ignored if controller cannot be resolved in current context
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (_: Throwable) {
            // Ignored in test environment
        }

        // Cold-launch mix restore from DataStore per FR-017 & T033
        if (mixPersistence != null) {
            scope.launch {
                try {
                    val saved = mixPersistence.getSavedMixState()
                    if (saved != null && saved.hasActiveTracks && !audioEngine.mixState.value.hasActiveTracks) {
                        val inactiveTracks = saved.activeTracks.mapValues { (_, track) ->
                            track.copy(status = TrackPlaybackStatus.PAUSED)
                        }
                        _mixState.value = saved.copy(
                            isMasterPaused = true,
                            activeTracks = inactiveTracks
                        )
                        _serviceState.value = ServiceState(
                            playbackState = ServicePlaybackState.STOPPED,
                            isForeground = false,
                            activeTrackCount = inactiveTracks.size,
                            pausedTimestampEpochMs = null,
                            hasNotificationPermission = _serviceState.value.hasNotificationPermission
                        )
                    }
                } catch (_: Throwable) {
                    // Ignore restore failures on cold launch
                }
            }
        }

        scope.launch {
            audioEngine.mixState.collect { mix ->
                val activeCount = mix.activeTracks.size
                val current = _serviceState.value

                if (mix.hasActiveTracks || mixPersistence == null || _mixState.value.activeTracks.isEmpty()) {
                    _mixState.value = mix
                }

                val newState = when {
                    mix.isPlaying -> {
                        ServiceState(
                            playbackState = ServicePlaybackState.PLAYING,
                            isForeground = true,
                            activeTrackCount = activeCount,
                            pausedTimestampEpochMs = null,
                            hasNotificationPermission = current.hasNotificationPermission
                        )
                    }
                    activeCount > 0 || mix.hasActiveTracks -> {
                        val timestamp = current.pausedTimestampEpochMs ?: System.currentTimeMillis()
                        ServiceState(
                            playbackState = ServicePlaybackState.PAUSED,
                            isForeground = false,
                            activeTrackCount = activeCount,
                            pausedTimestampEpochMs = timestamp,
                            hasNotificationPermission = current.hasNotificationPermission
                        )
                    }
                    else -> {
                        ServiceState(
                            playbackState = ServicePlaybackState.STOPPED,
                            isForeground = false,
                            activeTrackCount = 0,
                            pausedTimestampEpochMs = null,
                            hasNotificationPermission = current.hasNotificationPermission
                        )
                    }
                }
                _serviceState.value = newState
            }
        }
    }

    override fun play() {
        val controller = mediaController
        if (controller != null) {
            controller.play()
        } else {
            audioEngine.resumeAll()
        }
    }

    override fun pause() {
        val controller = mediaController
        if (controller != null) {
            controller.pause()
        } else {
            audioEngine.pauseAll()
        }
    }

    override fun stop() {
        val controller = mediaController
        if (controller != null) {
            controller.stop()
        } else {
            audioEngine.stopAll()
        }
    }

    override fun toggleTrack(trackId: String) {
        val currentTrack = mixState.value.activeTracks[trackId]
        if (currentTrack != null) {
            audioEngine.setTrackMuted(trackId, !currentTrack.isMuted)
        }
    }

    override fun setTrackVolume(trackId: String, volume: Float) {
        audioEngine.setTrackVolume(trackId, volume)
    }

    override fun setMasterVolume(volume: Float) {
        audioEngine.setMasterVolume(volume)
    }
}
