# LumenVoice

**LumenVoice** is a minimal, **100% offline** native Android screen brightness controller built in **Kotlin** and **Jetpack Compose**. It lets you adjust your device's screen brightness using **on-device voice commands**, a **0%–100% precision slider**, **exact numeric input**, and **quick presets (`25%`, `43%`, `75%`, `100%`)**—with **zero internet access, zero API keys, and zero cloud services**.

---

## Key Features

- **100% Offline & Private**: No `INTERNET` or `ACCESS_NETWORK_STATE` permissions are requested. All voice recognition preferences, command parsing, brightness scaling, and spoken confirmations run locally on-device.
- **Extra-Large Voice Trigger**: Prominent `140.dp` microphone button (`72.dp` icon) with live acoustic RMS ring animation powered by `android.speech.SpeechRecognizer`.
- **Local Kotlin Voice Command Engine**:
  - **Exact Percentage (`0`–`100`)**: Say `"43"`, `"forty three"`, or `"set brightness to 43 percent"` to set the exact percentage.
  - **Relative Increase (`+15%`)**: Say `"brighter"`, `"increase"`, `"more"`, `"higher"`, `"turn up"`, `"raise"`, `"lighter"`, or `"boost"`.
  - **Relative Decrease (`-15%`)**: Say `"dimmer"`, `"darker"`, `"decrease"`, `"less"`, `"lower"`, `"turn down"`, `"reduce"`, or `"dim"`.
  - **Quick Extremes**: Say `"max"` or `"full"` for **100%**; `"minimum"`, `"min"`, `"low"`, or `"lowest"` for **10%**.
- **Native Spoken Confirmation**: Uses Android's `TextToSpeech` engine to speak back confirmations (e.g., *"Brightness set to 43 percent"*), with an instant mute toggle in the header.
- **Automatic Manual Brightness Override**: Automatically switches Android's brightness mode to `SCREEN_BRIGHTNESS_MODE_MANUAL` (disabling adaptive brightness) and writes the converted `0–255` integer value to `Settings.System.SCREEN_BRIGHTNESS`.
- **Guided System Settings Permission**: Checks `Settings.System.canWrite(context)` and displays a prominent banner to launch `Settings.ACTION_MANAGE_WRITE_SETTINGS` whenever write-settings access is not yet granted.
- **Precision Manual Controls**: Smooth `0%–100%` slider, `-15%` / `+15%` step buttons, validated `0–100` numeric input field, and one-tap preset buttons (`25%`, `43%`, `75%`, `100%`).

---

## How Does This Work?

LumenVoice follows an **MVVM** architecture (`BrightnessScreen` → `BrightnessViewModel` → `OfflineVoiceController` / `VoiceBrightnessParser` / `SystemBrightnessController`). Every brightness change—whether spoken or tapped—flows through a deterministic 5-stage pipeline:

```text
┌───────────────────────────┐
│ 1. Permission & Sync      │  Checks RECORD_AUDIO & Settings.System.canWrite(context)
└─────────────┬─────────────┘  Reads current SCREEN_BRIGHTNESS (0–255) & MODE on resume
              ▼
┌───────────────────────────┐
│ 2. Speech Capture         │  SpeechRecognizer (ACTION_RECOGNIZE_SPEECH)
└─────────────┬─────────────┘  EXTRA_PREFER_OFFLINE = true, EXTRA_MAX_RESULTS = 5
              ▼                Streams live RMS dB to Compose pulse rings
┌───────────────────────────┐
│ 3. Local Kotlin Parser    │  VoiceBrightnessParser.resolveFromCandidates(...)
└─────────────┬─────────────┘  Normalizes text -> matches digits/words/keywords
              ▼
┌───────────────────────────┐
│ 4. System Brightness Write│  percentage -> round(pct * 255 / 100) clamped to 0..255
└─────────────┬─────────────┘  Sets SCREEN_BRIGHTNESS_MODE_MANUAL + SCREEN_BRIGHTNESS
              ▼                Updates active Activity window screenBrightness
┌───────────────────────────┐
│ 5. Native TTS Feedback    │  TextToSpeech.speak("Brightness set to <N> percent")
└───────────────────────────┘  Logs timestamped event in Recent Brightness Events
```

### Step-by-Step Breakdown

1. **Permission & State Synchronization**:
   - On launch and every `ON_RESUME` lifecycle event, `BrightnessViewModel.refreshSystemState()` queries `Settings.System.canWrite(context)`, `Settings.System.SCREEN_BRIGHTNESS`, and `Settings.System.SCREEN_BRIGHTNESS_MODE`.
   - If `canWriteSettings` is `false`, a prominent warning card opens `Settings.ACTION_MANAGE_WRITE_SETTINGS` scoped directly to `package:com.aistudio.voicebrightness.vxkpnq`.
   - Even before `WRITE_SETTINGS` is granted, the app updates `window.attributes.screenBrightness` (`0.01f..1.0f`) so you can preview brightness changes immediately inside the app window.
