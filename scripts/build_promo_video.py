"""
DriveVault Dashcam Promo Video Builder
Generates a 30-second promo video with TTS voiceover, screenshots, app icon, and captions.
Requirements: Python 3.8+, edge-tts, Pillow, numpy, ffmpeg
"""

import asyncio
import json
import math
import os
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ─── Configuration ───────────────────────────────────────────────
REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_FILE = REPO_ROOT / "docs" / "images" / "promo_video.mp4"
SCREENSHOTS_DIR = REPO_ROOT / "docs" / "images" / "screenshots"
TEMP_DIR = REPO_ROOT / "tmp_promo"
FONT_PATH = None  # Will use default if None

# Try to find a good font
POSSIBLE_FONTS = [
    "C:/Windows/Fonts/Inter.ttf",
    "C:/Windows/Fonts/Inter-SemiBold.ttf",
    "C:/Windows/Fonts/Inter-Bold.ttf",
    "C:/Windows/Fonts/segoeui.ttf",
    "C:/Windows/Fonts/seguiui.ttf",
    "C:/Windows/Fonts/arial.ttf",
    "C:/Windows/Fonts/arialbd.ttf",
    "/usr/share/fonts/truetype/inter/Inter.ttf",
    "/usr/share/fonts/Inter.ttf",
]
for f in POSSIBLE_FONTS:
    if os.path.exists(f):
        FONT_PATH = f
        break

# ─── Scene Definitions ───────────────────────────────────────────
# Each scene: (image_path, caption, voiceover_text, duration_sec)
SCENES = [
    {
        "image": None,  # icon-only scene
        "caption": "DriveVault Dashcam",
        "subtitle": "Your drives. Your data.",
        "voiceover": "DriveVault Dashcam. Your drives. Your data. A privacy-first dashcam app for Android.",
        "duration": 7.0,
    },
    {
        "image": SCREENSHOTS_DIR / "design_recording.png",
        "caption": "GPS & Telemetry Overlay",
        "subtitle": "Real-time speed, heading, coordinates & mini map",
        "voiceover": "Record your drives with real-time GPS overlays including speed, heading, and coordinates. Everything stays on your device, by default.",
        "duration": 8.0,
    },
    {
        "image": SCREENSHOTS_DIR / "design_dual_camera.png",
        "caption": "Dual Camera Support",
        "subtitle": "Simultaneous front + rear recording",
        "voiceover": "Dual camera support lets you record from both front and rear cameras simultaneously, with an integrated mini map.",
        "duration": 7.0,
    },
    {
        "image": SCREENSHOTS_DIR / "design_clip_library.png",
        "caption": "Clip Library & Management",
        "subtitle": "Browse, search, filter, and lock your footage",
        "voiceover": "Browse, search, and manage your clips with an intuitive library. Lock important footage to protect it from auto-deletion.",
        "duration": 7.0,
    },
    {
        "image": SCREENSHOTS_DIR / "design_clip_detail.png",
        "caption": "Privacy by Default",
        "subtitle": "No cloud uploads. No accounts. No tracking.",
        "voiceover": "Privacy by design. No cloud uploads, no accounts required, no ads, no tracking. You are always in control of your data.",
        "duration": 8.0,
    },
    {
        "image": SCREENSHOTS_DIR / "share_preview.png",
        "caption": "DriveVault Dashcam",
        "subtitle": "Open Source  ·  Free  ·  No Ads",
        "voiceover": "DriveVault Dashcam. Open source and completely free. Download now from GitHub.",
        "duration": 6.0,
    },
]

TOTAL_DURATION = sum(s["duration"] for s in SCENES)
W, H = 1920, 1080

# App icon SVG path data for Pillow rendering
APP_ICON_COLORS = {
    "bg_start": "#6c5ce7",
    "bg_end": "#a29bfe",
}


def ensure_dir(path):
    path.mkdir(parents=True, exist_ok=True)


def hex_to_rgb(hex_str):
    h = hex_str.lstrip("#")
    return tuple(int(h[i : i + 2], 16) for i in (0, 2, 4))


