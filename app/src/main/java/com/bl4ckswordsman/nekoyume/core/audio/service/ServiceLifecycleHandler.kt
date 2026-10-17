package com.bl4ckswordsman.nekoyume.core.audio.service

/**
 * Handles Android task lifecycle events including Recent app swiping (FR-013).
 */
object ServiceLifecycleHandler {

    /**
     * Handles app task removal from the Android Recent tasks list.
     *
     * In accordance with FR-013:
     * - If audio is actively playing, the foreground service MUST continue playing uninterrupted.
     * - If audio is paused or stopped, the service is dismissed and resources released.
     */
    fun handleTaskRemoved(isPlaying: Boolean, onStopService: () -> Unit) {
        if (!isPlaying) {
            onStopService()
        }
    }
}
