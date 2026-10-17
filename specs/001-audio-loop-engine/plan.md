# Implementation Plan: Core Multi-Stream Audio Looping Engine

**Branch**: `001-audio-loop-engine` | **Date**: 2026-10-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-audio-loop-engine/spec.md`

---

## Summary

Deliver a headless, multi-stream audio playback and looping engine built on AndroidX Media3 (ExoPlayer). The engine manages a pool of up to 8 concurrent looping audio streams with sample-accurate gapless repetition, 30ms micro-crossfade transitions, independent and master volume attenuation with 30ms anti-zipper ramping, Android platform audio focus ducking and interruption management, and a manifest-driven asset catalog (`assets/sounds/sounds.json`).

---

## Technical Context

**Language/Version**: Kotlin 2.4.21, JVM 17  
**Primary Dependencies**: `androidx.media3:media3-exoplayer:1.5.1`, `androidx.core:core-ktx:1.19.1`, `org.jetbrains.kotlinx:kotlinx-coroutines-android`  
**Storage**: Android `assets/sounds/` local packaged audio files (`.ogg`, `.opus`, `.mp3`, `.wav`) and `sounds.json` catalog manifest  
**Testing**: JUnit 4, Kotlinx Coroutines Test (`runTest`, `StandardTestDispatcher`), Mockito-Kotlin, AndroidX Test  
**Target Platform**: Android 10+ (`compileSdk = 37`, `minSdk = 29`, `targetSdk = 35`)  
**Project Type**: Mobile Application (Headless audio playback engine and domain layer)  
**Performance Goals**:
- Loop transition latency < 10ms (imperceptible gapless transition).
- Playback startup latency < 150ms for packaged sound assets.
- 8 simultaneous active audio streams consuming < 5% CPU on baseline Android reference hardware.
- Volume adjustments take effect within 50ms and complete a 30ms smoothing ramp.  
**Constraints**:
- Offline-capable (bundled local assets; zero network requirement).
- Headless execution completely decoupled from UI and Activities, ready to bind to future `MediaSessionService`.
- 8 concurrent audio streams maximum ceiling to protect system hardware channel limits.  
**Scale/Scope**: 8 simultaneous active streams, initial bundled catalog of 6–10 ambient soundscapes.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Compliance Verification |
| :--- | :---: | :--- |
| **I. Declarative UI & Material 3 Expressive** | **PASS (N/A)** | Feature 001 delivers the headless audio core and domain model. UI components (Expressive sound cards and squiggly sliders) will consume this engine in Feature 002. |
| **II. Unidirectional Data Flow & MVVM** | **PASS** | `AudioLoopEngine` exposes aggregate state via read-only `StateFlow<AudioMixState>` and track states via `Flow<TrackPlaybackState?>`. ViewModels in Feature 002 will observe these flows without direct player references. |
| **III. Multi-Stream Audio Playback Engine** | **PASS** | Built on AndroidX Media3 ExoPlayer pool with hardware mixing via AudioFlinger; supports 8 concurrent streams; 30ms micro-crossfade gapless loops; 30ms volume smoothing ramp; full Audio Focus ducking (≥60%) and pause. |
| **IV. Clean Foreground Service Boundaries** | **PASS** | Engine is architected as an independent domain singleton with lifecycle methods (`pauseAll`, `resumeAll`, `release`), completely ready to be driven by `MediaSessionService` in Feature 003. |
| **V. Dependency Injection via Hilt** | **PASS** | Engine components (`AudioLoopEngine`, `SoundCatalogRepository`, `NekoyumeAudioFocusManager`) are designed as modular interfaces ready for `@Singleton` injection bindings. |
| **VI. Test-Driven Verification** | **PASS** | Verified via automated unit tests runnable with `./gradlew testDebugUnitTest` covering volume ramps, concurrency limits, catalog parsing, and focus ducking before merging. |

---

## Project Structure

### Documentation (this feature)

```text
specs/001-audio-loop-engine/
├── spec.md                  # Feature specification with clarifications
├── plan.md                  # This implementation plan
├── research.md              # Phase 0: Architectural research & decisions
├── data-model.md            # Phase 1: Entity models, state machines & formulas
├── quickstart.md            # Phase 1: Test & verification guide
├── contracts/               # Phase 1: Interface & schema contracts
│   ├── audio-engine-contract.md
│   ├── catalog-repository-contract.md
│   └── manifest-schema.json
├── checklists/
│   └── requirements.md      # Specification quality checklist (16/16 passing)
└── tasks.md                 # Phase 2: Implementation tasks (/speckit-tasks output)
```

### Source Code (repository root)

```text
app/src/main/
├── assets/
│   └── sounds/
│       ├── sounds.json      # Packaged catalog manifest
│       ├── rain.ogg         # Bundled ambient sound samples
│       ├── forest.ogg
│       ├── white_noise.ogg
│       └── ...
└── java/com/bl4ckswordsman/nekoyume/
    └── core/
        └── audio/
            ├── model/
            │   ├── AudioMixState.kt
            │   ├── SoundCategory.kt
            │   ├── SoundTrack.kt
            │   └── TrackPlaybackState.kt
            ├── repository/
            │   ├── AssetSoundCatalogRepository.kt
            │   └── SoundCatalogRepository.kt
            ├── focus/
            │   ├── AudioFocusState.kt
            │   └── NekoyumeAudioFocusManager.kt
            ├── ramp/
            │   └── VolumeRampHandler.kt
            ├── AudioLoopEngine.kt
            └── Media3AudioLoopEngine.kt

app/src/test/java/com/bl4ckswordsman/nekoyume/
└── core/
    └── audio/
        ├── AudioFocusManagerTest.kt
        ├── AudioLoopEngineConcurrencyTest.kt
        ├── VolumeRampHandlerTest.kt
        └── repository/
            └── AssetSoundCatalogRepositoryTest.kt
```

**Structure Decision**: Standard Android single-application module structure with a clean `core.audio` package separating domain models, catalog repository, audio focus manager, volume ramp calculator, and Media3 engine implementation.

---

## Complexity Tracking

> **Constitution check fully passes without violations. No complexity exceptions required.**
