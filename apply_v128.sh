#!/bin/bash
set -e
cd ~/fh
echo "=== Applying v1.28 ==="
python3 fix_v128.py

sed -i 's/versionCode 8/versionCode 9/' app/build.gradle 2>/dev/null || true
sed -i 's/versionName "1.27"/versionName "1.28"/' app/build.gradle 2>/dev/null || true

git add .
git commit -m "v1.28 - fix broken playback (remove over-aggressive nav block on hidden resolver), sync source lists across settings/details/player, fix Continue Watching poster loss"
git push
echo "=== Done - check GitHub Actions ==="
