# Google Play Store Listing — DriveVault Dashcam

## Directory Structure

```
play-store/
├── listing/                        # Store listing text & metadata
│   ├── title.txt                   # "DriveVault Dashcam"
│   ├── short-description.txt       # Max 80 chars
│   ├── full-description.txt        # Full feature description
│   ├── keywords.txt                # Search keywords (comma-separated)
│   ├── category.txt                # Auto & Vehicles
│   ├── content-rating.txt          # Everyone 4+
│   ├── privacy-policy-url.txt
│   ├── website-url.txt
│   ├── support-email.txt
│   ├── package-name.txt            # com.drivevault.dashcam
│   └── version.txt                 # 1.0.0 (1), minSdk 26, targetSdk 36
│
├── graphics/
│   ├── app-icon/
│   │   ├── drivevault_app-icon.png  # 512x512 PNG (rendered from adaptive icon vector)
│   │   └── drivevault_app-icon.svg  # Source vector
│   ├── feature-graphic/            # NEEDED: 1024x500 PNG/JPG
│   ├── promo-graphic/              # Optional: 180x120
│   └── tv-banner/                  # Optional: 1280x720
│
└── screenshots/
    ├── phone/                      # 7 screenshots (1008x2244 / actual app)
    │   ├── drivevault_phone_01_recording.png
    │   ├── drivevault_phone_02_back-camera.png
    │   ├── drivevault_phone_03_home.png
    │   ├── drivevault_phone_04_clip-library.png
    │   ├── drivevault_phone_05_clip-detail.png
    │   ├── drivevault_phone_06_settings.png
    │   └── drivevault_phone_07_onboarding.png
    │
    ├── tablet-7in/                 # 3 screenshots (1600x1280 / design mockups)
    │   ├── drivevault_tablet7_01_recording.png
    │   ├── drivevault_tablet7_02_recording-alt.png
    │   └── drivevault_tablet7_03_dual-camera.png
    │
    └── tablet-10in/                # 3 screenshots (1600x1280 / design mockups)
        ├── drivevault_tablet10_01_recording.png
        ├── drivevault_tablet10_02_recording-alt.png
        └── drivevault_tablet10_03_dual-camera.png
```

## Required Image Specs

| Asset             | Required Size  | Format    | Status                  |
|-------------------|----------------|-----------|-------------------------|
| App Icon          | 512x512        | PNG 32bit | Rendered from SVG       |
| Feature Graphic   | 1024x500       | PNG/JPG   | Generated (PNG + JPG)   |
| Promo Graphic     | 180x120        | PNG/JPG   | Generated               |
| TV Banner         | 1280x720       | PNG/JPG   | Generated               |
| Phone Screenshots | 1080x1920+     | PNG/JPG   | 7 copied (actual app)   |
| 7" Tablet SS      | 1920x1200      | PNG/JPG   | 3 copied (design mockups) |
| 10" Tablet SS     | 2560x1600      | PNG/JPG   | 3 copied (design mockups) |

## Regenerating Graphics

Run `python generate_graphics.py` from the `play-store/` directory to regenerate the auto-generated images (feature graphic, TV banner, promo graphic).

## Notes

- **Tablet screenshots** are from design mockups at 1600x1280 — replace with actual tablet captures for best results.
- **Theme colors**: Deep Charcoal #1F0F0D (bg), Safety Red #FFB4AA (primary), Electric Blue #ADC6FF (accent)
- **Typography**: Inter font family
