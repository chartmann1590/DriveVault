# Contributing to DriveVault

## Setup

1. Clone and open in Android Studio Hedgehog (2023.1.1) or newer
2. Copy `local.properties.example` to `local.properties` and fill in your values
3. Run `python scripts/generate_google_services_json.py`
4. Build with `./gradlew assembleDebug`

See `README.md` and `SECURITY.md` for details on handling secrets.

## Project Structure

```
app/src/main/java/com/drivevault/dashcam/
├── app/            Application class
├── data/           Data layer (Room DB, DAOs, entities, SettingsRepository)
├── domain/         Domain models, enums, repository interfaces, utilities
├── export/         Clip export (video, JSON, GPX) and trimming
├── firebase/       Firebase init, FCM, clip sharing (Supabase), cleanup worker
├── immich/         Immich self-hosted sync client and worker
├── location/       FusedLocationProvider wrapper
├── map/            OSMDroid mini map overlay
├── notifications/  Notification action receiver
├── permissions/    Runtime permission manager
├── recording/      CameraX recording, foreground service, MLKit detection
├── sensors/        Heading provider and telemetry manager
├── storage/        Storage enforcement and cleanup
└── ui/             Jetpack Compose UI: screens, components, theme, navigation, ViewModels
```

## Architecture

- **MVVM** with ViewModels, StateFlow, and Jetpack Compose
- **Room** for local persistence (clips, location samples, heading samples, snapshots)
- **DataStore** + **EncryptedSharedPreferences** for settings and secrets
- **WorkManager** for background sync (Immich, share cleanup)
- **Foreground Service** (`RecordingService`) for background recording

## Running Tests

```bash
./gradlew testDebugUnitTest
```

## Code Style

- Kotlin with standard conventions
- No comments unless necessary for non-obvious logic
- Use existing patterns: sealed classes for state, Flow for reactivity, lazy DI

## Before Submitting

1. Run `./gradlew lintDebug` and fix warnings
2. Run `./gradlew testDebugUnitTest` and ensure all pass
3. Test on a physical device if possible (CameraX features vary by hardware)
