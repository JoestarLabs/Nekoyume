# Feature Specification: Core Multi-Stream Audio Looping Engine

**Feature Branch**: `001-audio-loop-engine`  
**Created**: 2026-10-17  
**Status**: Draft  
**Input**: User description: "Core multi-stream audio looping engine using AndroidX Media3"  

---

## Clarifications

### Session 2026-10-17
- Q: How should the audio engine handle loop transitions to prevent audible clicks at track boundaries? → A: Micro-crossfade (30ms fade-out/fade-in overlap at loop boundaries).
- Q: Where should local packaged sound effect assets reside and be discovered by the engine? → A: Manifest-driven `assets/sounds/` defined and registered via a local JSON manifest (`sounds.json`) containing metadata.
- Q: How should the audio engine apply volume changes when a sound track's volume level is adjusted? → A: Smoothed volume ramp (30ms curve to eliminate zipper noise during scrubbing).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Concurrent Ambient Sound Playback & Seamless Looping (Priority: P1)

As a listener seeking relaxation or deep focus, I want to activate one or more ambient sound tracks (such as rain, white noise, or forest sounds) so that they play simultaneously and repeat seamlessly without audible gaps, pops, or stutters at loop boundaries.

**Why this priority**: Concurrent, gapless looping is the fundamental core value of a white noise application. If sounds stutter, skip, or cannot play in parallel, the app fails its primary job of masking environmental distractions.

**Independent Test**: Can be tested headlessly by requesting two distinct audio tracks to play simultaneously, verifying that both output audio concurrently, and observing that when either track reaches its end, it immediately loops back to the beginning without click artifacts or silence gaps exceeding human audio perception thresholds (<10ms).

**Acceptance Scenarios**:
1. **Given** the audio engine is idle, **When** the user starts playback of a single ambient sound track, **Then** the sound begins playing and continuously loops indefinitely until stopped.
2. **Given** one ambient sound track is actively playing, **When** the user starts a second (or third, up to 8) sound track, **Then** all active tracks play simultaneously in parallel.
3. **Given** multiple tracks are playing, **When** any individual track reaches the end of its audio file duration, **Then** that track restarts from the beginning without audible disruption, while other concurrently running tracks continue uninterrupted.

---

### User Story 2 - Independent and Master Volume Attenuation (Priority: P2)

As a listener creating a customized soundscape, I want to adjust the volume level of each sound track independently, as well as control master playback volume, so that I can balance loud elements (e.g. heavy thunderstorm) against delicate sounds (e.g. distant wind chimes).

**Why this priority**: Effective sound masking depends on personal auditory comfort and track balance; static unadjustable levels cause ear fatigue.

**Independent Test**: Can be tested by starting two tracks, adjusting Track 1's volume to 20% and Track 2's volume to 80%, and asserting that real-time volume multipliers reflect those settings without causing digital audio clipping or sudden jumps.

**Acceptance Scenarios**:
1. **Given** an active sound track, **When** its volume slider is adjusted between 0% and 100%, **Then** the audio output level of that specific track changes smoothly in real time without affecting the volume of other playing tracks.
2. **Given** multiple active tracks with varied individual volumes, **When** the master volume is adjusted, **Then** all tracks scale proportionally relative to their individual levels.
3. **Given** multiple active tracks, **When** master playback is paused or muted, **Then** all tracks pause/mute simultaneously while preserving their relative volume configuration.

---

### User Story 3 - Audio Focus & System Event Handling (Priority: P3)

As a mobile user, I want the white noise playback to respond sensibly when external system audio events occur (e.g., incoming phone call, alarm, or navigation turn-by-turn prompt) so that important calls and alerts are never missed.

**Why this priority**: Android audio hygiene ensures the app behaves like a good platform citizen rather than blaring over phone conversations or emergency sirens.

**Independent Test**: Can be tested by simulating transient and permanent audio focus changes, verifying that permanent loss pauses all tracks, and transient loss with ducking lowers volume temporarily before restoring to prior levels.

**Acceptance Scenarios**:
1. **Given** ambient sounds are playing, **When** an incoming phone call or permanent audio focus loss occurs, **Then** playback automatically pauses and holds state.
2. **Given** ambient sounds are playing, **When** a transient audio alert occurs that permits ducking, **Then** playback volume lowers smoothly to an attenuated level and automatically returns to normal volume once the transient alert concludes.
3. **Given** playback was paused due to a transient phone call or voice assistant interaction, **When** the external event ends, **Then** playback resumes seamlessly if configured to auto-resume.

