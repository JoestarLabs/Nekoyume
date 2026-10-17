package com.bl4ckswordsman.nekoyume.core.audio.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceIdleTimeoutTest {

    private var timeoutTriggered = false

    @Before
    fun setUp() {
        timeoutTriggered = false
    }

    @Test
    fun timeout_triggersAfter15MinutesPaused() = runTest {
        val handler = ServiceIdleTimeoutHandler(
            scope = this,
            timeoutMs = 15 * 60 * 1000L,
            onTimeout = { timeoutTriggered = true }
        )

        // Paused with active tracks -> start timer
        handler.onPlaybackStateChanged(isPlaying = false, hasActiveTracks = true)
        runCurrent()
        assertTrue(handler.isTimerActive)
        assertFalse(timeoutTriggered)

        // Advance 14 minutes -> should not trigger yet
        advanceTimeBy(14 * 60 * 1000L)
        runCurrent()
        assertFalse(timeoutTriggered)
        assertTrue(handler.isTimerActive)

        // Advance remaining 1 minute -> triggers timeout
        advanceTimeBy(1 * 60 * 1000L)
        runCurrent()
        assertTrue(timeoutTriggered)
        assertFalse(handler.isTimerActive)
    }

    @Test
    fun timeout_cancelsWhenPlaybackResumes() = runTest {
        val handler = ServiceIdleTimeoutHandler(
            scope = this,
            timeoutMs = 15 * 60 * 1000L,
            onTimeout = { timeoutTriggered = true }
        )

        // Paused with active tracks
        handler.onPlaybackStateChanged(isPlaying = false, hasActiveTracks = true)
        runCurrent()
        assertTrue(handler.isTimerActive)

        // Advance 10 minutes
        advanceTimeBy(10 * 60 * 1000L)
        runCurrent()
        assertFalse(timeoutTriggered)

        // User resumes playback -> timer cancels
        handler.onPlaybackStateChanged(isPlaying = true, hasActiveTracks = true)
        runCurrent()
        assertFalse(handler.isTimerActive)

        // Advance past 15 minutes -> must not trigger
        advanceTimeBy(10 * 60 * 1000L)
        runCurrent()
        assertFalse(timeoutTriggered)
    }

    @Test
    fun timeout_cancelsWhenPlaybackStopped() = runTest {
        val handler = ServiceIdleTimeoutHandler(
            scope = this,
            timeoutMs = 15 * 60 * 1000L,
            onTimeout = { timeoutTriggered = true }
        )

        // Paused with active tracks
        handler.onPlaybackStateChanged(isPlaying = false, hasActiveTracks = true)
        runCurrent()
        assertTrue(handler.isTimerActive)

        // User stops all tracks
        handler.onPlaybackStateChanged(isPlaying = false, hasActiveTracks = false)
        runCurrent()
        assertFalse(handler.isTimerActive)

        advanceTimeBy(20 * 60 * 1000L)
        runCurrent()
        assertFalse(timeoutTriggered)
    }
}
