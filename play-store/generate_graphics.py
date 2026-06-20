import cairosvg
import os

base = r"H:\DriveVault\play-store"

def render(svg, path, w, h):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    cairosvg.svg2png(bytestring=svg.encode(), write_to=path, output_width=w, output_height=h)
    print(f"  {os.path.relpath(path, base)}  ({w}x{h})")

BG  = "#1A0D0B"  # deep charcoal
RED = "#FF8A7A"  # safety red
BLUE= "#A3C4FF"  # electric blue
WHT = "#F5F0EE"
GRY = "#9E948F"

def icon_art(s=1):
    return f"""<g transform="translate(0,0) scale({s})">
    <rect width="108" height="108" rx="20" fill="{BG}" stroke="{RED}" stroke-width="1.5"/>
    <path fill="{RED}" d="M54,30 L54,54 L38,54 Z"/>
    <path fill="{RED}" d="M54,30 L54,54 L70,54 Z"/>
    <path fill="#FCDBD6" d="M34,58 L74,58 L74,72 Q74,76 70,76 L38,76 Q34,76 34,72 Z"/>
    <circle cx="44" cy="64" r="3.5" fill="{BLUE}"/>
    <circle cx="64" cy="64" r="3.5" fill="{BLUE}"/>
</g>"""

# ────────────────────────────────────────────
# FEATURE GRAPHIC  1024 x 500
# ────────────────────────────────────────────
fg = f"""<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500">
<defs>
  <linearGradient id="g1" x1="0" y1="0" x2="1024" y2="500">
    <stop offset="0%" stop-color="{BG}"/>
    <stop offset="100%" stop-color="#2A1510"/>
  </linearGradient>
  <linearGradient id="g2" x1="0" y1="0" x2="200" y2="200">
    <stop offset="0%" stop-color="{RED}" stop-opacity=".12"/>
    <stop offset="100%" stop-color="{RED}" stop-opacity="0"/>
  </linearGradient>
</defs>
<rect width="1024" height="500" fill="url(#g1)"/>

<!-- subtle grid -->
<pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
  <path d="M 40 0 L 0 0 0 40" fill="none" stroke="{WHT}" stroke-opacity=".03" stroke-width=".5"/>
</pattern>
<rect width="1024" height="500" fill="url(#grid)"/>

<!-- glow behind icon -->
<circle cx="155" cy="250" r="140" fill="url(#g2)"/>

{icon_art(1.15)}

<text x="220" y="200" font-family="'Inter',system-ui,-apple-system,sans-serif" font-weight="800" font-size="52" fill="{WHT}" letter-spacing="-1">DriveVault</text>
<text x="220" y="248" font-family="'Inter',system-ui,-apple-system,sans-serif" font-weight="300" font-size="30" fill="{RED}" letter-spacing="2">DASHCAM</text>
<line x1="220" y1="270" x2="520" y2="270" stroke="{RED}" stroke-width="2" opacity=".4"/>

<text x="220" y="305" font-family="'Inter',system-ui,-apple-system,sans-serif" font-size="16" fill="{GRY}">Privacy-first dashcam for Android</text>
<text x="220" y="328" font-family="'Inter',system-ui,-apple-system,sans-serif" font-size="14" fill="{GRY}" opacity=".7">No account · No cloud uploads · No tracking</text>

<!-- badges -->
<g transform="translate(220,360)">
  <rect x="0" y="0" width="100" height="28" rx="14" fill="{RED}" opacity=".12"/>
  <text x="50" y="19" font-family="sans-serif" font-size="11" fill="{RED}" text-anchor="middle" font-weight="700">GPS Overlays</text>
  <rect x="110" y="0" width="100" height="28" rx="14" fill="{BLUE}" opacity=".12"/>
  <text x="160" y="19" font-family="sans-serif" font-size="11" fill="{BLUE}" text-anchor="middle" font-weight="700">Dual Camera</text>
  <rect x="220" y="0" width="120" height="28" rx="14" fill="{WHT}" opacity=".08"/>
  <text x="280" y="19" font-family="sans-serif" font-size="11" fill="{WHT}" text-anchor="middle" font-weight="600">Immich Sync</text>
  <rect x="350" y="0" width="100" height="28" rx="14" fill="{WHT}" opacity=".08"/>
  <text x="400" y="19" font-family="sans-serif" font-size="11" fill="{WHT}" text-anchor="middle" font-weight="600">MLKit AI</text>
</g>

<!-- right side decorative camera lens -->
<g transform="translate(820,250)" opacity=".06">
  <circle cx="0" cy="0" r="80" fill="none" stroke="{WHT}" stroke-width="1"/>
  <circle cx="0" cy="0" r="55" fill="none" stroke="{RED}" stroke-width="1.5"/>
  <circle cx="0" cy="0" r="25" fill="{WHT}"/>
  <circle cx="-15" cy="-18" r="5" fill="{BG}"/>
</g>

<!-- bottom-right route line -->
<polyline points="700,440 780,420 840,445 920,410 980,430" fill="none" stroke="{RED}" stroke-width="1.5" opacity=".08"/>
<circle cx="700" cy="440" r="3" fill="{RED}" opacity=".12"/>
<circle cx="980" cy="430" r="3" fill="{BLUE}" opacity=".12"/>
</svg>"""

