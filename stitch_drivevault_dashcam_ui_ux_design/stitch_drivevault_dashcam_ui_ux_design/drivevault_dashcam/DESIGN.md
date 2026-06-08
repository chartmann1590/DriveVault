---
name: DriveVault Dashcam
colors:
  surface: '#1f0f0d'
  surface-dim: '#1f0f0d'
  surface-bright: '#493431'
  surface-container-lowest: '#190a08'
  surface-container-low: '#291715'
  surface-container: '#2d1b18'
  surface-container-high: '#392522'
  surface-container-highest: '#44302d'
  on-surface: '#fcdbd6'
  on-surface-variant: '#e7bdb7'
  inverse-surface: '#fcdbd6'
  inverse-on-surface: '#402b29'
  outline: '#ad8883'
  outline-variant: '#5d3f3b'
  surface-tint: '#ffb4aa'
  primary: '#ffb4aa'
  on-primary: '#690003'
  primary-container: '#ff5545'
  on-primary-container: '#5c0002'
  inverse-primary: '#c0000a'
  secondary: '#c8c6c5'
  on-secondary: '#313030'
  secondary-container: '#4a4949'
  on-secondary-container: '#bab8b7'
  tertiary: '#adc6ff'
  on-tertiary: '#002e69'
  tertiary-container: '#4b8eff'
  on-tertiary-container: '#00285c'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdad5'
  primary-fixed-dim: '#ffb4aa'
  on-primary-fixed: '#410001'
  on-primary-fixed-variant: '#930005'
  secondary-fixed: '#e5e2e1'
  secondary-fixed-dim: '#c8c6c5'
  on-secondary-fixed: '#1c1b1b'
  on-secondary-fixed-variant: '#474646'
  tertiary-fixed: '#d8e2ff'
  tertiary-fixed-dim: '#adc6ff'
  on-tertiary-fixed: '#001a41'
  on-tertiary-fixed-variant: '#004493'
  background: '#1f0f0d'
  on-background: '#fcdbd6'
  surface-variant: '#44302d'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-md-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 26px
  label-xl:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '700'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 18px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  touch-target-min: 48dp
  gutter: 16px
  margin-edge: 24px
  container-gap: 12px
  safe-area-top: 32px
---

## Brand & Style
The brand personality is rooted in unwavering reliability and professional automotive safety. It targets high-end vehicle owners and professional drivers who require a "set-and-forget" experience that performs under pressure. 

The design style is a sophisticated evolution of **Modern Android Material 3**, specifically optimized for high-glare environments. It utilizes **Glassmorphism** for non-critical UI overlays to maintain situational awareness of the video feed behind the interface. The aesthetic is utilitarian yet premium, blending high-contrast functional elements with soft, translucent surfaces to reduce visual fatigue during night driving.

## Colors
The system operates in **Dark Mode by default** to minimize cabin distraction and preserve night vision. 

- **Primary (Safety Red):** Reserved exclusively for active recording states, emergency lock indicators, and critical errors.
- **Secondary (Deep Charcoal):** Used for the base canvas to ensure high-contrast legibility against white and amber text.
- **Accent (Electric Blue):** Denotes active connectivity, GPS synchronization, and cloud upload progress.
- **Warning (Amber):** Used for driver assistance alerts (LDWS, FCWS) and storage maintenance notifications.
- **Glass Overlays:** Semi-transparent panels use the Surface color with a 70% opacity and a 20px background blur to create depth without obscuring the camera view.

## Typography
**Inter** is selected for its exceptional legibility and neutral, systematic tone. In a dashcam context, readability at a glance is the highest priority. 

- **High Contrast:** All text must meet a minimum 7:1 contrast ratio against background surfaces.
- **Information Hierarchy:** Use `display-lg` for speedometers and time-stamps. `label-xl` (all caps) is utilized for status indicators (e.g., "REC", "GPS FIXED") to ensure they are readable from a distance.
- **Scale:** Sizes are slightly enlarged compared to standard mobile apps to accommodate for vibration and the physical distance between the driver and the mounted device.

## Layout & Spacing
The layout uses a **fluid grid** model that adapts between Landscape (primary for dashcam units) and Portrait (primary for companion mobile apps).

- **Touch Safety:** All interactive elements must maintain a minimum touch target of 48dp. In landscape mode, primary controls are anchored to the left and right edges for easy thumb access.
- **Visual Rhythm:** An 8px linear scale is used for internal component spacing, while 24px margins provide a safe buffer from screen bezels.
- **Adaptive Reflow:** In landscape, the video feed occupies the full background with glass overlays on the sides. In portrait, the video is pinned to the top 40% of the screen with a vertical list of controls/clips below.

## Elevation & Depth
This design system utilizes **Glassmorphism and Tonal Layering** instead of traditional drop shadows to maintain a sleek, modern automotive feel.

- **Level 0 (Base):** The live camera feed or the Deep Charcoal background.
- **Level 1 (Panels):** Semi-transparent glass panels with a 1px inner border (#FFFFFF at 10% opacity) to define edges against the video feed.
- **Level 2 (Active States):** Solid fills using the Accent or Primary colors to indicate interaction or critical status.
- **Depth:** Background blur (20px - 32px) is mandatory for all glass panels to ensure text remains legible regardless of the visual complexity of the road ahead.

## Shapes
Shapes follow a **Rounded (Level 2)** philosophy to align with modern vehicle interior design and Material 3 standards.

- **Standard Containers:** 16px (0.5rem) corner radius.
- **Large Cards/Overlays:** 24px (1rem) corner radius.
- **Buttons:** Fully pill-shaped (rounded-xl) to maximize the "touchable" affordance and distinguish them from informational panels.

## Components
- **Buttons:** Large, high-contrast buttons. The "Emergency Record" button is always solid Safety Red. Secondary actions use glass backgrounds with white icons.
- **Cards:** Used for video clips in the gallery. Feature a large 24px corner radius, a semi-transparent glass footer for metadata (date/time), and a 1px stroke.
- **Icons:** Use "Thick" or "Bold" weight variants (2px minimum stroke) to ensure they are identifiable at arm's length.
- **Recording Indicator:** A pulsating Safety Red dot paired with a `label-xl` text element.
- **Speedometer/Telemetry:** Minimalist, high-scale typography overlaid directly on the bottom-left of the glass panel.
- **Status Chips:** Small, semi-transparent capsules for "4K", "HDR", and "GPS" status, using the Accent Blue for "ON" states and low-opacity white for "OFF".
- **Input Fields:** Minimalist containers with 16px rounding, focusing on large tap areas for the keyboard.