package com.bl4ckswordsman.nekoyume.core.audio.ramp

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles asynchronous volume ramping over a fixed duration (30ms default)
 * to eliminate digital zipper noise and audible clicks during rapid volume scrubbing.
 */
class VolumeRampHandler(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val rampDurationMs: Long = DEFAULT_RAMP_DURATION_MS,
    private val stepIntervalMs: Long = DEFAULT_STEP_INTERVAL_MS
) {
    private val activeRamps = ConcurrentHashMap<String, Job>()

    /**
     * Smoothly ramps volume from [fromVolume] to [toVolume] for a given [targetId].
     * Any existing in-flight ramp for the same [targetId] is cancelled immediately.
     */
    fun rampVolume(
        targetId: String,
        fromVolume: Float,
        toVolume: Float,
        onVolumeStep: (Float) -> Unit,
        onComplete: (() -> Unit)? = null
    ) {
        // Cancel any active in-flight ramp for this target ID
        activeRamps.remove(targetId)?.cancel()

        if (fromVolume == toVolume || rampDurationMs <= 0L) {
            onVolumeStep(toVolume.coerceIn(0.0f, 1.0f))
            onComplete?.invoke()
            return
        }

        val stepCount = (rampDurationMs / stepIntervalMs).toInt().coerceAtLeast(1)
        val stepDelta = (toVolume - fromVolume) / stepCount

        val job = scope.launch(dispatcher) {
            var currentVolume = fromVolume
            for (i in 1..stepCount) {
                delay(stepIntervalMs)
                currentVolume += stepDelta
                onVolumeStep(currentVolume.coerceIn(0.0f, 1.0f))
            }
            onVolumeStep(toVolume.coerceIn(0.0f, 1.0f))
            activeRamps.remove(targetId)
            onComplete?.invoke()
        }
        activeRamps[targetId] = job
    }

    /**
     * Cancel all in-flight volume ramps across all targets.
     */
    fun cancelAll() {
        activeRamps.values.forEach { it.cancel() }
        activeRamps.clear()
    }

    companion object {
        const val DEFAULT_RAMP_DURATION_MS: Long = 30L
        const val DEFAULT_STEP_INTERVAL_MS: Long = 5L
    }
}
