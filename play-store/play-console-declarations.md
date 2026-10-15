# Play Console Policy Declarations — DriveVault

Copy-paste answers for the three permission declaration forms flagged in
`play-store-errors.txt`. Videos referenced below are in `play-store/videos/`
— upload each to YouTube (unlisted is fine) and paste the resulting link into
the matching Play Console field.

---

## 1. Location permissions

**App purpose** (≤500 chars):
```
DriveVault is a privacy-first dashcam app. It continuously records video from the phone's camera while driving and overlays live GPS data (speed, heading, coordinates) onto each clip so the driver has a complete record of the trip, including location, in case of an accident, dispute, or insurance claim.
```

**Location access — describe 1 background feature** (≤500 chars):
```
Continuous trip logging: once recording starts, DriveVault keeps recording video and logging GPS speed/heading/coordinates even if the driver locks the screen or switches to another app (e.g. a maps or music app) while driving. This background location access is required so the recorded trip has no gaps - a dashcam that stops logging location the moment the app loses focus would produce incomplete, unreliable footage.
```

**Video instructions — YouTube link:**
Upload `play-store/videos/video1_background_location.mp4` and paste the link here.
The video shows: the real in-app prominent disclosure card ("Required to keep
GPS recording when the app is in the background") shown before the system
permission dialog, the user granting "Allow all the time", live GPS/speed
overlay updating during recording, and recording/GPS continuing after the
app is backgrounded.

---

## 2. Foreground service permissions

Selected options (per the "you must pick one" requirement):

| Permission | Selected task |
|---|---|
| `FOREGROUND_SERVICE_CAMERA` | **Background camera streaming** |
| `FOREGROUND_SERVICE_LOCATION` | **Background location updates** |
| `FOREGROUND_SERVICE_MICROPHONE` | **Background audio input** |

Rationale (if a free-text justification field appears): DriveVault is a
dashcam — its entire purpose is to keep recording video (camera), GPS track
(location), and optional in-cabin audio (microphone) continuously while the
screen is off or another app is in the foreground, exactly like a physical
dashcam unit. All three foreground services start only when the driver taps
Record and stop when they tap Stop; a persistent notification is shown the
entire time per Android's foreground-service requirements.

**Video instructions — YouTube link:**
Upload `play-store/videos/video2_foreground_service.mp4` and paste the link here.
The video shows: tapping Record (which starts the camera+location+microphone
foreground service and posts the persistent "Recording in progress"
notification), the live GPS/speed overlay while recording, the app switcher
showing the recording still active (REC) after backgrounding, returning to
the app to confirm recording never stopped, and tapping Stop to end the
foreground service and save the clip.

---

## 3. Photo and video permissions (READ_MEDIA_VIDEO)

**Describe your app's use of READ_MEDIA_VIDEO** (≤250 chars):
```
DriveVault needs frequent, ongoing access to browse, play back, and export previously recorded dashcam clips in its built-in Clip Library - this is core app functionality used every session, not a one-time or infrequent picker use case.
```

**Video instructions — YouTube link:**
Upload `play-store/videos/video3_read_media_video.mp4` and paste the link here.
The video shows: the Clip Library listing multiple real saved clips with
storage usage, opening a clip into the Clip Detail screen, playing back the
actual recorded footage with full transport controls, returning to the
library, and opening a second clip - demonstrating this is repeated, everyday
browsing/playback functionality rather than a single one-off file pick.

---

## Notes

- All three videos were recorded on a physical Pixel 8 Pro running the real
  signed build, with real screenshots/screen-recording, a synthesized
  voiceover (Windows SAPI TTS), and burned-in captions - no scripted mockups.
- Keep each video's YouTube visibility at least "Unlisted" (Play Console just
  needs a working link; it does not need to be public/searchable).
- Content rating, Data safety, Ads declaration, and Target audience forms
  were already completed manually in Play Console per your confirmation -
  these three permission-declaration sections are the only remaining pieces
  from `play-store-errors.txt`.
