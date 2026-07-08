#!/bin/bash
set -e
cd ~/fh
echo "=== Applying v1.30 ==="
python3 fix_v130.py

sed -i 's/versionCode 10/versionCode 11/' app/build.gradle 2>/dev/null || true
sed -i 's/versionName "1.29"/versionName "1.30"/' app/build.gradle 2>/dev/null || true

git add .
git commit -m "v1.30 - remove popup/window blocking from the hidden stream resolver, which was silently cutting off every source's redirect to its real video request"
git push
echo "=== Done - check GitHub Actions ==="
