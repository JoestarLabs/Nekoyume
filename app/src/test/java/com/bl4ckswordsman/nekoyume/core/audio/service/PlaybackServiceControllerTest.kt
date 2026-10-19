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

    private class FakeMixStatePersistence(
        private var savedState: AudioMixState? = null
    ) : MixStatePersistence {
        override val savedMixState = kotlinx.coroutines.flow.flowOf(savedState)
        override suspend fun getSavedMixState(): AudioMixState? = savedState
        override suspend fun saveMixState(mixState: AudioMixState) {
            savedState = mixState
        }
    }

    @Test
    fun controller_coldLaunchRestore_restoresSavedMixAsInactiveWithoutAutoPlay() = runTest {
        val savedMix = AudioMixState(
            isMasterPaused = false,
            masterVolume = 0.9f,
            activeTracks = mapOf(
                "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PLAYING, 0.8f),
                "fire" to TrackPlaybackState("fire", TrackPlaybackStatus.PLAYING, 0.5f)
            )
        )
        val fakePersistence = FakeMixStatePersistence(savedMix)

        val controller = PlaybackServiceControllerImpl(
            context = fakeContext,
            audioEngine = fakeEngine,
            mixPersistence = fakePersistence,
            scope = testScope
        )
        testScope.advanceUntilIdle()

        // FR-017 & US1/AC4: Restored mix must display active tracks in inactive (stopped) state
        val restoredMix = controller.mixState.value
        assertEquals(2, restoredMix.activeTracks.size)
        assertTrue("Mix must be paused on cold launch restore", restoredMix.isMasterPaused)
        assertEquals(TrackPlaybackStatus.PAUSED, restoredMix.activeTracks["rain"]?.status)
        assertEquals(TrackPlaybackStatus.PAUSED, restoredMix.activeTracks["fire"]?.status)

        // Playback must NOT auto-start
        assertFalse("Playback must NOT auto-start on cold launch", restoredMix.isPlaying)
        assertEquals(ServicePlaybackState.STOPPED, controller.serviceState.value.playbackState)
        assertFalse(controller.serviceState.value.isForeground)
        assertFalse(fakeEngine.resumeAllCalled)
    }

    @Test
    fun controller_firstLaunch_initializesWithEmptyMix() = runTest {
        val fakePersistence = FakeMixStatePersistence(null)

        val controller = PlaybackServiceControllerImpl(
            context = fakeContext,
            audioEngine = fakeEngine,
            mixPersistence = fakePersistence,
            scope = testScope
        )
        testScope.advanceUntilIdle()

        val initialMix = controller.mixState.value
        assertTrue("First launch mix must be empty", initialMix.activeTracks.isEmpty())
        assertFalse(initialMix.isPlaying)
        assertEquals(ServicePlaybackState.STOPPED, controller.serviceState.value.playbackState)
    }
}
