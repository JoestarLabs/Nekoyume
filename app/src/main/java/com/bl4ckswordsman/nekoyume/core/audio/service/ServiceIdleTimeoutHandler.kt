package com.bl4ckswordsman.nekoyume.core.audio.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles automatic service auto-shutdown after 15 minutes in the paused state (FR-016, SC-004).
 */
class ServiceIdleTimeoutHandler(
    private val scope: CoroutineScope,
    private val timeoutMs: Long = DEFAULT_IDLE_TIMEOUT_MS,
    private val onTimeout: () -> Unit
) {
    private var job: Job? = null

    val isTimerActive: Boolean
        get() = job?.isActive == true

    /**
     * Updates timeout timer based on current playback and active tracks state.
     */
    fun onPlaybackStateChanged(isPlaying: Boolean, hasActiveTracks: Boolean) {
        if (isPlaying) {
            cancel()
        } else if (hasActiveTracks) {
            start()
        } else {
            cancel()
        }
    }

    /**
     * Starts the idle countdown timer.
     */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            delay(timeoutMs)
            job = null
            onTimeout()
        }
    }

    /**
     * Cancels any pending countdown timer.
     */
    fun cancel() {
        job?.cancel()
        job = null
    }

    companion object {
        const val DEFAULT_IDLE_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
    }
}
