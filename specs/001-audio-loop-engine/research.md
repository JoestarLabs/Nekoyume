# Phase 0 Research: Core Multi-Stream Audio Looping Engine

**Feature**: `001-audio-loop-engine`  
**Date**: 2026-10-17  
**Status**: Completed  

---

## 1. Multi-Stream Audio Playback Architecture in AndroidX Media3

### Context & Challenge
The audio engine must support concurrent playback of up to 8 independent audio streams (e.g. rain + wind + white noise + distant thunder), each looping seamlessly, with independent real-time volume attenuation and master volume scaling.

### Decision
Implement a pooled multi-player coordinator (`MultiStreamExoPlayerManager`) managing a collection of lightweight AndroidX Media3 `ExoPlayer` instances (up to 8 concurrent active instances). Each active sound track maps to its own `ExoPlayer` instance bound to an underlying Android `AudioTrack`.

### Rationale
- **Hardware Mixing via AudioFlinger**: Android's platform audio server (`AudioFlinger` / HAL) natively mixes multiple concurrent `AudioTrack` outputs in hardware/driver space with negligible CPU overhead (<3-5% for 8 streams).
- **Independent Control**: Each `ExoPlayer` instance independently manages its playback state, buffer replenishment, and track volume via `player.volume = (individualVolume * masterVolume * duckMultiplier)`.
- **Media3 Native Lifecycle**: AndroidX Media3 provides robust codec management, error propagation, and future compatibility with `MediaSessionService` foreground playback.
- **Resource Budgeting**: An 8-player ceiling prevents exceeding platform audio track allocation limits (typically 32 total on modern Android).

### Alternatives Considered
- *Android SoundPool*: SoundPool is lightweight and low-latency, but loads all decompressed PCM audio directly into memory (risking OOM on longer ambient tracks), lacks rich Media3 StateFlow events, and does not integrate cleanly with MediaSession background services.
- *Single ExoPlayer with Custom AudioProcessor / Software Mixer*: Decoding and mixing multiple PCM streams in software before pushing to a single AudioTrack incurs high CPU usage, battery drain, and risk of buffer under-runs.
- *Oboe (C++ AAudio/OpenSL)*: High performance but introduces substantial NDK build complexity, JNI marshaling overhead, and maintenance friction unnecessary for ambient audio.

---

## 2. Seamless Gapless Looping with Micro-Crossfade (30ms)

### Context & Challenge
FR-002 and SC-001 require seamless looping with a 30ms micro-crossfade overlap at loop boundaries to eliminate audible clicks, pops, or silence gaps (>10ms).

### Decision
- Configure each `ExoPlayer` instance with `Player.REPEAT_MODE_ONE`.
- Package ambient sound assets in container formats with gapless metadata (OGG Vorbis and Opus).
- Apply a 30ms micro-fade envelope (ramp-down at track end, ramp-up at track start) to smooth any potential DC-offset or waveform discontinuity across loop transitions.

### Rationale
- Media3 ExoPlayer natively supports sample-accurate gapless playback when `REPEAT_MODE_ONE` is active for OGG/Opus files, pre-buffering the start of the audio timeline before the end is reached.
- The 30ms volume envelope (fading out the final 30ms and fading in the initial 30ms) guarantees that waveform amplitude passes smoothly through zero, completely eliminating transducer click artifacts without altering the ambient texture.

### Alternatives Considered
- *Ping-Pong Players (Dual Players per Track)*: Alternating between two players per track with crossfading would require up to 16 active ExoPlayer instances, doubling file descriptors, memory footprint, and decoder load.
- *Hard Cut (Unprocessed Looping)*: If raw audio cuts do not align at zero-crossings, hard cuts create audible transient clicks on speakers and headphones.

---

## 3. Volume Scrubbing Smoothing (30ms Anti-Zipper Ramp)

### Context & Challenge
FR-003 and SC-004 mandate that volume adjustments apply a 30ms smoothing ramp to avoid digital zipper noise during rapid slider scrubbing.

### Decision
Implement an asynchronous `VolumeRampHandler` within the engine. When a volume adjustment is received:
1. Cancel any active in-flight ramp coroutine for that track.
2. Launch a high-frequency interpolation job on a dedicated `CoroutineDispatcher` (e.g. 6 steps at 5ms intervals over 30ms).
3. Compute the effective volume at each step using linear or perceptual logarithmic scaling:
   $$\text{effectiveVolume} = \text{targetVolume} \times \text{masterVolume} \times \text{duckMultiplier}$$
