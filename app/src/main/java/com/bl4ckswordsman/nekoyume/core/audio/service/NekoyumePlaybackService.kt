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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    @Inject
    lateinit var mixPersistence: MixStatePersistence

    private var mediaSession: MediaSession? = null
    private var playerAdapter: MultiStreamPlayerAdapter? = null
    private var basePlayer: ExoPlayer? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var idleTimeoutHandler: ServiceIdleTimeoutHandler

    override fun onCreate() {
        super.onCreate()

        idleTimeoutHandler = ServiceIdleTimeoutHandler(
            scope = serviceScope,
            timeoutMs = ServiceIdleTimeoutHandler.DEFAULT_IDLE_TIMEOUT_MS,
            onTimeout = {
                if (!audioEngine.mixState.value.isPlaying) {
                    audioEngine.stopAll()
                    stopSelf()
                }
            }
        )

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
        restoreSavedMixIfAvailable()
    }

    private fun observePlaybackLifecycle() {
        serviceScope.launch {
            audioEngine.mixState.collect { mixState ->
                idleTimeoutHandler.onPlaybackStateChanged(
                    isPlaying = mixState.isPlaying,
                    hasActiveTracks = mixState.hasActiveTracks
                )

                if (mixState.hasActiveTracks) {
                    mixPersistence.saveMixState(mixState)
                } else {
                    stopSelf()
                }
            }
        }
    }

    private fun restoreSavedMixIfAvailable() {
        serviceScope.launch {
            val restored = mixPersistence.getSavedMixState() ?: return@launch

            // In accordance with FR-014: state restored in paused condition
            if (restored.hasActiveTracks && !audioEngine.mixState.value.hasActiveTracks) {
                // Restore saved master volume
                audioEngine.setMasterVolume(restored.masterVolume)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // FR-013: If audio is actively playing, sustain background playback across task removal.
        ServiceLifecycleHandler.handleTaskRemoved(
            isPlaying = audioEngine.mixState.value.isPlaying,
            onStopService = { stopSelf() }
        )
    }

    override fun onDestroy() {
        idleTimeoutHandler.cancel()
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
