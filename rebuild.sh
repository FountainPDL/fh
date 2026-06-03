#!/bin/bash
set -e
cd ~/fh
echo "=== FOUNTAIN HOME COMPLETE REBUILD ==="

echo "Cleaning..."
rm -rf app/src/main/java/com/fountainhome
rm -rf app/src/main/res/layout
rm -rf app/src/main/res/values
rm -rf app/src/main/res/values-night
rm -rf app/src/main/res/drawable
rm -rf app/src/main/res/menu
rm -rf app/src/main/res/anim
rm -rf app/src/main/res/mipmap-anydpi-v26

mkdir -p app/src/main/java/com/fountainhome/streaming/{api,download,service,ui/{adapter,fragment,player,viewmodel},anime}
mkdir -p app/src/main/res/{layout,values,drawable,menu,anim,mipmap-anydpi-v26}
mkdir -p app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}

echo "Writing build files..."
cat > build.gradle << 'EOF'
plugins { id 'com.android.application' version '8.2.2' apply false }
EOF

cat > settings.gradle << 'EOF'
pluginManagement {
    repositories { gradlePluginPortal(); google(); mavenCentral() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven { url 'https://jitpack.io' } }
}
rootProject.name = "FountainHome"
include ':app'
EOF

cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=true
android.useAndroidX=true
android.enableJetifier=true
EOF

cat > app/build.gradle << 'EOF'
plugins { id 'com.android.application' }
android {
    namespace 'com.fountainhome.streaming'
    compileSdk 34
    defaultConfig {
        applicationId "com.fountainhome.streaming"
        minSdk 24; targetSdk 34; versionCode 4; versionName "1.24"
        buildConfigField "String", "TMDB_API_KEY", '"8baba8ab6b8bbe247645bcae7df63d0d"'
    }
    buildTypes {
        debug { debuggable true }
        release { minifyEnabled false; proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),'proguard-rules.pro' }
    }
    compileOptions { sourceCompatibility JavaVersion.VERSION_17; targetCompatibility JavaVersion.VERSION_17 }
    buildFeatures { viewBinding true; buildConfig true }
}
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.fragment:fragment:1.6.2'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'
    implementation 'androidx.media3:media3-datasource:1.2.1'
    implementation 'androidx.media3:media3-ui:1.2.1'
    implementation 'androidx.media3:media3-common:1.2.1'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
    implementation 'androidx.work:work-runtime:2.9.0'
}
EOF

cat > app/proguard-rules.pro << 'EOF'
-keep class com.fountainhome.** { *; }
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
EOF

echo "Running Python file writers..."
python3 ~/fh/setup_files.py
python3 ~/fh/write_java_files.py

echo "Resizing icon..."
python3 << 'PYEOF'
import os
try:
    from PIL import Image
    src = os.path.expanduser("~/fh/icon_source.png")
    if not os.path.exists(src):
        for p in ["/sdcard/Download/1002512526.png","/sdcard/Download/fountain_icon.png"]:
            if os.path.exists(p): src=p; break
    img = Image.open(src).convert("RGBA")
    w,h=img.size
    if w!=h:
        s=min(w,h); img=img.crop(((w-s)//2,(h-s)//2,(w+s)//2,(h+s)//2))
    for folder,size in {"mipmap-mdpi":48,"mipmap-hdpi":72,"mipmap-xhdpi":96,"mipmap-xxhdpi":144,"mipmap-xxxhdpi":192}.items():
        out=f"app/src/main/res/{folder}"
        os.makedirs(out,exist_ok=True)
        r=img.resize((size,size),Image.LANCZOS)
        r.save(f"{out}/ic_launcher.png"); r.save(f"{out}/ic_launcher_round.png")
        print(f"  {folder}: {size}px")
    print("Icon done!")
except Exception as e:
    print(f"Icon error: {e}")
PYEOF

echo "Verifying..."
COUNT=$(find app/src/main/java -name "*.java" | wc -l)
echo "  Java files: $COUNT"
test -f app/src/main/AndroidManifest.xml && echo "  Manifest: OK"
test -f app/src/main/res/values/themes.xml && echo "  Themes: OK"

echo "Pushing..."
git add .
git commit -m "v1.24 FULL REBUILD - all features, custom nav, video playback, adblock, anime hero"
git push
echo "=== DONE - check GitHub Actions ==="
