# Feature Specification: Continuous Background Audio Service & Dependency Injection

**Feature Branch**: `002-foreground-audio-service`  
**Created**: 2026-10-17  
**Status**: Draft  
**Input**: Continuous background audio playback via AndroidX MediaSessionService and Google Hilt dependency injection  

---

## Clarifications

### Session 2026-10-17
- Q: When the user clears or swipes away the application from the Android recent apps list while audio is actively playing, how should the background playback service behave? → A: Continue playing audio uninterrupted with the foreground service and notification active until explicitly paused or stopped by the user.
- Q: If the Android system terminates the playback service under extreme memory pressure while audio is playing, how should the service behave upon process revival? → A: Restore the mix in a paused state with notification available for immediate one-tap resumption.
- Q: How should the background playback service handle transient audio focus loss, such as system navigation prompts or notification sounds? → A: Duck (smoothly lower) volume during transient focus loss; pause completely on permanent focus loss.
- Q: Is an automatic sleep timer (e.g., countdown to auto-stop playback after a set duration) in scope for this feature or deferred to a subsequent feature? → A: Out of scope; defer sleep timer functionality to a dedicated future feature.
- Q: When audio playback is paused, should the background service automatically shut down after an idle timeout to prevent battery drain? → A: Automatically stop the service and dismiss foreground notification after 15 minutes of inactivity while paused.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Continuous Background Playback During Screen-Off & Multitasking (Priority: P1)

As a listener using ambient soundscapes for sleeping or focused work, I want audio playback to continue uninterrupted when I turn off my phone's screen, lock the device, or switch to other applications, so that my sleep or concentration is never broken by premature silence.

**Why this priority**: Ambient sound masking is primarily used while sleeping (screen off for 6–8 hours) or multitasking (reading, working, meditating). Without background service execution, the operating system pauses or terminates playback as soon as the app leaves the foreground, making the app practically unusable for its core purpose.

**Independent Test**: Start playback of an ambient sound mix, lock the device screen or switch to another heavy app (e.g., browser/camera), and verify that multi-stream audio continues to loop seamlessly without degradation, stutter, or premature termination for at least 30 minutes.

**Acceptance Scenarios**:
1. **Given** one or more ambient audio streams are playing, **When** the user turns off the screen or the screen times out, **Then** playback continues smoothly and uninterrupted.
2. **Given** ambient playback is active, **When** the user navigates to the home screen or opens another application, **Then** playback persists in the background.
3. **Given** the device enters system power-saving or low-power standby modes, **When** audio is actively playing, **Then** the audio service holds necessary system execution locks to prevent sleep-induced playback stalls.

---

### User Story 2 - System Media Notification & Lock Screen Transport Controls (Priority: P2)

As a user listening to ambient sounds while my phone is locked or in my pocket, I want a rich media notification on the lock screen and notification shade with instant transport controls (play, pause, stop), so that I can control my soundscape without needing to unlock the phone and reopen the application.

**Why this priority**: Quick, accessible controls are essential for convenient everyday usage—especially when waking up at night or receiving an in-person interruption.

**Independent Test**: Start playback and verify that a system media notification appears displaying the app identity, active playback summary (e.g., "Playing 3 sounds"), and interactive controls. Toggling pause from the notification halts audio and changes the icon to play; tapping play resumes audio immediately.

**Acceptance Scenarios**:
1. **Given** playback has started, **When** the user checks the notification shade or lock screen, **Then** a media notification is present showing the current soundscape state and playback controls.
2. **Given** active playback in the background, **When** the user taps the pause button on the media notification, **Then** all active sound streams pause smoothly, and the notification updates to show a play action.
3. **Given** playback is paused from the notification, **When** the user taps the play button, **Then** playback resumes with the previous track mix and volume configuration intact.
4. **Given** playback is active or paused, **When** the user taps the notification body, **Then** the application opens to the main screen.

---

### User Story 3 - Graceful Session Lifecycle, Service Tear-Down & Battery Preservation (Priority: P3)

As a battery-conscious mobile user, I want the background audio service and system notification to cleanly shut down and release all system resources when I stop playback or dismiss the soundscape, so that background processes do not drain battery or keep notifications pinned indefinitely.

**Why this priority**: Resource hygiene prevents battery drain, memory leaks, and user frustration with persistent, un-dismissible notifications.

**Independent Test**: Pause playback and tap "Stop" or swipe away the paused notification, verifying that the background service terminates, hardware audio channels close, and all foreground notification flags are cleared.

**Acceptance Scenarios**:
1. **Given** playback is paused, **When** the user dismisses the notification or triggers a stop command, **Then** the background audio service stops, removes the foreground notification, and releases all media resources.
2. **Given** the application is swiped away from the system Recents task manager while audio is actively playing, **When** the task is removed, **Then** playback continues uninterrupted with the foreground service and notification remaining active.
3. **Given** the application is explicitly swiped away from the system Recents task manager while playback is stopped, **When** the system cleans up the task, **Then** no orphaned service processes remain running in the background.
4. **Given** playback is active and the system experiences critical memory pressure, **When** the background service must manage its memory footprint, **Then** all internal allocations remain bounded and non-essential caches are trimmed.
5. **Given** playback is paused, **When** no user interaction occurs for 15 consecutive minutes, **Then** the service automatically stops, dismisses the foreground notification, and releases all audio resources.

