#!/bin/bash
set -e
cd ~/fh

echo "╔══════════════════════════════════════════╗"
echo "║   FOUNTAIN HOME v1.23 — FULL REBUILD     ║"
echo "╚══════════════════════════════════════════╝"

echo ""
echo "Step 1: Copying helper scripts..."
cp ~/fh/setup_files.py /tmp/setup_files.py 2>/dev/null || true
cp ~/fh/write_java_files.py /tmp/write_java_files.py 2>/dev/null || true

echo "Step 2: Cleaning old source..."
rm -rf app/src/main/java/com/fountainhome
rm -rf app/src/main/res/layout
rm -rf app/src/main/res/values
rm -rf app/src/main/res/values-night
rm -rf app/src/main/res/drawable
rm -rf app/src/main/res/menu
rm -rf app/src/main/res/mipmap-anydpi-v26

mkdir -p app/src/main/java/com/fountainhome/streaming/{api,download,service,ui/{adapter,fragment,player,viewmodel},anime}
mkdir -p app/src/main/res/{layout,values,values-night,drawable,menu,mipmap-anydpi-v26}
mkdir -p app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}

echo "Step 3: Writing build config files..."

cat > build.gradle << 'GRADLE'
plugins { id 'com.android.application' version '8.2.2' apply false }
GRADLE

cat > settings.gradle << 'GRADLE'
pluginManagement {
    repositories { gradlePluginPortal(); google(); mavenCentral() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven { url 'https://jitpack.io' } }
}
rootProject.name = "FountainHome"
include ':app'
GRADLE

cat > gradle.properties << 'GRADLE'
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=true
android.useAndroidX=true
android.enableJetifier=true
GRADLE

cat > app/proguard-rules.pro << 'GRADLE'
-keep class com.fountainhome.** { *; }
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
GRADLE

cat > app/build.gradle << 'GRADLE'
plugins { id 'com.android.application' }
android {
    namespace 'com.fountainhome.streaming'
    compileSdk 34
    defaultConfig {
        applicationId "com.fountainhome.streaming"
        minSdk 24; targetSdk 34; versionCode 3; versionName "1.23"
        buildConfigField "String", "TMDB_API_KEY", '"8baba8ab6b8bbe247645bcae7df63d0d"'
    }
    buildTypes {
        debug { debuggable true }
        release { minifyEnabled false; proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro' }
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
GRADLE

echo "Step 4: Running Python file writers..."
python3 ~/fh/setup_files.py

echo ""
echo "Step 5: Verifying key files..."
test -f app/src/main/AndroidManifest.xml && echo "  manifest OK" || echo "  MISSING manifest!"
test -f app/src/main/res/layout/activity_main.xml && echo "  activity_main OK" || echo "  MISSING activity_main!"
test -f app/src/main/res/layout/fragment_home.xml && echo "  fragment_home OK" || echo "  MISSING fragment_home!"
test -f app/src/main/java/com/fountainhome/streaming/ui/MainActivity.java && echo "  MainActivity OK" || echo "  MISSING MainActivity!"
test -f app/src/main/java/com/fountainhome/streaming/FountainApp.java && echo "  FountainApp OK" || echo "  MISSING FountainApp!"
COUNT=$(find app/src/main/java -name "*.java" | wc -l)
echo "  Total Java files: $COUNT"

echo ""
echo "Step 6: Committing and pushing..."
git add .
git commit -m "v1.23 COMPLETE REBUILD — all features, custom icon, clean from scratch"
git push

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║  DONE! Watch GitHub Actions for build.   ║"
echo "╚══════════════════════════════════════════╝"
