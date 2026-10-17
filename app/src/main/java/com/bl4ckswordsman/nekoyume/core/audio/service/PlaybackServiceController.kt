package com.bl4ckswordsman.nekoyume.core.audio.service

import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level controller interface mediated by Hilt for observing
 * background audio service state and issuing transport commands.
 */
interface PlaybackServiceController {
    /**
     * Reactive stream of aggregate background service state.
     */
    val serviceState: StateFlow<ServiceState>

    /**
     * Reactive stream of current multi-stream audio mix state.
     */
    val mixState: StateFlow<AudioMixState>

    /**
     * Starts or resumes playback across all active sound tracks.
     */
    fun play()

    /**
     * Smoothly pauses playback across all active sound tracks.
     */
    fun pause()

    /**
     * Stops playback and demotes foreground service.
     */
    fun stop()

    /**
     * Toggles a specific sound track by ID.
     */
    fun toggleTrack(trackId: String)

    /**
     * Sets volume for an individual sound track [0.0f..1.0f].
     */
    fun setTrackVolume(trackId: String, volume: Float)

    /**
     * Sets master attenuation volume [0.0f..1.0f].
     */
    fun setMasterVolume(volume: Float)
}
