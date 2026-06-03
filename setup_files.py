import os

def w(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True) if os.path.dirname(path) else None
    open(path, 'w', encoding='utf-8').write(text)

# ── Manifest ─────────────────────────────────────────────────────
w("app/src/main/AndroidManifest.xml", """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <application android:name=".FountainApp" android:allowBackup="true"
        android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name" android:theme="@style/Theme.FountainHome"
        android:usesCleartextTraffic="true" android:hardwareAccelerated="true">
        <activity android:name=".ui.MainActivity" android:exported="true" android:windowSoftInputMode="adjustResize">
            <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
        </activity>
        <activity android:name=".ui.WatchActivity" android:exported="false"/>
        <activity android:name=".ui.SearchActivity" android:exported="false"/>
        <activity android:name=".ui.AnimeDetailActivity" android:exported="false"/>
        <activity android:name=".ui.SettingsActivity" android:exported="false"/>
        <activity android:name=".ui.player.PlayerActivity" android:exported="false"
            android:screenOrientation="unspecified" android:supportsPictureInPicture="true"
            android:configChanges="screenSize|screenLayout|smallestScreenSize|orientation|keyboardHidden"/>
        <service android:name=".service.DownloadService" android:exported="false" android:foregroundServiceType="dataSync"/>
    </application>
</manifest>""")

w("app/src/main/res/values/strings.xml", '<resources><string name="app_name">Fountain Home</string></resources>\n')

w("app/src/main/res/values/themes.xml", """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FountainHome" parent="Theme.MaterialComponents.NoActionBar.Bridge">
        <item name="colorPrimary">#BB86FC</item>
        <item name="colorPrimaryVariant">#9B59FC</item>
        <item name="colorOnPrimary">#000000</item>
        <item name="colorSecondary">#BB86FC</item>
        <item name="colorSecondaryVariant">#9B59FC</item>
        <item name="colorOnSecondary">#000000</item>
        <item name="colorError">#CF6679</item>
        <item name="colorOnError">#FFFFFF</item>
        <item name="colorSurface">#141414</item>
        <item name="colorOnSurface">#FFFFFF</item>
        <item name="android:colorBackground">#0A0A0A</item>
        <item name="colorOnBackground">#FFFFFF</item>
        <item name="android:textColorPrimary">#FFFFFF</item>
        <item name="android:textColorSecondary">#AAAAAA</item>
        <item name="android:windowBackground">#0A0A0A</item>
        <item name="android:statusBarColor">#0A0A0A</item>
        <item name="android:navigationBarColor">#141414</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>""")

w("app/src/main/res/values/colors.xml", """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple">#FFBB86FC</color>
    <color name="surface">#FF141414</color>
    <color name="bg">#FF0A0A0A</color>
</resources>""")

# Drawables
for name, path in {
    "gradient_hero": None,
    "spinner_bg": None,
    "circle_bg": None,
    "ic_launcher_fg": None,
}.items():
    pass  # handled below

open("app/src/main/res/drawable/gradient_hero.xml","w").write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:startColor="#00000000" android:endColor="#FF000000" android:angle="270"/>
</shape>''')

open("app/src/main/res/drawable/spinner_bg.xml","w").write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#1E1E1E"/><corners android:radius="4dp"/>
    <stroke android:width="1dp" android:color="#333333"/>
</shape>''')

open("app/src/main/res/drawable/circle_bg.xml","w").write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#BB86FC"/>
</shape>''')

open("app/src/main/res/drawable/ic_launcher_fg.xml","w").write('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0E3A1A" android:pathData="M54,85 Q36,78 34,65 Q32,52 38,47 Q46,42 54,44 Q62,42 70,47 Q76,52 74,65 Q72,78 54,85Z"/>
    <path android:fillColor="#145C28" android:pathData="M54,72 Q42,67 41,60 Q40,54 46,51 Q54,50 62,51 Q68,54 67,60 Q66,67 54,72Z"/>
    <path android:fillColor="#00C878" android:fillAlpha="0.5" android:pathData="M54,48 Q54,30 54,5 Q54,25 54,48Z"/>
    <path android:fillColor="#BB86FC" android:pathData="M54,14 L66,34 L54,50 L42,34 Z"/>
    <path android:fillColor="#D4AAFF" android:pathData="M54,18 L63,33 L54,44 L48,33 Z"/>
    <path android:fillColor="#000000" android:pathData="M50,30 L50,42 L62,36 Z"/>
</vector>''')

