#!/bin/bash
set -e
cd ~/fh
echo "=== Applying v1.31 ==="
python3 fix_v131a.py
echo ""
python3 fix_v131b.py
echo ""
python3 fix_v131c.py

sed -i 's/versionCode 11/versionCode 12/' app/build.gradle 2>/dev/null || true
sed -i 's/versionName "1.30"/versionName "1.31"/' app/build.gradle 2>/dev/null || true

git add .
git commit -m "v1.31 - fix anime items opening as wrong content everywhere, attach resolver WebView to a real view, wire Grid Columns setting, real shimmer animation on Fountain Home title"
git push
echo "=== Done - check GitHub Actions ==="
