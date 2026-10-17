package com.bl4ckswordsman.nekoyume.core.audio

import androidx.media3.common.Player
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
class AudioMixerVolumeScalingTest {

    private class VolumeTrackingPlayer : Player by FakePlayerStub() {
        var hardwareVolume: Float = 1.0f

        override fun setVolume(volume: Float) {
            hardwareVolume = volume
        }

        override fun getVolume(): Float = hardwareVolume
    }

    @Test
    fun setMasterVolume_scalesAllActiveTrackVolumesProportionally() = runTest {
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

        // Track 1 at 0.5, Track 2 at 1.0, initial master at 1.0
        engine.startTrack("track_1", "sounds/track_1.ogg", 0.5f)
        engine.startTrack("track_2", "sounds/track_2.ogg", 1.0f)

        testScheduler.advanceTimeBy(35L)
        assertEquals(0.5f, players["track_1"]?.hardwareVolume ?: 0f, 0.01f)
        assertEquals(1.0f, players["track_2"]?.hardwareVolume ?: 0f, 0.01f)

        // Set master volume to 0.5
        engine.setMasterVolume(0.5f)
        testScheduler.advanceTimeBy(35L)

        // Track 1 should scale to 0.5 * 0.5 = 0.25
        // Track 2 should scale to 1.0 * 0.5 = 0.50
        assertEquals(0.25f, players["track_1"]?.hardwareVolume ?: 0f, 0.01f)
        assertEquals(0.50f, players["track_2"]?.hardwareVolume ?: 0f, 0.01f)
    }

    @Test
    fun setTrackMuted_silencesHardwareWithoutModifyingStoredVolume() = runTest {
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

        engine.startTrack("rain", "sounds/rain.ogg", 0.8f)
        testScheduler.advanceTimeBy(35L)

        // Mute track
        engine.setTrackMuted("rain", true)
        testScheduler.advanceTimeBy(35L)

        assertEquals(0.0f, players["rain"]?.hardwareVolume ?: -1f, 0.001f)
        assertEquals(0.8f, engine.mixState.first().activeTracks["rain"]?.volume ?: -1f, 0.001f)
        assertTrue(engine.mixState.first().activeTracks["rain"]?.isMuted == true)

        // Unmute track
        engine.setTrackMuted("rain", false)
        testScheduler.advanceTimeBy(35L)

        assertEquals(0.8f, players["rain"]?.hardwareVolume ?: -1f, 0.01f)
        assertEquals(false, engine.mixState.first().activeTracks["rain"]?.isMuted)
    }
}
