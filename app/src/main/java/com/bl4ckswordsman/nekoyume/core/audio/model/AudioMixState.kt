package com.bl4ckswordsman.nekoyume.core.audio.model

/**
 * Aggregate runtime state representing the current mix across all audio tracks.
 * Serves as the Single Source of Truth for engine playback state.
 *
 * @property masterVolume Global volume multiplier clamped to [0.0f, 1.0f].
 * @property isMasterPaused True if playback is paused globally across all tracks.
 * @property activeTracks Map of soundId to individual [TrackPlaybackState]. Max 8 active entries.
 * @property audioFocusState Current platform audio focus state.
 */
data class AudioMixState(
    val masterVolume: Float = 1.0f,
    val isMasterPaused: Boolean = false,
    val activeTracks: Map<String, TrackPlaybackState> = emptyMap(),
    val audioFocusState: AudioFocusState = AudioFocusState.NONE
) {
    init {
        require(masterVolume in 0.0f..1.0f) {
            "masterVolume must be between 0.0 and 1.0, was: $masterVolume"
        }
        require(activeTracks.size <= MAX_CONCURRENT_TRACKS) {
            "Active tracks cannot exceed $MAX_CONCURRENT_TRACKS, was: ${activeTracks.size}"
        }
    }

    /**
     * Count of tracks actively in [TrackPlaybackStatus.PLAYING] state.
     */
    val playingTrackCount: Int
        get() = activeTracks.values.count { it.status == TrackPlaybackStatus.PLAYING }

    /**
     * True if at least one track is currently registered in the active mix.
     */
    val hasActiveTracks: Boolean
        get() = activeTracks.isNotEmpty()

    companion object {
        /**
         * Hard ceiling for simultaneous active audio streams.
         */
        const val MAX_CONCURRENT_TRACKS: Int = 8
    }
}
