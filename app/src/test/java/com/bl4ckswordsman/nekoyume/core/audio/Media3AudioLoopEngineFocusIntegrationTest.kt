package com.bl4ckswordsman.nekoyume.core.audio

import androidx.media3.common.Player
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioFocusState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class Media3AudioLoopEngineFocusIntegrationTest {

    private class VolumeTrackingPlayer : Player by FakePlayerStub() {
        var hardwareVolume: Float = 1.0f
        var isPlayingInternal: Boolean = false

        override fun play() {
            isPlayingInternal = true
        }

        override fun pause() {
            isPlayingInternal = false
        }

        override fun setVolume(volume: Float) {
            hardwareVolume = volume
        }

        override fun getVolume(): Float = hardwareVolume
    }

    @Test
    fun onFocusDucked_smoothlyAttenuatesHardwareVolumeByAtLeast60Percent() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val players = ConcurrentHashMap<String, VolumeTrackingPlayer>()

        val engine = Media3AudioLoopEngine(
            scope = testScope,
            dispatcher = testDispatcher,
            playerFactory = { soundId, _ ->
                VolumeTrackingPlayer().also { players[soundId] = it }
            }
        )

        engine.startTrack("rain", "sounds/rain.ogg", 1.0f)
        testScheduler.advanceTimeBy(35L)
        assertEquals(1.0f, players["rain"]?.hardwareVolume ?: 0f, 0.01f)

        // Simulate transient focus loss (e.g. navigation prompt or notification)
        engine.onFocusDucked()
        testScheduler.advanceTimeBy(35L)

        assertEquals(AudioFocusState.DUCKED, engine.mixState.first().audioFocusState)
        // 1.0 * 1.0 * 0.3 = 0.3 (70% attenuation)
        assertEquals(0.3f, players["rain"]?.hardwareVolume ?: 0f, 0.01f)

        // Focus regained
        engine.onFocusGained()
        testScheduler.advanceTimeBy(35L)

        assertEquals(AudioFocusState.ACQUIRED, engine.mixState.first().audioFocusState)
        assertEquals(1.0f, players["rain"]?.hardwareVolume ?: 0f, 0.01f)
    }

    @Test
    fun onFocusLost_pausesAllActiveTracks() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val players = ConcurrentHashMap<String, VolumeTrackingPlayer>()

        val engine = Media3AudioLoopEngine(
            scope = testScope,
            dispatcher = testDispatcher,
            playerFactory = { soundId, _ ->
                VolumeTrackingPlayer().also { players[soundId] = it }
            }
        )

        engine.startTrack("rain", "sounds/rain.ogg", 1.0f)
        engine.startTrack("forest", "sounds/forest.ogg", 1.0f)
        testScheduler.advanceTimeBy(35L)

        // Permanent focus loss (e.g. incoming call)
        engine.onFocusLost()

        assertEquals(AudioFocusState.LOST, engine.mixState.first().audioFocusState)
        assertTrue(engine.mixState.first().isMasterPaused)
        assertEquals(TrackPlaybackStatus.PAUSED, engine.mixState.first().activeTracks["rain"]?.status)
        assertEquals(TrackPlaybackStatus.PAUSED, engine.mixState.first().activeTracks["forest"]?.status)
    }

    @Test
    fun onBecomingNoisy_pausesPlaybackImmediately() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val players = ConcurrentHashMap<String, VolumeTrackingPlayer>()

        val engine = Media3AudioLoopEngine(
            scope = testScope,
            dispatcher = testDispatcher,
            playerFactory = { soundId, _ ->
                VolumeTrackingPlayer().also { players[soundId] = it }
            }
        )

        engine.startTrack("rain", "sounds/rain.ogg", 1.0f)
        testScheduler.advanceTimeBy(35L)

        // Headphone unplugged
        engine.onBecomingNoisy()

        assertTrue(engine.mixState.first().isMasterPaused)
        assertEquals(false, players["rain"]?.isPlayingInternal)
    }
}
