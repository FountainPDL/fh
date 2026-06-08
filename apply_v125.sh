#!/bin/bash
set -e
cd ~/fh
echo "=== Applying v1.25 updates ==="

# 1. Java files
echo "Writing Java files..."
python3 ~/fh/write_java_v125.py

# 2. Layout files
echo "Writing layouts..."
python3 ~/fh/write_layouts_v125.py

# 3. Fix icon
echo "Fixing icon..."
bash ~/fh/fix_icon.sh

# 4. Update version in build.gradle
sed -i 's/versionCode 4/versionCode 5/' app/build.gradle
sed -i 's/versionName "1.24"/versionName "1.25"/' app/build.gradle

echo "Committing..."
git add .
git commit -m "v1.25 — iframe touch fix, auto-advance sources, anime sources, expanded settings, icon fix, about page"
git push
echo "=== Done! Check GitHub Actions ==="
