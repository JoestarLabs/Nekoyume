package com.bl4ckswordsman.nekoyume.core.audio.service

import android.content.Context
import androidx.media3.session.MediaSessionService
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class NekoyumePlaybackServiceTest {

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
    fun playbackService_hasRequiredAnnotationsAndInheritance() {
        val serviceClass = NekoyumePlaybackService::class.java
        assertTrue(
            "Service must extend MediaSessionService",
            MediaSessionService::class.java.isAssignableFrom(serviceClass)
        )
        assertTrue(
            "Service must inherit from Hilt generated entry point superclass",
            serviceClass.superclass?.name?.contains("Hilt_") == true ||
                dagger.hilt.internal.GeneratedComponentManager::class.java.isAssignableFrom(serviceClass) ||
                dagger.hilt.internal.GeneratedComponentManagerHolder::class.java.isAssignableFrom(serviceClass)
        )
    }

    @Test
    fun controller_initialState_isStopped() {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)
        testScope.advanceUntilIdle()

        val state = controller.serviceState.value
        assertEquals(ServicePlaybackState.STOPPED, state.playbackState)
        assertEquals(false, state.isForeground)
        assertEquals(0, state.activeTrackCount)
        assertNull(state.pausedTimestampEpochMs)
    }

    @Test
    fun controller_playingState_promotesToForeground() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)

        fakeEngine.emitMixState(
            AudioMixState(
                isMasterPaused = false,
                activeTracks = mapOf(
                    "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PLAYING, 0.8f),
                    "wind" to TrackPlaybackState("wind", TrackPlaybackStatus.PLAYING, 0.5f)
                )
            )
        )
        testScope.advanceUntilIdle()

        val state = controller.serviceState.value
        assertEquals(ServicePlaybackState.PLAYING, state.playbackState)
        assertTrue("Service must be promoted to foreground when playing", state.isForeground)
        assertEquals(2, state.activeTrackCount)
        assertNull(state.pausedTimestampEpochMs)
    }

    @Test
    fun controller_pausedState_demotesForegroundAndSetsTimestamp() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)

        fakeEngine.emitMixState(
            AudioMixState(
                isMasterPaused = true,
                activeTracks = mapOf(
                    "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PAUSED, 0.8f)
                )
            )
        )
        testScope.advanceUntilIdle()

        val state = controller.serviceState.value
        assertEquals(ServicePlaybackState.PAUSED, state.playbackState)
        assertEquals(false, state.isForeground)
        assertNotNull("Paused timestamp must be set", state.pausedTimestampEpochMs)
    }

    @Test
    fun controller_playCommand_forwardsResumeAll() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)
        controller.play()

        assertTrue(fakeEngine.resumeAllCalled)
    }

    @Test
    fun controller_pauseCommand_forwardsPauseAll() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)
        controller.pause()

        assertTrue(fakeEngine.pauseAllCalled)
    }

    @Test
    fun controller_stopCommand_forwardsStopAll() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)
        controller.stop()

        assertTrue(fakeEngine.stopAllCalled)
    }

    @Test
    fun controller_volumeControls_forwardToEngine() = runTest {
        val controller = PlaybackServiceControllerImpl(fakeContext, fakeEngine, testScope)

        controller.setMasterVolume(0.75f)
        assertEquals(0.75f, fakeEngine.lastMasterVolumeSet)

        controller.setTrackVolume("rain", 0.45f)
        assertEquals("rain" to 0.45f, fakeEngine.lastTrackVolumeSet)
    }
}
