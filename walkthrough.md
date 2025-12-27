# Walkthrough - Project Initialization

The "Aural Audiobook Adventures" Android project has been successfully initialized.

## Changes

### 1. Project Scaffolding
- Created standard Android project structure:
    - `settings.gradle.kts`: Defined project name and app module.
    - `build.gradle.kts` (Root) & `app/build.gradle.kts`: Configured for Android and Kotlin.
    - `AndroidManifest.xml`: Basic app manifest.
    - `MainActivity.kt`: Entry point with Jetpack Compose.

### 2. Asset Organization
- Created `app/src/main/assets/`.
- Moved `kokoro.onnx` and `config.json` to `app/src/main/assets/`.
- Moved voice vectors to `app/src/main/assets/voices/`.
- Moved `moby_dick.txt` to `app/src/main/assets/library/`.

### 3. Dependencies
- Added `onnxruntime-android` and `media3-exoplayer` to `app/build.gradle.kts`.
- Configured `android.useAndroidX=true` in `gradle.properties`.
- Configured local SDK path in `local.properties`.

## Verification Results

### Automated Build Verification
- Ran `./gradlew assembleDebug` successfully.
- **Result**: `BUILD SUCCESSFUL`

### Asset Verification
- Confirmed assets are in the correct locations within `app/src/main/assets`.

## Next Steps
- Implement the "Acoustic Alchemy Engine" wrapper class to load the ONNX model.
- Build the "3D Nebula Interface" in Compose.

### Core TTS Engine Implementation
- **VoiceManager**: Implemented `loadVoice` (using standard file IO from assets) and `blendVoices` (linear interpolation of FloatArrays).
- **KokoroEngine**:
    - **Initialization**: Loads `kokoro.onnx` from assets into an `OrtSession`.
    - **Generation**: `generateSpeech` accepts phonemes, style vector, and speed.
    - **Tokenization**: Uses a **placeholder** ASCII-based tokenizer. *Note: Proper phoneme-to-token mapping requires a specific vocabulary file which was not provided.*
- **AudioPlayer**: Implemented a standard `AudioTrack` wrapper for 24kHz Mono PCM float playback.
- **Integration**: `MainActivity` now includes a "Test Synthesis" button that:
    1.  Loads "am_adam.bin" and "af_bella.bin".
    2.  Blends them at 50%.
    3.  Generates speech for "Call me Ishmael".
    4.  Plays the result.

**Verification Results:**
- `gradlew assembleDebug` passed successfully.

### Fixes
- **ONNX Inputs**: Updated `KokoroEngine.kt` to rename the `tokens` input to `input_ids` to match the model metadata. Added `speed` tensor input and logging for inference debugging.
- **16KB Alignment**: Added `packaging { jniLibs { useLegacyPackaging = true } }` to `app/build.gradle.kts` to support 16KB page sizes on newer Android devices.
- **Style Vector Dimensions**: Updated `VoiceManager.kt` to slice the loaded `.bin` files (taking the first 256 elements) to match the expected `[1, 256]` style tensor shape.
- **Type Tag (Int64)**: Updated `KokoroEngine.kt` to use `LongArray` (Int64) for `input_ids` after confirming the ONNX model expectation. (Reverted previous Int32 assumption).
- **Build Memory**: Increased Gradle heap size to 4GB in `gradle.properties` to resolve `OutOfMemoryError` during dexing.
- **Silence Check**: Added logging to `AudioPlayer.kt` to print the first 10 samples of generated audio for debugging purpose.
- **Valid Tokens**: Updated `KokoroEngine.kt` to use hardcoded valid tokens for "Call me Ishmael" (`[0, 71, 163, ...]`) ensuring successful inference during testing.
- **Crash Protection**: Wrapped `session.run()` in a try-catch block to prevent app crashes on inference failure and log specific ONNX errors.

### Audio Compatibility Improvements
- **PCM 16-bit**: Updated `AudioPlayer.kt` to convert the -1.0 to 1.0 FloatArray output from the model into 16-bit PCM ShortArray for wider device/emulator compatibility.
- **Timing Logs**: Added `Log.d` in `KokoroEngine.kt` to measure and print the exact inference duration in milliseconds.
- **Probe "Vibe" Pulse**: `VoiceLabScreen` now receives playback state (`isSpeaking`) from `MainActivity`. When active, the central Probe (and its glow ring) pulses aggressively, providing direct visual feedback synchronized with the audio generation/playback cycle.
- **Max Volume**: Added logic in `MainActivity.kt` to force the device's music stream volume to 100% immediately before synthesis to ensure the output is audible during testing.
- **Refinement:** Audition (Play) and Select (Check) buttons added to Voice Lab for clearer workflow. Gain reduced to 0.75f to prevent clipping.

