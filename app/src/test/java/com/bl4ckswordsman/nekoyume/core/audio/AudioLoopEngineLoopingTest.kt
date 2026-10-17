package com.bl4ckswordsman.nekoyume.core.audio

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class AudioLoopEngineLoopingTest {

    private class MockLoopingPlayer : Player by FakePlayerStub() {
        var repeatModeConfigured: Int = Player.REPEAT_MODE_OFF
        var mediaItemConfigured: MediaItem? = null
        var isPlayingInternal: Boolean = false
        var prepareCalled: Boolean = false
        val listeners = mutableListOf<Player.Listener>()

        override fun setRepeatMode(repeatMode: Int) {
            repeatModeConfigured = repeatMode
        }

        override fun getRepeatMode(): Int = repeatModeConfigured

        override fun setMediaItem(mediaItem: MediaItem) {
            mediaItemConfigured = mediaItem
        }

        override fun prepare() {
            prepareCalled = true
        }

        override fun play() {
            isPlayingInternal = true
            listeners.forEach {
                it.onPlaybackStateChanged(Player.STATE_READY)
                it.onIsPlayingChanged(true)
            }
        }

        override fun pause() {
            isPlayingInternal = false
            listeners.forEach {
                it.onIsPlayingChanged(false)
            }
        }

        override fun addListener(listener: Player.Listener) {
            listeners.add(listener)
        }
    }

    private fun createEngine(
        testScope: TestScope,
        players: ConcurrentHashMap<String, MockLoopingPlayer>
    ): Media3AudioLoopEngine {
        return Media3AudioLoopEngine(
            scope = testScope,
            dispatcher = StandardTestDispatcher(testScope.testScheduler),
            playerFactory = { soundId, _ ->
                MockLoopingPlayer().also { players[soundId] = it }
            }
        )
    }

    @Test
    fun startTrack_configuresRepeatModeOneAndPreparesPlayer() = runTest {
        val players = ConcurrentHashMap<String, MockLoopingPlayer>()
        val engine = createEngine(this, players)

        val result = engine.startTrack("rain", "sounds/rain.ogg", 0.8f)
        assertTrue(result.isSuccess)

        val player = players["rain"]
        assertEquals(
            "Track must be configured with REPEAT_MODE_ONE for continuous gapless repetition",
            Player.REPEAT_MODE_ONE,
            player?.repeatModeConfigured
        )
        assertTrue("prepare() must be called on player", player?.prepareCalled == true)
        assertTrue("play() must be called on player", player?.isPlayingInternal == true)

        val trackState = engine.mixState.first().activeTracks["rain"]
        assertEquals(TrackPlaybackStatus.PLAYING, trackState?.status)
    }

    @Test
    fun pauseAll_pausesAllActivePlayersWithoutLosingConfiguration() = runTest {
        val players = ConcurrentHashMap<String, MockLoopingPlayer>()
        val engine = createEngine(this, players)

        engine.startTrack("rain", "sounds/rain.ogg")
        engine.startTrack("forest", "sounds/forest.ogg")

        engine.pauseAll()

        assertTrue(engine.mixState.first().isMasterPaused)
        assertEquals(TrackPlaybackStatus.PAUSED, engine.mixState.first().activeTracks["rain"]?.status)
        assertEquals(TrackPlaybackStatus.PAUSED, engine.mixState.first().activeTracks["forest"]?.status)
        assertEquals(false, players["rain"]?.isPlayingInternal)
        assertEquals(false, players["forest"]?.isPlayingInternal)

        engine.resumeAll()

        assertEquals(false, engine.mixState.first().isMasterPaused)
        assertEquals(TrackPlaybackStatus.PLAYING, engine.mixState.first().activeTracks["rain"]?.status)
        assertEquals(TrackPlaybackStatus.PLAYING, engine.mixState.first().activeTracks["forest"]?.status)
    }
}
