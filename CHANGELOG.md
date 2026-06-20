# Changelog

## [1.0.0] — Unreleased

### Added
- Continuous loop recording in 60-second segments
- Back, front, and dual-camera (concurrent CameraX) modes
- Background recording via foreground service with notification controls (stop, lock clip)
- Real-time GPS telemetry overlay: speed, heading, coordinates, compass direction
- OSMDroid offline mini map with configurable style and tile caching
- Telemetry recording per clip: location samples, heading samples, snapshots
- Clip library with search and filter (by camera mode, time range, lock status)
- Clip detail view with ExoPlayer video playback and full metadata
- Export: raw video, metadata JSON, GPX route file
- Location redaction for privacy-preserving exports
- Share via Android share sheet with trimmed 50MB limit
- Storage management with configurable quota and auto-delete oldest unlocked
- Immich integration (optional, off by default): video upload, auto-album, metadata sync
- Anonymous clip sharing via Supabase with 24-hour expiry
- In-app bug reporting via GitHub Issues API
- MLKit vehicle and person detection overlay
- Firebase: Crashlytics, Analytics, Performance, Remote Config, Messaging (all opt-in)
- 46 configurable settings via DataStore + EncryptedSharedPreferences
- Onboarding flow with permissions screen
- CI/CD pipeline: debug/release builds, lint, unit tests, GitHub Pages deploy
