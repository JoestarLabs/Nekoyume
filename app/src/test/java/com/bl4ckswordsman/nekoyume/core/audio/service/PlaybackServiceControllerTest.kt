package com.bl4ckswordsman.nekoyume.core.audio.service

import android.content.Context
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackServiceControllerTest {

    private lateinit var fakeEngine: FakeAudioLoopEngine
    private lateinit var fakeContext: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeContext : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun startForegroundService(service: android.content.Intent?): android.content.ComponentName? = null
        override fun startService(service: android.content.Intent?): android.content.ComponentName? = null
        override fun stopService(name: android.content.Intent?): Boolean = true
    }

    @Before
    fun setUp() {
        fakeEngine = FakeAudioLoopEngine()
        fakeContext = FakeContext()
    }

    @Test
    fun controller_synchronizesStateFlowWithEngineMixState() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)
        testScope.advanceUntilIdle()

        // Initially STOPPED
        assertEquals(ServicePlaybackState.STOPPED, controller.serviceState.value.playbackState)
        assertFalse(controller.serviceState.value.isForeground)

        // Switch to PLAYING
        fakeEngine.emitMixState(
            AudioMixState(
                isMasterPaused = false,
                activeTracks = mapOf(
                    "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PLAYING, 0.7f),
                    "birds" to TrackPlaybackState("birds", TrackPlaybackStatus.PLAYING, 0.4f)
                )
            )
        )
        testScope.advanceUntilIdle()

        assertEquals(ServicePlaybackState.PLAYING, controller.serviceState.value.playbackState)
        assertTrue(controller.serviceState.value.isForeground)
        assertEquals(2, controller.serviceState.value.activeTrackCount)
        assertNull(controller.serviceState.value.pausedTimestampEpochMs)

        // Switch to PAUSED
        fakeEngine.emitMixState(
            AudioMixState(
                isMasterPaused = true,
                activeTracks = mapOf(
                    "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PAUSED, 0.7f),
                    "birds" to TrackPlaybackState("birds", TrackPlaybackStatus.PAUSED, 0.4f)
                )
            )
        )
        testScope.advanceUntilIdle()

        assertEquals(ServicePlaybackState.PAUSED, controller.serviceState.value.playbackState)
        assertFalse(controller.serviceState.value.isForeground)
        assertEquals(2, controller.serviceState.value.activeTrackCount)
        assertNotNull(controller.serviceState.value.pausedTimestampEpochMs)

        // Switch to STOPPED (no active tracks)
        fakeEngine.emitMixState(
            AudioMixState(
                isMasterPaused = false,
                activeTracks = emptyMap()
            )
        )
        testScope.advanceUntilIdle()

        assertEquals(ServicePlaybackState.STOPPED, controller.serviceState.value.playbackState)
        assertFalse(controller.serviceState.value.isForeground)
        assertEquals(0, controller.serviceState.value.activeTrackCount)
        assertNull(controller.serviceState.value.pausedTimestampEpochMs)
    }

    @Test
    fun controller_forwardsTransportCommands() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)

        controller.play()
        assertTrue(fakeEngine.resumeAllCalled)

        controller.pause()
        assertTrue(fakeEngine.pauseAllCalled)

        controller.stop()
        assertTrue(fakeEngine.stopAllCalled)
    }

    @Test
    fun controller_forwardsVolumeAdjustments() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)

        controller.setMasterVolume(0.85f)
        assertEquals(0.85f, fakeEngine.lastMasterVolumeSet)

        controller.setTrackVolume("ocean", 0.65f)
        assertEquals("ocean" to 0.65f, fakeEngine.lastTrackVolumeSet)
    }
}
