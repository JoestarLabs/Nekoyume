# Research: Continuous Background Audio Service & Dependency Injection

**Feature**: `002-foreground-audio-service`  
**Date**: 2026-10-17  
**Status**: Completed  

---

## 1. Multi-Stream AndroidX Media3 MediaSessionService Architecture

### Decision
Implement `NekoyumePlaybackService` extending AndroidX Media3 `MediaSessionService`. Interface `AudioLoopEngine` with `MediaSession` via an aggregated forwarder `Player` adapter (`MultiStreamPlayerAdapter`) and `MediaSession.Callback` that routes transport commands (Play, Pause, Stop) to `AudioLoopEngine` and synchronizes multi-track playback state.

### Rationale
- AndroidX Media3 `MediaSessionService` is the modern standard for background media playback on Android, managing system notification rendering, lock screen media controls, Bluetooth hardware button events, and platform media routing automatically.
- `AudioLoopEngine` manages multiple concurrent looping tracks with independent volume scaling, whereas Android platform media controls represent a single aggregate session. A dedicated adapter encapsulates this bridging logic cleanly (SoC, Constitution Principle IV & VII).

### Alternatives Considered
- **Legacy `Service` + `NotificationCompat.MediaStyle` + `MediaSessionCompat`**: Deprecated in modern AndroidX, brittle across Android 13+ notification permissions and Android 14+ FGS restrictions.
- **Single ExoPlayer playing merged audio**: Incapable of independent volume sliders and independent loop points required by Constitution Principle III.

---

## 2. Android 14+ Foreground Service Management & Permissions

### Decision
Declare `foregroundServiceType="mediaPlayback"` on `NekoyumePlaybackService` in `AndroidManifest.xml`. Request standard manifest permissions:
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.WAKE_LOCK`

Leverage `MediaSessionService`'s built-in foreground lifecycle to automatically promote the service to the foreground when active playback starts, and demote it when playback stops or pauses.

### Rationale
- Android 14+ (API 34+) enforces strict runtime checks and manifest declarations for foreground service types. Failure to declare `mediaPlayback` throws `SecurityException` upon starting foreground execution.
- `MediaSessionService` integrates directly with system notification policies, ensuring notifications appear properly across Android 10 through 15+.

### Alternatives Considered
- **Background Service without Foreground Promotion**: Terminated within seconds by Android's background execution limits when the screen turns off.
- **Short-service FGS type**: Restricted to maximum 3 minutes by Android 14+ and disallowed for media playback.

---

## 3. Inactivity Idle Timeout & Recents Task Removal

### Decision
- **Recents Task Removal (`onTaskRemoved`)**: Inspect `AudioLoopEngine.mixState`. If one or more audio streams are actively playing, do not stop the service—allow continuous playback in the foreground (FR-013). If playback is paused or stopped, execute `stopSelf()` to cleanly clean up resources.
- **Paused Inactivity Idle Timeout**: When playback transitions from playing to paused, schedule a cancellable 15-minute coroutine timer. If playback resumes before 15 minutes, cancel the timer. If the timer expires, call `stopSelf()`, dismiss the foreground notification, and release audio resources (FR-016, SC-004).

### Rationale
- Users frequently swipe away apps from the recent tasks list while meditating or sleeping; maintaining playback honors user expectations.
- Automatically terminating the service after 15 minutes in the paused state prevents battery drain and orphaned notifications, complying with Constitution Principle VII.

### Alternatives Considered
- **AlarmManager Exact Alarm**: Too heavy and invasive for an in-memory paused service timeout.
- **Indefinite Paused State**: Leaves notifications pinned and holds system process priority, frustrating users and draining battery.

---

## 4. Audio Focus & Transient Ducking Integration

### Decision
Integrate `NekoyumeAudioFocusManager` directly with `AudioLoopEngine` and `NekoyumePlaybackService`.
- **Transient Focus Loss (e.g., GPS directions, notification alerts)**: Smoothly duck ambient volume to 20% (0.2f factor) using the 30ms anti-zipper volume ramp, and smoothly restore upon focus return.
- **Permanent Focus Loss (e.g., incoming phone call, music app playback)**: Immediately pause audio playback, update `MediaSession` state to paused, and reflect the paused state on the system media notification.

### Rationale
- Complies strictly with Constitution Principle III and specification FR-015.
- Prevents abrupt cutoffs during brief system alerts while ensuring phone calls take immediate priority.

### Alternatives Considered
- **Pausing on all focus losses**: Creates annoying silent gaps whenever an SMS or notification arrives.
- **Ignoring audio focus**: Violates Android Compatibility Definition Document (CDD) and produces conflicting audio streams.

---

## 5. Dependency Injection Architecture via Google Hilt

### Decision
Integrate Google Hilt for application-wide dependency injection:
- Annotate `NekoyumeApplication` with `@HiltAndroidApp`.
- Annotate `MainActivity` and `NekoyumePlaybackService` with `@AndroidEntryPoint`.
- Provide an `AudioModule` installed in `SingletonComponent` that provides:
  - `@Singleton fun provideSoundCatalogRepository(@ApplicationContext context: Context): SoundCatalogRepository`
  - `@Singleton fun provideAudioFocusManager(@ApplicationContext context: Context): NekoyumeAudioFocusManager`
  - `@Singleton fun provideAudioLoopEngine(repository: SoundCatalogRepository, focusManager: NekoyumeAudioFocusManager, @ApplicationContext context: Context): AudioLoopEngine`

### Rationale
- Strictly required by Constitution Principle V ("Dependency Injection via Hilt: All dependencies across Activities, ViewModels, Repositories, and Services MUST be provided via Google Hilt").
- Eliminates manual service locators or global singletons, avoiding memory leaks and facilitating automated testing.

### Alternatives Considered
- **Manual Service Locator**: Explicitly prohibited by Constitution Principle V.
- **Static Companion Object Singletons**: Prohibited by Constitution Principle V and leads to memory leaks in Android service lifecycles.

---

## 6. Process Revival & State Restoration

### Decision
Preserve the active `AudioMixState` across service lifecycles. If the foreground service process is killed by the system Low Memory Killer and recreated, restore the saved mix configuration in a **paused state** with the media notification active (FR-014), ready for the user to resume with one tap.

### Rationale
- Prevents sudden, unexpected loud audio if the system revives the service in an inappropriate environment (e.g. library, meeting).
- Preserves the user's custom blend of ambient sounds so they don't have to reconfigure their soundscape.

### Alternatives Considered
- **Auto-resuming full playback on restart**: Risky and disruptive to users in quiet environments.
- **Discarding state**: Annoying UX as the user's carefully adjusted multi-sound mix is lost.
