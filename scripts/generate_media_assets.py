#!/usr/bin/env python3
"""Generate Play Store media assets for Ether VPN.

Outputs:
  media-assets/icon/play_store_icon.png        (512x512)
  media-assets/feature/feature_graphic.png     (1024x500)
  media-assets/screenshots/<name>.png          (raw device shots, untouched)
  media-assets/framed/<n>_<name>.png           (marketing-styled portrait shots)
"""

import math
import os
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "media-assets"
RAW = ASSETS / "screenshots"
ICON_OUT = ASSETS / "icon"
FEATURE_OUT = ASSETS / "feature"
FRAMED_OUT = ASSETS / "framed"

for p in (ICON_OUT, FEATURE_OUT, FRAMED_OUT):
    p.mkdir(parents=True, exist_ok=True)

# Brand palette (from app/src/main/res/values/colors.xml)
BG_0 = (6, 8, 15)        # deepest
BG_1 = (11, 16, 32)
BG_2 = (14, 21, 48)
ACCENT = (185, 138, 255)         # violet B98AFF
ACCENT_DIM = (122, 86, 200)      # 7A56C8
INK_0 = (232, 236, 244)          # primary text
INK_1 = (168, 181, 200)          # secondary
PROTECTED = (74, 222, 128)        # 4ADE80

# Fonts
FONT_HEADLINE = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
FONT_BODY = "/System/Library/Fonts/Supplemental/Arial.ttf"
FONT_MONO = "/System/Library/Fonts/Menlo.ttc"


def font(size, weight="bold"):
    path = FONT_HEADLINE if weight == "bold" else FONT_BODY
    if not Path(path).exists():
        return ImageFont.load_default()
    return ImageFont.truetype(path, size)


def vertical_gradient(size, top, bottom):
    w, h = size
    base = Image.new("RGB", size, top)
    px = base.load()
    for y in range(h):
        t = y / max(1, h - 1)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        for x in range(w):
            px[x, y] = (r, g, b)
    return base


def radial_glow(size, color, radius_ratio=0.6, intensity=255):
    """Soft radial glow centered in canvas, returned as RGBA."""
    w, h = size
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    px = img.load()
    cx, cy = w / 2, h / 2
    max_r = min(w, h) * radius_ratio
    for y in range(h):
        for x in range(w):
            d = math.hypot(x - cx, y - cy)
            if d >= max_r:
                continue
            t = 1 - (d / max_r)
            t = t * t  # ease
            a = int(intensity * t)
            px[x, y] = (color[0], color[1], color[2], a)
    return img


def starfield(size, density=0.0008, seed=7):
    """Subtle dotted starfield over transparent."""
    w, h = size
    rng = random.Random(seed)
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    count = int(w * h * density)
    for _ in range(count):
        x = rng.randint(0, w - 1)
        y = rng.randint(0, h - 1)
        r = rng.choice([1, 1, 1, 2, 2, 3])
        a = rng.randint(80, 220)
        col = rng.choice([(255, 255, 255, a), (200, 215, 255, a), (185, 138, 255, a)])
        draw.ellipse((x - r, y - r, x + r, y + r), fill=col)
    img = img.filter(ImageFilter.GaussianBlur(radius=0.6))
    return img


def soft_circle(size, color, blur=20):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((0, 0, size, size), fill=color)
    return img.filter(ImageFilter.GaussianBlur(radius=blur))


# ──────────────────────────────────────────────────────────────────────
# 1. Play Store icon (512x512)
# ──────────────────────────────────────────────────────────────────────