def create_gradient(w, h, color_start, color_end):
    """Create a vertical gradient image."""
    start = hex_to_rgb(color_start)
    end = hex_to_rgb(color_end)
    img = Image.new("RGB", (w, h))
    for y in range(h):
        r = int(start[0] + (end[0] - start[0]) * y / h)
        g = int(start[1] + (end[1] - start[1]) * y / h)
        b = int(start[2] + (end[2] - start[2]) * y / h)
        for x in range(w):
            img.putpixel((x, y), (r, g, b))
    return img


def create_phone_frame(image_path, target_w, target_h):
    """Load a phone screenshot and return (centered_on_bg, phone_only) composites."""
    phone = Image.open(image_path).convert("RGBA")

    # Calculate max phone size (width ~420px max)
    max_phone_w = 420
    max_phone_h = int(target_h * 0.75)
    scale = min(max_phone_w / phone.width, max_phone_h / phone.height)
    new_w = int(phone.width * scale)
    new_h = int(phone.height * scale)
    phone_resized = phone.resize((new_w, new_h), Image.LANCZOS)

    # Create rounded corners mask
    mask = Image.new("L", (new_w, new_h), 0)
    draw = ImageDraw.Draw(mask)
    radius = int(24 * scale / phone.width * 420 / max_phone_w)
    radius = max(radius, 12)
    draw.rounded_rectangle([0, 0, new_w, new_h], radius=radius, fill=255)

    phone_rounded = Image.new("RGBA", (new_w, new_h), (0, 0, 0, 0))
    phone_rounded.paste(phone_resized, (0, 0), mask)

    # Phone border
    border_img = Image.new("RGBA", (new_w + 6, new_h + 6), (0, 0, 0, 0))
    border_draw = ImageDraw.Draw(border_img)
    border_draw.rounded_rectangle(
        [0, 0, new_w + 5, new_h + 5], radius=radius + 3, outline=(255, 255, 255, 60), width=3
    )

    return phone_rounded, border_img


