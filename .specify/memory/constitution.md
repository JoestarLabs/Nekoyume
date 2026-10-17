# Nekoyume Constitution

## Core Principles

### I. Declarative UI & Material 3 Expressive
All user interface components MUST be built using Jetpack Compose and adhere to Material 3 Expressive guidelines. The UI experience is centered on:
- Big, expressive sound toggle cards with dynamic shape morphing upon selection.
- Sliders for individual sound track volume control that visually transform into animated squiggly loading/playing indicators while maintaining smooth touch interaction.
- Composables MUST be stateless by default or hoist state to ViewModels.
- Composables MUST NOT execute side-effects directly in composition; side-effects MUST be managed via Compose effect handlers (`LaunchedEffect`, `rememberCoroutineScope`, `DisposableEffect`). Reusable UI components MUST include interactive previews where practical.

### II. Unidirectional Data Flow & MVVM
The application MUST follow unidirectional data flow (UDF). ViewModels MUST expose UI state via read-only `StateFlow` and process user actions via explicit methods. Repositories and Audio Managers MUST encapsulate playback and persistence data sources, exposing asynchronous state streams using Kotlin Coroutines `Flow`. ViewModels MUST NOT hold direct references to Android Views, Activities, or context objects (except Application context when strictly necessary and mediated via DI).

### III. Multi-Stream Audio Playback Engine
The audio system MUST provide reliable, concurrent playback of multiple ambient sound loops:
- Built on AndroidX Media3 (ExoPlayer) or dedicated low-latency looping audio pools.
- Support real-time independent volume attenuation and master volume control.
- Implement seamless gapless looping.
- Handle audio focus changes gracefully (pausing or ducking volume during phone calls, navigation prompts, or external audio events).

### IV. Clean Foreground Service Boundaries
Continuous background audio playback MUST be decoupled from UI activities into an AndroidX `MediaSessionService` foreground service:
- Strictly adhere to Android 14+ (API 34+) `mediaPlayback` foreground service types and runtime notification permissions.
- Provide responsive system media notifications with play/pause and track controls.
- Handle system audio session lifecycle, device sleep, and battery optimization gracefully.

### V. Dependency Injection via Hilt
All dependencies across Activities, ViewModels, Repositories, and Services MUST be provided via Google Hilt:
- Manual service locator patterns, global mutable singletons, or ad-hoc constructor instantiation of managed dependencies are prohibited.
- Injection bindings MUST be scoped appropriately (`@Singleton`, `@ViewModelScoped`, etc.) to prevent memory leaks and ensure testability.

### VI. Test-Driven Verification
All audio logic, state transitions, and data operations MUST be covered by automated tests before merging to mainline branches:
- Unit tests (`./gradlew testDebugUnitTest`) MUST validate audio mixer calculations, volume curves, state transitions, and ViewModels in isolation using JUnit, Mockito-Kotlin, and Coroutines test dispatchers.
- Instrumented tests (`./gradlew connectedDebugAndroidTest`) MUST validate end-to-end user workflows and Compose UI interactions against supported Android API targets.
- Every new feature or bug fix MUST include corresponding unit or instrumentation tests demonstrating expected behavior.

### VII. Code Excellence: SoC, DRY, Readability & Efficiency
All application code MUST adhere to strict software craftsmanship, architectural cleanliness, and performance standards:
- **Separation of Concerns (SoC) & Single Responsibility (SRP)**:
  - Core domain models MUST remain pure Kotlin data classes with zero dependencies on Android framework or UI classes.
  - Distinct responsibilities—such as audio focus management, volume curve interpolation, asset catalog parsing, and player coordination—MUST be encapsulated in dedicated, single-purpose classes and exposed via clear interfaces.
- **Don't Repeat Yourself (DRY) & Single Source of Truth**:
  - The aggregate mix state (`AudioMixState`) MUST serve as the sole source of truth for runtime playback; duplicating state across disconnected local variables is prohibited.
  - Shared domain formulas (such as volume attenuation and decibel scaling) MUST be centralized in reusable utility functions or domain extensions.
- **Performance & Resource Hygiene**:
  - Audio mixing MUST leverage native hardware offload (`AudioFlinger`), avoiding redundant CPU-bound software PCM buffer manipulations.
  - Under no circumstances may media players, decoders, or hardware audio channels leak; all underlying resources MUST be explicitly and promptly released when stopped or torn down.
  - High-frequency asynchronous operations (such as volume scrubbing ramps) MUST debounce and cancel preceding in-flight coroutines immediately to avoid wasted CPU cycles.
- **Modern Kotlin Idioms & Readability**:
  - Code MUST prefer immutability (`val`, immutable collections) by default.
  - Public interfaces, functions, and state flows MUST be documented with clear KDoc comments specifying units, ranges, and invariants.
  - Variable and function names MUST be self-descriptive and expressive, avoiding cryptic abbreviations or magic numbers.

## Technical Constraints & Platform Boundaries

- **Target Platform**: The codebase MUST target Android with `compileSdk = 37`, `minSdk = 29`, and `targetSdk = 35`, compiled using JDK 17 with modern AGP and Kotlin.
- **R8 / ProGuard Minification**: Release builds enforce code and resource shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`). Any reflection, serialization, or third-party libraries requiring keep rules MUST be documented and explicitly retained in `proguard-rules.pro`.
- **Packaging & Dependency Governance**: Library dependencies MUST be cataloged in `gradle/libs.versions.toml` and adhere to modern AndroidX and Kotlin best practices.

## Quality Gates & Verification Workflow

- **Static Analysis & Linting**: All builds MUST pass `./gradlew lintDebug` without unresolved errors before PR integration.
- **Commit Standards**: Commits MUST follow Conventional Commits formatting (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`) to ensure seamless automation with Release Please.
- **Continuous Integration**: The CI pipeline (GitHub Actions) MUST pass linting, automated unit tests, and build checks for every pull request and push to `master`.

## Governance

This Constitution supersedes informal agreements and ad-hoc practices. Every architectural proposal, feature specification, and code contribution MUST comply with the principles and constraints established herein.

- **Amendments**: Modifications to this constitution MUST be proposed via pull request, documented in `.specify/memory/constitution.md`, and accompanied by a semantic version increment.
- **Versioning Policy**:
  - **MAJOR** version bumps occur when principles are removed or fundamentally redefined.
  - **MINOR** version bumps occur when new principles, sections, or materially expanded architectural guidance are added.
  - **PATCH** version bumps occur for non-semantic refinements, typos, or wording clarifications.
- **Compliance**: Code reviews and Spec Kit analysis (`speckit-analyze`, `speckit-converge`) MUST verify compliance against this active document.

**Version**: 1.1.0 | **Ratified**: 2026-10-17 | **Last Amended**: 2026-10-17
