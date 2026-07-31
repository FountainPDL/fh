#!/bin/bash
set -e
cd ~/fh

if [ ! -f icon_source.png ]; then
  echo "!!! icon_source.png not found at ~/fh/icon_source.png"
  echo "!!! Copy the downloaded icon_source.png there first, then re-run this script."
  exit 1
fi

echo "=== Applying v1.32 ==="
python3 fix_v132.py
python3 rewrite_icon_v132.py

sed -i 's/versionCode 12/versionCode 13/' app/build.gradle 2>/dev/null || true
sed -i 's/versionName "1.31"/versionName "1.32"/' app/build.gradle 2>/dev/null || true

git add .
git commit -m "v1.32 - fix HomeFragment build error (ObjectAnimator.setRepeatDelay doesn't exist), remove old vector icon and adaptive-icon logic, regenerate icon from new source"
git push
echo "=== Done - check GitHub Actions ==="