---

### Edge Cases

- **Notification Permission Denied (Android 13+)**: If the user denies or revokes the notification runtime permission, background audio must still function, but the user should be gracefully notified that lock screen controls will be unavailable.
- **App Task Removal During Playback**: Swiping away the application from the Android Recents task manager while audio is actively playing does not stop playback; the service remains running in the foreground with notification controls intact.
- **System Memory Pressure (Low Memory Killer)**: If the system kills the foreground service under severe memory constraints, the last mix configuration is preserved; upon process revival, the service restores the mix in a paused state with the media notification ready for immediate one-tap resumption, avoiding unexpected audio playback.
- **Audio Becoming Noisy**: When headphones are unplugged or Bluetooth disconnects while the screen is off, the service must immediately pause playback and update the notification to reflect the paused state.
- **Audio Focus Transitions**: When system audio focus is lost transiently (e.g., turn-by-turn navigation prompt or notification chime), the service MUST smoothly duck ambient volume and restore it once focus is regained; when focus is lost permanently (e.g., incoming phone call or external media app playback), the service MUST pause playback immediately.
- **Rapid Notification Scrubbing**: Rapidly tapping play/pause on the lock screen widget must debounce cleanly without spawning overlapping player instances or corrupting the playback state machine.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST host audio playback inside a dedicated background service complying with modern platform media playback specifications.
- **FR-002**: The background service MUST be promoted to a foreground service with the media playback type whenever one or more audio streams are actively playing.
- **FR-003**: The system MUST publish and maintain an ongoing system media notification while the service is in the foreground.
- **FR-004**: The media notification MUST provide actionable transport controls: Play, Pause, and Stop.
- **FR-005**: The media notification MUST dynamically reflect the active soundscape state, including track count and playback status.
- **FR-006**: Tapping the media notification MUST navigate the user back to the application's primary interface.
- **FR-007**: When playback is paused, the media notification MUST become dismissible, allowing the user to swipe it away to stop the service.
- **FR-008**: Stopping playback from the notification or application MUST demote the service from foreground status and release all audio hardware locks.
- **FR-009**: The system MUST establish clean dependency injection bindings for all audio core components (`AudioLoopEngine`, `SoundCatalogRepository`, audio focus managers) scoped as singletons.
- **FR-010**: All UI components, ViewModels, and Services MUST receive their dependencies through dependency injection without manual service locators or static singletons.
- **FR-011**: The system MUST handle device sleep and CPU power management so background playback does not stall or drop samples when the display is powered off.
- **FR-012**: The background service MUST synchronize state bi-directionally with the core audio loop engine and expose reactive state updates to connected clients.
- **FR-013**: Swiping the application away from the Android Recent tasks list while audio is actively playing MUST NOT terminate playback; the foreground service MUST continue playing uninterrupted until explicitly paused or stopped by the user.
- **FR-014**: If the background service is recreated after being terminated due to system memory pressure, it MUST restore the saved mix state in a paused condition and publish a media notification ready for user resumption.
- **FR-015**: The system MUST manage Android audio focus transitions, smoothly ducking ambient playback volume during transient focus loss and pausing playback immediately upon permanent focus loss.
- **FR-016**: The background service MUST implement an automatic idle timeout that stops the service, dismisses the foreground notification, and releases audio resources after 15 minutes of continuous inactivity in the paused state.

---

### Key Entities

- **Media Session**: The platform bridge exposing active soundscape metadata and accepting remote transport commands from notifications, lock screens, and Bluetooth devices.
- **Media Notification**: The system-level UI component representing the active audio service in the notification drawer and lock screen.
- **Playback Service**: The long-running Android service component managing the audio engine lifecycle, foreground promotion, and system wakefulness.
- **Service Connection/Controller**: The client-side interface utilized by UI components and ViewModels to communicate commands and observe state from the playback service.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Background audio playback sustains continuous, gapless looping for 8+ continuous hours with the screen turned off without audio drops or process termination.
- **SC-002**: Transport control actions triggered from the lock screen notification (play, pause, stop) execute within 100 milliseconds.
- **SC-003**: The background audio service consumes less than 1.5% battery per hour during continuous screen-off multi-stream playback.
- **SC-004**: When playback is stopped, dismissed, or idle timeout (15 minutes) expires while paused, 100% of background service instances, notification banners, and audio wake locks are cleanly released within 1 second.
- **SC-005**: Dependency injection graph resolves completely at application startup in under 50 milliseconds without circular dependencies or runtime initialization errors.

---

## Assumptions

- The target platform is Android 10+ (API 29+), with explicit support for Android 13+ notification permissions and Android 14+ foreground service types.
- The playback service integrates directly with the headless `AudioLoopEngine` implemented in Feature 001.
- Bluetooth / media button hardware key events (e.g., headphone click to pause) will be mediated through the platform MediaSession.
- Automatic sleep timer functionality (e.g., countdown timer to auto-stop playback after a specified duration) is explicitly out of scope for Feature 002 and deferred to a dedicated future feature.