ICONS = {
    "ic_nav_home":"M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z",
    "ic_nav_movies":"M18,4l2,4h-3l-2,-4h-2l2,4h-3l-2,-4H8l2,4H7L5,4H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V4h-4z",
    "ic_nav_tv":"M21,3H3C1.9,3 1,3.9 1,5v12c0,1.1 0.9,2 2,2h5v2h8v-2h5c1.1,0 2,-0.9 2,-2V5C23,3.9 22.1,3 21,3zM21,17H3V5h18V17z",
    "ic_nav_more":"M3,18h18v-2H3v2zm0,-5h18v-2H3v2zm0,-7v2h18V6H3z",
    "ic_search":"M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
    "ic_close":"M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
    "ic_add":"M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
    "ic_favorite":"M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z",
    "ic_watch_later":"M12,2C6.5,2 2,6.5 2,12s4.5,10 10,10 10,-4.5 10,-10S17.5,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8-3.59,8 -8,8zM12.5,7L11,7v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
    "ic_download":"M19,9h-4L15,3L9,3v6L5,9l7,7 7,-7zM5,18v2h14v-2L5,18z",
    "ic_check":"M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z",
    "ic_fullscreen":"M7,14H5v5h5v-2H7v-3zM5,10h2V7h3V5H5v5zM17,17h-3v2h5v-5h-2v3zM14,5v2h3v3h2V5h-5z",
    "ic_pip":"M19,7h-8v6h8L19,7zM21,3L3,3c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h18c1.1,0 2,-0.9 2,-2L23,5c0,-1.1 -0.9,-2 -2,-2zM21,19L3,19L3,4.97h18L21,19z",
    "ic_share":"M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3-3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z",
    "ic_info":"M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-6h2v6zM13,9h-2V7h2v2z",
    "ic_filter":"M10,18h4v-2h-4v2zM3,6v2h18L21,6L3,6zM6,13h12v-2L6,11v2z",
    "ic_play":"M8,5v14l11,-7z",
    "ic_anime":"M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,6c1.1,0 2,0.9 2,2s-0.9,2-2,2-2,-0.9 -2,-2 0.9,-2 2,-2zM12,18c-2.49,0-4.71-1.28-6-3.22 0.03,-1.99 4,-3.08 6,-3.08 1.99,0 5.97,1.09 6,3.08C16.71,16.72 14.49,18 12,18z",
    "ic_arrow_right":"M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z",
    "ic_star":"M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z",
}
for name, pd in ICONS.items():
    open(f"app/src/main/res/drawable/{name}.xml","w").write(
        f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="{pd}"/>
</vector>''')

w("app/src/main/res/menu/bottom_nav.xml", """<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/nav_home"   android:icon="@drawable/ic_nav_home"   android:title="Home"/>
    <item android:id="@+id/nav_movies" android:icon="@drawable/ic_nav_movies" android:title="Movies"/>
    <item android:id="@+id/nav_tv"     android:icon="@drawable/ic_nav_tv"     android:title="TV"/>
    <item android:id="@+id/nav_anime"  android:icon="@drawable/ic_anime"      android:title="Anime"/>
    <item android:id="@+id/nav_more"   android:icon="@drawable/ic_nav_more"   android:title="More"/>
</menu>""")

for fn, xml in {
    "title_pulse": '''<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <alpha android:fromAlpha="0.7" android:toAlpha="1.0" android:duration="1500"
        android:repeatMode="reverse" android:repeatCount="infinite"/>
</set>''',
    "slide_up": '''<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <translate android:fromYDelta="30dp" android:toYDelta="0" android:duration="350"/>
    <alpha android:fromAlpha="0" android:toAlpha="1" android:duration="350"/>
</set>''',
    "fade_scale_in": '''<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <scale android:fromXScale="0.96" android:toXScale="1.0" android:fromYScale="0.96"
        android:toYScale="1.0" android:pivotX="50%" android:pivotY="50%" android:duration="280"/>
    <alpha android:fromAlpha="0" android:toAlpha="1" android:duration="280"/>
</set>'''
}.items():
    w(f"app/src/main/res/anim/{fn}.xml", xml)

# Adaptive icon
adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background><color android:color="#120024"/></background>
    <foreground android:drawable="@drawable/ic_launcher_fg"/>
</adaptive-icon>'''
w("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml", adaptive)
w("app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml", adaptive)

# Resize icon PNG if available
try:
    from PIL import Image
    import glob
    src = None
    for p in ["icon_source.png", "/sdcard/Download/1002512526.png"]:
        if os.path.exists(p): src=p; break
    if src:
        img = Image.open(src).convert("RGBA")
        w2,h2=img.size
        if w2!=h2:
            s=min(w2,h2); img=img.crop(((w2-s)//2,(h2-s)//2,(w2+s)//2,(h2+s)//2))
        for folder,size in {"mipmap-mdpi":48,"mipmap-hdpi":72,"mipmap-xhdpi":96,"mipmap-xxhdpi":144,"mipmap-xxxhdpi":192}.items():
            out=f"app/src/main/res/{folder}"
            os.makedirs(out,exist_ok=True)
            r=img.resize((size,size),Image.LANCZOS)
            r.save(f"{out}/ic_launcher.png"); r.save(f"{out}/ic_launcher_round.png")
        print("Icon PNGs resized!")
    else:
        print("icon_source.png not found - using vector fallback")
except Exception as e:
    print(f"Icon: {e}")

print("Writing Java files...")
exec(open(os.path.expanduser("~/fh/write_java_files.py")).read())
print("All files written!")
