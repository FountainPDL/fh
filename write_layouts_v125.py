import os

def w(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w').write(text)

print("Writing v1.25 layouts...")

# Anime detail - full TV-series quality layout
w("app/src/main/res/layout/activity_anime_detail.xml", """<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent" android:background="#0A0A0A">
    <androidx.core.widget.NestedScrollView android:layout_width="match_parent"
        android:layout_height="match_parent" android:overScrollMode="never">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical">
            <FrameLayout android:layout_width="match_parent" android:layout_height="220dp">
                <ImageView android:id="@+id/backdrop_image"
                    android:layout_width="match_parent" android:layout_height="match_parent"
                    android:scaleType="centerCrop" android:background="#111111"/>
                <View android:layout_width="match_parent" android:layout_height="match_parent"
                    android:background="@drawable/gradient_hero"/>
                <ImageButton android:id="@+id/back_btn"
                    android:layout_width="40dp" android:layout_height="40dp" android:layout_margin="10dp"
                    android:src="@drawable/ic_close" android:background="#88000000"
                    android:tint="#FFFFFF" android:contentDescription="Back"/>
            </FrameLayout>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:padding="12dp">
                <ImageView android:id="@+id/poster_image"
                    android:layout_width="100dp" android:layout_height="150dp"
                    android:scaleType="centerCrop" android:background="#1E1E1E" android:layout_marginEnd="12dp"/>
                <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
                    android:layout_weight="1" android:orientation="vertical">
                    <TextView android:id="@+id/title_text"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:textColor="#FFFFFF" android:textSize="18sp" android:textStyle="bold"/>
                    <TextView android:id="@+id/rating_text"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:textColor="#999999" android:textSize="12sp" android:layout_marginTop="4dp"/>
                    <Button android:id="@+id/status_btn"
                        android:layout_width="wrap_content" android:layout_height="32dp"
                        android:layout_marginTop="8dp" android:text="Set Status"
                        android:textSize="11sp" android:textColor="#888888" android:backgroundTint="#1E1E1E"
                        android:paddingStart="12dp" android:paddingEnd="12dp"/>
                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="horizontal" android:layout_marginTop="10dp">
                        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
                            android:layout_weight="1" android:orientation="vertical" android:gravity="center">
                            <ImageButton android:id="@+id/favorite_btn" android:layout_width="36dp" android:layout_height="36dp"
                                android:src="@drawable/ic_favorite" android:tint="#BB86FC"
                                android:background="?attr/selectableItemBackground" android:alpha="0.4"/>
                            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                                android:text="Favorite" android:textColor="#888888" android:textSize="10sp"/>
                        </LinearLayout>
                        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
                            android:layout_weight="1" android:orientation="vertical" android:gravity="center">
                            <ImageButton android:id="@+id/watchlist_btn" android:layout_width="36dp" android:layout_height="36dp"
                                android:src="@drawable/ic_watch_later" android:tint="#FFFFFF"
                                android:background="?attr/selectableItemBackground" android:alpha="0.4"/>
                            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                                android:text="Watchlist" android:textColor="#888888" android:textSize="10sp"/>
                        </LinearLayout>
                        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
                            android:layout_weight="1" android:orientation="vertical" android:gravity="center">
                            <ImageButton android:id="@+id/share_btn" android:layout_width="36dp" android:layout_height="36dp"
                                android:src="@drawable/ic_share" android:tint="#FFFFFF"
                                android:background="?attr/selectableItemBackground"/>
                            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                                android:text="Share" android:textColor="#888888" android:textSize="10sp"/>
                        </LinearLayout>
                    </LinearLayout>
                </LinearLayout>
            </LinearLayout>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:paddingStart="12dp" android:paddingEnd="12dp" android:paddingBottom="8dp">
                <Button android:id="@+id/watch_btn"
                    android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="2"
                    android:text="PLAY" android:textSize="14sp" android:textStyle="bold"
                    android:textColor="#000000" android:backgroundTint="#BB86FC" android:layout_marginEnd="6dp"/>
                <Button android:id="@+id/sub_btn"
                    android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="1"
                    android:text="SUB" android:textSize="12sp" android:textColor="#BB86FC"
                    android:backgroundTint="#22BB86FC" android:layout_marginEnd="4dp"/>
                <Button android:id="@+id/dub_btn"
                    android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="1"
                    android:text="DUB" android:textSize="12sp" android:textColor="#FFFFFF"
                    android:backgroundTint="#1E1E1E"/>
            </LinearLayout>
            <Spinner android:id="@+id/source_spinner"
                android:layout_width="match_parent" android:layout_height="44dp"
                android:background="@drawable/spinner_bg"
                android:layout_marginStart="12dp" android:layout_marginEnd="12dp" android:layout_marginBottom="8dp"/>
            <TextView android:id="@+id/desc_text"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="#BBBBBB" android:textSize="13sp" android:lineSpacingExtra="3dp"
                android:paddingStart="14dp" android:paddingEnd="14dp" android:paddingBottom="12dp" android:visibility="gone"/>
            <LinearLayout android:id="@+id/episode_section"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:visibility="gone">
                <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:text="Episodes" android:textColor="#FFFFFF"
                    android:textSize="15sp" android:textStyle="bold" android:padding="14dp"/>
                <androidx.recyclerview.widget.RecyclerView android:id="@+id/episode_rv"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:nestedScrollingEnabled="false"/>
            </LinearLayout>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</FrameLayout>
""")

# Expanded settings layout
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

            <!-- ACCENT -->
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

            <!-- PLAYER -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="PLAYER" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:background="#141414">
                <!-- Source -->
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Default Source" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/source_spinner" android:layout_width="150dp" android:layout_height="40dp"
                        android:background="@drawable/spinner_bg"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <!-- Speed -->
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Playback Speed" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/speed_spinner" android:layout_width="120dp" android:layout_height="40dp"
                        android:background="@drawable/spinner_bg"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp" android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <!-- Autoplay -->
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
                <!-- PiP -->
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
                <!-- HW Accel -->
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
                <!-- Keep screen on -->
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

            <!-- SUBTITLES -->
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

            <!-- ANIME -->
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

            <!-- DOWNLOADS -->
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

            <!-- UI -->
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

            <!-- STORAGE -->
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

# About page
w("app/src/main/res/layout/fragment_about.xml", """<?xml version="1.0" encoding="utf-8"?>
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
            android:text="About" android:textSize="18sp" android:textStyle="bold"
            android:textColor="#FFFFFF" android:layout_marginStart="8dp"/>
    </LinearLayout>
    <ScrollView android:layout_width="match_parent" android:layout_height="match_parent" android:overScrollMode="never">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" android:padding="20dp">
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="FOUNTAIN HOME" android:textColor="#BB86FC"
                android:textSize="28sp" android:textStyle="bold" android:gravity="center" android:layout_marginBottom="4dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Version 1.25  \u2014  by FountainPDL"
                android:textColor="#888888" android:textSize="13sp" android:gravity="center" android:layout_marginBottom="24dp"/>
            <View android:layout_width="match_parent" android:layout_height="1dp"
                android:background="#222222" android:layout_marginBottom="20dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="What is Fountain Home?"
                android:textColor="#BB86FC" android:textSize="15sp" android:textStyle="bold" android:layout_marginBottom="8dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Fountain Home is a free, all-in-one streaming app for Movies, TV Series, and Anime. Built natively for Android by FountainPDL, it aggregates content from multiple embed sources and provides a clean, dark, ad-free experience with no account required."
                android:textColor="#CCCCCC" android:textSize="13sp" android:lineSpacingExtra="4dp" android:layout_marginBottom="20dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="How It Works"
                android:textColor="#BB86FC" android:textSize="15sp" android:textStyle="bold" android:layout_marginBottom="8dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="1. Metadata is fetched from TMDB API and AniList GraphQL.\n\n2. Pressing Play loads an embed URL from the selected source.\n\n3. StreamExtractor attempts to intercept the direct .m3u8 or .mp4 stream for native ExoPlayer. If extraction fails, the embed loads in WebView.\n\n4. Sources auto-advance on failure. You can also switch manually from the spinner."
                android:textColor="#CCCCCC" android:textSize="13sp" android:lineSpacingExtra="4dp" android:layout_marginBottom="20dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="How To Use"
                android:textColor="#BB86FC" android:textSize="15sp" android:textStyle="bold" android:layout_marginBottom="8dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Movies / TV \u2014 Browse or search, tap a title, then tap PLAY. Change source from the dropdown if one fails.\n\nAnime \u2014 Go to the Anime tab. Tap a title, choose SUB or DUB, select your episode and play.\n\nPlayer \u2014 Tap screen to toggle controls. Tap fullscreen for landscape mode. Use X or back to exit.\n\nDownloads \u2014 Tap Save on any detail page. View under More \u2192 Downloads.\n\nWatchlist / Status \u2014 Long-press any card for quick options. Or use buttons on the detail page to set Planning / Watching / Watched / Dropped."
                android:textColor="#CCCCCC" android:textSize="13sp" android:lineSpacingExtra="4dp" android:layout_marginBottom="20dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Content Sources"
                android:textColor="#BB86FC" android:textSize="15sp" android:textStyle="bold" android:layout_marginBottom="8dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Movies / TV:\nVidSrc  \u00b7  2Embed  \u00b7  AutoEmbed  \u00b7  SuperEmbed\n\nAnime (separate sources):\nAutoEmbed Anime  \u00b7  2Anime  \u00b7  Yugen\n\nMetadata:\nTMDB API  \u00b7  AniList GraphQL"
                android:textColor="#CCCCCC" android:textSize="13sp" android:lineSpacingExtra="4dp" android:layout_marginBottom="20dp"/>
            <View android:layout_width="match_parent" android:layout_height="1dp"
                android:background="#222222" android:layout_marginBottom="20dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Made with passion by FountainPDL\nFountain Home \u2014 Ministry Brand\nAll content belongs to respective owners."
                android:textColor="#555555" android:textSize="12sp" android:gravity="center" android:lineSpacingExtra="4dp"/>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
""")

print("v1.25 layouts written!")
