package com.bl4ckswordsman.nekoyume.core.audio.service

import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceLifecycleRecoveryTest {

    @Test
    fun onTaskRemoved_sustainsPlayingAudioAndDismissesPausedAudio() {
        var stopSelfCalled = false

        // When actively playing, should NOT stop service
        ServiceLifecycleHandler.handleTaskRemoved(
            isPlaying = true,
            onStopService = { stopSelfCalled = true }
        )
        assertFalse("Service must NOT be stopped when playing on task removed", stopSelfCalled)

        // When paused/stopped, SHOULD stop service
        ServiceLifecycleHandler.handleTaskRemoved(
            isPlaying = false,
            onStopService = { stopSelfCalled = true }
        )
        assertTrue("Service must be stopped when not playing on task removed", stopSelfCalled)
    }

    @Test
    fun processRevival_restoresSavedMixStateInPausedCondition() {
        val originalMix = AudioMixState(
            isMasterPaused = false,
            masterVolume = 0.85f,
            activeTracks = mapOf(
                "rain" to TrackPlaybackState("rain", TrackPlaybackStatus.PLAYING, 0.75f, isMuted = false),
                "fire" to TrackPlaybackState("fire", TrackPlaybackStatus.PLAYING, 0.40f, isMuted = true)
            )
        )

        // Serialize mix state
        val serialized = ServiceStateRestorer.serialize(originalMix)
        assertNotNull(serialized)
        assertTrue(serialized.contains("rain"))
        assertTrue(serialized.contains("fire"))

        // Deserialize on process revival (FR-014)
        val restored = ServiceStateRestorer.deserialize(serialized)
        assertNotNull(restored)

        // Must restore in PAUSED condition
        assertTrue("Restored mix must have isMasterPaused = true", restored!!.isMasterPaused)
        assertEquals(0.85f, restored.masterVolume, 0.001f)
        assertEquals(2, restored.activeTracks.size)

        val rainTrack = restored.activeTracks["rain"]
        assertNotNull(rainTrack)
        assertEquals(TrackPlaybackStatus.PAUSED, rainTrack!!.status)
        assertEquals(0.75f, rainTrack.volume, 0.001f)
        assertFalse(rainTrack.isMuted)

        val fireTrack = restored.activeTracks["fire"]
        assertNotNull(fireTrack)
        assertEquals(TrackPlaybackStatus.PAUSED, fireTrack!!.status)
        assertEquals(0.40f, fireTrack.volume, 0.001f)
        assertTrue(fireTrack.isMuted)

        // Verified FR-014: restored.isPlaying must be false
        assertFalse(restored.isPlaying)
    }

    @Test
    fun processRevival_handlesEmptyOrCorruptStateGracefully() {
        val emptyRestored = ServiceStateRestorer.deserialize("")
        assertNull(emptyRestored)

        val corruptRestored = ServiceStateRestorer.deserialize("invalid json string")
        assertNull(corruptRestored)
    }
}
