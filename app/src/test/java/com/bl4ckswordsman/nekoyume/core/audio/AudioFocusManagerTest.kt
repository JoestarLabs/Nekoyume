package com.bl4ckswordsman.nekoyume.core.audio

import android.media.AudioManager
import com.bl4ckswordsman.nekoyume.core.audio.focus.AudioFocusChangeListener
import com.bl4ckswordsman.nekoyume.core.audio.focus.NekoyumeAudioFocusManager
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioFocusState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusManagerTest {

    private class TestFocusListener : AudioFocusChangeListener {
        var gainedCalled = false
        var duckedCalled = false
        var lostCalled = false
        var noisyCalled = false

        override fun onFocusGained() {
            gainedCalled = true
        }

        override fun onFocusDucked() {
            duckedCalled = true
        }

        override fun onFocusLost() {
            lostCalled = true
        }

        override fun onBecomingNoisy() {
            noisyCalled = true
        }

        fun reset() {
            gainedCalled = false
            duckedCalled = false
            lostCalled = false
            noisyCalled = false
        }
    }

    @Test
    fun handleFocusChange_transientCanDuck_transitionsToDucked() {
        val listener = TestFocusListener()
        val manager = NekoyumeAudioFocusManager(listener = listener)

        manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        assertEquals(AudioFocusState.DUCKED, manager.focusState.value)
        assertTrue(listener.duckedCalled)
    }

    @Test
    fun handleFocusChange_gain_transitionsToAcquired() {
        val listener = TestFocusListener()
        val manager = NekoyumeAudioFocusManager(listener = listener)

        manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        listener.reset()

        manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(AudioFocusState.ACQUIRED, manager.focusState.value)
        assertTrue(listener.gainedCalled)
    }

    @Test
    fun handleFocusChange_permanentLoss_transitionsToLost() {
        val listener = TestFocusListener()
        val manager = NekoyumeAudioFocusManager(listener = listener)

        manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        assertEquals(AudioFocusState.LOST, manager.focusState.value)
        assertTrue(listener.lostCalled)
    }

    @Test
    fun handleBecomingNoisy_triggersListenerCallback() {
        val listener = TestFocusListener()
        val manager = NekoyumeAudioFocusManager(listener = listener)

        manager.onBecomingNoisy()

        assertTrue(listener.noisyCalled)
    }
}
