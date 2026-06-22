# DriveVault Dashcam

<div align="center">

**[Privacy Policy](https://chartmann1590.github.io/DriveVault/privacy-policy.html)**

**Privacy-first dashcam for Android**

Record your drives with GPS overlays, dual-camera support, and full control over your data.

[Features](#-features) · [Screenshots](#-screenshots) · [Download](#-download) · [Setup](#-getting-started)

</div>

---

## Why DriveVault?

Most dashcam apps send your data to the cloud. DriveVault keeps everything on your device by default - no accounts, no cloud uploads, no tracking. Your drives stay private unless you choose to share them.

## Features

### Recording
- Continuous loop recording in 60-second segments
- Rear, front, and dual-camera modes
- Background recording via foreground service
- Auto-deletes oldest unlocked clips when storage is full

### GPS & Telemetry
- Real-time speed, heading, and coordinates overlay
- Compass direction display
- OSM-based mini map (no API keys needed)
- Full telemetry recording per clip

### Clip Management
- Browse, search, and filter your clip library
- Lock important clips to prevent auto-deletion
- Clip detail view with playback and metadata

### Export & Sharing
- Export raw video, metadata JSON, or GPX route files
- Share clips via Android share sheet
- Include or exclude location data in exports

### Immich Integration (Optional)
- Sync clips to your self-hosted Immich server
- Encrypted API key storage
- Configurable sync rules (Wi-Fi only, charging only, etc.)

### Anonymous Clip Sharing (Optional)
- Share clips via time-limited links (24-hour expiry)
- Trim to 50MB limit for sharing
- No account required to view shared clips
- All sharing is opt-in and off by default

### AI Detection (Optional)
- Real-time vehicle and person detection via MLKit
- Bounding box overlay on camera preview
- Off by default; no cloud inference used

### In-App Bug Reporting (Optional)
- Submit bug reports and feature requests directly from Settings
- Option to attach screenshots (uploaded to GitHub assets)
- Automatically compiles basic device diagnostics (OS version, device model, free memory/storage, app version) to speed up troubleshooting
- Direct in-app communication thread (post and read comments on your submitted issues)

## Screenshots

| Recording | Clip Library | Clip Detail |
|-----------|-------------|-------------|
| ![Recording Portrait](stitch_drivevault_dashcam_ui_ux_design/stitch_drivevault_dashcam_ui_ux_design/dashcam_recording_portrait_1/screen.png) | ![Clip Library](stitch_drivevault_dashcam_ui_ux_design/stitch_drivevault_dashcam_ui_ux_design/clip_library/screen.png) | ![Clip Detail](stitch_drivevault_dashcam_ui_ux_design/stitch_drivevault_dashcam_ui_ux_design/clip_detail_playback/screen.png) |

| Settings | Export | Onboarding |
|----------|--------|------------|
| ![Settings](stitch_drivevault_dashcam_ui_ux_design/stitch_drivevault_dashcam_ui_ux_design/settings_recording_sync/screen.png) | ![Export](stitch_drivevault_dashcam_ui_ux_design/stitch_drivevault_dashcam_ui_ux_design/export_overlay_customization/screen.png) | ![Onboarding](stitch_drivevault_dashcam_ui_ux_design/stitch_drivevault_dashcam_ui_ux_design/onboarding_welcome_safety/screen.png) |

## Download

> Pre-built APKs will be available in [Releases](https://github.com/chartmann1590/DriveVault/releases) once the first version is published.

### Requirements
- Android 8.0 (Oreo) or later
- Device with rear camera
- GPS recommended for full feature set

## Getting Started

### For Users
1. Download and install the APK
2. Grant camera, microphone, and location permissions
3. Mount your phone in a car dock
4. Hit record

### For Developers

#### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with compileSdk 36

#### Build from Source
``bash
git clone https://github.com/chartmann1590/DriveVault.git
cd DriveVault
cp local.properties.example local.properties
# Edit local.properties with your Firebase, Supabase, and GitHub values.
python scripts/generate_google_services_json.py
./gradlew assembleDebug
``

#### Firebase Configuration
Firebase values are read from `local.properties` (never committed to the repo). Copy `local.properties.example` to `local.properties` and fill in your values.

Before building release (or any variant with the Google Services plugin), generate `app/google-services.json`:

``bash
python scripts/generate_google_services_json.py
``

See `SECURITY.md` for guidance on handling secrets.

## Permissions Explained

| Permission | Why We Need It |
|---|---|
| Camera | Record video of your drives |
| Microphone | Capture audio with video clips |
| Location | GPS coordinates, speed, and map overlay |
| Background Location | Keep GPS recording when the app is in the background |
| Notifications | Keep recording status visible |
| Storage | Save clips and exports |
| Foreground Service | Continue recording in background |
| Wake Lock | Prevent recording interruption |

## Privacy

Your data stays on your device. Here is what DriveVault does NOT do:

- Does NOT upload videos to any cloud service
- Does NOT track your location remotely
- Does NOT require an account or sign-up
- Does NOT show ads
- Does NOT sell your data

The only network features are:
- Optional Immich sync to YOUR OWN server
- Optional anonymous clip sharing via Supabase (off by default, 24-hour expiry)
- Firebase crash reporting and analytics (app health only, no clip content)
- Optional MLKit object detection (on-device, no network for inference)
- Optional In-App Bug Reporting (transmits bug reports, optional screenshot attachments, and opt-out device diagnostics to the project's GitHub issues tracker)

## Tech Stack

Built with modern Android development tools:

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **CameraX** for camera control
- **Room** for local database
- **OSMDroid** for maps
- **Media3/ExoPlayer** for video playback
- **MLKit** for on-device object detection
- **Firebase** for crash reporting and analytics
- **Supabase** for optional anonymous clip sharing
- **GitHub REST API** for optional in-app support and bug reporting

<div align="center">

**[Privacy Policy](https://chartmann1590.github.io/DriveVault/privacy-policy.html)**
Made with care for drivers who value privacy.
</div>
