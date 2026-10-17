package com.bl4ckswordsman.nekoyume.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioFocusState
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * AndroidX Media3 implementation of [AudioLoopEngine].
 *
 * Coordinates a pool of up to 8 lightweight [Player] instances with hardware-mixed output,
 * seamless continuous looping, independent volume attenuation, master volume scaling,
 * and reactive [AudioMixState] distribution.
 */
class Media3AudioLoopEngine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val playerFactory: (soundId: String, assetPath: String) -> Player
) : AudioLoopEngine {

    /**
     * Primary Android runtime constructor taking application [Context].
     */
    constructor(context: Context) : this(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        dispatcher = Dispatchers.Main.immediate,
        playerFactory = { _, assetPath ->
            val player = ExoPlayer.Builder(context).build()
            val uri = Uri.parse("asset:///$assetPath")
            player.setMediaItem(MediaItem.fromUri(uri))
            player
        }
    )

    private val mutex = Mutex()
    private val players = ConcurrentHashMap<String, Player>()
    private val rampHandler = com.bl4ckswordsman.nekoyume.core.audio.ramp.VolumeRampHandler(scope, dispatcher)

    private val _mixState = MutableStateFlow(AudioMixState())
    override val mixState: StateFlow<AudioMixState> = _mixState.asStateFlow()

    override suspend fun startTrack(
        soundId: String,
        assetPath: String,
        initialVolume: Float
    ): Result<Unit> = mutex.withLock {
        require(soundId.isNotBlank()) { "soundId cannot be blank" }
        val clampedVolume = initialVolume.coerceIn(0.0f, 1.0f)

        // If track is already active, return success
        if (players.containsKey(soundId)) {
            return Result.success(Unit)
        }

        // Enforce hard concurrency ceiling
        if (players.size >= AudioMixState.MAX_CONCURRENT_TRACKS) {
            return Result.failure(MaxTracksExceededException())
        }

        try {
            val player = playerFactory(soundId, assetPath)
            player.repeatMode = Player.REPEAT_MODE_ONE

            // Calculate and set initial volume
            val effectiveVolume = calculateEffectiveVolume(clampedVolume)
            player.volume = effectiveVolume

            // Attach listener for playback lifecycle and errors
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> updateTrackStatus(soundId, TrackPlaybackStatus.LOADING)
                        Player.STATE_READY -> {
                            val currentStatus = if (_mixState.value.isMasterPaused) {
                                TrackPlaybackStatus.PAUSED
                            } else {
                                TrackPlaybackStatus.PLAYING
                            }
                            updateTrackStatus(soundId, currentStatus)
                        }
                        Player.STATE_IDLE -> {
                            // Player stopped
                        }
                        Player.STATE_ENDED -> {
                            // Handled by REPEAT_MODE_ONE
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _mixState.update { state ->
                        val updated = state.activeTracks.toMutableMap()
                        val current = updated[soundId]
                        if (current != null) {
                            updated[soundId] = current.copy(
                                status = TrackPlaybackStatus.ERROR,
                                errorMessage = error.message ?: "Playback error"
                            )
                        }
                        state.copy(activeTracks = updated)
                    }
                }
            })

            players[soundId] = player

            // Register track in active mix state
            _mixState.update { state ->
                val updated = state.activeTracks.toMutableMap()
                updated[soundId] = TrackPlaybackState(
                    soundId = soundId,
                    status = TrackPlaybackStatus.LOADING,
                    volume = clampedVolume
                )
                state.copy(activeTracks = updated)
            }

            player.prepare()
            if (!_mixState.value.isMasterPaused) {
                player.play()
                updateTrackStatus(soundId, TrackPlaybackStatus.PLAYING)
            } else {
                updateTrackStatus(soundId, TrackPlaybackStatus.PAUSED)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopTrack(soundId: String): Result<Unit> = mutex.withLock {
        val player = players.remove(soundId) ?: return Result.success(Unit)
        releasePlayer(player)

        _mixState.update { state ->
            val updated = state.activeTracks.toMutableMap()
            updated.remove(soundId)
            state.copy(activeTracks = updated)
        }
        Result.success(Unit)
    }

    override fun setTrackVolume(soundId: String, volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _mixState.update { state ->
            val updated = state.activeTracks.toMutableMap()
            val current = updated[soundId] ?: return@update state
            updated[soundId] = current.copy(volume = clamped)
            state.copy(activeTracks = updated)
        }
        applyTrackHardwareVolume(soundId)
    }

    override fun setTrackMuted(soundId: String, isMuted: Boolean) {
        _mixState.update { state ->
            val updated = state.activeTracks.toMutableMap()
            val current = updated[soundId] ?: return@update state
            updated[soundId] = current.copy(isMuted = isMuted)
            state.copy(activeTracks = updated)
        }
        applyTrackHardwareVolume(soundId)
    }

    override fun setMasterVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _mixState.update { it.copy(masterVolume = clamped) }
        players.keys.forEach { applyTrackHardwareVolume(it) }
    }

    override fun pauseAll() {
        _mixState.update { state ->
            val updated = state.activeTracks.mapValues { (_, track) ->
                if (track.status == TrackPlaybackStatus.PLAYING) {
                    track.copy(status = TrackPlaybackStatus.PAUSED)
                } else {
                    track
                }
            }
            state.copy(isMasterPaused = true, activeTracks = updated)
        }
        players.values.forEach { it.pause() }
    }

    override fun resumeAll() {
        _mixState.update { state ->
            val updated = state.activeTracks.mapValues { (_, track) ->
                if (track.status == TrackPlaybackStatus.PAUSED) {
                    track.copy(status = TrackPlaybackStatus.PLAYING)
                } else {
                    track
                }
            }
            state.copy(isMasterPaused = false, activeTracks = updated)
        }
        players.values.forEach { it.play() }
    }

    override fun stopAll() {
        players.forEach { (_, player) ->
            releasePlayer(player)
        }
        players.clear()
        _mixState.update { it.copy(activeTracks = emptyMap(), isMasterPaused = false) }
    }

    override fun getTrackState(soundId: String): Flow<TrackPlaybackState?> {
        return mixState.map { it.activeTracks[soundId] }.distinctUntilChanged()
    }

    override fun release() {
        rampHandler.cancelAll()
        stopAll()
    }

    private fun updateTrackStatus(soundId: String, status: TrackPlaybackStatus) {
        _mixState.update { state ->
            val updated = state.activeTracks.toMutableMap()
            val current = updated[soundId] ?: return@update state
            updated[soundId] = current.copy(status = status)
            state.copy(activeTracks = updated)
        }
    }

    private fun calculateEffectiveVolume(trackVolume: Float): Float {
        val master = _mixState.value.masterVolume
        val duckMultiplier = if (_mixState.value.audioFocusState == AudioFocusState.DUCKED) 0.3f else 1.0f
        return (trackVolume * master * duckMultiplier).coerceIn(0.0f, 1.0f)
    }

    private fun applyTrackHardwareVolume(soundId: String) {
        val player = players[soundId] ?: return
        val track = _mixState.value.activeTracks[soundId] ?: return
        val targetVolume = if (track.isMuted) 0.0f else calculateEffectiveVolume(track.volume)
        rampHandler.rampVolume(
            targetId = soundId,
            fromVolume = player.volume,
            toVolume = targetVolume,
            onVolumeStep = { player.volume = it }
        )
    }

    private fun releasePlayer(player: Player) {
        try {
            player.stop()
            player.release()
        } catch (_: Exception) {
            // Player already stopped or released
        }
    }
}
