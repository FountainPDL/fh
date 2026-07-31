import os

print("=== v1.32: icon logic removed and rewritten from scratch ===")

# ── 1. Remove the old hand-drawn vector icon and adaptive-icon definitions.
#    You're using a real designed PNG now, so none of this is needed anymore. ──
old_icon_files = [
    "app/src/main/res/drawable/ic_launcher_fg.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
]
for f in old_icon_files:
    if os.path.exists(f):
        os.remove(f)
        print(f"[OK] removed {f}")
    else:
        print(f"[SKIP] {f} not present")

anydpi_dir = "app/src/main/res/mipmap-anydpi-v26"
if os.path.isdir(anydpi_dir) and not os.listdir(anydpi_dir):
    os.rmdir(anydpi_dir)
    print(f"[OK] removed now-empty {anydpi_dir}")

# ── 2. Fresh icon generation, PNG-only, all 5 densities + round variant. ──
try:
    from PIL import Image, ImageDraw
except ImportError:
    os.system("pip install Pillow --break-system-packages -q")
    from PIL import Image, ImageDraw

src = None
for p in ["icon_source.png", "/sdcard/Download/icon_source.png"]:
    if os.path.exists(p):
        src = p
        break

if src is None:
    print("[!!!] icon_source.png not found in ~/fh. Copy your new icon there as")
    print("      icon_source.png, then re-run this script.")
else:
    img = Image.open(src).convert("RGBA")
    w, h = img.size
    print(f"Source: {src} ({w}x{h})")
    if w != h:
        s = min(w, h)
        x, y = (w - s) // 2, (h - s) // 2
        img = img.crop((x, y, x + s, y + s))
        print(f"Cropped to square: {s}x{s}")

    sizes = {"mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96, "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192}
    for folder, size in sizes.items():
        out = f"app/src/main/res/{folder}"
        os.makedirs(out, exist_ok=True)
        # Clear anything old sitting in this folder first (XML or otherwise) so only
        # the two PNGs this script writes remain.
        for existing in os.listdir(out):
            if existing not in ("ic_launcher.png", "ic_launcher_round.png"):
                try: os.remove(os.path.join(out, existing))
                except Exception: pass

        square = img.resize((size, size), Image.LANCZOS)
        square.save(f"{out}/ic_launcher.png", optimize=True)

        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_img.paste(square, mask=mask)
        round_img.save(f"{out}/ic_launcher_round.png", optimize=True)
        print(f"[OK] {folder}: {size}x{size}")

print()
print("=== v1.32 icon rewrite complete ===")