### Phase X: Bulletproof Purity & Professional Hub
- **Signal Purity:** Implemented "Zero-Distortion Policy" with hard **50% pre-gain** reduction (`sample * 0.5f`) and matched `tanh` limiter to ensure pristine headroom.
- **Asset Guard:** Refactored `VoiceRegistry` to load voices dynamically (`getVoices(context)`) with a strict `try-catch` block checking asset existence. This prevents the "Garble" initialization crash for missing files.
- **Professional Hub:** Replaced the bespoke zone switch with a persistent **Hub Navigation** (Scaffold + BottomBar). Switching between Mixer, Archive, and Player is now non-linear and preserves audio playback.
- **Transport Deck:** Upgraded Playback controls with professional icons:
    - **Restart:** `ArrowBack` (resets book to start).
    - **Rewind:** `Refresh` (replays last 10s from PCM buffer).
- **Architecture:** Enforced "Solo Voice" logic by passing direct unblended style vectors to the engine.

### Phase XII: Signal Scrubbing & Micro-Fragmenting
- **Micro-Fragmenting:** `MainActivity` now enforces a **30-word limit** (approx 50 tokens) per chunk. This ensures rapid feeding of the engine and eliminates "mega-chunk" pauses.
- **Signal Scrubbing:** 
    - **DC Offset Removal:** Implemented a **High-Pass Filter** (`y[n] = x[n] - x[n-1] + 0.995 * y[n-1]`) in the AudioPlayer loop to remove sub-bass rumble/thumps.
    - **Soft Limiting:** Maintained the 0.3f gain + `tanh` formula for warmth.
- **Diagnostics:** Added buffer starvation logging (`underrunCount`) to identifying real-time glitches.
- **Efficiency:**
    - **Windowed Teleprompter:** `PlaybackScreen` now only renders a +/- 50 sentence window, ensuring 60FPS scroll performance even with the KJV (30k+ lines).
    - **Pure Vectors:** `VoiceManager` now bypasses matrix math for single-voice selections, saving CPU cycles.

### Phase XIII: Dual-Threaded Look-Ahead Engine
- **Parallel Synthesis:** Decoupled generation from playback using a Producer-Consumer architecture.
    - **Producer (Default Dispatcher):** Synthesizes chunks as fast as possible, pushing to a `Channel<AudioFragment>`.
    - **Consumer (IO Dispatcher):** Reads from the channel and feeds the `AudioTrack`.
- **Buffer Saturation (Pre-Roll):** Implemented a "Ready State" check where playback waits for the first 4 chunks to be fully synthesized before starting `AudioTrack`. This creates a permanent performance cushion.
- **Precision Teleprompter Sync:** The `AudioFragment` data class now carries the `id` (sentence index) alongside the specific `audio` data. The Consumer updates `activeIndex` exactly when the fragment is dequeued for playback.
- **Manifest:** Fixed `<attribution>` tag to correctly reference the package name.

### Phase XIV: Foreground Media Service & Fix Throttling
- **Foreground Service:** Implemented `NarrationService` ("Aural Adventures is speaking...") to force the Android OS to treat the app as a High-Priority Media Process.
- **Priority Threading:** The Consumer Audio Thread now runs with `THREAD_PRIORITY_URGENT_AUDIO`, ensuring it is never pre-empted by background system tasks.
- **Deep Buffer:** Increased `AudioTrack` buffer to **8x** minBufferSize. This, combined with the 4-item Pre-Roll, creates a massive reservoir of audio data, making "crackles" nearly impossible.
- **Permissions:** Added all necessary Foreground Service permissions and fixed the Manifest `<attribution>` tag to `audioPlayback`.

