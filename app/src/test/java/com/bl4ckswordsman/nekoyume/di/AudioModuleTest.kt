package com.bl4ckswordsman.nekoyume.di

import com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngine
import com.bl4ckswordsman.nekoyume.core.audio.repository.SoundCatalogRepository
import com.bl4ckswordsman.nekoyume.core.audio.service.ServicePlaybackState
import com.bl4ckswordsman.nekoyume.core.audio.service.ServiceState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Singleton

class AudioModuleTest {

    @Test
    fun audioModule_hasRequiredAnnotations() {
        val moduleClass = AudioModule::class.java
        val annotationNames = moduleClass.annotations.map { it.annotationClass.java.name }
        assertTrue(
            "AudioModule must have @Module or @InstallIn, found: $annotationNames",
            annotationNames.any { it.contains("Module") || it.contains("InstallIn") }
        )
    }

    @Test
    fun provideSoundCatalogRepository_hasSingletonAndProvidesAnnotations() {
        val method = AudioModule::class.java.methods.firstOrNull { it.name == "provideSoundCatalogRepository" }
        assertNotNull("provideSoundCatalogRepository method must exist", method)
        assertTrue("Method must be annotated with @Provides", method!!.isAnnotationPresent(Provides::class.java))
        assertTrue("Method must be annotated with @Singleton", method.isAnnotationPresent(Singleton::class.java))
        assertEquals(SoundCatalogRepository::class.java, method.returnType)
    }

    @Test
    fun provideAudioFocusManager_hasSingletonAndProvidesAnnotations() {
        val method = AudioModule::class.java.methods.firstOrNull { it.name == "provideAudioFocusManager" }
        assertNotNull("provideAudioFocusManager method must exist", method)
        assertTrue("Method must be annotated with @Provides", method!!.isAnnotationPresent(Provides::class.java))
        assertTrue("Method must be annotated with @Singleton", method.isAnnotationPresent(Singleton::class.java))
        assertEquals(com.bl4ckswordsman.nekoyume.core.audio.focus.NekoyumeAudioFocusManager::class.java, method.returnType)
    }

    @Test
    fun providePreferencesDataStore_hasSingletonAndProvidesAnnotations() {
        val method = AudioModule::class.java.methods.firstOrNull { it.name == "providePreferencesDataStore" }
        assertNotNull("providePreferencesDataStore method must exist", method)
        assertTrue("Method must be annotated with @Provides", method!!.isAnnotationPresent(Provides::class.java))
        assertTrue("Method must be annotated with @Singleton", method.isAnnotationPresent(Singleton::class.java))
    }

    @Test
    fun provideSessionToken_hasSingletonAndProvidesAnnotations() {
        val method = AudioModule::class.java.methods.firstOrNull { it.name == "provideSessionToken" }
        assertNotNull("provideSessionToken method must exist", method)
        assertTrue("Method must be annotated with @Provides", method!!.isAnnotationPresent(Provides::class.java))
        assertTrue("Method must be annotated with @Singleton", method.isAnnotationPresent(Singleton::class.java))
        assertEquals(androidx.media3.session.SessionToken::class.java, method.returnType)
    }

    @Test
    fun provideAudioLoopEngine_hasSingletonAndProvidesAnnotations() {
        val method = AudioModule::class.java.methods.firstOrNull { it.name == "provideAudioLoopEngine" }
        assertNotNull("provideAudioLoopEngine method must exist", method)
        assertTrue("Method must be annotated with @Provides", method!!.isAnnotationPresent(Provides::class.java))
        assertTrue("Method must be annotated with @Singleton", method.isAnnotationPresent(Singleton::class.java))
        assertEquals(AudioLoopEngine::class.java, method.returnType)
    }

    @Test
    fun serviceState_defaultValues_areValid() {
        val defaultState = ServiceState()
        assertEquals(ServicePlaybackState.STOPPED, defaultState.playbackState)
        assertEquals(false, defaultState.isForeground)
        assertEquals(0, defaultState.activeTrackCount)
        assertNull(defaultState.pausedTimestampEpochMs)
        assertTrue(defaultState.hasNotificationPermission)
    }

    @Test
    fun serviceState_playingState_enforcesForegroundInvariant() {
        // Valid PLAYING state with isForeground = true
        val validPlaying = ServiceState(
            playbackState = ServicePlaybackState.PLAYING,
            isForeground = true,
            activeTrackCount = 2
        )
        assertEquals(ServicePlaybackState.PLAYING, validPlaying.playbackState)
        assertTrue(validPlaying.isForeground)
    }

    @Test(expected = IllegalArgumentException::class)
    fun serviceState_playingState_throwsIfForegroundIsFalse() {
        ServiceState(
            playbackState = ServicePlaybackState.PLAYING,
            isForeground = false,
            activeTrackCount = 2
        )
    }

    @Test
    fun serviceState_pausedState_enforcesTimestampInvariant() {
        val paused = ServiceState(
            playbackState = ServicePlaybackState.PAUSED,
            isForeground = false,
            activeTrackCount = 1,
            pausedTimestampEpochMs = 123456789L
        )
        assertEquals(ServicePlaybackState.PAUSED, paused.playbackState)
        assertEquals(123456789L, paused.pausedTimestampEpochMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun serviceState_pausedState_throwsIfTimestampIsNull() {
        ServiceState(
            playbackState = ServicePlaybackState.PAUSED,
            isForeground = false,
            activeTrackCount = 1,
            pausedTimestampEpochMs = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun serviceState_stoppedState_throwsIfTimestampIsNotNull() {
        ServiceState(
            playbackState = ServicePlaybackState.STOPPED,
            isForeground = false,
            activeTrackCount = 0,
            pausedTimestampEpochMs = 9999L
        )
    }
}
