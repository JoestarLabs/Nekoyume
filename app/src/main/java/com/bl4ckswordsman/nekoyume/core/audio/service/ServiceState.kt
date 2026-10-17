package com.bl4ckswordsman.nekoyume.core.audio.service

/**
 * Operational playback states of the Nekoyume background playback service.
 */
enum class ServicePlaybackState {
    STOPPED,
    PLAYING,
    PAUSED
}

/**
 * Snapshot of the background playback service state.
 *
 * Invariants:
 * - [isForeground] is true whenever [playbackState] == [ServicePlaybackState.PLAYING].
 * - [activeTrackCount] is >= 0.
 * - When [playbackState] == [ServicePlaybackState.PAUSED], [pausedTimestampEpochMs] is non-null.
 * - When [playbackState] != [ServicePlaybackState.PAUSED], [pausedTimestampEpochMs] is null.
 */
data class ServiceState(
    val playbackState: ServicePlaybackState = ServicePlaybackState.STOPPED,
    val isForeground: Boolean = false,
    val activeTrackCount: Int = 0,
    val pausedTimestampEpochMs: Long? = null,
    val hasNotificationPermission: Boolean = true
) {
    init {
        require(activeTrackCount >= 0) { "activeTrackCount must be non-negative: $activeTrackCount" }
        if (playbackState == ServicePlaybackState.PLAYING) {
            require(isForeground) { "Service must be in foreground when state is PLAYING" }
        }
        if (playbackState == ServicePlaybackState.PAUSED) {
            require(pausedTimestampEpochMs != null) { "pausedTimestampEpochMs must not be null when state is PAUSED" }
        } else {
            require(pausedTimestampEpochMs == null) { "pausedTimestampEpochMs must be null when state is not PAUSED" }
        }
    }
}