def create_app_icon(size=120):
    """Draw the DriveVault camera icon on a gradient rounded square."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # Rounded rect background with gradient
    draw = ImageDraw.Draw(img)
    # Draw rounded rectangle with gradient effect (simplified: use single color)
    bg_color = hex_to_rgb("#6c5ce7")
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.22), fill=bg_color + (255,))

    # Draw camera icon using simple shapes
    cx, cy = size // 2, size // 2
    s = size
    # Upper lens triangles
    draw.polygon([(int(s * 0.22), int(s * 0.6)), (int(s * 0.38), int(s * 0.35)),
                  (int(s * 0.54), int(s * 0.6))], fill=(255, 255, 255, 240))
    draw.polygon([(int(s * 0.46), int(s * 0.6)), (int(s * 0.62), int(s * 0.35)),
                  (int(s * 0.78), int(s * 0.6))], fill=(255, 255, 255, 140))

    # Bottom bar
    bar_y = int(s * 0.56)
    draw.rounded_rectangle([int(s * 0.1), bar_y, int(s * 0.9), int(s * 0.7)],
                           radius=3, fill=(255, 255, 255, 77))

    # Lens circles
    draw.ellipse([int(s * 0.25), int(s * 0.64), int(s * 0.38), int(s * 0.78)],
                 fill=(255, 255, 255, 102))
    draw.ellipse([int(s * 0.62), int(s * 0.64), int(s * 0.75), int(s * 0.78)],
                 fill=(255, 255, 255, 102))

    return img


def load_font(size, bold=False):
    """Try to load a font, fallback to default."""
    if FONT_PATH:
        try:
            return ImageFont.truetype(FONT_PATH, size)
        except Exception:
            pass
    try:
        return ImageFont.truetype("arial.ttf", size)
    except Exception:
        return ImageFont.load_default()


def wrap_text(text, font, max_width, draw):
    """Wrap text to fit within max_width."""
    words = text.split()
    lines = []
    current_line = ""
    for word in words:
        test_line = f"{current_line} {word}".strip()
        bbox = draw.textbbox((0, 0), test_line, font=font)
        if bbox[2] - bbox[0] <= max_width:
            current_line = test_line
        else:
            if current_line:
                lines.append(current_line)
            current_line = word
    if current_line:
        lines.append(current_line)
    return lines


def create_scene_image(scene, index):
    """Create a single composite image for one scene."""
    img = Image.new("RGBA", (W, H), (10, 10, 15, 255))

    # Background: dark gradient
    bg_gradient = create_gradient(W, H, "#0a0a0f", "#12121a")
    img.paste(bg_gradient, (0, 0))

    draw = ImageDraw.Draw(img)

    if scene["image"] and scene["image"].exists():
        # Create blurred background from screenshot
        phone_bg = Image.open(scene["image"]).convert("RGB")
        phone_bg = phone_bg.resize((W, H), Image.LANCZOS)
        phone_bg = phone_bg.filter(ImageFilter.GaussianBlur(radius=40))
        # Dim the blurred background
        dimmer = Image.new("RGBA", (W, H), (0, 0, 0, 160))
        phone_bg = Image.alpha_composite(phone_bg.convert("RGBA"), dimmer)
        img.paste(phone_bg, (0, 0), phone_bg)

        # Place phone screenshot
        phone_rounded, border = create_phone_frame(scene["image"], W, H)
        px = (W - phone_rounded.width) // 2
        py = int(H * 0.08)
        img.paste(phone_rounded, (px, py), phone_rounded)
        img.paste(border, (px - 3, py - 3), border)
    else:
        # Icon scene - show large app icon
        icon = create_app_icon(200)
        ix = (W - 200) // 2
        iy = int(H * 0.15)
        img.paste(icon, (ix, iy), icon)

    # Semi-transparent bottom bar for captions
    bar_h = 160
    bar = Image.new("RGBA", (W, bar_h), (0, 0, 0, 180))
    # Add blur effect by feathering top edge
    bar_draw = ImageDraw.Draw(bar)
    for i in range(20):
        alpha = int(180 * (1 - i / 20))
        bar_draw.line([(0, i), (W, i)], fill=(0, 0, 0, alpha))
    img.paste(bar, (0, H - bar_h), bar)

    # Caption text
    title_font = load_font(42, bold=True)
    subtitle_font = load_font(28)

    caption = scene["caption"]
    subtitle = scene["subtitle"]

    # Measure and center text
    title_bbox = draw.textbbox((0, 0), caption, font=title_font)
    title_w = title_bbox[2] - title_bbox[0]
    tx = (W - title_w) // 2
    ty = H - bar_h + 20
    draw.text((tx, ty), caption, fill=(240, 240, 245, 255), font=title_font)

    sub_bbox = draw.textbbox((0, 0), subtitle, font=subtitle_font)
    sub_w = sub_bbox[2] - sub_bbox[0]
    sx = (W - sub_w) // 2
    sy = ty + 52
    draw.text((sx, sy), subtitle, fill=(160, 160, 181, 255), font=subtitle_font)

    # App icon watermark top-left
    icon_small = create_app_icon(56)
    img.paste(icon_small, (24, 24), icon_small)

    # "DriveVault" text next to icon
    brand_font = load_font(22, bold=True)
    draw.text((90, 34), "DriveVault", fill=(240, 240, 245, 220), font=brand_font)

    # Scene indicator dots at bottom
    dot_y = H - 18
    for i in range(len(SCENES)):
        dot_color = (108, 92, 231, 255) if i == index else (255, 255, 255, 60)
        dot_r = 4
        dot_cx = W // 2 + (i - len(SCENES) // 2) * 24
        draw.ellipse([dot_cx - dot_r, dot_y - dot_r, dot_cx + dot_r, dot_y + dot_r],
                     fill=dot_color)

    return img


async def generate_voiceover():
    """Generate TTS audio using edge-tts."""
    print("=" * 60)
    print("Generating voiceover with edge-tts...")
    print("=" * 60)

    # Build full script with SSML for natural pauses
    ssml_parts = []
    for i, scene in enumerate(SCENES):
        text = scene["voiceover"]
        ssml_parts.append(f'<s>{text}</s>')

    ssml = f"""<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="en-US">