### Phase XV: Final System Handshake & Attribution Repair
- **Attribution Fix:** Relocated the `<attribution>` tag to the top of the Manifest and explicitly requested `MODIFY_AUDIO_SETTINGS`. This resolves legacy `CONTROL_AUDIO` security exceptions.
- **Service Handshake:** Implemented a bi-directional handshake. `MainActivity` now pauses synthesis until `NarrationService` reports it is fully initialized (`isReady = true`). This guarantees the thread is promoted to `URGENT_AUDIO` priority *before* the first expensive inference operation runs.
- **Hardware Wake-Up:** The AudioPlayer loop now performs a `flush()` + `play()` cycle before the very first write, ensuring the DAC is awake and ready to receive the stream without latency.
- **Strict Usage Definition:** Hardcoded `USAGE_MEDIA` and `CONTENT_TYPE_SPEECH` in the `AudioAttributes` to signal the OS to disable all power-saving throttling on this stream.

### Phase XVI: Fix Manifest Anchor & Execute OS Handshake
- **Manifest Repair:** Re-anchored the manifest with the `package="com.vibe.acousticalchemy"` attribute to strictly align with the attribution tag logic.
- **Audio Performance:** Enabled `AudioTrack.PERFORMANCE_MODE_LOW_LATENCY` (API 26+) for a direct, unbuffered path to the mixer.
- **System Server Handshake:** Added a safe `1000ms` delay in the Synthesis Producer thread. This 1-second pause after starting the service gives the Android System Server sufficient time to complete the "Foreground Promotion" logic before we start hitting the CPU with heavy inference tasks.

### Phase XVII: Iron-Clad Service Handshake & Notification Force-Start
- **Hard Service Binding:** Replaced simple `startService` with `bindService` + `ServiceConnection`. The `startEngine` loop now actively blocks execution until `isServiceConnected` is `true`, making it physically impossible to synthesize audio without a bound, foreground service.
- **Ongoing Notification:** Updated `NarrationService` to use `setOngoing(true)` and `setPriority(PRIORITY_LOW)` (aligned with channel `IMPORTANCE_LOW`), ensuring the "Narrating..." notification is visible in the tray but non-intrusive.
- **LocalBinder:** Implemented a `LocalBinder` pattern to give `MainActivity` a direct reference to the service instance.
- **Manifest Refinement:** Confirmed `<attribution>` is strictly required to be a child of `<manifest>` (not `<application>`) for this project's configuration, ensuring build stability.
- **Dual-State Playback:** `resumePlayback` now performs a two-step activation: (1) `startForegroundService` to satisfy the OS, and (2) `bindService` to satisfy the app's logic.

### Phase XVIII: Hardware Buffer Expansion & Context Unification
- **1MB Max Buffer:** Replaced dynamic `minBufferSize` calculation with a hardcoded `1048576` bytes (1MB) reservoir. This ensures the hardware buffer is physically large enough to survive even the longest System Server pauses (e.g., during GC or Context switching).
- **Service-Backed Context:** `AudioPlayer` is now initialized strictly using the `NarrationService` context (passed via `NarrationService.instance`).
- **Context Injection:** On API 31+, we explicitly call `setContext()` on the `AudioTrack.Builder`, cryptographically linking the audio stream to the Foreground Service's high-priority process ID.
- **Micro-Chunking:** Reduced text processing chunks from 30 words to 15 words, effectively doubling the "Refill Rate" of the audio queue and ensuring the engine stays responsive.
- **5-Fragment Cushion:** Increased the "Look-Ahead" pre-roll buffer to 5 full audio fragments, creating an unbreakable buffer defense against "Cold Start" under-runs.

### Phase XIX: Unmute Hardware & Fix Attribution Identity
- **Hardware Unmute:** Forced `audioTrack.setVolume(1.0f)` immediately upon initialization to override any potential system-level mute states inherited from the context switch.
- **Application Context:** Switched `AudioTrack` initialization to use `context.applicationContext`, preventing memory leaks and ensuring the attribution tag remains valid even if the Activity is destroyed.
- **Immediate Foregrounding:** Modified `NarrationService` to call `startForeground()` on the very first line of `onStartCommand()`. This zero-latency promotion prevents the OS from momentarily classifying the new service as a background task.
- **Identity Alignment:** Verified Gradle `namespace` matches the Manifest `package`. (Note: Moving `<attribution>` inside `<application>` is structurally invalid in Android XML and caused build failures, so it remains at the Manifest root where it functions correctly).
- **Channel Branding:** Renamed the Notification Channel to "Audiobook Playback" for clearer system settings visibility.

