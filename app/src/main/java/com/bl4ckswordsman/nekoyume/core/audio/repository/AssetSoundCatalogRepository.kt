package com.bl4ckswordsman.nekoyume.core.audio.repository

import android.content.res.AssetManager
import com.bl4ckswordsman.nekoyume.core.audio.model.SoundCategory
import com.bl4ckswordsman.nekoyume.core.audio.model.SoundTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Implementation of [SoundCatalogRepository] that loads packaged sounds from application assets.
 * Results are cached in memory after first successful read.
 */
class AssetSoundCatalogRepository(
    private val openAsset: (String) -> InputStream
) : SoundCatalogRepository {

    /**
     * Primary Android constructor using [AssetManager].
     */
    constructor(assetManager: AssetManager) : this({ assetManager.open(it) })

    private val mutex = Mutex()
    private var cachedSounds: List<SoundTrack>? = null

    override suspend fun getAvailableSounds(): Result<List<SoundTrack>> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedSounds?.let { return@withContext Result.success(it) }

            try {
                val inputStream = openAsset(MANIFEST_FILE_PATH)
                val jsonString = inputStream.bufferedReader().use(BufferedReader::readText)
                val sounds = parseManifest(jsonString)
                cachedSounds = sounds
                Result.success(sounds)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSoundsByCategory(category: SoundCategory): Result<List<SoundTrack>> {
        return getAvailableSounds().map { sounds ->
            sounds.filter { it.category == category }
        }
    }

    override suspend fun getSoundById(id: String): SoundTrack? {
        return getAvailableSounds().getOrNull()?.find { it.id == id }
    }

    private fun parseManifest(jsonString: String): List<SoundTrack> {
        val root = JSONObject(jsonString)
        val soundsArray = root.getJSONArray("sounds")
        val result = mutableListOf<SoundTrack>()

        for (i in 0 until soundsArray.length()) {
            val item = soundsArray.getJSONObject(i)
            val id = item.getString("id")
            val name = item.getString("name")
            val categoryStr = item.getString("category")
            val category = SoundCategory.valueOf(categoryStr.uppercase())
            val assetPath = item.getString("assetPath")
            val defaultVolume = item.optDouble("defaultVolume", 0.7).toFloat()

            result.add(
                SoundTrack(
                    id = id,
                    name = name,
                    category = category,
                    assetPath = assetPath,
                    defaultVolume = defaultVolume
                )
            )
        }
        return result.toList()
    }

    companion object {
        const val MANIFEST_FILE_PATH: String = "sounds/sounds.json"
    }
}
