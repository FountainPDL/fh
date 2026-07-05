#!/bin/bash
set -e
cd ~/fh
echo "=== Applying v1.27b hotfix ==="
python3 fix_v127b.py

git add .
git commit -m "v1.27b hotfix - fix DownloadManager2 call to getAnimeSources (missing idMal arg after v1.27's signature change)"
git push
echo "=== Done - check GitHub Actions ==="
