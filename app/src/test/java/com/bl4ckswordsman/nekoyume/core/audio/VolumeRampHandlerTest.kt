package com.bl4ckswordsman.nekoyume.core.audio

import com.bl4ckswordsman.nekoyume.core.audio.ramp.VolumeRampHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

@OptIn(ExperimentalCoroutinesApi::class)
class VolumeRampHandlerTest {

    @Test
    fun rampVolume_interpolatesAcrossStepsOver30ms() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val rampHandler = VolumeRampHandler(
            scope = this,
            dispatcher = testDispatcher,
            rampDurationMs = 30L,
            stepIntervalMs = 5L
        )

        val steps = Collections.synchronizedList(mutableListOf<Float>())
        var completed = false

        rampHandler.rampVolume(
            targetId = "track_1",
            fromVolume = 0.0f,
            toVolume = 1.0f,
            onVolumeStep = { steps.add(it) },
            onComplete = { completed = true }
        )

        testScheduler.runCurrent()
        // Advance 12ms (reaches past 2 steps of 5ms)
        testScheduler.advanceTimeBy(12L)
        testScheduler.runCurrent()

        assertTrue("Should have recorded at least 2 steps", steps.size >= 2)
        assertTrue(steps[0] > 0.0f && steps[0] < 0.5f)

        // Advance past 30ms total
        testScheduler.advanceTimeBy(25L)
        testScheduler.runCurrent()

        assertTrue("Ramp should be marked complete", completed)
        assertEquals(1.0f, steps.last(), 0.001f)
    }

    @Test
    fun rampVolume_rapidScrubbing_cancelsPriorRamps() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val rampHandler = VolumeRampHandler(
            scope = this,
            dispatcher = testDispatcher,
            rampDurationMs = 30L,
            stepIntervalMs = 5L
        )

        val steps = Collections.synchronizedList(mutableListOf<Float>())
        var firstCompleted = false
        var secondCompleted = false

        // Start initial ramp 0.0 -> 0.8
        rampHandler.rampVolume(
            targetId = "track_1",
            fromVolume = 0.0f,
            toVolume = 0.8f,
            onVolumeStep = { steps.add(it) },
            onComplete = { firstCompleted = true }
        )

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(6L)
        testScheduler.runCurrent()

        assertTrue("Should have recorded first step", steps.isNotEmpty())
        val currentVolume = steps.last()

        // Interrupt with new target: 0.2
        rampHandler.rampVolume(
            targetId = "track_1",
            fromVolume = currentVolume,
            toVolume = 0.2f,
            onVolumeStep = { steps.add(it) },
            onComplete = { secondCompleted = true }
        )

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(35L)
        testScheduler.runCurrent()

        assertEquals("First ramp should have been cancelled", false, firstCompleted)
        assertEquals("Second ramp should complete", true, secondCompleted)
        assertEquals(0.2f, steps.last(), 0.001f)
    }

    @Test
    fun rampVolume_zeroDelta_completesImmediately() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val rampHandler = VolumeRampHandler(
            scope = this,
            dispatcher = testDispatcher
        )

        var finalVolume = -1.0f
        var completed = false

        rampHandler.rampVolume(
            targetId = "track_1",
            fromVolume = 0.5f,
            toVolume = 0.5f,
            onVolumeStep = { finalVolume = it },
            onComplete = { completed = true }
        )

        assertEquals(0.5f, finalVolume, 0.001f)
        assertEquals(true, completed)
    }
}
