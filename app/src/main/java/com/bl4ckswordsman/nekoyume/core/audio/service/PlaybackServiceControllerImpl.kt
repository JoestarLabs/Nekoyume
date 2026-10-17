package com.bl4ckswordsman.nekoyume.core.audio.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngine
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [PlaybackServiceController] coordinating between
 * UI callers, the headless [AudioLoopEngine], and the background [NekoyumePlaybackService].
 */
@Singleton
class PlaybackServiceControllerImpl @Inject constructor(
    private val context: Context,
    private val audioEngine: AudioLoopEngine,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : PlaybackServiceController {

    private val _serviceState = MutableStateFlow(ServiceState())
    override val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    override val mixState: StateFlow<AudioMixState> = audioEngine.mixState

    init {
        scope.launch {
            audioEngine.mixState.collect { mix ->
                val activeCount = mix.activeTracks.size
                val current = _serviceState.value

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
        startServiceIfNecessary()
        audioEngine.resumeAll()
    }

    override fun pause() {
        audioEngine.pauseAll()
    }

    override fun stop() {
        audioEngine.stopAll()
        stopService()
    }

    override fun toggleTrack(trackId: String) {
        val currentTrack = audioEngine.mixState.value.activeTracks[trackId]
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

    private fun startServiceIfNecessary() {
        try {
            val intent = Intent(context, NekoyumePlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
            // Service startup fallback or already running
        }
    }

    private fun stopService() {
        try {
            val intent = Intent(context, NekoyumePlaybackService::class.java)
            context.stopService(intent)
        } catch (_: Exception) {
            // Ignore if service not running
        }
    }
}