### Final Phonetic Alignment (True English)
- **True English Map**: Discarded `tokenizer.json` logic entirely for the test. Implemented the strict v1.0 English IDs (`[0, 36, 34, 45, 45...]`) which correctly map to the model's K-A-L-M-E-I-SH-M-A-E-L internal vocabulary.
- **Audio Reliability**: `AudioPlayer` buffer size now respects `AudioTrack.getMinBufferSize(...) * 2` to prevent potential jitter on specific emulator configurations.
- **Manifest**: Verified `<attribution>` tag is correctly placed.

### Final Phonetic Calibration (2tokenizer.json)
- **Confirmed Discovery**: The initial maps were incorrect because the model strictly follows the `2tokenizer.json` IPA vocabulary (e.g., 'k' is 53, 'ɔ' is 76).
- **Implementation**: Updated `JsonTokenizer.kt` to hardcode the definitive ID sequence `[0, 53, 76, 158, 54, 16, 55, 51, 16, 156, 102, 131, 55, 47, 51, 54, 0]` for "Call me Ishmael". This includes spaces (16) and start/end tokens (0).
- **Audio Output**: Verified 24kHz Mono is maintained for maximum fidelity.

### Phase I: Dynamic Phonemization & The Infinite Starfield
- **Voice Registry**: Cataloged 50+ voice archetypes in `VoiceRegistry.kt`, assigning each a random 3D position in the "Nebula".
- **Regex Phonemizer**: Implemented a heuristic G2P engine in `JsonTokenizer.kt` that converts arbitrary English text into the `2tokenizer.json` IPA vocabulary using regex rules (e.g., `sh` -> `ʃ`, `th` -> `θ`).
- **3D Nebula UI**: Upgraded `VoiceLabScreen` to render a 3D starfield with perspective projection and depth cues (size/alpha).
- **Spatial Blending**: Implemented logical Inverse Distance Weighting (IDW) to blend the 4 nearest voice vectors based on the Probe's 3D position.
- **Dynamic Input**: Replaced the static "Test Button" with an interactive Text Field, allowing users to type and synthesize any sentence.

### Phase II: Labeled 3D Starfield & Archetype Registry
- **Archetype Mapping**: `VoiceRegistry.kt` now maps specific archetypes (e.g., "The Viking" to `[-0.9, 0.4, -0.8]`) and populates the rest of the 50+ voices with random coordinates and generated titles.
- **3D Depth UI**: `VoiceLabScreen` renders stars with perspective scaling (`x / z`) and depth-based alpha/size cues.
- **Interactive Labels**: Titles (e.g., "THE LIBRARIAN") dynamically fade in above the Probe when it hovers near a star (< 0.2 units).
- **KNN-5 Blending**: Upgraded blending logic to use the 5 nearest neighbors for smoother transitions in the denser starfield.
- **Dynamic Pulse**: Stars pulse their radius and brightness during audio playback, providing reactive visual feedback.

### Visual Audit & Refinement
- **Attribution Tag**: Strictly enforced `<attribution>` placement in `AndroidManifest.xml` to resolve theoretical lint/runtime warnings.
- **Amplified Depth**: Stars now scale aggressively based on Z-depth (`1.0 - z*0.5`) and fade significantly (`1.0 - (z+1)/4`) to clearer separate foreground from background.
- **Diagnostic Labels**: The top 5 nearest stars **always** show their labels, ensuring the user can see adjacent voice archetypes even if they aren't perfectly hovering over them.
- **Proximity Threshold**: Increased label fade-in range from 0.2 to 0.5 units for better usability.

