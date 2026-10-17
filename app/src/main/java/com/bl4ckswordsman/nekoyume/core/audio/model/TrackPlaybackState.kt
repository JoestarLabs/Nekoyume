package com.bl4ckswordsman.nekoyume.core.audio.model

/**
 * Operational runtime status of an individual audio stream.
 */
enum class TrackPlaybackStatus {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    ERROR
}

/**
 * Platform audio focus states indicating system interruption status.
 */
enum class AudioFocusState {
    NONE,
    ACQUIRED,
    DUCKED,
    LOST
}

/**
 * Immutable runtime state snapshot of an individual sound track.
 *
 * @property soundId Unique sound identifier.
 * @property status Current playback status.
 * @property volume Track-specific volume level clamped to [0.0f, 1.0f].
 * @property isMuted True if this track is silenced while keeping its player active.
 * @property errorMessage Descriptive message if status is [TrackPlaybackStatus.ERROR].
 */
data class TrackPlaybackState(
    val soundId: String,
    val status: TrackPlaybackStatus = TrackPlaybackStatus.IDLE,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val errorMessage: String? = null
) {
    init {
        require(soundId.isNotBlank()) { "soundId cannot be blank" }
        require(volume in 0.0f..1.0f) { "volume must be between 0.0 and 1.0, was: $volume" }
    }

    /**
     * True if this track is actively playing, unmuted, and has non-zero volume.
     */
    val isAudible: Boolean
        get() = status == TrackPlaybackStatus.PLAYING && !isMuted && volume > 0f
}
