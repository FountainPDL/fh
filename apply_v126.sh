#!/bin/bash
set -e
cd ~/fh
echo "=== Applying v1.26 - redirects fixed, AniLab-style player, more settings ==="

python3 write_java_v126.py
python3 write_layouts_v126.py

sed -i 's/versionCode 6/versionCode 7/' app/build.gradle 2>/dev/null || true
sed -i 's/versionName "1.25.1"/versionName "1.26"/' app/build.gradle 2>/dev/null || true

git add .
git commit -m "v1.26 - block redirects/popups in player, AniLab-style gesture controls, expanded player settings"
git push
echo "=== Done - check GitHub Actions ==="