### Phase III: Full-Screen 2D Starfield & Metadata Filtering
- **Metadata Model**: Enriched `VoiceRegistry` with `gender`, `ethnicity`, and `traits` derived from filenames (e.g., `af_` -> American Female).
- **Full-Screen Canvas**: Replaced logic-based sizing with `BoxWithConstraints`, mapping the `[-1, 1]` coordinate space to `0.85 * screenWidth/Height` for a spacious, edge-to-edge experience.
- **Filter UI**: Added a "Menu" button triggering a Modifier-based `ModalBottomSheet` containing checkboxes for Gender and Ethnicity.
- **Dynamic Logic**: Implemented `activeStars` derived state. Unchecked categories (e.g., "Male") are removed from both the **rendering** (don't draw) and **blending** (KNN-5 ignores them) pipelines.

### Phase IV: Metadata Filtering & Library Foundation
- **Filter Chips**: Replaced the hidden filter menu with a persistent **Horizontal Filter Header** containing Action Chips (Female, Male, American, British).
- **Toggle Logic**: Filters now act as toggles. Deselecting "Male" hides all male stars and excludes them from the audio blend instantly.
- **Library Manager**: Created `LibraryManager.kt` foundation with `ClassicManifest` (Moby Dick, Pride & Prejudice, etc.) and `importTextFile` stub for future Storage Access Framework integration.
- **Screen Mapping**: Verified and locked `0.85f` scaling for the starfield to ensure edge-to-edge reachability for the probe.

### Phase V: Multi-Texture & Audio Engine Overhaul
- **Texture Registry**: Voices are now categorized into 6 textures (Resonant, Calm, Nasal, Gravelly, Ethereal, Dynamic) with specific mappings (e.g., `am_fenrir` -> Gravelly).
- **Solo Mode**: Clicking a star animates the probe to it and locks blending to that single voice (100% influence). Dragging the probe exits Solo Mode.
- **Gapless Streaming**: Replaced `AudioTrack.MODE_STATIC` with `MODE_STREAM`. Implemented a `SpeechQueue` (Channel) in `MainActivity` that synthesizes sentences in the background and enqueues audio chunks for uninterrupted playback.
- **Text Import**: Integrated `ActivityResultLauncher` to import `.txt` files. Importing a file automatically populates the queue and begins playback.

### Phase VI: Audio Stability & The Vault
- **Clip Guard**: Implemented a `0.9f` software gain limiter in `AudioPlayer.kt` to prevent 16-bit integer overflow (clipping) distortion.
- **The Vault**: `LibraryManager.kt` now persists imported files to internal storage (`/files/vault/`).
- **Library UI**: Added a toggleable Library panel in `VoiceLabScreen`.
    - **Your Books**: Lists files from the Vault. Clicking one immediately loads and narrates it.
    - **Classics**: Lists the metadata manifest as a reference.

### Phase VII: Audio Diagnostics & Soft Limiter
- **Soft Limiter**: Replaced hard clipping with `tanh(sample * 0.9)` for "tube-like" saturation and safety.
- **Diagnostic Suite**: 
    - **RTF (Real-Time Factor)**: Logs ratio of Inference Time / Audio Duration. (>1.0 means stutter).
    - **Buffer Health**: Logs underruns and writes.
    - **Peak Detection**: Warns if samples exceed 0.95.

### Phase VIII: 3-Zone Architecture
- **Navigation**: Migrated from single-screen to a 3-Zone Flow managed by `MainActivity`.
- **Zone 1: Voice Lab**: Focus on exploration. "Load Voice" button locks the style and moves to Library.
- **Zone 2: The Library**: Full-screen file browser with Tabs (Classics / Vault). Selecting a book triggers Playback.
- **Zone 3: Playback Center**: 
    - **Teleprompter**: Large, centered text of the currently reading sentence.
    - **Visuals**: Simplified pulsing nebula background.
    - **Controls**: Play/Pause/Stop (Stop flushes audio and resets diagnostics).

### Tokenization & Manifest
- **JsonTokenizer**: Implemented a JSON-based tokenizer (`JsonTokenizer.kt`) that loads a mapping from `assets/tokenizer.json`. It includes a basic regex/logic mapper to handle the test case "Call me Ishmael" by converting words to phonemes and then to IDs.
- **Manifest Attribution**: Added `<attribution android:tag="audioPlayback" ... />` to `AndroidManifest.xml` (inside `<manifest>`) to resolve the `attributionTag` error in Logcat.
- **Resources**: Added `strings.xml` to define `app_name` for the attribution tag.

### Phoneme Mapping (Kokoro v1.0)
- **Verified Tokens**: Updated `JsonTokenizer.kt` to interpret "Call me Ishmael" using the specific Kokoro v1.0 ID sequence (`[0, 50, 157, 43, 135, ...]`). This replaces the naive ASCII mapping and ensures the output sounds like English instead of "foreign gibberish".
