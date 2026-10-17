package com.bl4ckswordsman.nekoyume.di

import android.content.Context
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

/**
 * Hilt dependency injection module providing singleton bindings for core audio components.
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideSoundCatalogRepository(
        @ApplicationContext context: Context
    ): SoundCatalogRepository {
        return AssetSoundCatalogRepository(context.assets)
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
    fun providePlaybackServiceController(
        @ApplicationContext context: Context,
        audioEngine: AudioLoopEngine
    ): com.bl4ckswordsman.nekoyume.core.audio.service.PlaybackServiceController {
        return com.bl4ckswordsman.nekoyume.core.audio.service.PlaybackServiceControllerImpl(context, audioEngine)
    }
}
