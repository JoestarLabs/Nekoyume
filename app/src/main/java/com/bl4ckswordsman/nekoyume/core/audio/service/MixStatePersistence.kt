package com.bl4ckswordsman.nekoyume.core.audio.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles persistence and retrieval of [AudioMixState] across service and app restarts
 * in compliance with FR-008, FR-014, and FR-017.
 */
interface MixStatePersistence {
    val savedMixState: Flow<AudioMixState?>
    suspend fun saveMixState(mixState: AudioMixState)
    suspend fun getSavedMixState(): AudioMixState?

    companion object {
        val KEY_SAVED_MIX = stringPreferencesKey("saved_mix_state")
    }
}

/**
 * DataStore implementation of [MixStatePersistence].
 */
@Singleton
class DataStoreMixStatePersistence @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : MixStatePersistence {

    override val savedMixState: Flow<AudioMixState?> = dataStore.data.map { preferences ->
        val serialized = preferences[MixStatePersistence.KEY_SAVED_MIX]
        ServiceStateRestorer.deserialize(serialized)
    }

    override suspend fun saveMixState(mixState: AudioMixState) {
        if (mixState.hasActiveTracks) {
            val serialized = ServiceStateRestorer.serialize(mixState)
            dataStore.edit { preferences ->
                preferences[MixStatePersistence.KEY_SAVED_MIX] = serialized
            }
        }
    }

    override suspend fun getSavedMixState(): AudioMixState? {
        val preferences = dataStore.data.first()
        val serialized = preferences[MixStatePersistence.KEY_SAVED_MIX]
        return ServiceStateRestorer.deserialize(serialized)
    }
}
