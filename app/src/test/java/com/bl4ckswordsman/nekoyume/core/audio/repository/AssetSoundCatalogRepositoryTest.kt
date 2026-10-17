package com.bl4ckswordsman.nekoyume.core.audio.repository

import com.bl4ckswordsman.nekoyume.core.audio.model.SoundCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

class AssetSoundCatalogRepositoryTest {

    private val validManifestJson = """
        {
          "version": 1,
          "sounds": [
            {
              "id": "rain",
              "name": "Gentle Rain",
              "category": "WEATHER",
              "assetPath": "sounds/rain.ogg",
              "defaultVolume": 0.7
            },
            {
              "id": "forest",
              "name": "Deep Forest",
              "category": "NATURE",
              "assetPath": "sounds/forest.ogg",
              "defaultVolume": 0.6
            },
            {
              "id": "white_noise",
              "name": "White Noise",
              "category": "NOISE",
              "assetPath": "sounds/white_noise.ogg",
              "defaultVolume": 0.5
            }
          ]
        }
    """.trimIndent()

    @Test
    fun getAvailableSounds_parsesManifestCorrectly() = runTest {
        val repository = AssetSoundCatalogRepository { ByteArrayInputStream(validManifestJson.toByteArray()) }

        val result = repository.getAvailableSounds()

        assertTrue("Expected successful parse", result.isSuccess)
        val sounds = result.getOrThrow()
        assertEquals(3, sounds.size)

        val rain = sounds[0]
        assertEquals("rain", rain.id)
        assertEquals("Gentle Rain", rain.name)
        assertEquals(SoundCategory.WEATHER, rain.category)
        assertEquals("sounds/rain.ogg", rain.assetPath)
        assertEquals(0.7f, rain.defaultVolume, 0.001f)
    }

    @Test
    fun getAvailableSounds_cachesResultsAfterFirstRead() = runTest {
        val openCount = AtomicInteger(0)
        val repository = AssetSoundCatalogRepository {
            openCount.incrementAndGet()
            ByteArrayInputStream(validManifestJson.toByteArray())
        }

        val firstCall = repository.getAvailableSounds()
        val secondCall = repository.getAvailableSounds()

        assertTrue(firstCall.isSuccess)
        assertTrue(secondCall.isSuccess)
        assertEquals("Should only open asset stream once due to caching", 1, openCount.get())
    }

    @Test
    fun getSoundsByCategory_filtersProperly() = runTest {
        val repository = AssetSoundCatalogRepository { ByteArrayInputStream(validManifestJson.toByteArray()) }

        val weatherResult = repository.getSoundsByCategory(SoundCategory.WEATHER)
        assertTrue(weatherResult.isSuccess)
        val weatherSounds = weatherResult.getOrThrow()
        assertEquals(1, weatherSounds.size)
        assertEquals("rain", weatherSounds.first().id)

        val sleepResult = repository.getSoundsByCategory(SoundCategory.SLEEP)
        assertTrue(sleepResult.isSuccess)
        assertTrue(sleepResult.getOrThrow().isEmpty())
    }

    @Test
    fun getSoundById_returnsCorrectTrackOrNull() = runTest {
        val repository = AssetSoundCatalogRepository { ByteArrayInputStream(validManifestJson.toByteArray()) }

        val forest = repository.getSoundById("forest")
        assertNotNull(forest)
        assertEquals("Deep Forest", forest?.name)

        val missing = repository.getSoundById("ocean_waves")
        assertNull(missing)
    }

    @Test
    fun getAvailableSounds_missingFile_returnsFailure() = runTest {
        val repository = AssetSoundCatalogRepository { throw FileNotFoundException("sounds.json not found") }

        val result = repository.getAvailableSounds()
        assertTrue("Should return failure when asset is missing", result.isFailure)
        assertTrue(result.exceptionOrNull() is FileNotFoundException)
    }

    @Test
    fun getAvailableSounds_malformedJson_returnsFailure() = runTest {
        val malformedJson = "{ invalid json content"
        val repository = AssetSoundCatalogRepository { ByteArrayInputStream(malformedJson.toByteArray()) }

        val result = repository.getAvailableSounds()
        assertTrue("Should return failure on malformed JSON", result.isFailure)
    }
}
