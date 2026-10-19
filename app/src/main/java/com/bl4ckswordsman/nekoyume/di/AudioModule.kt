package com.bl4ckswordsman.nekoyume.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngine
import com.bl4ckswordsman.nekoyume.core.audio.Media3AudioLoopEngine
import com.bl4ckswordsman.nekoyume.core.audio.repository.AssetSoundCatalogRepository
import com.bl4ckswordsman.nekoyume.core.audio.repository.SoundCatalogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "nekoyume_playback_prefs")

/**
 * Hilt dependency injection module providing singleton bindings for core audio components.
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.playbackDataStore
    }

    @Provides
    @Singleton
    fun provideMixStatePersistence(
        dataStore: DataStore<Preferences>
    ): com.bl4ckswordsman.nekoyume.core.audio.service.MixStatePersistence {
        return com.bl4ckswordsman.nekoyume.core.audio.service.DataStoreMixStatePersistence(dataStore)
    }

    @Provides
    @Singleton
    fun provideSoundCatalogRepository(
        @ApplicationContext context: Context
    ): SoundCatalogRepository {
        return AssetSoundCatalogRepository(context.assets)
    }

    @Provides
    @Singleton
    fun provideAudioFocusManager(
        @ApplicationContext context: Context
    ): com.bl4ckswordsman.nekoyume.core.audio.focus.NekoyumeAudioFocusManager {
        return com.bl4ckswordsman.nekoyume.core.audio.focus.NekoyumeAudioFocusManager(context = context)
    }

    @Provides
    @Singleton
    fun provideAudioLoopEngine(
        @ApplicationContext context: Context
    ): AudioLoopEngine {
        return Media3AudioLoopEngine(context)
    }

    @Provides
    @Singleton
    fun provideSessionToken(
        @ApplicationContext context: Context
    ): androidx.media3.session.SessionToken {
        return androidx.media3.session.SessionToken(
            context,
            android.content.ComponentName(context, com.bl4ckswordsman.nekoyume.core.audio.service.NekoyumePlaybackService::class.java)
        )
    }

    @Provides
    @Singleton
    fun providePlaybackServiceController(
        @ApplicationContext context: Context,
        audioEngine: AudioLoopEngine,
        mixPersistence: com.bl4ckswordsman.nekoyume.core.audio.service.MixStatePersistence,
        sessionToken: androidx.media3.session.SessionToken
    ): com.bl4ckswordsman.nekoyume.core.audio.service.PlaybackServiceController {
        return com.bl4ckswordsman.nekoyume.core.audio.service.PlaybackServiceControllerImpl(
            context = context,
            audioEngine = audioEngine,
            mixPersistence = mixPersistence,
            sessionToken = sessionToken
        )
    }
}
