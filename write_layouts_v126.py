import os

def w(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w').write(text)

print("Writing v1.26 layouts...")

# ── New drawables ───────────────────────────────────────────────
w("app/src/main/res/drawable/ic_pause.xml", """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M6,19h4L10,5L6,5v14zM14,5v14h4L18,5h-4z"/>
</vector>
""")

w("app/src/main/res/drawable/ic_replay.xml", """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M12,5V1L7,6l5,5V7c3.31,0 6,2.69 6,6s-2.69,6 -6,6 -6,-2.69 -6,-6H4c0,4.42 3.58,8 8,8s8,-3.58 8,-8 -3.58,-8 -8,-8z"/>
</vector>
""")

w("app/src/main/res/drawable/ic_lock.xml", """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M12,17c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM18,8h-1L17,6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2L20,10c0,-1.1 -0.9,-2 -2,-2zM8.9,6c0,-1.71 1.39,-3.1 3.1,-3.1s3.1,1.39 3.1,3.1v2L8.9,8L8.9,6zM18,20L6,20L6,10h12v10z"/>
</vector>
""")

w("app/src/main/res/drawable/ic_lock_open.xml", """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M12,17c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM18,8h-1L17,6c0,-2.76 -2.24,-5 -5,-5 -2.76,0 -5,2.24 -5,5h1.9c0,-1.71 1.39,-3.1 3.1,-3.1s3.1,1.39 3.1,3.1v2L6,8c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2L20,10c0,-1.1 -0.9,-2 -2,-2zM18,20L6,20L6,10h12v10z"/>
</vector>
""")

w("app/src/main/res/drawable/ic_brightness.xml", """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M20,15.31L23.31,12 20,8.69V4h-4.69L12,0.69 8.69,4H4v4.69L0.69,12 4,15.31V20h4.69L12,23.31 15.31,20H20v-4.69zM12,18L12,6c3.31,0 6,2.69 6,6s-2.69,6 -6,6z"/>
</vector>
""")

w("app/src/main/res/drawable/ic_volume.xml", """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M3,9v6h4l5,5L12,4L7,9L3,9zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"/>
</vector>
""")

w("app/src/main/res/drawable/ic_more.xml", """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z"/>
</vector>
""")

w("app/src/main/res/drawable/gradient_top.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:startColor="#99000000" android:endColor="#00000000" android:angle="270"/>
</shape>
""")

w("app/src/main/res/drawable/circle_touch_bg.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#33000000"/>
</shape>
""")

w("app/src/main/res/drawable/circle_touch_bg_solid.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#40FFFFFF"/>
</shape>
""")

w("app/src/main/res/drawable/pill_bg.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#CC000000"/>
    <corners android:radius="20dp"/>
    <stroke android:width="1dp" android:color="#33FFFFFF"/>
</shape>
""")

w("app/src/main/res/drawable/card_bg.xml", """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#DD141414"/>
    <corners android:radius="12dp"/>
    <stroke android:width="1dp" android:color="#22FFFFFF"/>
</shape>
""")

# ── Redesigned activity_player.xml — AniLab-style gesture player ──
w("app/src/main/res/layout/activity_player.xml", """<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent" android:background="#000000">

    <ProgressBar android:id="@+id/loading_bar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent" android:layout_height="4dp"
        android:max="100" android:progressTint="#BB86FC"
        android:layout_gravity="top" android:visibility="gone"/>

    <androidx.media3.ui.PlayerView android:id="@+id/player_view"
        android:layout_width="match_parent" android:layout_height="match_parent" android:visibility="gone"/>

    <WebView android:id="@+id/player_web_view"
        android:layout_width="match_parent" android:layout_height="match_parent" android:visibility="gone"/>

    <FrameLayout android:id="@+id/fullscreen_container"
        android:layout_width="match_parent" android:layout_height="match_parent"
        android:background="#000000" android:visibility="gone"/>

    <TextView android:id="@+id/extracting_text"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center" android:text="Finding stream..."
        android:textColor="#BB86FC" android:textSize="14sp" android:visibility="gone"/>

    <!-- Gesture layer: tap / double-tap seek / brightness+volume drag (native playback only) -->
    <View android:id="@+id/touch_interceptor"
        android:layout_width="match_parent" android:layout_height="match_parent"
        android:clickable="true" android:focusable="true"/>

    <TextView android:id="@+id/flash_left"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center_vertical|start" android:layout_marginStart="48dp"
        android:textColor="#FFFFFF" android:textSize="18sp" android:textStyle="bold"
        android:background="@drawable/pill_bg" android:padding="10dp" android:visibility="gone"/>
    <TextView android:id="@+id/flash_right"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center_vertical|end" android:layout_marginEnd="48dp"
        android:textColor="#FFFFFF" android:textSize="18sp" android:textStyle="bold"
        android:background="@drawable/pill_bg" android:padding="10dp" android:visibility="gone"/>

    <LinearLayout android:id="@+id/bv_indicator"
        android:layout_width="140dp" android:layout_height="wrap_content"
        android:layout_gravity="center" android:orientation="vertical" android:gravity="center"
        android:background="@drawable/card_bg" android:padding="14dp" android:visibility="gone">
        <ImageView android:id="@+id/bv_icon"
            android:layout_width="26dp" android:layout_height="26dp"
            android:src="@drawable/ic_brightness" android:tint="#FFFFFF" android:layout_marginBottom="8dp"/>
        <ProgressBar android:id="@+id/bv_level_bar"
            style="?android:attr/progressBarStyleHorizontal"
            android:layout_width="match_parent" android:layout_height="4dp"
            android:max="100" android:progressTint="#BB86FC"/>
        <TextView android:id="@+id/bv_text"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textColor="#FFFFFF" android:textSize="12sp" android:layout_marginTop="6dp"/>
    </LinearLayout>

    <TextView android:id="@+id/skip_intro_pill"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="top|end" android:layout_marginTop="70dp" android:layout_marginEnd="16dp"
        android:text="Skip Intro  \u203a" android:textColor="#FFFFFF" android:textSize="13sp"
        android:textStyle="bold" android:background="@drawable/pill_bg"
        android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingTop="8dp" android:paddingBottom="8dp"
        android:visibility="gone"/>

    <LinearLayout android:id="@+id/up_next_card"
        android:layout_width="280dp" android:layout_height="wrap_content"
        android:layout_gravity="bottom|end" android:layout_margin="16dp"
        android:orientation="vertical" android:background="@drawable/card_bg" android:padding="14dp"
        android:visibility="gone">
        <TextView android:id="@+id/up_next_title"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Up Next" android:textColor="#FFFFFF" android:textSize="13sp" android:textStyle="bold"/>
        <TextView android:id="@+id/up_next_countdown"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="5s" android:textColor="#BB86FC" android:textSize="12sp" android:layout_marginBottom="10dp"/>
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal">
            <Button android:id="@+id/up_next_cancel"
                android:layout_width="0dp" android:layout_height="36dp" android:layout_weight="1"
                android:text="Cancel" android:textSize="12sp" android:textColor="#CCCCCC"
                android:backgroundTint="#2A2A2A" android:layout_marginEnd="8dp"/>
            <Button android:id="@+id/up_next_play_now"
                android:layout_width="0dp" android:layout_height="36dp" android:layout_weight="1"
                android:text="Play Now" android:textSize="12sp" android:textColor="#000000"
                android:backgroundTint="#BB86FC"/>
        </LinearLayout>
    </LinearLayout>

    <LinearLayout android:id="@+id/center_controls"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center" android:orientation="horizontal" android:gravity="center"
        android:visibility="gone">
        <ImageButton android:id="@+id/skip_back_btn"
            android:layout_width="52dp" android:layout_height="52dp"
            android:src="@drawable/ic_replay" android:tint="#FFFFFF" android:padding="10dp"
            android:background="@drawable/circle_touch_bg" android:layout_marginEnd="28dp"
            android:contentDescription="Back 10s"/>
        <ImageButton android:id="@+id/play_pause_btn"
            android:layout_width="68dp" android:layout_height="68dp"
            android:src="@drawable/ic_pause" android:tint="#FFFFFF" android:padding="16dp"
            android:background="@drawable/circle_touch_bg_solid"
            android:contentDescription="Play or pause"/>
        <ImageButton android:id="@+id/skip_forward_btn"
            android:layout_width="52dp" android:layout_height="52dp"
            android:src="@drawable/ic_replay" android:tint="#FFFFFF" android:padding="10dp"
            android:scaleX="-1"
            android:background="@drawable/circle_touch_bg" android:layout_marginStart="28dp"
            android:contentDescription="Forward 10s"/>
    </LinearLayout>

    <LinearLayout android:id="@+id/top_controls"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:layout_gravity="top"
        android:padding="8dp" android:gravity="center_vertical" android:background="@drawable/gradient_top">
        <ImageButton android:id="@+id/back_btn"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@drawable/ic_close" android:tint="#FFFFFF" android:padding="8dp"
            android:background="?attr/selectableItemBackground" android:contentDescription="Back"/>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:orientation="vertical" android:layout_marginStart="8dp">
            <TextView android:id="@+id/title_text"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="#FFFFFF" android:textSize="14sp" android:textStyle="bold"
                android:maxLines="1" android:ellipsize="end"/>
            <TextView android:id="@+id/episode_label"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="#BB86FC" android:textSize="11sp" android:textStyle="bold"
                android:visibility="gone"/>
        </LinearLayout>
        <ImageButton android:id="@+id/lock_btn"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@drawable/ic_lock_open" android:tint="#FFFFFF" android:padding="7dp"
            android:background="?attr/selectableItemBackground" android:contentDescription="Lock"/>
        <Button android:id="@+id/speed_btn"
            android:layout_width="wrap_content" android:layout_height="36dp" android:minWidth="0dp"
            android:text="1x" android:textSize="12sp" android:textColor="#FFFFFF"
            android:backgroundTint="#33FFFFFF" android:paddingStart="10dp" android:paddingEnd="10dp"
            android:layout_marginStart="4dp"/>
        <ImageButton android:id="@+id/fullscreen_btn"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@drawable/ic_fullscreen" android:tint="#FFFFFF" android:padding="7dp"
            android:background="?attr/selectableItemBackground" android:layout_marginStart="4dp"
            android:contentDescription="Fullscreen"/>
        <ImageButton android:id="@+id/more_btn"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@drawable/ic_more" android:tint="#FFFFFF" android:padding="7dp"
            android:background="?attr/selectableItemBackground" android:layout_marginStart="4dp"
            android:contentDescription="More"/>
    </LinearLayout>

    <LinearLayout android:id="@+id/controls_bar"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:layout_gravity="bottom" android:background="@drawable/gradient_hero"
        android:paddingTop="24dp">
        <LinearLayout android:id="@+id/seek_row"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingStart="12dp" android:paddingEnd="12dp">
            <TextView android:id="@+id/current_time_text"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="0:00" android:textColor="#EEEEEE" android:textSize="11sp"/>
            <SeekBar android:id="@+id/seek_bar"
                android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                android:layout_marginStart="8dp" android:layout_marginEnd="8dp" android:max="1000"/>
            <TextView android:id="@+id/duration_text"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="0:00" android:textColor="#EEEEEE" android:textSize="11sp"/>
        </LinearLayout>
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:paddingStart="8dp" android:paddingEnd="8dp"
            android:paddingTop="6dp" android:paddingBottom="10dp" android:gravity="center_vertical">
            <Spinner android:id="@+id/source_spinner"
                android:layout_width="0dp" android:layout_height="38dp" android:layout_weight="1"
                android:background="@drawable/spinner_bg" android:layout_marginEnd="8dp"/>
            <LinearLayout android:id="@+id/tv_controls"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:orientation="horizontal" android:visibility="gone">
                <Button android:id="@+id/prev_btn"
                    android:layout_width="wrap_content" android:layout_height="38dp"
                    android:text="Prev" android:textSize="11sp" android:layout_marginEnd="6dp" android:backgroundTint="#1F1F1F"/>
                <Button android:id="@+id/next_btn"
                    android:layout_width="wrap_content" android:layout_height="38dp"
                    android:text="Next" android:textSize="11sp" android:backgroundTint="#BB86FC" android:textColor="#000000"/>
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>

    <ImageView android:id="@+id/lock_indicator"
        android:layout_width="44dp" android:layout_height="44dp"
        android:layout_gravity="bottom|start" android:layout_margin="16dp"
        android:src="@drawable/ic_lock" android:tint="#FFFFFF" android:padding="10dp"
        android:background="@drawable/card_bg" android:visibility="gone"
        android:contentDescription="Unlock"/>

</FrameLayout>
""")

# ── fragment_settings.xml — add PLAYER GESTURES section ──
w("app/src/main/res/layout/fragment_settings.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414"
        android:paddingStart="4dp" android:paddingEnd="8dp">
        <ImageButton android:id="@+id/back_btn" android:layout_width="44dp" android:layout_height="44dp"
            android:src="@drawable/ic_close" android:tint="#FFFFFF"
            android:background="?attr/selectableItemBackground" android:contentDescription="Back"/>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Settings" android:textSize="18sp" android:textStyle="bold"
            android:textColor="#FFFFFF" android:layout_marginStart="8dp"/>
    </LinearLayout>
    <ScrollView android:layout_width="match_parent" android:layout_height="match_parent" android:overScrollMode="never">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" android:paddingBottom="32dp">

            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="ACCENT COLOR" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="72dp"
                android:orientation="horizontal" android:background="#141414"
                android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                <View android:id="@+id/color_purple" android:layout_width="40dp" android:layout_height="40dp" android:layout_marginEnd="8dp"/>
                <View android:id="@+id/color_blue"   android:layout_width="40dp" android:layout_height="40dp" android:layout_marginEnd="8dp"/>
                <View android:id="@+id/color_red"    android:layout_width="40dp" android:layout_height="40dp" android:layout_marginEnd="8dp"/>
                <View android:id="@+id/color_green"  android:layout_width="40dp" android:layout_height="40dp" android:layout_marginEnd="8dp"/>
                <View android:id="@+id/color_orange" android:layout_width="40dp" android:layout_height="40dp" android:layout_marginEnd="8dp"/>
                <View android:id="@+id/color_pink"   android:layout_width="40dp" android:layout_height="40dp" android:layout_marginEnd="8dp"/>
                <View android:id="@+id/color_teal"   android:layout_width="40dp" android:layout_height="40dp"/>
            </LinearLayout>

            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="PLAYER" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:background="#141414">
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Default Source" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/source_spinner" android:layout_width="150dp" android:layout_height="40dp"
                        android:background="@drawable/spinner_bg"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Playback Speed" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/speed_spinner" android:layout_width="120dp" android:layout_height="40dp"
                        android:background="@drawable/spinner_bg"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Autoplay Next Episode" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Auto-advance when episode ends" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/autoplay_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Picture in Picture" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Float video when leaving app" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/pip_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Hardware Acceleration" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Smoother video playback" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/hw_accel_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Keep Screen On" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Prevent screen timeout while watching" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/keep_screen_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
            </LinearLayout>

            <!-- NEW: Player Gestures section -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="PLAYER GESTURES" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:background="#141414">
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Swipe Brightness / Volume" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Drag left/right half of screen up-down" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/gesture_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Double-Tap Seek Amount" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/dt_seek_spinner" android:layout_width="100dp" android:layout_height="40dp"
                        android:background="@drawable/spinner_bg"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Show Skip Intro Button" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/show_skip_intro_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Auto-Skip Intro" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Skip first 90s automatically, no button" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/auto_skip_intro_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Background Playback" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Keep audio playing when app is backgrounded" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/bg_playback_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
            </LinearLayout>

            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="SUBTITLES" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:background="#141414">
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Enable Subtitles" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/sub_enabled_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Subtitle Language" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/sub_lang_spinner" android:layout_width="160dp" android:layout_height="40dp"
                        android:background="@drawable/spinner_bg"/>
                </LinearLayout>
            </LinearLayout>

            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="ANIME" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                android:gravity="center_vertical" android:background="#141414"
                android:paddingStart="16dp" android:paddingEnd="16dp">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="Default Sub / Dub" android:textColor="#FFFFFF" android:textSize="15sp"/>
                <Spinner android:id="@+id/dub_sub_spinner" android:layout_width="150dp" android:layout_height="40dp"
                    android:background="@drawable/spinner_bg"/>
            </LinearLayout>

            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="DOWNLOADS" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:background="#141414">
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="WiFi Only" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Only download on WiFi" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/wifi_only_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Download Subtitles" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Save subtitle file with download" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/dl_sub_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Download Quality" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/quality_spinner" android:layout_width="120dp" android:layout_height="40dp"
                        android:background="@drawable/spinner_bg"/>
                </LinearLayout>
            </LinearLayout>

            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="DISPLAY" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:background="#141414">
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Show Ratings" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Show star ratings on cards" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/show_rating_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Continue Watching Row" android:textColor="#FFFFFF" android:textSize="15sp"/>
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:text="Show on home screen" android:textColor="#888888" android:textSize="11sp"/>
                    </LinearLayout>
                    <androidx.appcompat.widget.SwitchCompat android:id="@+id/show_continue_switch"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"/>
                </LinearLayout>
            </LinearLayout>

            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="STORAGE" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:background="#141414">
                <TextView android:id="@+id/storage_text"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:text="Cache: calculating..." android:textColor="#888888" android:textSize="12sp"
                    android:paddingStart="16dp" android:paddingTop="12dp" android:paddingBottom="4dp"/>
                <Button android:id="@+id/clear_cache_btn"
                    android:layout_width="match_parent" android:layout_height="48dp"
                    android:text="Clear Cache" android:textColor="#FFFFFF" android:backgroundTint="#2A2A2A"
                    android:layout_marginStart="16dp" android:layout_marginEnd="16dp" android:layout_marginTop="8dp"/>
                <Button android:id="@+id/clear_history_btn"
                    android:layout_width="match_parent" android:layout_height="48dp"
                    android:text="Clear Watch History" android:textColor="#FFFFFF" android:backgroundTint="#2A2A2A"
                    android:layout_marginStart="16dp" android:layout_marginEnd="16dp" android:layout_marginTop="6dp"/>
                <Button android:id="@+id/clear_all_btn"
                    android:layout_width="match_parent" android:layout_height="48dp"
                    android:text="Clear All Data" android:textColor="#CF6679" android:backgroundTint="#2A2A2A"
                    android:layout_marginStart="16dp" android:layout_marginEnd="16dp"
                    android:layout_marginTop="6dp" android:layout_marginBottom="16dp"/>
            </LinearLayout>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
""")

print("v1.26 layouts done!")
