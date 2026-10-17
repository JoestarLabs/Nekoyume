package com.bl4ckswordsman.nekoyume.core.audio.service

import com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngine
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [AudioLoopEngine] for unit testing.
 */
class FakeAudioLoopEngine : AudioLoopEngine {

    private val _mixState = MutableStateFlow(AudioMixState())
    override val mixState: StateFlow<AudioMixState> = _mixState.asStateFlow()

    var resumeAllCalled = false
    var pauseAllCalled = false
    var stopAllCalled = false
    var lastMasterVolumeSet: Float? = null
    var lastTrackVolumeSet: Pair<String, Float>? = null

    fun emitMixState(newState: AudioMixState) {
        _mixState.value = newState
    }

    override suspend fun startTrack(soundId: String, assetPath: String, initialVolume: Float): Result<Unit> {
        val tracks = _mixState.value.activeTracks.toMutableMap()
        tracks[soundId] = TrackPlaybackState(soundId = soundId, status = TrackPlaybackStatus.PLAYING, volume = initialVolume)
        _mixState.value = _mixState.value.copy(activeTracks = tracks, isMasterPaused = false)
        return Result.success(Unit)
    }

    override suspend fun stopTrack(soundId: String): Result<Unit> {
        val tracks = _mixState.value.activeTracks.toMutableMap()
        tracks.remove(soundId)
        _mixState.value = _mixState.value.copy(activeTracks = tracks)
        return Result.success(Unit)
    }

    override fun setTrackVolume(soundId: String, volume: Float) {
        lastTrackVolumeSet = soundId to volume
        val tracks = _mixState.value.activeTracks.toMutableMap()
        tracks[soundId]?.let {
            tracks[soundId] = it.copy(volume = volume)
            _mixState.value = _mixState.value.copy(activeTracks = tracks)
        }
    }

    override fun setTrackMuted(soundId: String, isMuted: Boolean) {
        val tracks = _mixState.value.activeTracks.toMutableMap()
        tracks[soundId]?.let {
            tracks[soundId] = it.copy(isMuted = isMuted)
            _mixState.value = _mixState.value.copy(activeTracks = tracks)
        }
    }

    override fun setMasterVolume(volume: Float) {
        lastMasterVolumeSet = volume
        _mixState.value = _mixState.value.copy(masterVolume = volume)
    }

    override fun pauseAll() {
        pauseAllCalled = true
        _mixState.value = _mixState.value.copy(isMasterPaused = true)
    }

    override fun resumeAll() {
        resumeAllCalled = true
        _mixState.value = _mixState.value.copy(isMasterPaused = false)
    }

    override fun stopAll() {
        stopAllCalled = true
        _mixState.value = _mixState.value.copy(isMasterPaused = false, activeTracks = emptyMap())
    }

    override fun getTrackState(soundId: String): Flow<TrackPlaybackState?> {
        return _mixState.map { it.activeTracks[soundId] }
    }

    override fun release() {
        stopAll()
    }
}
