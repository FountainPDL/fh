import os

print("=== v1.31c: resolver container, grid columns row, Fountain Home shimmer ===")

# ── activity_player.xml — add the invisible resolver container ──
path = "app/src/main/res/layout/activity_player.xml"
if os.path.exists(path):
    content = open(path, encoding='utf-8').read()
    anchor = '<View android:id="@+id/touch_interceptor"'
    addition = (
        '<FrameLayout android:id="@+id/resolver_container"\n'
        '        android:layout_width="320dp" android:layout_height="180dp"\n'
        '        android:layout_gravity="top|start" android:visibility="invisible"/>\n\n'
        '    '
    )
    if anchor in content and 'resolver_container' not in content:
        content = content.replace(anchor, addition + anchor, 1)
        open(path, 'w', encoding='utf-8').write(content)
        print("[OK] activity_player.xml — resolver_container added")
    elif 'resolver_container' in content:
        print("[SKIP] activity_player.xml already patched")
    else:
        print("[WARN] Could not find touch_interceptor in activity_player.xml.")
else:
    print("[WARN] activity_player.xml not found — skipping")

# ── fragment_settings.xml — Grid Columns row, right before Show Ratings ──
path = "app/src/main/res/layout/fragment_settings.xml"
if os.path.exists(path):
    content = open(path, encoding='utf-8').read()
    anchor = '''<LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Show Ratings" android:textColor="#FFFFFF" android:textSize="15sp"/>'''
    addition = '''<LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Grid Columns" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/grid_cols_spinner" android:layout_width="90dp" android:layout_height="40dp"
                        android:background="@drawable/spinner_bg"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                '''
    if anchor in content and 'grid_cols_spinner' not in content:
        content = content.replace(anchor, addition + anchor, 1)
        open(path, 'w', encoding='utf-8').write(content)
        print("[OK] fragment_settings.xml — Grid Columns row added")
    elif 'grid_cols_spinner' in content:
        print("[SKIP] fragment_settings.xml already patched")
    else:
        print("[WARN] Could not find the Show Ratings row in fragment_settings.xml.")
else:
    print("[WARN] fragment_settings.xml not found — skipping")

# ── fragment_home.xml — wrap app_title so a shimmer streak can overlay it ──
path = "app/src/main/res/layout/fragment_home.xml"
if os.path.exists(path):
    content = open(path, encoding='utf-8').read()
    old = '''<TextView android:id="@+id/app_title"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:text="FOUNTAIN HOME" android:textColor="#BB86FC"
            android:textSize="20sp" android:textStyle="bold" android:letterSpacing="0.1"/>'''
    new = '''<FrameLayout android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1"
            android:clipChildren="true">
            <TextView android:id="@+id/app_title"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_gravity="center_vertical"
                android:text="FOUNTAIN HOME" android:textColor="#BB86FC"
                android:textSize="20sp" android:textStyle="bold" android:letterSpacing="0.1"/>
            <View android:id="@+id/title_shimmer"
                android:layout_width="36dp" android:layout_height="match_parent"
                android:background="@drawable/shimmer_gradient"/>
        </FrameLayout>'''
    if old in content and 'title_shimmer' not in content:
        content = content.replace(old, new, 1)
        open(path, 'w', encoding='utf-8').write(content)
        print("[OK] fragment_home.xml — shimmer overlay added over the title")
    elif 'title_shimmer' in content:
        print("[SKIP] fragment_home.xml already patched")
    else:
        print("[WARN] Could not find the app_title TextView block in fragment_home.xml.")
        print("       (If v1.24-era whitespace has drifted, wrap app_title in a FrameLayout manually")
        print("       and add a title_shimmer View using @drawable/shimmer_gradient beside it.)")
else:
    print("[WARN] fragment_home.xml not found — skipping")

# ── New drawable: the shimmer streak itself ──
os.makedirs("app/src/main/res/drawable", exist_ok=True)
open("app/src/main/res/drawable/shimmer_gradient.xml", 'w').write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:startColor="#00FFFFFF" android:centerColor="#77FFFFFF" android:endColor="#00FFFFFF" android:angle="45"/>
</shape>
''')
print("[OK] shimmer_gradient.xml drawable created")

# ── HomeFragment.java — swap the old alpha-pulse for a real shimmer sweep ──
path = "app/src/main/java/com/fountainhome/streaming/ui/fragment/HomeFragment.java"
if os.path.exists(path):
    content = open(path, encoding='utf-8').read()
    old = "b.appTitle.startAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.title_pulse));"
    new = "startTitleShimmer();"
    if old in content and "startTitleShimmer" not in content:
        content = content.replace(old, new, 1)
        if "import android.animation.ObjectAnimator;" not in content:
            content = content.replace("package com.fountainhome.streaming.ui.fragment;",
                                       "package com.fountainhome.streaming.ui.fragment;\nimport android.animation.ObjectAnimator;", 1)
        # Append the new method just before the final closing brace of the class
        method = (
            "\n    private void startTitleShimmer() {\n"
            "        if (b == null) return;\n"
            "        b.titleShimmer.post(() -> {\n"
            "            if (b == null || b.appTitle.getWidth() == 0) return;\n"
            "            float startX = -b.appTitle.getWidth() * 0.6f;\n"
            "            float endX = b.appTitle.getWidth() * 1.2f;\n"
            "            b.titleShimmer.setTranslationX(startX);\n"
            "            ObjectAnimator anim = ObjectAnimator.ofFloat(b.titleShimmer, \"translationX\", startX, endX);\n"
            "            anim.setDuration(1400);\n"
            "            anim.setStartDelay(500);\n"
            "            anim.setRepeatCount(ObjectAnimator.INFINITE);\n"
            "            anim.setRepeatDelay(1800);\n"
            "            anim.start();\n"
            "        });\n"
            "    }\n"
        )
        idx = content.rstrip().rfind('}')
        content = content[:idx] + method + content[idx:]
        open(path, 'w', encoding='utf-8').write(content)
        print("[OK] HomeFragment.java — shimmer sweep replaces the old alpha pulse")
    elif "startTitleShimmer" in content:
        print("[SKIP] HomeFragment.java already patched")
    else:
        print("[WARN] Could not find the title_pulse animation call in HomeFragment.java.")
else:
    print("[WARN] HomeFragment.java not found — skipping")

print()
print("=== v1.31c complete ===")