---

### Edge Cases

- **Missing or Corrupted Audio Asset**: If an audio file resource cannot be resolved or is corrupted, the engine must flag that specific track with an error status and notify the caller without crashing or halting other playing tracks.
- **Maximum Concurrent Track Capacity**: Attempting to start more tracks than the platform or configured concurrency ceiling (8 concurrent streams) must either reject the addition with an informative status or notify the user rather than exhausting audio hardware channels.
- **Rapid Toggle / Debounce**: Rapidly clicking play/pause or toggling tracks in quick succession (<100ms) must not leak audio player instances or create runaway background threads.
- **Audio Output Route Change**: When headphones/Bluetooth are disconnected (becoming "noisy"), playback must pause automatically to prevent unintended speaker broadcast.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST support simultaneous playback of up to 8 independent audio streams.
- **FR-002**: Each active stream MUST loop continuously from end-to-beginning applying a 30ms micro-crossfade overlap at boundaries to prevent audible clicks.
- **FR-003**: The system MUST allow real-time volume adjustment for each stream independently on a linear 0.0 to 1.0 (0% to 100%) scale, applying a 30ms smoothing ramp to prevent digital zipper noise.
- **FR-004**: The system MUST provide master playback controls: Play All, Pause All, Stop All, and Master Volume (0.0 to 1.0).
- **FR-005**: The system MUST expose real-time playback state for each track (Idle, Buffering, Playing, Paused, Error) via asynchronous streams.
- **FR-006**: The system MUST properly acquire, monitor, and release platform Audio Focus according to modern Android standards.
- **FR-007**: When Audio Focus is lost permanently, the system MUST pause all active streams and preserve the mix configuration.
- **FR-008**: When Audio Focus is lost transiently with ducking allowed, the system MUST temporarily attenuate volume by at least 60% and restore upon focus regain.
- **FR-009**: The system MUST automatically pause playback when the active audio output routing becomes noisy (e.g., headphone unplugged or Bluetooth disconnected).
- **FR-010**: The system MUST support standard ambient audio formats including OGG Vorbis, Opus, MP3, and WAV.
- **FR-011**: The system MUST release all underlying audio decoders and hardware buffers when playback is stopped or the engine is torn down.
- **FR-012**: The engine MUST be architected to run headless or attach seamlessly to a future foreground media service without coupling to UI components.
- **FR-013**: Sound tracks MUST be loaded from application assets (`assets/sounds/`) and registered via a structured JSON catalog manifest (`sounds.json`) containing track identifiers, display names, categories, and relative file paths.

---

### Key Entities

- **Sound Track**: Represents an individual sound element cataloged in the asset manifest (identifier, display title, category, asset path, default volume).
- **Track Playback State**: The runtime state of an individual track (Track ID, playback state [IDLE/LOADING/PLAYING/PAUSED/ERROR], track volume [0.0..1.0], isMuted).
- **Active Audio Mix**: The aggregate runtime state of the audio engine (list of active tracks, master volume [0.0..1.0], master state [PLAYING/PAUSED/STOPPED], audio focus state).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Audio track looping exhibits zero perceptible pause or audio discontinuity at loop boundaries (loop transition latency < 10ms).
- **SC-002**: Playback initiates within 150 milliseconds of user command for cached or packaged local sound assets.
- **SC-003**: Engine sustains 8 simultaneous ambient audio streams without CPU spikes exceeding 5% on baseline Android reference hardware.
- **SC-004**: Volume attenuation changes take effect within 50 milliseconds of user adjustment and complete a 30ms smoothing ramp without audible audio clicking, zipper noise, or distortion.
- **SC-005**: 100% of simulated audio focus interruptions (calls, alerts, noisy disconnections) transition playback state correctly and recover without resource leaks.

---

## Assumptions

- Audio assets are stored locally within the application package (`assets/sounds/`) and registered via `sounds.json` for this core phase; remote streaming is out of scope.
- Platform target is modern Android (SDK 29+ minimum, compiled against modern SDK 37 standards).
- Visual UI components (Expressive buttons, squiggly sliders) will consume this engine in Feature 002; this feature delivers the verified headless audio engine and mixer API.
- Up to 8 concurrent tracks is sufficient to cover realistic ambient sound mixing (e.g., rain + wind + fire + river + night crickets).
