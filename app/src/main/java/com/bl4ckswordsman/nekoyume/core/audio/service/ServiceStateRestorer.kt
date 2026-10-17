package com.bl4ckswordsman.nekoyume.core.audio.service

import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackState
import com.bl4ckswordsman.nekoyume.core.audio.model.TrackPlaybackStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles serialization and restoration of active mix state across low-memory killer process revival (FR-014).
 * Restores mix configurations strictly in a PAUSED state with notification ready.
 */
object ServiceStateRestorer {

    /**
     * Serializes current mix state to a JSON string.
     */
    fun serialize(mixState: AudioMixState): String {
        val root = JSONObject()
        root.put("masterVolume", mixState.masterVolume.toDouble())

        val tracksArray = JSONArray()
        for ((_, track) in mixState.activeTracks) {
            val trackObj = JSONObject()
            trackObj.put("soundId", track.soundId)
            trackObj.put("volume", track.volume.toDouble())
            trackObj.put("isMuted", track.isMuted)
            tracksArray.put(trackObj)
        }
        root.put("tracks", tracksArray)
        return root.toString()
    }

    /**
     * Deserializes a saved JSON string into an [AudioMixState] restored strictly in a PAUSED condition.
     */
    fun deserialize(json: String?): AudioMixState? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            val masterVolume = root.optDouble("masterVolume", 1.0).toFloat()
            val tracksArray = root.optJSONArray("tracks") ?: return null

            val activeTracks = mutableMapOf<String, TrackPlaybackState>()
            for (i in 0 until tracksArray.length()) {
                val trackObj = tracksArray.getJSONObject(i)
                val soundId = trackObj.getString("soundId")
                val volume = trackObj.optDouble("volume", 1.0).toFloat()
                val isMuted = trackObj.optBoolean("isMuted", false)

                // In accordance with FR-014: restore in PAUSED condition
                activeTracks[soundId] = TrackPlaybackState(
                    soundId = soundId,
                    status = TrackPlaybackStatus.PAUSED,
                    volume = volume,
                    isMuted = isMuted
                )
            }

            AudioMixState(
                isMasterPaused = true,
                masterVolume = masterVolume,
                activeTracks = activeTracks
            )
        } catch (_: Exception) {
            null
        }
    }
}