def cleaned_astronaut(size, feather_ratio=0.92, watermark_patch=True):
    """Load astronaut PNG, cover the AI watermark, feather the edges so the
    square photo background blends seamlessly into our scene."""
    src = ROOT / "app/src/main/res/mipmap-xxxhdpi/astronaut_foreground.png"
    if not src.exists():
        # fall back to the playstore icon
        src = ROOT / "app/src/main/astronaut_icon-playstore.png"
    img = Image.open(src).convert("RGBA")

    # Tighten any padding then resize to a square `size`.
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
    img = img.resize((size, size), Image.LANCZOS)

    # Cover the AI generator pencil watermark sitting in the bottom-right ~7% area.
    if watermark_patch:
        d = ImageDraw.Draw(img)
        wm = int(size * 0.16)
        # Sample a chunk of the surrounding sky to use as a fill colour.
        sample = img.getpixel((int(size * 0.6), int(size * 0.85)))[:3]
        d.rectangle((size - wm, size - wm, size, size), fill=sample + (255,))
        # Blur just that patch to break up the seam.
        patch = img.crop((size - wm - 10, size - wm - 10, size, size))
        patch = patch.filter(ImageFilter.GaussianBlur(6))
        img.paste(patch, (size - wm - 10, size - wm - 10))

    # Feather the edges with a large soft circular alpha mask so the square
    # photographic background fades into our gradient.
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    pad = int(size * (1 - feather_ratio) / 2)
    md.ellipse((pad, pad, size - pad, size - pad), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(size * 0.06))

    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def make_icon():
    S = 512
    canvas = vertical_gradient((S, S), BG_2, BG_0).convert("RGBA")

    # Strong violet glow upper-left for drama
    glow = radial_glow((S, S), ACCENT, radius_ratio=0.95, intensity=170)
    glow_pos = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    glow_pos.paste(glow, (-90, -120), glow)
    canvas = Image.alpha_composite(canvas, glow_pos)

    # Cooler indigo from bottom-right for depth
    glow2 = radial_glow((S, S), (80, 100, 220), radius_ratio=0.85, intensity=130)
    pos2 = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pos2.paste(glow2, (140, 180), glow2)
    canvas = Image.alpha_composite(canvas, pos2)

    # Starfield
    canvas = Image.alpha_composite(canvas, starfield((S, S), density=0.0010, seed=11))

    # Subtle concentric orbital rings (echoes the in-app orb)
    rings = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rings)
    cx, cy = S // 2, S // 2
    for radius, alpha, w in [(248, 28, 2), (212, 42, 2), (180, 60, 2)]:
        rd.ellipse((cx - radius, cy - radius, cx + radius, cy + radius),
                   outline=(ACCENT[0], ACCENT[1], ACCENT[2], alpha), width=w)
    rings = rings.filter(ImageFilter.GaussianBlur(0.6))
    canvas = Image.alpha_composite(canvas, rings)

    # Big violet halo behind the astronaut
    halo = soft_circle(420, (ACCENT[0], ACCENT[1], ACCENT[2], 170), blur=55)
    canvas.alpha_composite(halo, ((S - 420) // 2, (S - 420) // 2 + 6))

    # Astronaut — feathered into the scene
    astro_size = int(S * 0.86)
    astro = cleaned_astronaut(astro_size, feather_ratio=0.94)
    canvas.alpha_composite(
        astro, ((S - astro_size) // 2, (S - astro_size) // 2 + 4))

    # Front rim light to lift the astronaut shoulders
    rim = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rd2 = ImageDraw.Draw(rim)
    rd2.ellipse((cx - 200, cy - 60, cx + 200, cy + 280),
                fill=(ACCENT[0], ACCENT[1], ACCENT[2], 0))
    rd2.arc((cx - 200, cy - 60, cx + 200, cy + 280), start=200, end=340,
            fill=(ACCENT[0], ACCENT[1], ACCENT[2], 80), width=3)
    rim = rim.filter(ImageFilter.GaussianBlur(8))
    canvas = Image.alpha_composite(canvas, rim)

    # Bottom shadow/vignette to ground the icon
    vignette = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    vd = ImageDraw.Draw(vignette)
    vd.rectangle((0, S - 110, S, S), fill=(0, 0, 0, 130))
    vignette = vignette.filter(ImageFilter.GaussianBlur(radius=40))
    canvas = Image.alpha_composite(canvas, vignette)

    out = ICON_OUT / "play_store_icon.png"
    canvas.convert("RGB").save(out, "PNG", optimize=True)
    print(f"icon -> {out}")

    # Adaptive launcher foreground (circular safe-zone within 432x432)
    fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    fg_canvas = canvas.resize((432, 432), Image.LANCZOS)
    mask = Image.new("L", (432, 432), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse((30, 30, 432 - 30, 432 - 30), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(3))
    fg.paste(fg_canvas, (0, 0), mask)
    fg.save(ICON_OUT / "adaptive_foreground_432.png", "PNG")
    print(f"icon -> {ICON_OUT / 'adaptive_foreground_432.png'}")


# ──────────────────────────────────────────────────────────────────────
# 2. Feature graphic (1024x500)
# ──────────────────────────────────────────────────────────────────────

def make_feature_graphic():
    W, H = 1024, 500
    canvas = vertical_gradient((W, H), BG_2, BG_0).convert("RGBA")

    # Big violet glow from right
    glow = radial_glow((900, 900), ACCENT, radius_ratio=0.9, intensity=140)
    pos = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pos.paste(glow, (W - 700, -200), glow)
    canvas = Image.alpha_composite(canvas, pos)

    # Secondary cyan-ish tint glow from bottom-left
    glow2 = radial_glow((700, 700), (90, 110, 200), radius_ratio=0.9, intensity=110)
    pos2 = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pos2.paste(glow2, (-280, 100), glow2)
    canvas = Image.alpha_composite(canvas, pos2)

    # Stars
    canvas = Image.alpha_composite(canvas, starfield((W, H), density=0.0007, seed=3))

    # Orbital ring on right side
    rings = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rings)
    cx, cy = int(W * 0.78), int(H * 0.5)
    for radius, alpha in [(240, 40), (200, 60), (160, 80), (120, 110)]:
        rd.ellipse((cx - radius, cy - radius, cx + radius, cy + radius),
                   outline=(ACCENT[0], ACCENT[1], ACCENT[2], alpha), width=2)
    rings = rings.filter(ImageFilter.GaussianBlur(0.6))
    canvas = Image.alpha_composite(canvas, rings)

    # Astronaut on the right with halo
    halo = soft_circle(460, (ACCENT[0], ACCENT[1], ACCENT[2], 170), blur=55)
    canvas.alpha_composite(halo, (cx - 230, cy - 230))
    astro_size = 460
    astro = cleaned_astronaut(astro_size, feather_ratio=0.94)
    canvas.alpha_composite(astro, (cx - astro_size // 2, cy - astro_size // 2 + 6))

    # Text
    draw = ImageDraw.Draw(canvas)
    # Eyebrow
    draw.text((68, 110), "DECENTRALIZED · OPENVPN", font=font(20, "bold"),
              fill=(ACCENT[0], ACCENT[1], ACCENT[2]))
    # Title
    draw.text((64, 144), "Ether VPN", font=font(96, "bold"), fill=INK_0)
    # Subtitle
    draw.text((68, 264), "Free, ad-free, end-to-end encrypted.",
              font=font(28, "regular"), fill=INK_0)
    draw.text((68, 304), "Browse the metaverse without a trace.",
              font=font(28, "regular"), fill=INK_1)

    # Tag chip
    chip_text = "● PROTECTED"
    chip_font = font(20, "bold")
    tw = draw.textlength(chip_text, font=chip_font)
    chip_w, chip_h = int(tw + 28), 40
    chip_x, chip_y = 64, 380
    chip = Image.new("RGBA", (chip_w, chip_h), (0, 0, 0, 0))
    cd = ImageDraw.Draw(chip)
    cd.rounded_rectangle((0, 0, chip_w - 1, chip_h - 1), radius=20,
                         fill=(PROTECTED[0], PROTECTED[1], PROTECTED[2], 38),
                         outline=(PROTECTED[0], PROTECTED[1], PROTECTED[2], 200), width=2)
    cd.text((14, 9), chip_text, font=chip_font, fill=PROTECTED)
    canvas.alpha_composite(chip, (chip_x, chip_y))

    out = FEATURE_OUT / "feature_graphic.png"
    canvas.convert("RGB").save(out, "PNG", optimize=True)
    print(f"feature -> {out}")


# ──────────────────────────────────────────────────────────────────────
# 3. Marketing-styled framed screenshots (1080x1920 portrait)
# ──────────────────────────────────────────────────────────────────────

SCREENSHOT_COPY = {
    "01_home_disconnected": (
        "Take back your privacy.",
        "One tap to encrypt every connection.",
    ),
    "02_server_list": (
        "Servers on every continent.",
        "Free and premium locations, zero logs.",
    ),
    "03_connecting": (
        "Establishing a secure tunnel.",
        "OpenVPN end-to-end in seconds.",
    ),
    "04_connected": (
        "Protected. Always.",
        "Ad-free, no tracking, no subscription.",
    ),
    "05_locations_drawer": (
        "Hop continents instantly.",
        "Pick a region and you're there.",
    ),
    "06_about": (
        "Built for the community.",
        "Open source. DAO governed. $EVPN-powered.",
    ),
}

ORDER = [
    "01_home_disconnected",
    "04_connected",
    "02_server_list",
    "03_connecting",
    "05_locations_drawer",
    "06_about",
]


def round_corners(img, radius):
    w, h = img.size
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle((0, 0, w, h), radius=radius, fill=255)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.paste(img, (0, 0))
    out.putalpha(mask)
    return out


def drop_shadow(img, offset=(0, 18), blur=28, alpha=160):
    w, h = img.size
    pad = blur * 2
    shadow_canvas = Image.new("RGBA", (w + pad * 2, h + pad * 2), (0, 0, 0, 0))
    base = Image.new("RGBA", img.size, (0, 0, 0, alpha))
    base.putalpha(img.split()[-1].point(lambda a: alpha if a > 0 else 0))
    shadow_canvas.paste(base, (pad + offset[0], pad + offset[1]), base)
    shadow_canvas = shadow_canvas.filter(ImageFilter.GaussianBlur(blur))
    return shadow_canvas, pad


def make_framed(name, headline, subhead, index):
    raw_path = RAW / f"{name}.png"
    if not raw_path.exists():
        print(f"!! missing {raw_path}")
        return
    shot = Image.open(raw_path).convert("RGBA")

    W, H = 1242, 2208      # Play Store-friendly portrait canvas
    canvas = vertical_gradient((W, H), BG_2, BG_0).convert("RGBA")

    # Glow tinted by slot
    tint = ACCENT if index % 2 == 0 else (90, 130, 220)
    glow = radial_glow((1200, 1200), tint, radius_ratio=0.9, intensity=150)
    pos = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pos.paste(glow, (-300 + (index % 3) * 200, -200), glow)
    canvas = Image.alpha_composite(canvas, pos)
    canvas = Image.alpha_composite(canvas, starfield((W, H), density=0.0006, seed=index * 7 + 1))

    # Headline (top)
    draw = ImageDraw.Draw(canvas)
    eyebrow = "ETHER VPN"
    draw.text((W // 2, 90), eyebrow, font=font(28, "bold"),
              fill=(ACCENT[0], ACCENT[1], ACCENT[2]), anchor="mm")

    # Wrap headline if too long
    head_font = font(72, "bold")
    sub_font = font(36, "regular")
    draw.text((W // 2, 180), headline, font=head_font, fill=INK_0, anchor="mm")
    draw.text((W // 2, 250), subhead, font=sub_font, fill=INK_1, anchor="mm")

    # Phone screenshot — round corners, drop shadow, place center
    target_w = 820
    ratio = target_w / shot.size[0]
    target_h = int(shot.size[1] * ratio)
    shot_resized = shot.resize((target_w, target_h), Image.LANCZOS)
    shot_rounded = round_corners(shot_resized, radius=64)

    shadow, pad = drop_shadow(shot_rounded, offset=(0, 24), blur=36, alpha=180)
    shot_x = (W - target_w) // 2
    shot_y = 320
    canvas.alpha_composite(shadow, (shot_x - pad, shot_y - pad))

    # Subtle white outline around the frame
    outline = Image.new("RGBA", shot_rounded.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(outline)
    od.rounded_rectangle((0, 0, target_w - 1, target_h - 1), radius=64,
                         outline=(255, 255, 255, 38), width=2)
    canvas.alpha_composite(shot_rounded, (shot_x, shot_y))
    canvas.alpha_composite(outline, (shot_x, shot_y))

    out = FRAMED_OUT / f"{index+1:02d}_{name}.png"
    canvas.convert("RGB").save(out, "PNG", optimize=True)
    print(f"framed -> {out}")


def make_all_framed():
    for i, name in enumerate(ORDER):
        head, sub = SCREENSHOT_COPY[name]
        make_framed(name, head, sub, i)


if __name__ == "__main__":
    make_icon()
    make_feature_graphic()
    make_all_framed()
