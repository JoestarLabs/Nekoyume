# Interface Contract: MediaSession & Playback Service

**Feature**: `002-foreground-audio-service`  
**Date**: 2026-10-17  
**Status**: Completed  

---

## 1. External MediaSession Contract (System-to-Service)

The `NekoyumePlaybackService` publishes an AndroidX `MediaSession` that accepts remote platform commands from:
- System Media Notifications (lock screen and notification shade)
- Bluetooth accessories and headsets (AVRCP `KEYCODE_MEDIA_PLAY`, `KEYCODE_MEDIA_PAUSE`, `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_STOP`)
- Android Auto / External Media Controllers

### 1.1 Supported Transport Commands

| Command | Action on AudioLoopEngine | Target ServiceState |
|---|---|---|
| `Player.COMMAND_PLAY_PAUSE` (when paused/stopped) | `audioEngine.play()` | `PLAYING` |
| `Player.COMMAND_PLAY_PAUSE` (when playing) | `audioEngine.pause()` | `PAUSED` |
| `Player.COMMAND_STOP` | `audioEngine.stop()` | `STOPPED` |

### 1.2 System Notification Intent Contract

- **Click Action (Notification Body)**:
  - Intent: `Intent(context, MainActivity::class.java)` with `FLAG_ACTIVITY_SINGLE_TOP`.
  - Behavior: Opens or brings `MainActivity` to the foreground without restarting the activity task (FR-006).
- **Dismiss Action (Swipe-away when paused)**:
  - Behavior: Stops foreground service (`stopSelf()`) and releases media resources (FR-007, US-3).

---

## 2. In-App Service Controller Contract (App-to-Service)

Client UI components (ViewModels and Composables) interact with the playback service through the `PlaybackServiceController` interface, injected via Hilt.

```kotlin
package com.bl4ckswordsman.nekoyume.core.audio.service

import com.bl4ckswordsman.nekoyume.core.audio.model.AudioMixState
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level controller interface mediated by Hilt for observing
 * background audio service state and issuing transport commands.
 */
interface PlaybackServiceController {
    /**
     * Reactive stream of aggregate background service state.
     */
    val serviceState: StateFlow<ServiceState>

    /**
     * Reactive stream of current multi-stream audio mix state.
     */
    val mixState: StateFlow<AudioMixState>

    /**
     * Starts or resumes playback across all active sound tracks.
     */
    fun play()

    /**
     * Smoothly pauses playback across all active sound tracks.
     */
    fun pause()

    /**
     * Stops playback and demotes foreground service.
     */
    fun stop()

    /**
     * Toggles a specific sound track by ID.
     */
    fun toggleTrack(trackId: String)

    /**
     * Sets volume for an individual sound track [0.0f..1.0f].
     */
    fun setTrackVolume(trackId: String, volume: Float)

    /**
     * Sets master attenuation volume [0.0f..1.0f].
     */
    fun setMasterVolume(volume: Float)
}
```

---

## 3. Dependency Injection Module Contract (Hilt)

Components and bindings registered in Hilt's `SingletonComponent`:

```kotlin
package com.bl4ckswordsman.nekoyume.di

import android.content.Context
import com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngine
import com.bl4ckswordsman.nekoyume.core.audio.focus.NekoyumeAudioFocusManager
import com.bl4ckswordsman.nekoyume.core.audio.repository.SoundCatalogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideSoundCatalogRepository(
        @ApplicationContext context: Context
    ): SoundCatalogRepository

    @Provides
    @Singleton
    fun provideAudioFocusManager(
        @ApplicationContext context: Context
    ): NekoyumeAudioFocusManager

    @Provides
    @Singleton
    fun provideAudioLoopEngine(
        repository: SoundCatalogRepository,
        focusManager: NekoyumeAudioFocusManager,
        @ApplicationContext context: Context
    ): AudioLoopEngine
}
```
