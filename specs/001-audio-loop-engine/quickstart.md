# Quickstart: Core Multi-Stream Audio Looping Engine

**Feature**: `001-audio-loop-engine`  
**Date**: 2026-10-17  
**Status**: Completed  

---

## 1. Prerequisites

- **JDK**: Java 17 (configured via `gradle/gradle-daemon-jvm.properties`)
- **Android SDK**: `compileSdk = 37`, `minSdk = 29`, `targetSdk = 35`
- **Build Tool**: Gradle 9.8.1 wrapper (`./gradlew`)
- **Dependencies**: `androidx.media3:media3-exoplayer` added to `gradle/libs.versions.toml`

---

## 2. Validation Scenarios

### Scenario 1: Sound Catalog Manifest Loading
Validates that `sounds.json` is located in `assets/sounds/` and successfully deserialized into domain `SoundTrack` entities.

- **Contract Reference**: [SoundCatalogRepository](contracts/catalog-repository-contract.md)
- **Command**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.bl4ckswordsman.nekoyume.core.audio.repository.AssetSoundCatalogRepositoryTest"
  ```
- **Expected Outcome**:
  - All test cases pass (`BUILD SUCCESSFUL`).
  - Catalog correctly loads bundled sounds (e.g. `rain`, `forest`, `white_noise`).
  - Missing manifest or corrupted JSON fails gracefully with `Result.failure`.

---

### Scenario 2: Concurrent Multi-Stream Audio Playback & Capacity Ceiling
Validates that up to 8 streams play in parallel, and attempting to start a 9th stream returns an error without crashing.

- **Contract Reference**: [AudioLoopEngine](contracts/audio-engine-contract.md)
- **Command**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.bl4ckswordsman.nekoyume.core.audio.AudioLoopEngineConcurrencyTest"
  ```
- **Expected Outcome**:
  - Engine accepts tracks 1 through 8.
  - Mix state emits `playingTrackCount == 8`.
  - 9th track invocation returns `Result.failure` with `MaxTracksExceededException`.
  - Stopping a track frees a slot allowing a new track to start.

---

### Scenario 3: Volume Ramping & Master Attenuation Math
Validates that independent track volumes scale proportionally by master volume, and volume adjustments execute a 30ms smoothing ramp to avoid audio clicking.

- **Data Model Reference**: [Volume Attenuation Formula](data-model.md#22-volume-attenuation-formula)
- **Command**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.bl4ckswordsman.nekoyume.core.audio.VolumeRampHandlerTest"
  ```
- **Expected Outcome**:
  - Volume transitions from start to target in 30ms across 6 interpolated steps.
  - Rapid scrubbing cancels prior ramps without memory leaks.
  - Master volume scaling correctly applies to individual tracks.

---

### Scenario 4: Audio Focus Lifecycle & Ducking
Validates that transient audio focus loss attenuates volume by ≥60% (`duckMultiplier = 0.3f`), permanent loss pauses playback, and headphone disconnection triggers `pauseAll()`.

- **Contract Reference**: [Audio Focus Etiquette](contracts/audio-engine-contract.md#2-invariants--guarantees)
- **Command**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.bl4ckswordsman.nekoyume.core.audio.AudioFocusManagerTest"
  ```
- **Expected Outcome**:
  - Focus loss transient (duck) reduces output volume smoothly.
  - Focus gain restores pre-duck volume level.
  - Broadcast of `ACTION_AUDIO_BECOMING_NOISY` transitions state to paused.

---

## 3. Full Verification Suite

Run all verification checks to ensure quality gate compliance before merging:

```bash
# 1. Run all unit tests
./gradlew testDebugUnitTest

# 2. Run Android lint checks
./gradlew lintDebug
```