4. Call `player.setVolume(stepVolume)` at each 5ms tick.

### Rationale
- Discrete volume jumps cause instantaneous step changes in audio amplitude, perceived by human ears as digital zipper noise or clicks.
- A 30ms smooth ramp over 5-6 steps is imperceptible to user touch latency (<50ms perception window) while completely eliminating high-frequency zipper harmonics.

### Alternatives Considered
- *Android VolumeShaper*: Platform API available on API 26+, but tightly coupled to low-level AudioTrack instances and conflicts with ExoPlayer's internal volume handling.
- *Instant setVolume*: Zero computation, but produces jarring zipper noise when users drag sliders quickly.

---

## 4. Local Audio Asset Storage & Catalog Manifest Schema

### Context & Challenge
FR-013 requires assets to be loaded from `assets/sounds/` with a manifest (`sounds.json`) containing metadata for discovery and playback.

### Decision
- Store audio files in `app/src/main/assets/sounds/` (e.g. `rain.ogg`, `forest.ogg`, `white_noise.ogg`, `fire.ogg`, `wind.ogg`, `waves.ogg`, `crickets.ogg`, `thunder.ogg`).
- Define `app/src/main/assets/sounds/sounds.json` catalog manifest containing:
  - `id`: Unique alphanumeric identifier (e.g. `"rain"`).
  - `name`: Human-readable display title (e.g. `"Gentle Rain"`).
  - `category`: Category group (e.g. `"NATURE"`, `"WEATHER"`, `"AMBIENCE"`, `"NOISE"`).
  - `assetPath`: Relative path to file (e.g. `"sounds/rain.ogg"`).
  - `defaultVolume`: Baseline volume recommendation (0.0 to 1.0, e.g. `0.7`).
- Expose via `SoundCatalogRepository` that parses `sounds.json` into immutable domain `SoundTrack` models.

### Rationale
- Manifest-driven catalog decoupling ensures new sounds can be added or categorized without changing engine code.
- Asset loading via `android.content.res.AssetManager` or ExoPlayer's `RawResourceDataSource` / `asset:///` URI scheme is zero-network, reliable, and instant (<150ms startup).

---

## 5. Audio Focus & System Event Handling

### Context & Challenge
FR-006 through FR-009 require graceful audio focus management (transient ducking by ≥60%, permanent pause, and automatic pause on noisy route change).

### Decision
- Encapsulate focus logic in `NekoyumeAudioFocusManager` using `AudioManager` / `AudioFocusRequestCompat` with `AudioAttributesCompat` configured for `USAGE_MEDIA` and `CONTENT_TYPE_MUSIC`.
- On `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`: Apply ducking multiplier (`0.3f`, attenuating volume by 70%) to all playing tracks via the 30ms volume ramp.
- On `AUDIOFOCUS_LOSS` (permanent): Pause all active streams and preserve configuration.
- On `AUDIOFOCUS_GAIN`: Restore duck multiplier to `1.0f` and resume playback if previously playing.
- Register a `BroadcastReceiver` for `AudioManager.ACTION_AUDIO_BECOMING_NOISY`: Automatically trigger `pauseAll()` when headphones or Bluetooth audio disconnects.

### Rationale
- Strict compliance with Android audio etiquette ensures user safety and seamless coexistence with phone calls, alarms, and navigation apps.

---

## 6. Dependency Selection & Architecture Summary

| Component | Choice | Rationale |
| :--- | :--- | :--- |
| **Audio Core** | `androidx.media3:media3-exoplayer:1.5.1` (or latest stable) | Official AndroidX standard for media playback; clean Kotlin API. |
| **Async & State** | Kotlin Coroutines `StateFlow` & `SharedFlow` | Reactive, thread-safe state distribution across ViewModels and UI. |
| **Serialization** | `org.json` (Android built-in) or lightweight parser | Parses `sounds.json` without adding large binary dependencies. |
| **Testing** | JUnit 4, Kotlinx Coroutines Test (`runTest`, `StandardTestDispatcher`), Mockito-Kotlin | Complete unit test coverage for engine math, states, and focus handlers. |
