#!/bin/bash
set -e
cd ~/fh

if [ ! -f app/debug.keystore ]; then
  echo "!!! app/debug.keystore is missing."
  echo "!!! Copy the downloaded debug.keystore file to ~/fh/app/debug.keystore first, then re-run this script."
  exit 1
fi

echo "=== Applying v1.27 ==="
python3 write_java_v127.py
python3 write_layouts_v127.py
python3 patch_v127.py

sed -i 's/versionCode 7/versionCode 8/' app/build.gradle 2>/dev/null || true
sed -i 's/versionName "1.26"/versionName "1.27"/' app/build.gradle 2>/dev/null || true

git add .
git commit -m "v1.27 - no more WebView playback, fixed controls bug, 10 new sources incl. VidLink via AniList MAL id, download next to Play, stable APK signing"
git push
echo "=== Done - check GitHub Actions ==="
