# LumenVoice Releases

This page documents official builds, release notes, and how to download and install the **LumenVoice** Android APK.

---

## Latest Release: `v1.0.0`

**Release Highlights**
- **100% Offline Screen Brightness Controller**: No internet permission, no cloud services, and no API keys required.
- **Extra-Large Voice Control Button**: `140.dp` microphone button (`72.dp` icon) with live RMS acoustic pulse rings using Android's native `SpeechRecognizer`.
- **Local Kotlin Voice Command Parser**:
  - Exact numeric values (`0`–`100` digits and spoken English words such as `"forty three"`).
  - Relative adjustments: `"brighter"`, `"increase"`, `"more"` (`+15%`); `"dimmer"`, `"darker"`, `"decrease"`, `"less"` (`-15%`).
  - Quick extremes: `"max"` / `"full"` (`100%`); `"minimum"` / `"low"` (`10%`).
- **Native Text-to-Speech Confirmation**: Speaks back confirmations (`"Brightness set to <N> percent"`) with an instant mute toggle.
- **Direct System Brightness & Manual Mode Enforcement**: Disables adaptive brightness (`SCREEN_BRIGHTNESS_MODE_MANUAL`) and writes `0–255` integer brightness values via `Settings.System.SCREEN_BRIGHTNESS`, with guided `Settings.ACTION_MANAGE_WRITE_SETTINGS` onboarding.

---

## How to Download the APK

### Option 1: GitHub Releases Page (Recommended)
1. Open the **Releases** tab on the right sidebar of the GitHub repository (or navigate to `<your-repo-url>/releases`).
2. Under the latest release tag (e.g., `v1.0.0`), expand **Assets**.
3. Download **`LumenVoice-debug.apk`**.

### Option 2: GitHub Actions CI Artifacts
1. Open the **Actions** tab in GitHub and select the **Android CI** workflow.
2. Click the latest successful run on `main` / `master` (or trigger **Run workflow** manually via `workflow_dispatch`).
3. Scroll to **Artifacts** and download **`LumenVoice-debug-apk`**.

### Option 3: AI Studio Export
1. Open the **Settings** menu in Google AI Studio.
2. Select **Generate APK / AAB** to build and download the APK directly.

---

## Creating a New GitHub Release

The repository workflow (`.github/workflows/android.yml`) runs on **Node 24** actions and automatically publishes a GitHub Release page with the compiled APK attached whenever you push a version tag matching `v*` (or trigger `workflow_dispatch` with `create_release: true`):

```bash
git tag v1.0.0
git push origin v1.0.0
```

Once the workflow completes, the release page is automatically created in GitHub **Releases** with:
- Compiled `LumenVoice-debug.apk` binary asset
- Generated `SHA-256` checksum file (`LumenVoice-debug.apk.sha256`)
- Structured release notes from `RELEASE.md`

---

## Installing on Android

1. Transfer `LumenVoice-debug.apk` to your Android device (Android 7.0 / API 24 or higher).
2. Tap the APK file and allow **Install unknown apps** if prompted.
3. Open **LumenVoice**:
   - Tap **Grant Modify System Settings** to allow system-wide brightness and adaptive-mode control.
   - Tap the microphone button and grant **Microphone (`RECORD_AUDIO`)** permission to start controlling brightness by voice.
