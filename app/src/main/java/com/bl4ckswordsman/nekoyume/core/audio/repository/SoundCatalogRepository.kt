package com.bl4ckswordsman.nekoyume.core.audio.repository

import com.bl4ckswordsman.nekoyume.core.audio.model.SoundCategory
import com.bl4ckswordsman.nekoyume.core.audio.model.SoundTrack

/**
 * Repository interface for discovering and loading available ambient sound tracks
 * packaged within application assets.
 */
interface SoundCatalogRepository {

    /**
     * Retrieve all cataloged sound tracks from assets manifest.
     * Caches in-memory after initial load.
     */
    suspend fun getAvailableSounds(): Result<List<SoundTrack>>

    /**
     * Retrieve sounds filtered by category.
     */
    suspend fun getSoundsByCategory(category: SoundCategory): Result<List<SoundTrack>>

    /**
     * Retrieve a specific sound track by unique ID.
     */
    suspend fun getSoundById(id: String): SoundTrack?
}
