# Contract: Audio Loop Engine (`AudioLoopEngine`)

**Feature**: `001-audio-loop-engine`  
**Date**: 2026-10-17  
**Status**: Stable  

---

## 1. Interface Definition

```kotlin
package com.bl4ckswordsman.nekoyume.core.audio

import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Headless multi-stream audio playback and looping engine.
 * Capable of managing up to 8 simultaneous looping audio streams with independent volume,
 * master volume attenuation, seamless gapless looping, and platform audio focus management.
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
     * @param assetPath Relative asset path (e.g. "sounds/rain.ogg").
     * @param initialVolume Starting volume level between 0.0f and 1.0f (defaults to 1.0f).
     * @return Result.success if initiated or Result.failure if concurrency ceiling (8) reached.
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
```

---

## 2. Invariants & Guarantees

1. **Max Concurrency Ceiling**:
   - At most 8 sound tracks can be in `LOADING`, `PLAYING`, or `PAUSED` state simultaneously.
   - Calling `startTrack()` when 8 tracks are already active will return `Result.failure(MaxTracksExceededException)`.

2. **Loop Continuity**:
   - Tracks loop continuously from end-to-beginning without audible silence gaps (>10ms) or DC-offset click artifacts.

3. **Smooth Volume Attenuation**:
   - All volume modifications (`setTrackVolume`, `setMasterVolume`, and audio focus ducking) complete over a 30ms smoothing ramp, preventing digital zipper noise.

4. **Audio Focus Etiquette**:
   - Permanent focus loss pauses all tracks and retains configuration.
   - Transient focus loss ducks volume by ≥60% (`duckMultiplier = 0.3f`) and automatically restores when focus returns.
   - Becoming noisy (headphone disconnect) automatically triggers `pauseAll()`.