# ────────────────────────────────────────────
# TV BANNER  1280 x 720
# ────────────────────────────────────────────
tv = f"""<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720">
<defs>
  <linearGradient id="g1t" x1="0" y1="0" x2="1280" y2="720">
    <stop offset="0%" stop-color="{BG}"/>
    <stop offset="100%" stop-color="#2A1510"/>
  </linearGradient>
  <linearGradient id="g2t" x1="0" y1="0" x2="280" y2="280">
    <stop offset="0%" stop-color="{RED}" stop-opacity=".10"/>
    <stop offset="100%" stop-color="{RED}" stop-opacity="0"/>
  </linearGradient>
</defs>
<rect width="1280" height="720" fill="url(#g1t)"/>

<pattern id="gridt" width="50" height="50" patternUnits="userSpaceOnUse">
  <path d="M 50 0 L 0 0 0 50" fill="none" stroke="{WHT}" stroke-opacity=".025" stroke-width=".5"/>
</pattern>
<rect width="1280" height="720" fill="url(#gridt)"/>

<circle cx="200" cy="360" r="180" fill="url(#g2t)"/>

{icon_art(1.5)}

<text x="300" y="290" font-family="'Inter',system-ui,-apple-system,sans-serif" font-weight="800" font-size="64" fill="{WHT}" letter-spacing="-1">DriveVault</text>
<text x="300" y="355" font-family="'Inter',system-ui,-apple-system,sans-serif" font-weight="300" font-size="38" fill="{RED}" letter-spacing="2">DASHCAM</text>
<line x1="300" y1="380" x2="680" y2="380" stroke="{RED}" stroke-width="2" opacity=".35"/>

<text x="300" y="425" font-family="'Inter',system-ui,-apple-system,sans-serif" font-size="20" fill="{GRY}">Privacy-first dashcam for Android</text>
<text x="300" y="455" font-family="'Inter',system-ui,-apple-system,sans-serif" font-size="16" fill="{GRY}" opacity=".65">No account · No cloud uploads · No tracking</text>

<g transform="translate(300,500)">
  <rect x="0" y="0" width="120" height="34" rx="17" fill="{RED}" opacity=".12"/>
  <text x="60" y="22" font-family="sans-serif" font-size="13" fill="{RED}" text-anchor="middle" font-weight="700">GPS Overlays</text>
  <rect x="135" y="0" width="120" height="34" rx="17" fill="{BLUE}" opacity=".12"/>
  <text x="195" y="22" font-family="sans-serif" font-size="13" fill="{BLUE}" text-anchor="middle" font-weight="700">Dual Camera</text>
  <rect x="270" y="0" width="140" height="34" rx="17" fill="{WHT}" opacity=".08"/>
  <text x="340" y="22" font-family="sans-serif" font-size="13" fill="{WHT}" text-anchor="middle" font-weight="600">Immich Sync</text>
  <rect x="425" y="0" width="120" height="34" rx="17" fill="{WHT}" opacity=".08"/>
  <text x="485" y="22" font-family="sans-serif" font-size="13" fill="{WHT}" text-anchor="middle" font-weight="600">MLKit AI</text>
  <rect x="560" y="0" width="140" height="34" rx="17" fill="{WHT}" opacity=".08"/>
  <text x="630" y="22" font-family="sans-serif" font-size="13" fill="{WHT}" text-anchor="middle" font-weight="600">Open Source</text>
</g>

<!-- right decorative camera lens large -->
<g transform="translate(1040,360)" opacity=".06">
  <circle cx="0" cy="0" r="110" fill="none" stroke="{WHT}" stroke-width="1"/>
  <circle cx="0" cy="0" r="75" fill="none" stroke="{RED}" stroke-width="1.5"/>
  <circle cx="0" cy="0" r="35" fill="{WHT}"/>
  <circle cx="-20" cy="-25" r="7" fill="{BG}"/>
</g>

<polyline points="880,640 980,610 1060,650 1160,600 1240,630" fill="none" stroke="{RED}" stroke-width="1.5" opacity=".06"/>
</svg>"""

# ────────────────────────────────────────────
# PROMO GRAPHIC  180 x 120
# ────────────────────────────────────────────
promo = f"""<svg xmlns="http://www.w3.org/2000/svg" width="180" height="120">
<rect width="180" height="120" fill="{BG}"/>
<rect width="180" height="120" fill="url(#g1)" opacity="0"/>
{icon_art(0.35)}
<text x="62" y="50" font-family="sans-serif" font-weight="800" font-size="15" fill="{WHT}">DriveVault</text>
<text x="62" y="66" font-family="sans-serif" font-weight="400" font-size="10" fill="{RED}">Dashcam</text>
<text x="62" y="86" font-family="sans-serif" font-weight="400" font-size="7" fill="{GRY}">Privacy-first</text>
<text x="62" y="97" font-family="sans-serif" font-weight="400" font-size="7" fill="{GRY}">dashcam for</text>
<text x="62" y="108" font-family="sans-serif" font-weight="400" font-size="7" fill="{GRY}">Android</text>
</svg>"""

render(fg, os.path.join(base, "graphics", "feature-graphic", "drivevault_feature-graphic.png"), 1024, 500)
render(fg, os.path.join(base, "graphics", "feature-graphic", "drivevault_feature-graphic.jpg"), 1024, 500)
render(tv,  os.path.join(base, "graphics", "tv-banner", "drivevault_tv-banner.png"), 1280, 720)
render(promo, os.path.join(base, "graphics", "promo-graphic", "drivevault_promo-graphic.png"), 180, 120)
print("All graphics regenerated.")