<voice name="en-US-AvaMultilingualNeural">
<prosody rate="+5%" pitch="+0%">
{" ".join(ssml_parts)}
</prosody>
</voice>
</speak>"""

    audio_path = TEMP_DIR / "voiceover.wav"

    # Write SSML to temp file
    ssml_path = TEMP_DIR / "voiceover.ssml"
    ssml_path.write_text(ssml, encoding="utf-8")

    # Use edge-tts
    cmd = [
        sys.executable, "-m", "edge_tts",
        "--voice", "en-US-AvaMultilingualNeural",
        "--rate", "+5%",
        "--text", SCENES[0]["voiceover"] + " " + SCENES[1]["voiceover"] + " " +
                  SCENES[2]["voiceover"] + " " + SCENES[3]["voiceover"] + " " +
                  SCENES[4]["voiceover"] + " " + SCENES[5]["voiceover"],
        "--write-media", str(audio_path),
    ]

    print(f"  Running: {' '.join(cmd)}")
    proc = await asyncio.create_subprocess_exec(
        *cmd, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE
    )
    stdout, stderr = await proc.communicate()

    if proc.returncode != 0:
        print(f"  edge-tts failed: {stderr.decode()}")
        return None

    # Check if audio file was created
    if audio_path.exists() and audio_path.stat().st_size > 0:
        print(f"  Voiceover saved: {audio_path} ({audio_path.stat().st_size / 1024:.1f} KB)")
        return audio_path
    else:
        print("  Failed to generate voiceover audio")
        return None


def get_audio_duration(audio_path):
    """Get audio duration using ffprobe."""
    cmd = [
        "ffprobe", "-v", "quiet", "-print_format", "json",
        "-show_format", str(audio_path)
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    data = json.loads(result.stdout)
    return float(data["format"]["duration"])


def create_scene_images():
    """Create composite images for all scenes."""
    print("\n" + "=" * 60)
    print("Creating scene composite images...")
    print("=" * 60)

    scene_dir = TEMP_DIR / "scenes"
    ensure_dir(scene_dir)

    for i, scene in enumerate(SCENES):
        print(f"  Scene {i + 1}/{len(SCENES)}: {scene['caption']}")
        img = create_scene_image(scene, i)
        path = scene_dir / f"scene_{i:02d}.png"
        img.save(str(path), "PNG")
        print(f"    Saved: {path}")

    return scene_dir


def create_video_segments(scene_dir):
    """Create video segments with Ken Burns zoom effect."""
    print("\n" + "=" * 60)
    print("Creating video segments with Ken Burns effect...")
    print("=" * 60)

    segments_dir = TEMP_DIR / "segments"
    ensure_dir(segments_dir)

    segment_files = []

    for i, scene in enumerate(SCENES):
        duration = scene["duration"]
        input_img = scene_dir / f"scene_{i:02d}.png"
        output_seg = segments_dir / f"seg_{i:02d}.mp4"

        # Ken Burns: slow zoom from 1.0 to 1.05
        # Use ffmpeg zoompan filter
        # fps=30, zoom from 1.0 to 1.05 over the duration
        fps = 30
        n_frames = int(duration * fps)

        # Simplify: use scale and zoompan for Ken Burns
        cmd = [
            "ffmpeg", "-y",
            "-loop", "1",
            "-i", str(input_img),
            "-vf",
            f"zoompan=z='min(zoom+0.0005,1.05)':d={n_frames}:s={W}x{H}:fps={fps}",
            "-c:v", "libx264",
            "-preset", "medium",
            "-crf", "18",
            "-pix_fmt", "yuv420p",
            "-t", str(duration),
            str(output_seg),
        ]

        print(f"  Segment {i + 1}/{len(SCENES)} ({duration}s): {scene['caption']}")
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"    Error: {result.stderr[:200]}")
            return None
        print(f"    Created: {output_seg}")
        segment_files.append(str(output_seg))

    return segment_files


def concatenate_segments(segment_files):
    """Concatenate video segments with crossfade transitions."""
    print("\n" + "=" * 60)
    print("Concatenating segments with crossfade...")
    print("=" * 60)

    final_no_audio = TEMP_DIR / "video_no_audio.mp4"

    if len(segment_files) == 1:
        # Simple copy
        cmd = [
            "ffmpeg", "-y",
            "-i", segment_files[0],
            "-c", "copy",
            str(final_no_audio),
        ]
        subprocess.run(cmd, capture_output=True)
        return final_no_audio

    # Use concat protocol with a file list
    list_file = TEMP_DIR / "segments.txt"
    with open(list_file, "w") as f:
        for seg in segment_files:
            f.write(f"file '{seg}'\n")

    cmd = [
        "ffmpeg", "-y",
        "-f", "concat",
        "-safe", "0",
        "-i", str(list_file),
        "-c", "copy",
        str(final_no_audio),
    ]

    print(f"  Concatenating {len(segment_files)} segments...")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  Error: {result.stderr[:300]}")
        return None

    print(f"  Created: {final_no_audio}")
    return final_no_audio


def add_audio_to_video(video_path, audio_path):
    """Mix audio into the final video, trimming/padding to match."""
    print("\n" + "=" * 60)
    print("Adding audio track...")
    print("=" * 60)

    # Get video and audio durations
    video_dur = get_audio_duration(video_path)
    audio_dur = get_audio_duration(audio_path)

    print(f"  Video duration: {video_dur:.2f}s")
    print(f"  Audio duration: {audio_dur:.2f}s")

    # If audio is shorter, we pad with silence at the end
    # If audio is longer, we trim
    if audio_dur < video_dur:
        # Pad audio with silence
        padded_audio = TEMP_DIR / "audio_padded.wav"
        silence_dur = video_dur - audio_dur
        cmd = [
            "ffmpeg", "-y",
            "-i", str(audio_path),
            "-filter_complex",
            f"adelay=0|0,apad=pad_dur={silence_dur}",
            str(padded_audio),
        ]
        subprocess.run(cmd, capture_output=True, text=True)
        audio_source = padded_audio
    elif audio_dur > video_dur:
        # Trim audio to video duration
        trimmed_audio = TEMP_DIR / "audio_trimmed.wav"
        cmd = [
            "ffmpeg", "-y",
            "-i", str(audio_path),
            "-t", str(video_dur),
            str(trimmed_audio),
        ]
        subprocess.run(cmd, capture_output=True, text=True)
        audio_source = trimmed_audio
    else:
        audio_source = audio_path

    # Mix audio into video
    cmd = [
        "ffmpeg", "-y",
        "-i", str(video_path),
        "-i", str(audio_source),
        "-c:v", "libx264",
        "-preset", "medium",
        "-crf", "18",
        "-c:a", "aac",
        "-b:a", "192k",
        "-shortest",
        "-movflags", "+faststart",
        str(OUTPUT_FILE),
    ]

    print(f"  Final render: {OUTPUT_FILE}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  Error: {result.stderr[:500]}")
        return False

    return True


def create_burned_captions():
    """Create an SRT subtitle file for hardcoded captions."""
    print("\n" + "=" * 60)
    print("Creating subtitle file...")
    print("=" * 60)

    srt_path = TEMP_DIR / "captions.srt"
    current_time = 0.0

    with open(srt_path, "w", encoding="utf-8") as f:
        for i, scene in enumerate(SCENES):
            start = current_time
            end = current_time + scene["duration"]

            # Format timestamps: HH:MM:SS,mmm
            def fmt(t):
                h = int(t // 3600)
                m = int((t % 3600) // 60)
                s = int(t % 60)
                ms = int((t - int(t)) * 1000)
                return f"{h:02d}:{m:02d}:{s:02d},{ms:03d}"

            f.write(f"{i + 1}\n")
            f.write(f"{fmt(start)} --> {fmt(end)}\n")
            f.write(f"{scene['caption']}\n")
            f.write(f"{scene['subtitle']}\n\n")

            current_time = end

    print(f"  Created: {srt_path} ({len(SCENES)} captions)")
    return srt_path


async def main():
    print("=" * 60)
    print("DriveVault Dashcam - Promo Video Builder")
    print("=" * 60)
    print(f"Output: {OUTPUT_FILE}")
    print(f"Total duration: {TOTAL_DURATION:.1f}s")
    print(f"Font: {FONT_PATH or 'default'}")

    # Ensure temp dir
    ensure_dir(TEMP_DIR)

    # Step 1: Generate voiceover
    audio_path = await generate_voiceover()
    if not audio_path:
        print("\nERROR: Failed to generate voiceover. Check edge-tts installation.")
        return False

    # Step 2: Get actual audio duration
    actual_dur = get_audio_duration(audio_path)
    print(f"\n  Actual audio duration: {actual_dur:.2f}s")

    # Step 3: Create scene images
    scene_dir = create_scene_images()

    # Step 4: Create video segments with Ken Burns
    segment_files = create_video_segments(scene_dir)
    if not segment_files:
        print("\nERROR: Failed to create video segments.")
        return False

    # Step 5: Concatenate segments
    video_no_audio = concatenate_segments(segment_files)
    if not video_no_audio:
        print("\nERROR: Failed to concatenate segments.")
        return False

    # Step 6: Create subtitle file
    srt_path = create_burned_captions()

    # Step 7: Burn subtitles and add audio
    # We'll redo the final render to include burned-in subtitles
    print("\n" + "=" * 60)
    print("Final render with captions...")
    print("=" * 60)

    # Get durations for syncing
    video_dur = get_audio_duration(video_no_audio)
    audio_dur = get_audio_duration(audio_path)

    # Prepare audio (pad/trim)
    if audio_dur < video_dur:
        padded_audio = TEMP_DIR / "audio_final.wav"
        silence_dur = video_dur - audio_dur
        cmd = [
            "ffmpeg", "-y",
            "-i", str(audio_path),
            "-filter_complex", f"apad=pad_dur={silence_dur}",
            str(padded_audio),
        ]
        subprocess.run(cmd, capture_output=True, text=True)
        final_audio = padded_audio
    elif audio_dur > video_dur:
        trimmed_audio = TEMP_DIR / "audio_final.wav"
        cmd = [
            "ffmpeg", "-y",
            "-i", str(audio_path),
            "-t", str(video_dur),
            str(trimmed_audio),
        ]
        subprocess.run(cmd, capture_output=True, text=True)
        final_audio = trimmed_audio
    else:
        final_audio = audio_path

    # Final render with burned-in captions
    cmd = [
        "ffmpeg", "-y",
        "-i", str(video_no_audio),
        "-i", str(final_audio),
        "-vf", f"subtitles={srt_path}:force_style='FontName=Arial,FontSize=20,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,BorderStyle=1,Outline=1,Shadow=0,MarginV=80'",
        "-c:v", "libx264",
        "-preset", "medium",
        "-crf", "18",
        "-c:a", "aac",
        "-b:a", "192k",
        "-shortest",
        "-movflags", "+faststart",
        str(OUTPUT_FILE),
    ]

    print(f"  Rendering: {OUTPUT_FILE}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  Error: {result.stderr[:500]}")
        return False

    if OUTPUT_FILE.exists():
        size_mb = OUTPUT_FILE.stat().st_size / (1024 * 1024)
        print(f"\n{'=' * 60}")
        print(f"SUCCESS! Promo video created: {OUTPUT_FILE}")
        print(f"  Size: {size_mb:.1f} MB")
        print(f"  Duration: {get_audio_duration(OUTPUT_FILE):.1f}s")
        print(f"{'=' * 60}")
        return True
    else:
        print("\nERROR: Output file not created.")
        return False


if __name__ == "__main__":
    success = asyncio.run(main())
    sys.exit(0 if success else 1)
