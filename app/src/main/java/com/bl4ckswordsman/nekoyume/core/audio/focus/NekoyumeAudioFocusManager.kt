package com.bl4ckswordsman.nekoyume.core.audio.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioFocusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Callback listener for platform audio focus transitions and audio route disruptions.
 */
interface AudioFocusChangeListener {
    fun onFocusGained()
    fun onFocusDucked()
    fun onFocusLost()
    fun onBecomingNoisy()
}

/**
 * Manages platform audio focus acquisition, ducking, and noisy route disconnections
 * adhering to modern Android media guidelines.
 */
class NekoyumeAudioFocusManager(
    private val context: Context? = null,
    private val audioManager: AudioManager? = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager,
    private val listener: AudioFocusChangeListener
) : AudioManager.OnAudioFocusChangeListener {

    private val _focusState = MutableStateFlow(AudioFocusState.NONE)
    val focusState: StateFlow<AudioFocusState> = _focusState.asStateFlow()

    private var focusRequest: AudioFocusRequest? = null
    private var isReceiverRegistered: Boolean = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                onBecomingNoisy()
            }
        }
    }

    /**
     * Request platform audio focus for media playback.
     *
     * @return True if focus was granted immediately.
     */
    fun requestAudioFocus(): Boolean {
        registerNoisyReceiver()
        val am = audioManager ?: return true

        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(this)
                .build()

            focusRequest = request
            am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        if (granted) {
            _focusState.value = AudioFocusState.ACQUIRED
            listener.onFocusGained()
        }
        return granted
    }

    /**
     * Abandon platform audio focus and unregister receivers.
     */
    fun abandonAudioFocus() {
        unregisterNoisyReceiver()
        val am = audioManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(this)
        }
        _focusState.value = AudioFocusState.NONE
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                _focusState.value = AudioFocusState.ACQUIRED
                listener.onFocusGained()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                _focusState.value = AudioFocusState.DUCKED
                listener.onFocusDucked()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _focusState.value = AudioFocusState.LOST
                listener.onFocusLost()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                _focusState.value = AudioFocusState.LOST
                listener.onFocusLost()
            }
        }
    }

    /**
     * Public handler for headphone/Bluetooth disconnection.
     */
    fun onBecomingNoisy() {
        listener.onBecomingNoisy()
    }

    private fun registerNoisyReceiver() {
        if (!isReceiverRegistered && context != null) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            ContextCompat.registerReceiver(
                context,
                noisyReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            isReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isReceiverRegistered && context != null) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (_: Exception) {
                // Ignore if receiver not registered
            }
            isReceiverRegistered = false
        }
    }
}
