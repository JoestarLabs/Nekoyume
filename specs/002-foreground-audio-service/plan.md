# Implementation Plan: Continuous Background Audio Service & Dependency Injection

**Branch**: `002-foreground-audio-service` | **Date**: 2026-10-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-foreground-audio-service/spec.md`

---

## Summary

This feature establishes continuous background audio playback for multi-stream ambient soundscapes and application-wide dependency injection:
1. **Background Playback & Platform Media Session**: Implements `NekoyumePlaybackService` extending AndroidX Media3 `MediaSessionService` with Android 14+ `mediaPlayback` foreground service compliance. It bridges the headless `AudioLoopEngine` to Android's platform `MediaSession`, providing lock screen transport controls, system notification rendering, Bluetooth media button support, and continuous screen-off playback.
2. **Audio Focus & Lifecycle Hygiene**: Integrates `NekoyumeAudioFocusManager` for smooth transient audio ducking (to 20% volume) and permanent pause, handles `onTaskRemoved` (preserving playback if active, stopping if idle), and enforces a 15-minute paused idle timeout to prevent battery drain.
3. **Dependency Injection with Google Hilt**: Integrates Hilt across the application (`@HiltAndroidApp`, `@AndroidEntryPoint`, and `@Module` in `SingletonComponent`), binding core audio components (`AudioLoopEngine`, `SoundCatalogRepository`, `NekoyumeAudioFocusManager`) without manual service locators.

---

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java 17  
**Primary Dependencies**:
- AndroidX Media3 (ExoPlayer 1.5.1, Common 1.5.1, Session 1.5.1)
- Google Hilt (2.59.2)
- Kotlinx Coroutines (1.10.1)
- Jetpack Compose Material 3  
**Storage**: In-memory reactive state (`StateFlow`), lightweight persistence for process revival mix restoration  
**Testing**: JUnit 4, Kotlinx Coroutines Test, AndroidX Test (`./gradlew testDebugUnitTest`)  
**Target Platform**: Android (minSdk = 29, targetSdk = 35, compileSdk = 37)  
**Project Type**: Android Mobile Application  
**Performance Goals**:
- Lock screen transport response < 100ms (SC-002)
- Continuous screen-off battery consumption < 1.5%/hour (SC-003)
- Resource cleanup upon stop < 1 second (SC-004)
- Dependency injection startup resolution < 50ms (SC-005)  
**Constraints**:
- Android 14+ (API 34+) `mediaPlayback` foreground service type and runtime notification permissions
- R8/ProGuard code shrinking compatibility (`isMinifyEnabled = true`, `isShrinkResources = true`)  
**Scale/Scope**: Multi-stream concurrent loop playback (up to 8 simultaneous audio streams), background service execution, lock screen and notification media controls.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Requirement | Plan Alignment | Status |
|---|---|---|---|
| **I. Declarative UI & Material 3** | Compose UI stateless or hoists state to ViewModels; side effects managed via effect handlers. | UI connects via `PlaybackServiceController` StateFlows; no direct Activity references in service. | **PASS** |
| **II. UDF & MVVM** | ViewModels expose read-only `StateFlow`; explicit user actions; asynchronous state streams using Coroutines `Flow`. | `ServiceState` and `AudioMixState` exposed as read-only `StateFlow`. | **PASS** |
| **III. Multi-Stream Audio Engine** | Reliable concurrent playback, gapless looping, independent volume attenuation, audio focus ducking/pausing. | Leverages `AudioLoopEngine`; integrates `NekoyumeAudioFocusManager` for transient ducking and permanent pause. | **PASS** |
| **IV. Clean Foreground Service** | AndroidX `MediaSessionService` foreground service, API 34+ `mediaPlayback` type, notification controls, battery optimization. | `NekoyumePlaybackService` extends `MediaSessionService`, declares `mediaPlayback` FGS type, and enforces 15-minute idle timeout. | **PASS** |
| **V. Dependency Injection via Hilt** | All dependencies provided via Hilt; manual service locators or global static singletons prohibited; scoped appropriately. | Application annotated with `@HiltAndroidApp`; `AudioModule` provides `@Singleton` scoped audio engine, repository, and focus manager. | **PASS** |
| **VI. Test-Driven Verification** | All audio logic, state transitions, and ViewModels covered by automated unit/instrumentation tests. | Unit tests validate service lifecycle transitions, player adapter commands, idle timeout, and DI graph. | **PASS** |
| **VII. Code Excellence: SoC & DRY** | Domain models pure Kotlin; clear separation of concerns; no memory leaks; debounced high-frequency actions. | `ServiceState` and `MediaSessionMetadata` cleanly decoupled; explicit service lifecycle cleanup on stop. | **PASS** |

**Gate Result**: **ALL GATES PASS**. No constitutional violations.

---

## Project Structure

### Documentation (this feature)

```text
specs/002-foreground-audio-service/
├── spec.md                  # Feature specification
├── plan.md                  # This file (/speckit-plan command output)
├── research.md              # Phase 0 output (/speckit-plan command)
├── data-model.md            # Phase 1 output (/speckit-plan command)
├── quickstart.md            # Phase 1 output (/speckit-plan command)
├── contracts/               # Phase 1 output (/speckit-plan command)
│   └── media-session-contract.md
├── checklists/
│   └── requirements.md      # Spec quality checklist
└── tasks.md                 # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository layout for this feature)

```text
app/src/main/
├── AndroidManifest.xml                          # Foreground service & media permissions
└── java/com/bl4ckswordsman/nekoyume/
    ├── NekoyumeApplication.kt                   # Annotated with @HiltAndroidApp
    ├── MainActivity.kt                          # Annotated with @AndroidEntryPoint
    ├── di/
    │   └── AudioModule.kt                       # Hilt Module providing AudioLoopEngine, FocusManager, Repo
    └── core/audio/
        ├── AudioLoopEngine.kt
        ├── Media3AudioLoopEngine.kt
        ├── focus/
        │   └── NekoyumeAudioFocusManager.kt
        ├── model/
        │   ├── AudioMixState.kt
        │   ├── SoundTrack.kt
        │   └── TrackPlaybackState.kt
        └── service/
            ├── NekoyumePlaybackService.kt       # MediaSessionService with FGS lifecycle (@AndroidEntryPoint)
            ├── MultiStreamPlayerAdapter.kt      # Translates MediaSession commands to AudioLoopEngine
            ├── PlaybackServiceController.kt     # Client-facing interface for observing service & mix state
            ├── PlaybackServiceControllerImpl.kt # Implementation binding to service / engine
            └── ServiceState.kt                  # ServiceState and ServicePlaybackState data models

app/src/test/java/com/bl4ckswordsman/nekoyume/
└── core/audio/service/
    ├── NekoyumePlaybackServiceTest.kt           # Service lifecycle & foreground promotion tests
    ├── MultiStreamPlayerAdapterTest.kt          # MediaSession transport command tests
    ├── PlaybackServiceControllerTest.kt         # Client controller StateFlow synchronization tests
    └── ServiceIdleTimeoutTest.kt                # 15-minute paused idle timeout verification
```

**Structure Decision**: Standard Android single-application structure with modularized packages (`di`, `core/audio`, `core/audio/service`) preserving strict separation of concerns between headless audio processing, foreground platform services, and UI components.

---

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| *None* | N/A | Fully compliant with Constitution principles |
