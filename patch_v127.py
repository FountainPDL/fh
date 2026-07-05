import os

print("=== v1.27 targeted patches ===")

# ── 1. Add a real-download button next to Play/Resume in WatchActivity ──
watch_java = "app/src/main/java/com/fountainhome/streaming/ui/WatchActivity.java"
if os.path.exists(watch_java):
    content = open(watch_java, encoding='utf-8').read()
    anchor = "b.resumeBtn.setOnClickListener(v->openPlayer(ci));}"
    addition = (
        "\n        b.downloadMovieBtn.setOnClickListener(v->{"
        "DownloadManager2.downloadVideo(this,ci,selSeason,selEpisode);"
        "Toast.makeText(this,\"Download started \\u2014 check the notification for progress\",Toast.LENGTH_LONG).show();});"
    )
    if anchor in content and "downloadMovieBtn" not in content:
        content = content.replace(anchor, anchor + addition, 1)
        open(watch_java, 'w', encoding='utf-8').write(content)
        print("[OK] WatchActivity.java patched with download-next-to-play button")
    elif "downloadMovieBtn" in content:
        print("[SKIP] WatchActivity.java already patched")
    else:
        print("[WARN] Could not find expected anchor in WatchActivity.java.")
        print("       Add this manually inside populate(ContentItem ci), right after the resume button setup:")
        print("      ", addition.strip())
else:
    print("[WARN] WatchActivity.java not found at expected path — skipping")

# ── 2. Add the button itself to the layout, beside Play/Resume ──
watch_xml = "app/src/main/res/layout/activity_watch.xml"
if os.path.exists(watch_xml):
    content = open(watch_xml, encoding='utf-8').read()
    anchor = ('<Button android:id="@+id/resume_btn"\n'
              '                        android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="1"\n'
              '                        android:text="RESUME" android:textSize="13sp" android:textColor="#BB86FC"\n'
              '                        android:backgroundTint="#22BB86FC" android:visibility="gone"/>')
    addition = ('\n                    <ImageButton android:id="@+id/download_movie_btn"\n'
                '                        android:layout_width="48dp" android:layout_height="48dp"\n'
                '                        android:src="@drawable/ic_download" android:tint="#FFFFFF"\n'
                '                        android:background="#1E1E1E" android:padding="12dp"\n'
                '                        android:layout_marginStart="8dp"\n'
                '                        android:contentDescription="Download video"/>')
    if anchor in content and "download_movie_btn" not in content:
        content = content.replace(anchor, anchor + addition, 1)
        open(watch_xml, 'w', encoding='utf-8').write(content)
        print("[OK] activity_watch.xml patched with download_movie_btn")
    elif "download_movie_btn" in content:
        print("[SKIP] activity_watch.xml already patched")
    else:
        print("[WARN] Could not find expected resume_btn block in activity_watch.xml.")
        print("       Add this manually right after the resume_btn Button element:")
        print(addition)
else:
    print("[WARN] activity_watch.xml not found at expected path — skipping")

# ── 3. Stable debug signing so updates install without uninstalling first ──
gradle_path = "app/build.gradle"
if os.path.exists(gradle_path):
    content = open(gradle_path, encoding='utf-8').read()
    if 'signingConfigs' not in content:
        content = content.replace(
            "    buildTypes {",
            "    signingConfigs {\n"
            "        debug {\n"
            "            storeFile file('debug.keystore')\n"
            "            storePassword 'android'\n"
            "            keyAlias 'androiddebugkey'\n"
            "            keyPassword 'android'\n"
            "        }\n"
            "    }\n"
            "    buildTypes {",
            1
        )
        if "debug { debuggable true }" in content:
            content = content.replace(
                "debug { debuggable true }",
                "debug { debuggable true; signingConfig signingConfigs.debug }",
                1
            )
            open(gradle_path, 'w', encoding='utf-8').write(content)
            print("[OK] app/build.gradle patched with stable debug signing config")
        else:
            print("[WARN] Found buildTypes but not the expected 'debug { debuggable true }' line.")
            print("       Add 'signingConfig signingConfigs.debug' inside buildTypes.debug manually.")
    else:
        print("[SKIP] app/build.gradle already has signingConfigs — skipping")
else:
    print("[WARN] app/build.gradle not found — skipping")

if not os.path.exists("app/debug.keystore"):
    print()
    print("[!!!] app/debug.keystore is MISSING. Copy the downloaded debug.keystore file to")
    print("      ~/fh/app/debug.keystore BEFORE pushing, or every build will fail.")

# ── 4. Regenerate icon assets from the (possibly replaced) icon_source.png ──
try:
    from PIL import Image, ImageDraw
    src = None
    for p in ["icon_source.png", "/sdcard/Download/icon_source.png"]:
        if os.path.exists(p):
            src = p
            break
    if src:
        img = Image.open(src).convert("RGBA")
        w2, h2 = img.size
        if w2 != h2:
            s = min(w2, h2)
            x, y = (w2 - s) // 2, (h2 - s) // 2
            img = img.crop((x, y, x + s, y + s))
        sizes = {"mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96, "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192}
        for folder, size in sizes.items():
            out = f"app/src/main/res/{folder}"
            os.makedirs(out, exist_ok=True)
            square = img.resize((size, size), Image.LANCZOS)
            square.save(f"{out}/ic_launcher.png", optimize=True)
            mask = Image.new("L", (size, size), 0)
            ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
            round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            round_img.paste(square, mask=mask)
            round_img.save(f"{out}/ic_launcher_round.png", optimize=True)
        print(f"[OK] Icon regenerated from {src} at 5 densities")
    else:
        print("[WARN] icon_source.png not found in ~/fh — icon not regenerated")
except ImportError:
    print("[WARN] Pillow not installed — run: pip install Pillow --break-system-packages")

print()
print("=== Patches complete ===")
