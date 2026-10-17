package com.bl4ckswordsman.nekoyume.core.audio

import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Exception thrown when an attempt is made to start more concurrent tracks
 * than the engine's hard ceiling ([AudioMixState.MAX_CONCURRENT_TRACKS]).
 */
class MaxTracksExceededException(
    message: String = "Maximum concurrent active tracks (${AudioMixState.MAX_CONCURRENT_TRACKS}) reached"
) : IllegalStateException(message)

/**
 * Headless multi-stream audio playback and looping engine.
 *
 * Capable of managing up to 8 simultaneous looping audio streams with independent volume attenuation,
 * master volume scaling, seamless gapless repetition, and platform audio focus management.
 */
interface AudioLoopEngine {

    /**
     * Observable aggregate mix state of the audio engine.
     */
    val mixState: StateFlow<AudioMixState>

    /**
     * Start playing and continuous looping of a sound asset.
     *
     * @param soundId Unique sound identifier matching catalog manifest.
     * @param assetPath Relative asset path (e.g., "sounds/rain.ogg").
     * @param initialVolume Starting volume level between 0.0f and 1.0f (defaults to 1.0f).
     * @return [Result.success] if initiated, or [Result.failure] if concurrency ceiling (8) reached.
     */
    suspend fun startTrack(
        soundId: String,
        assetPath: String,
        initialVolume: Float = 1.0f
    ): Result<Unit>

    /**
     * Stop and release an individual sound track.
     *
     * @param soundId Unique sound identifier.
     */
    suspend fun stopTrack(soundId: String): Result<Unit>

    /**
     * Adjust volume of a specific sound track with a 30ms smoothing ramp.
     *
     * @param soundId Unique sound identifier.
     * @param volume Target volume clamped to [0.0f, 1.0f].
     */
    fun setTrackVolume(soundId: String, volume: Float)

    /**
     * Mute or unmute an individual sound track.
     *
     * @param soundId Unique sound identifier.
     * @param isMuted True to silence output; false to restore prior volume.
     */
    fun setTrackMuted(soundId: String, isMuted: Boolean)

    /**
     * Adjust global master volume with a 30ms smoothing ramp applied to all active tracks.
     *
     * @param volume Global volume multiplier clamped to [0.0f, 1.0f].
     */
    fun setMasterVolume(volume: Float)

    /**
     * Pause all active sound streams while maintaining mixer track configuration.
     */
    fun pauseAll()

    /**
     * Resume playback of previously paused sound streams.
     */
    fun resumeAll()

    /**
     * Stop and unload all active sound streams, resetting the engine to idle.
     */
    fun stopAll()

    /**
     * Observe individual track playback state changes.
     *
     * @param soundId Unique sound identifier.
     */
    fun getTrackState(soundId: String): Flow<TrackPlaybackState?>

    /**
     * Release all underlying media decoders and hardware audio tracks.
     */
    fun release()
}