2. **Audio Capture (`OfflineVoiceController`)**:
   - Tapping the microphone button checks `Manifest.permission.RECORD_AUDIO` (requesting it via `ActivityResultContracts.RequestPermission` if needed).
   - `SpeechRecognizer` starts listening with `EXTRA_PREFER_OFFLINE = true` and `EXTRA_PARTIAL_RESULTS = true`.
   - If a device does not have an offline speech pack downloaded for the active locale and returns a client/language-unavailable error, `OfflineVoiceController` automatically retries once without `EXTRA_PREFER_OFFLINE` so recognition still succeeds smoothly.
3. **Deterministic Local Parsing (`VoiceBrightnessParser`)**:
   - Up to 5 recognition candidates (`RESULTS_RECOGNITION`) are inspected in order; the first actionable match wins.
   - Input is lowercased, `%` is expanded to ` percent `, hyphens become spaces, and punctuation is stripped.
   - **Priority order**:
     1. **Digits (`0–100`)** via `\b(\d{1,3})\b`, or **English number words** (`"zero"` through `"one hundred"`, including compound tens like `"forty three"`).
     2. **Extreme keywords**: `"max"`, `"maximum"`, `"full"` → `100%`; `"minimum"`, `"min"`, `"low"`, `"lowest"` → `10%`.
     3. **Relative increase**: `"brighter"`, `"increase"`, `"more"`, `"higher"`, `"turn up"`, `"raise"`, `"lighter"`, `"boost"` → `current + 15%`.
     4. **Relative decrease**: `"dimmer"`, `"darker"`, `"decrease"`, `"less"`, `"lower"`, `"turn down"`, `"reduce"`, `"dim"` → `current - 15%`.
4. **0–255 Integer Scale & Adaptive Mode Disable (`SystemBrightnessController`)**:
   - Percentage `p ∈ [0, 100]` converts to Android's 8-bit integer brightness scale via:
     `system255 = round((p * 255f) / 100f).coerceIn(0, 255)`
   - Writes `SCREEN_BRIGHTNESS_MODE = SCREEN_BRIGHTNESS_MODE_MANUAL` (`0`) to disable adaptive brightness, followed by `SCREEN_BRIGHTNESS = system255`.
5. **Spoken Confirmation (`TextToSpeech`)**:
   - Speaks `"Brightness set to <p> percent"` using `TextToSpeech.QUEUE_FLUSH` (unless muted via the header speaker toggle).

---

## Voice-to-Text & Speech Technical Specifications

| Component | Technical Specification |
| :--- | :--- |
| **Speech Recognition API** | `android.speech.SpeechRecognizer` (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) |
| **Language Model** | `RecognizerIntent.LANGUAGE_MODEL_FREE_FORM` |
| **Locale Handling** | `Locale.getDefault()` for STT; `Locale.US` (with `Locale.getDefault()` fallback) for TTS |
| **Offline Execution Mode** | `RecognizerIntent.EXTRA_PREFER_OFFLINE = true` (single automatic fallback if offline pack missing) |
| **Hypothesis Candidates** | `EXTRA_MAX_RESULTS = 5` evaluated sequentially by `VoiceBrightnessParser` |
| **Streaming Partial Text** | `EXTRA_PARTIAL_RESULTS = true` rendered live inside the voice feedback banner |
| **Acoustic Telemetry** | `RecognitionListener.onRmsChanged(rmsdB)` clamped to `0f..12f` driving animated Compose pulse rings |
| **TTS Engine** | `android.speech.tts.TextToSpeech` • Speech Rate: `1.02f` • Queue Mode: `QUEUE_FLUSH` • Utterance ID: `lumen_brightness_confirm` |
| **Brightness Scale Math** | `0–100%` ↔ `0–255` integer (`round(pct * 255 / 100)`), clamped at boundaries |
| **System Settings Keys** | `Settings.System.SCREEN_BRIGHTNESS_MODE` (`0` = Manual), `Settings.System.SCREEN_BRIGHTNESS` (`0..255`) |
| **Permissions Declared** | `android.permission.RECORD_AUDIO`, `android.permission.WRITE_SETTINGS` (no `INTERNET` permission) |
| **Android 11+ `<queries>`** | `android.speech.RecognitionService`, `android.speech.action.RECOGNIZE_SPEECH`, `android.intent.action.TTS_SERVICE` |

---

## Tech Stack & Build Environment

- **Language**: Kotlin `2.2.10`
- **UI Framework**: Jetpack Compose (`androidx.compose:compose-bom:2024.09.00`) + Material 3
- **Typography**: Locally bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`)
- **Architecture**: MVVM (`AndroidViewModel` + `StateFlow` + `collectAsStateWithLifecycle`)
- **SDK Levels**: `minSdk = 24` (Android 7.0) • `compileSdk = 36.1` • `targetSdk = 36`
- **Build System**: Gradle `9.3.1` • Android Gradle Plugin `9.1.1` • JDK `21`
- **Testing**: JUnit 4 + Robolectric `4.16.1` (`ExampleUnitTest`, `ExampleRobolectricTest`)

---

## Releases & APK Downloads

See **[RELEASE.md](./RELEASE.md)** for full release notes, checksum verification, and instructions for downloading pre-built APKs from the **GitHub Releases** page or **GitHub Actions** artifacts.

### Local Build & Tests

```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK (output: app/build/outputs/apk/debug/app-debug.apk)
gradle :app:assembleDebug
```
