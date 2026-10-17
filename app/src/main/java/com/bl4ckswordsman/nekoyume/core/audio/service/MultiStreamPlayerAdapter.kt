package com.bl4ckswordsman.nekoyume.core.audio.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.ForwardingPlayer
import com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Bridges the multi-stream [AudioLoopEngine] to AndroidX Media3's [Player] interface.
 *
 * Forwards system transport commands (play, pause, stop) to the underlying multi-stream
 * engine and reflects aggregate playback state and soundscape metadata to the [androidx.media3.session.MediaSession].
 */
class MultiStreamPlayerAdapter(
    player: Player,
    private val audioEngine: AudioLoopEngine,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : ForwardingPlayer(player) {

    private val customListeners = CopyOnWriteArraySet<Player.Listener>()

    init {
        scope.launch {
            audioEngine.mixState
                .map { it.isPlaying }
                .distinctUntilChanged()
                .collect { isPlaying ->
                    val playbackState = if (audioEngine.mixState.value.activeTracks.isEmpty()) {
                        Player.STATE_IDLE
                    } else {
                        Player.STATE_READY
                    }
                    for (listener in customListeners) {
                        listener.onIsPlayingChanged(isPlaying)
                        listener.onPlayWhenReadyChanged(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                        listener.onPlaybackStateChanged(playbackState)
                    }
                }
        }

        scope.launch {
            audioEngine.mixState
                .map { state -> state.playingTrackCount }
                .distinctUntilChanged()
                .collect {
                    val metadata = mediaMetadata
                    for (listener in customListeners) {
                        listener.onMediaMetadataChanged(metadata)
                    }
                }
        }
    }

    override fun addListener(listener: Player.Listener) {
        super.addListener(listener)
        customListeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(listener)
        customListeners.remove(listener)
    }

    override fun play() {
        audioEngine.resumeAll()
    }

    override fun pause() {
        audioEngine.pauseAll()
    }

    override fun stop() {
        audioEngine.stopAll()
    }

    override fun isPlaying(): Boolean {
        return audioEngine.mixState.value.isPlaying
    }

    override fun getPlayWhenReady(): Boolean {
        return audioEngine.mixState.value.isPlaying
    }

    override fun getPlaybackState(): Int {
        val mix = audioEngine.mixState.value
        return if (mix.activeTracks.isEmpty()) Player.STATE_IDLE else Player.STATE_READY
    }

    override fun getAvailableCommands(): Player.Commands {
        return Player.Commands.Builder()
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_STOP)
            .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
            .add(Player.COMMAND_GET_METADATA)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return command == Player.COMMAND_PLAY_PAUSE ||
                command == Player.COMMAND_STOP ||
                command == Player.COMMAND_GET_CURRENT_MEDIA_ITEM ||
                command == Player.COMMAND_GET_METADATA
    }

    override fun getMediaMetadata(): MediaMetadata {
        val activeCount = audioEngine.mixState.value.playingTrackCount
        val title = if (activeCount > 0) "Nekoyume Soundscape" else "Nekoyume"
        val description = when {
            activeCount == 0 -> "Ready"
            activeCount == 1 -> "Playing 1 sound"
            else -> "Playing $activeCount sounds"
        }
        return MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(description)
            .setDescription(description)
            .build()
    }

    override fun getCurrentMediaItem(): MediaItem? {
        val activeCount = audioEngine.mixState.value.playingTrackCount
        return MediaItem.Builder()
            .setMediaId("nekoyume_multi_stream")
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    fun releaseAdapter() {
        scope.cancel()
        customListeners.clear()
    }
}
