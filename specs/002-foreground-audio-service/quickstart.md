# Quickstart: Continuous Background Audio Service & Dependency Injection

**Feature**: `002-foreground-audio-service`  
**Date**: 2026-10-17  
**Status**: Ready for Validation  

This guide provides step-by-step instructions and test scenarios to validate that the background audio playback service, Hilt dependency injection, system media notification, and lifecycle transitions work end-to-end.

---

## 1. Prerequisites & Environment Setup

- **Android SDK**: `compileSdk = 37`, `minSdk = 29`, `targetSdk = 35`
- **JDK**: Java 17
- **Test Device / Emulator**: Running Android 10+ (API 29+) or Android 14+ (API 34+) to test foreground service types and notification permissions.

Ensure project compiles and passes static analysis:
```bash
./gradlew lintDebug
./gradlew testDebugUnitTest
```

---

## 2. Automated Test Verification

Execute all unit and integration tests covering the service lifecycle, audio focus ducking, and Hilt graph resolution:

```bash
./gradlew testDebugUnitTest --tests "com.bl4ckswordsman.nekoyume.*"
```

### Expected Outcomes:
- **Hilt Dependency Injection**: Injection graph builds cleanly without circular dependencies or runtime crashes (< 50ms startup time, SC-005).
- **Service State Transitions**: Unit tests verify transitions between `STOPPED`, `PLAYING`, and `PAUSED`.
- **Audio Focus Integration**: Verifies ducking to 20% on transient focus loss and pausing on permanent focus loss.
- **Idle Timeout**: Verifies that 15 minutes of inactivity while paused triggers shutdown.

---

## 3. Manual End-to-End Scenarios

### Scenario 1: Continuous Background Playback During Screen-Off (US-1 / SC-001)
1. Launch Nekoyume on a physical device or emulator.
2. Toggle on one or more ambient sound streams (e.g. Rain, River) and tap Play.
3. Lock the device (power button) or switch to another heavy app (e.g., Chrome).
4. **Verification**: Audio continues playing smoothly and loop seamlessly without stutter or termination.

### Scenario 2: Lock Screen & Notification Transport Controls (US-2 / SC-002)
1. While audio is playing, swipe down the Android notification shade or view the lock screen.
2. Confirm the media notification appears displaying:
   - App title ("Nekoyume Soundscape")
   - Active playback summary (e.g., "Playing 2 sounds")
   - Play/Pause and Stop transport controls.
3. Tap **Pause**: Audio stops smoothly with volume ramp; icon switches to Play.
4. Tap **Play**: Audio resumes immediately (< 100ms) with previous mix configuration intact.
5. Tap notification body: App opens to the main screen.

### Scenario 3: Recents Task Swiping Behavior (FR-013 / Clarification Q1)
1. While audio is actively playing, open Android Recent Apps and swipe away Nekoyume.
2. **Verification**: Playback continues uninterrupted in the background; foreground service and notification remain active.
3. Pause playback and swipe away Nekoyume: Service terminates and leaves no orphaned background processes.

### Scenario 4: Paused Inactivity Idle Timeout (FR-016 / SC-004 / Clarification Q5)
1. Pause playback from the notification or app.
2. Leave the device idle for 15 minutes.
3. **Verification**: Service automatically terminates, dismisses the media notification, and releases all system audio resources.

### Scenario 5: Audio Becoming Noisy (Edge Case)
1. While playing through headphones or Bluetooth, disconnect the audio device.
2. **Verification**: Playback pauses immediately, preventing audio from blasting through external speakers.
