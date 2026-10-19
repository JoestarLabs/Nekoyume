package com.bl4ckswordsman.nekoyume.core.audio.service

import androidx.media3.common.Player
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class MultiStreamPlayerAdapterTest {

    private lateinit var fakeEngine: FakeAudioLoopEngine
    private lateinit var dummyPlayer: Player
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        fakeEngine = FakeAudioLoopEngine()
        dummyPlayer = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Float::class.javaPrimitiveType -> 0f
                else -> null
            }
        } as Player
    }

    @Test
    fun adapter_playCommand_forwardsResumeAll() {
        val adapter = MultiStreamPlayerAdapter(dummyPlayer, fakeEngine, testScope)
        adapter.play()
        assertTrue(fakeEngine.resumeAllCalled)
    }

    @Test
    fun adapter_pauseCommand_forwardsPauseAll() {
        val adapter = MultiStreamPlayerAdapter(dummyPlayer, fakeEngine, testScope)
        adapter.pause()
        assertTrue(fakeEngine.pauseAllCalled)
    }

    @Test
    fun adapter_stopCommand_forwardsStopAll() {
        val adapter = MultiStreamPlayerAdapter(dummyPlayer, fakeEngine, testScope)
        adapter.stop()
        assertTrue(fakeEngine.stopAllCalled)
    }

    @Test
    fun adapter_commandsAvailable_includesTransportControls() {
        val adapter = MultiStreamPlayerAdapter(dummyPlayer, fakeEngine, testScope)
        val commands = adapter.availableCommands
        assertNotNull(commands)

        assertTrue(adapter.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
        assertTrue(adapter.isCommandAvailable(Player.COMMAND_STOP))
        assertTrue(adapter.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM))
        assertTrue(adapter.isCommandAvailable(Player.COMMAND_GET_METADATA))
        assertFalse(adapter.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
    }

    @Test
    fun adapter_mediaMetadata_formatsTrackCount() = runTest {
        val adapter = MultiStreamPlayerAdapter(dummyPlayer, fakeEngine, testScope)

        // 0 tracks
        val metadataZero = adapter.mediaMetadata
        assertEquals("Nekoyume", metadataZero.title.toString())
        assertEquals("Ready", metadataZero.artist.toString())

        // 1 track
        fakeEngine.emitMixState(
            AudioMixState(
                isMasterPaused = false,
                activeTracks = mapOf(
                    "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PLAYING, 0.8f)
                )
            )
        )
        testScope.advanceUntilIdle()

        val metadataOne = adapter.mediaMetadata
        assertEquals("Nekoyume Soundscape", metadataOne.title.toString())
        assertEquals("Rain", metadataOne.displayTitle.toString())
        assertEquals("Rain", metadataOne.subtitle.toString())
        assertEquals("1 sound playing", metadataOne.artist.toString())

        // 3 tracks
        fakeEngine.emitMixState(
            AudioMixState(
                isMasterPaused = false,
                activeTracks = mapOf(
                    "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PLAYING, 0.8f),
                    "wind" to TrackPlaybackState("wind", TrackPlaybackStatus.PLAYING, 0.5f),
                    "fire" to TrackPlaybackState("fire", TrackPlaybackStatus.PLAYING, 0.6f)
                )
            )
        )
        testScope.advanceUntilIdle()

        val metadataThree = adapter.mediaMetadata
        assertEquals("Nekoyume Soundscape", metadataThree.title.toString())
        assertEquals("Rain · Wind · Fire", metadataThree.displayTitle.toString())
        assertEquals("Rain · Wind · Fire", metadataThree.subtitle.toString())
        assertEquals("3 sounds playing", metadataThree.artist.toString())

        val mediaItem = adapter.currentMediaItem
        assertNotNull(mediaItem)
        assertEquals("nekoyume_multi_stream", mediaItem?.mediaId)
    }

    @Test
    fun adapter_listenerNotifications_onMixStateChange() = runTest {
        val adapter = MultiStreamPlayerAdapter(dummyPlayer, fakeEngine, testScope)
        var isPlayingReported: Boolean? = null
        var playbackStateReported: Int? = null

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingReported = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                playbackStateReported = playbackState
            }
        }

        adapter.addListener(listener)

        fakeEngine.emitMixState(
            AudioMixState(
                isMasterPaused = false,
                activeTracks = mapOf(
                    "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PLAYING, 0.8f)
                )
            )
        )
        testScope.advanceUntilIdle()

        assertEquals(true, isPlayingReported)
        assertEquals(Player.STATE_READY, playbackStateReported)

        adapter.removeListener(listener)
    }
}
