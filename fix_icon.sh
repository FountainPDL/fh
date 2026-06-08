#!/bin/bash
cd ~/fh

echo "Fixing icon..."

# Remove ALL XMLs from anydpi-v26 — use PNGs only in density folders
rm -f app/src/main/res/mipmap-anydpi-v26/*.xml
rm -f app/src/main/res/mipmap-anydpi-v26/*.png
rmdir app/src/main/res/mipmap-anydpi-v26 2>/dev/null || true

echo "anydpi-v26 cleared"

# Crop and resize icon from source PNG
python3 << 'PYEOF'
import os, sys
try:
    from PIL import Image, ImageDraw
except ImportError:
    os.system("pip install Pillow --break-system-packages -q")
    from PIL import Image, ImageDraw

# Find icon source
src = None
for p in [
    "icon_source.png",
    "/sdcard/Download/1002512526.png",
    "/sdcard/Download/fountain_icon.png",
    "/sdcard/Download/icon.png",
]:
    if os.path.exists(p):
        src = p
        print(f"Found icon: {src}")
        break

if src is None:
    print("ERROR: No icon source PNG found.")
    print("Put your icon at ~/fh/icon_source.png and re-run")
    sys.exit(1)

img = Image.open(src).convert("RGBA")
w, h = img.size
print(f"Original: {w}x{h}")

# Crop to square from center — smarter crop: use more of the image
if w != h:
    s = min(w, h)
    x = (w - s) // 2
    y = (h - s) // 2
    img = img.crop((x, y, x + s, y + s))
    print(f"Cropped to: {s}x{s}")

sizes = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

for folder, size in sizes.items():
    out = f"app/src/main/res/{folder}"
    os.makedirs(out, exist_ok=True)

    # Standard square icon
    square = img.resize((size, size), Image.LANCZOS)
    square.save(f"{out}/ic_launcher.png", optimize=True)

    # Round icon — circular mask
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    result.paste(square, mask=mask)
    result.save(f"{out}/ic_launcher_round.png", optimize=True)

    print(f"  {folder}: {size}x{size} done")

print("Icon resize complete!")
PYEOF

