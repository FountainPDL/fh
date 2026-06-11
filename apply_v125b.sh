#!/bin/bash
set -e
cd ~/fh
echo "=== Applying v1.25b (items 6-12) ==="

python3 write_java_v125b.py
python3 write_layouts_v125b.py

# Add AnimeListActivity to manifest if missing
if ! grep -q "AnimeListActivity" app/src/main/AndroidManifest.xml; then
    sed -i 's|<activity android:name=".ui.AnimeDetailActivity"|<activity android:name=".ui.AnimeListActivity" android:exported="false"/>\n        <activity android:name=".ui.AnimeDetailActivity"|' app/src/main/AndroidManifest.xml
    echo "Manifest patched"
fi

# Version bump to 1.25.1
sed -i 's/versionCode 5/versionCode 6/' app/build.gradle 2>/dev/null || true
sed -i 's/versionName "1.25"/versionName "1.25.1"/' app/build.gradle 2>/dev/null || true

git add .
git commit -m "v1.25.1 — infinite scroll Movies/TV, anime view-all pages, download tabs, episode touch fix, real video download"
git push
echo "=== Done ==="
