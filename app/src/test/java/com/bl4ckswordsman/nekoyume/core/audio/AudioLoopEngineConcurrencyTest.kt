package com.bl4ckswordsman.nekoyume.core.audio

import androidx.media3.common.Player
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class AudioLoopEngineConcurrencyTest {

    private class FakeAudioPlayer : Player by FakePlayerStub() {
        var isPlayingInternal = false
        var repeatModeInternal = Player.REPEAT_MODE_OFF
        var volumeInternal = 1.0f
        var isReleased = false
        val listeners = mutableListOf<Player.Listener>()

        override fun play() {
            isPlayingInternal = true
            listeners.forEach { it.onIsPlayingChanged(true) }
            listeners.forEach { it.onPlaybackStateChanged(Player.STATE_READY) }
        }

        override fun pause() {
            isPlayingInternal = false
            listeners.forEach { it.onIsPlayingChanged(false) }
        }

        override fun stop() {
            isPlayingInternal = false
            listeners.forEach { it.onPlaybackStateChanged(Player.STATE_IDLE) }
        }

        override fun release() {
            isReleased = true
            isPlayingInternal = false
        }

        override fun setRepeatMode(repeatMode: Int) {
            repeatModeInternal = repeatMode
        }

        override fun getRepeatMode(): Int = repeatModeInternal

        override fun setVolume(volume: Float) {
            volumeInternal = volume
        }

        override fun getVolume(): Float = volumeInternal

        override fun addListener(listener: Player.Listener) {
            listeners.add(listener)
        }

        override fun removeListener(listener: Player.Listener) {
            listeners.remove(listener)
        }
    }

    private fun createEngine(
        testScope: TestScope,
        createdPlayers: ConcurrentHashMap<String, FakeAudioPlayer> = ConcurrentHashMap()
    ): Media3AudioLoopEngine {
        return Media3AudioLoopEngine(
            scope = testScope,
            dispatcher = StandardTestDispatcher(testScope.testScheduler),
            playerFactory = { soundId, _ ->
                FakeAudioPlayer().also { createdPlayers[soundId] = it }
            }
        )
    }

    @Test
    fun startTrack_allowsUpTo8ConcurrentTracks() = runTest {
        val createdPlayers = ConcurrentHashMap<String, FakeAudioPlayer>()
        val engine = createEngine(this, createdPlayers)

        for (i in 1..8) {
            val result = engine.startTrack("track_$i", "sounds/track_$i.ogg", 0.5f)
            assertTrue("Track $i should start successfully", result.isSuccess)
        }

        val state = engine.mixState.first()
        assertEquals(8, state.activeTracks.size)
        assertEquals(8, createdPlayers.size)
    }

    @Test
    fun startTrack_rejects9thTrackWithMaxTracksExceededException() = runTest {
        val engine = createEngine(this)

        for (i in 1..8) {
            val res = engine.startTrack("track_$i", "sounds/track_$i.ogg")
            assertTrue(res.isSuccess)
        }

        val overflowResult = engine.startTrack("track_9", "sounds/track_9.ogg")
        assertTrue("9th track should fail", overflowResult.isFailure)
        assertTrue(
            "Expected MaxTracksExceededException",
            overflowResult.exceptionOrNull() is MaxTracksExceededException
        )

        assertEquals(8, engine.mixState.first().activeTracks.size)
    }

    @Test
    fun stopTrack_freesSlotForNewTrack() = runTest {
        val engine = createEngine(this)

        for (i in 1..8) {
            engine.startTrack("track_$i", "sounds/track_$i.ogg")
        }

        val stopResult = engine.stopTrack("track_1")
        assertTrue(stopResult.isSuccess)
        assertEquals(7, engine.mixState.first().activeTracks.size)

        // Slot is freed, 9th track can now start
        val newTrackResult = engine.startTrack("track_9", "sounds/track_9.ogg")
        assertTrue(newTrackResult.isSuccess)
        assertEquals(8, engine.mixState.first().activeTracks.size)
        assertNotNull(engine.mixState.first().activeTracks["track_9"])
    }

    @Test
    fun stopAll_releasesAllPlayersAndEmitsIdleState() = runTest {
        val createdPlayers = ConcurrentHashMap<String, FakeAudioPlayer>()
        val engine = createEngine(this, createdPlayers)

        engine.startTrack("rain", "sounds/rain.ogg")
        engine.startTrack("forest", "sounds/forest.ogg")

        assertEquals(2, engine.mixState.first().activeTracks.size)

        engine.stopAll()

        assertEquals(0, engine.mixState.first().activeTracks.size)
        assertTrue(createdPlayers["rain"]?.isReleased == true)
        assertTrue(createdPlayers["forest"]?.isReleased == true)
    }
}
