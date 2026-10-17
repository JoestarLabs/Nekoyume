package com.bl4ckswordsman.nekoyume.core.audio.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.bl4ckswordsman.nekoyume.MainActivity
import com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AndroidX Media3 foreground playback service hosting the system [MediaSession]
 * for continuous background white noise and ambient multi-stream playback.
 */
@AndroidEntryPoint
class NekoyumePlaybackService : MediaSessionService() {

    @Inject
    lateinit var audioEngine: AudioLoopEngine

    private var mediaSession: MediaSession? = null
    private var playerAdapter: MultiStreamPlayerAdapter? = null
    private var basePlayer: ExoPlayer? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var idleTimeoutJob: Job? = null

    companion object {
        const val IDLE_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
    }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build()
        basePlayer = player

        val adapter = MultiStreamPlayerAdapter(player, audioEngine)
        playerAdapter = adapter

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val sessionActivity = PendingIntent.getActivity(this, 0, sessionActivityIntent, flags)

        mediaSession = MediaSession.Builder(this, adapter)
            .setSessionActivity(sessionActivity)
            .build()

        observePlaybackLifecycle()
    }

    private fun observePlaybackLifecycle() {
        serviceScope.launch {
            audioEngine.mixState.collect { mixState ->
                if (mixState.isPlaying) {
                    cancelIdleTimeout()
                } else if (mixState.activeTracks.isNotEmpty()) {
                    startIdleTimeout()
                } else {
                    cancelIdleTimeout()
                    stopSelf()
                }
            }
        }
    }

    private fun startIdleTimeout() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = serviceScope.launch {
            delay(IDLE_TIMEOUT_MS)
            if (!audioEngine.mixState.value.isPlaying) {
                audioEngine.stopAll()
                stopSelf()
            }
        }
    }

    private fun cancelIdleTimeout() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // FR-013: If audio is actively playing, sustain background playback across task removal.
        if (audioEngine.mixState.value.isPlaying) {
            super.onTaskRemoved(rootIntent)
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        cancelIdleTimeout()
        serviceScope.cancel()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        playerAdapter?.releaseAdapter()
        playerAdapter = null

        basePlayer?.release()
        basePlayer = null

        super.onDestroy()
    }
}
