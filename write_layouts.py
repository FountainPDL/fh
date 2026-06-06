import os

def w(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w').write(text)

print("Writing layout files...")

w("app/src/main/res/layout/activity_main.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <FrameLayout android:id="@+id/fragment_container"
        android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1"/>
    <LinearLayout android:id="@+id/bottom_nav_bar"
        android:layout_width="match_parent" android:layout_height="64dp"
        android:orientation="horizontal" android:background="#141414">
        <LinearLayout android:id="@+id/nav_home"
            android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1"
            android:orientation="vertical" android:gravity="center"
            android:background="?attr/selectableItemBackground">
            <ImageView android:id="@+id/icon_home" android:layout_width="24dp" android:layout_height="24dp"
                android:src="@drawable/ic_nav_home" android:layout_marginBottom="2dp"/>
            <TextView android:id="@+id/label_home" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="Home" android:textSize="10sp" android:textColor="#888888"/>
        </LinearLayout>
        <LinearLayout android:id="@+id/nav_movies"
            android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1"
            android:orientation="vertical" android:gravity="center"
            android:background="?attr/selectableItemBackground">
            <ImageView android:id="@+id/icon_movies" android:layout_width="24dp" android:layout_height="24dp"
                android:src="@drawable/ic_nav_movies" android:layout_marginBottom="2dp"/>
            <TextView android:id="@+id/label_movies" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="Movies" android:textSize="10sp" android:textColor="#888888"/>
        </LinearLayout>
        <LinearLayout android:id="@+id/nav_tv"
            android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1"
            android:orientation="vertical" android:gravity="center"
            android:background="?attr/selectableItemBackground">
            <ImageView android:id="@+id/icon_tv" android:layout_width="24dp" android:layout_height="24dp"
                android:src="@drawable/ic_nav_tv" android:layout_marginBottom="2dp"/>
            <TextView android:id="@+id/label_tv" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="TV" android:textSize="10sp" android:textColor="#888888"/>
        </LinearLayout>
        <LinearLayout android:id="@+id/nav_anime"
            android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1"
            android:orientation="vertical" android:gravity="center"
            android:background="?attr/selectableItemBackground">
            <ImageView android:id="@+id/icon_anime" android:layout_width="24dp" android:layout_height="24dp"
                android:src="@drawable/ic_anime" android:layout_marginBottom="2dp"/>
            <TextView android:id="@+id/label_anime" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="Anime" android:textSize="10sp" android:textColor="#888888"/>
        </LinearLayout>
        <LinearLayout android:id="@+id/nav_more"
            android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1"
            android:orientation="vertical" android:gravity="center"
            android:background="?attr/selectableItemBackground">
            <ImageView android:id="@+id/icon_more" android:layout_width="24dp" android:layout_height="24dp"
                android:src="@drawable/ic_nav_more" android:layout_marginBottom="2dp"/>
            <TextView android:id="@+id/label_more" android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="More" android:textSize="10sp" android:textColor="#888888"/>
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
""")

w("app/src/main/res/layout/fragment_home.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="60dp"
        android:gravity="center_vertical" android:background="#141414"
        android:paddingStart="16dp" android:paddingEnd="8dp">
        <TextView android:id="@+id/app_title"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:text="FOUNTAIN HOME" android:textColor="#BB86FC"
            android:textSize="20sp" android:textStyle="bold" android:letterSpacing="0.1"/>
        <ImageButton android:id="@+id/search_btn"
            android:layout_width="44dp" android:layout_height="44dp"
            android:src="@drawable/ic_search" android:tint="#FFFFFF"
            android:background="?attr/selectableItemBackground" android:contentDescription="Search"/>
    </LinearLayout>
    <SearchView android:id="@+id/search_bar"
        android:layout_width="match_parent" android:layout_height="52dp"
        android:queryHint="Search movies, TV, anime..."
        android:iconifiedByDefault="false" android:background="#1A1A1A" android:visibility="gone"/>
    <LinearLayout android:layout_width="match_parent" android:layout_height="44dp"
        android:orientation="horizontal" android:paddingStart="16dp" android:paddingEnd="16dp"
        android:gravity="center_vertical">
        <TextView android:id="@+id/tab_trending"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Trending" android:textSize="14sp" android:textStyle="bold"
            android:textColor="#BB86FC" android:paddingEnd="20dp"/>
        <TextView android:id="@+id/tab_popular"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Popular" android:textSize="14sp" android:textColor="#888888"/>
    </LinearLayout>
    <ScrollView android:layout_width="match_parent" android:layout_height="match_parent"
        android:overScrollMode="never" android:scrollbars="none">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical">
            <FrameLayout android:layout_width="match_parent" android:layout_height="360dp">
                <ImageView android:id="@+id/featured_banner"
                    android:layout_width="match_parent" android:layout_height="match_parent"
                    android:scaleType="centerCrop" android:background="#111111"/>
                <View android:layout_width="match_parent" android:layout_height="match_parent"
                    android:background="@drawable/gradient_hero"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:layout_gravity="bottom" android:orientation="vertical"
                    android:paddingLeft="20dp" android:paddingRight="20dp" android:paddingBottom="20dp">
                    <TextView android:id="@+id/featured_title"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:textColor="#FFFFFF" android:textSize="24sp" android:textStyle="bold"
                        android:gravity="center" android:shadowColor="#000" android:shadowRadius="10"/>
                    <TextView android:id="@+id/featured_genre"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:textColor="#CCCCCC" android:textSize="12sp"
                        android:gravity="center" android:layout_marginBottom="14dp"/>
                    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="horizontal" android:gravity="center">
                        <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:orientation="vertical" android:gravity="center" android:layout_marginEnd="24dp">
                            <ImageButton android:id="@+id/detail_btn"
                                android:layout_width="44dp" android:layout_height="44dp"
                                android:src="@drawable/ic_info" android:tint="#FFFFFF"
                                android:background="?attr/selectableItemBackground" android:contentDescription="Info"/>
                            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                                android:text="Info" android:textColor="#CCCCCC" android:textSize="11sp"/>
                        </LinearLayout>
                        <Button android:id="@+id/watch_now_btn"
                            android:layout_width="160dp" android:layout_height="50dp"
                            android:text="PLAY NOW" android:textSize="14sp" android:textStyle="bold"
                            android:textColor="#000000" android:backgroundTint="#BB86FC"/>
                        <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:orientation="vertical" android:gravity="center" android:layout_marginStart="24dp">
                            <TextView android:id="@+id/featured_add"
                                android:layout_width="44dp" android:layout_height="44dp"
                                android:gravity="center" android:text="+"
                                android:textColor="#FFFFFF" android:textSize="26sp"
                                android:background="?attr/selectableItemBackground"/>
                            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                                android:text="Add" android:textColor="#CCCCCC" android:textSize="11sp"/>
                        </LinearLayout>
                    </LinearLayout>
                </LinearLayout>
            </FrameLayout>
            <LinearLayout android:id="@+id/continue_section"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:visibility="gone">
                <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:text="Continue watching" android:textColor="#FFFFFF"
                    android:textSize="15sp" android:textStyle="bold" android:padding="14dp"/>
                <androidx.recyclerview.widget.RecyclerView android:id="@+id/continue_rv"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:paddingLeft="8dp" android:paddingRight="8dp" android:nestedScrollingEnabled="false"/>
            </LinearLayout>
            <androidx.recyclerview.widget.RecyclerView android:id="@+id/trending_rv"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:paddingLeft="8dp" android:paddingRight="8dp" android:paddingTop="8dp"
                android:nestedScrollingEnabled="false"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:paddingLeft="14dp" android:paddingRight="14dp"
                android:paddingTop="20dp" android:paddingBottom="8dp" android:gravity="center_vertical">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="Latest Movies" android:textColor="#FFFFFF" android:textSize="15sp" android:textStyle="bold"/>
                <TextView android:id="@+id/view_all_movies"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="View all" android:textColor="#BB86FC" android:textSize="12sp"/>
            </LinearLayout>
            <androidx.recyclerview.widget.RecyclerView android:id="@+id/latest_movies_rv"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:paddingLeft="8dp" android:paddingRight="8dp" android:nestedScrollingEnabled="false"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:paddingLeft="14dp" android:paddingRight="14dp"
                android:paddingTop="20dp" android:paddingBottom="8dp" android:gravity="center_vertical">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="Latest TV Series" android:textColor="#FFFFFF" android:textSize="15sp" android:textStyle="bold"/>
                <TextView android:id="@+id/view_all_tv"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="View all" android:textColor="#BB86FC" android:textSize="12sp"/>
            </LinearLayout>
            <androidx.recyclerview.widget.RecyclerView android:id="@+id/latest_tv_rv"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:paddingLeft="8dp" android:paddingRight="8dp" android:paddingBottom="24dp"
                android:nestedScrollingEnabled="false"/>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
""")

w("app/src/main/res/layout/fragment_browse.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414"
        android:paddingStart="4dp" android:paddingEnd="4dp">
        <ImageButton android:id="@+id/back_btn"
            android:layout_width="44dp" android:layout_height="44dp"
            android:src="@drawable/ic_close" android:tint="#FFFFFF"
            android:background="?attr/selectableItemBackground"
            android:contentDescription="Back" android:visibility="gone"/>
        <TextView android:id="@+id/page_title"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:textColor="#FFFFFF" android:textSize="18sp" android:textStyle="bold"
            android:layout_marginStart="8dp"/>
        <ImageButton android:id="@+id/search_btn"
            android:layout_width="44dp" android:layout_height="44dp"
            android:src="@drawable/ic_search" android:tint="#FFFFFF"
            android:background="?attr/selectableItemBackground" android:contentDescription="Search"/>
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="44dp"
        android:orientation="horizontal" android:paddingStart="14dp" android:paddingEnd="14dp"
        android:gravity="center_vertical">
        <TextView android:id="@+id/tab_latest"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Latest" android:textSize="13sp" android:textColor="#BB86FC"
            android:textStyle="bold" android:paddingEnd="16dp"/>
        <TextView android:id="@+id/tab_trending"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Trending" android:textSize="13sp" android:textColor="#888888" android:paddingEnd="16dp"/>
        <TextView android:id="@+id/tab_popular"
            android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:text="Popular" android:textSize="13sp" android:textColor="#888888"/>
        <ImageButton android:id="@+id/filter_btn"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@drawable/ic_filter" android:tint="#888888"
            android:background="?attr/selectableItemBackground" android:contentDescription="Filter"/>
    </LinearLayout>
    <ProgressBar android:id="@+id/progress_bar"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center" android:visibility="gone"/>
    <androidx.recyclerview.widget.RecyclerView android:id="@+id/content_rv"
        android:layout_width="match_parent" android:layout_height="match_parent" android:padding="4dp"/>
</LinearLayout>
""")

w("app/src/main/res/layout/fragment_watchlist.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414" android:paddingStart="16dp">
        <TextView android:id="@+id/page_title"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Watch List" android:textColor="#FFFFFF" android:textSize="20sp" android:textStyle="bold"/>
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="44dp"
        android:orientation="horizontal" android:paddingStart="16dp" android:paddingEnd="16dp"
        android:gravity="center_vertical">
        <TextView android:id="@+id/tab_watchlist"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Watch List" android:textSize="14sp" android:textColor="#BB86FC" android:paddingEnd="20dp"/>
        <TextView android:id="@+id/tab_favorites"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Favorites" android:textSize="14sp" android:textColor="#888888" android:paddingEnd="20dp"/>
        <TextView android:id="@+id/tab_continue"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Continue" android:textSize="14sp" android:textColor="#888888"/>
    </LinearLayout>
    <View android:layout_width="match_parent" android:layout_height="2dp" android:background="#BB86FC"/>
    <TextView android:id="@+id/empty_text"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:gravity="center" android:textColor="#555555" android:textSize="14sp"
        android:padding="48dp" android:visibility="gone"/>
    <androidx.recyclerview.widget.RecyclerView android:id="@+id/content_rv"
        android:layout_width="match_parent" android:layout_height="match_parent" android:padding="4dp"/>
</LinearLayout>
""")

w("app/src/main/res/layout/fragment_anime.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414"
        android:paddingStart="16dp" android:paddingEnd="8dp">
        <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:text="ANIME" android:textColor="#BB86FC" android:textSize="20sp" android:textStyle="bold"/>
        <ImageButton android:id="@+id/search_btn"
            android:layout_width="44dp" android:layout_height="44dp"
            android:src="@drawable/ic_search" android:tint="#FFFFFF"
            android:background="?attr/selectableItemBackground" android:contentDescription="Search"/>
    </LinearLayout>
    <SearchView android:id="@+id/search_bar"
        android:layout_width="match_parent" android:layout_height="52dp"
        android:queryHint="Search anime..." android:iconifiedByDefault="false"
        android:background="#1A1A1A" android:visibility="gone"/>
    <LinearLayout android:layout_width="match_parent" android:layout_height="44dp"
        android:orientation="horizontal" android:paddingStart="14dp" android:paddingEnd="14dp"
        android:gravity="center_vertical">
        <TextView android:id="@+id/tab_trending"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Trending" android:textSize="14sp" android:textStyle="bold"
            android:textColor="#BB86FC" android:paddingEnd="20dp"/>
        <TextView android:id="@+id/tab_popular"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Popular" android:textSize="14sp" android:textColor="#888888" android:paddingEnd="20dp"/>
        <TextView android:id="@+id/tab_season"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="This Season" android:textSize="14sp" android:textColor="#888888"/>
    </LinearLayout>
    <ProgressBar android:id="@+id/progress_bar"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center" android:visibility="gone"/>
    <ScrollView android:layout_width="match_parent" android:layout_height="match_parent"
        android:overScrollMode="never" android:scrollbars="none">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical">
            <FrameLayout android:id="@+id/anime_hero_section"
                android:layout_width="match_parent" android:layout_height="260dp" android:visibility="gone">
                <ImageView android:id="@+id/anime_hero_banner"
                    android:layout_width="match_parent" android:layout_height="match_parent"
                    android:scaleType="centerCrop" android:background="#111111"/>
                <View android:layout_width="match_parent" android:layout_height="match_parent"
                    android:background="@drawable/gradient_hero"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:layout_gravity="bottom" android:orientation="vertical"
                    android:paddingStart="20dp" android:paddingEnd="20dp" android:paddingBottom="16dp">
                    <TextView android:id="@+id/anime_hero_title"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:textColor="#FFFFFF" android:textSize="22sp" android:textStyle="bold"/>
                    <TextView android:id="@+id/anime_hero_meta"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:textColor="#CCCCCC" android:textSize="12sp" android:layout_marginBottom="12dp"/>
                    <Button android:id="@+id/anime_hero_play"
                        android:layout_width="140dp" android:layout_height="44dp"
                        android:text="WATCH" android:textSize="13sp" android:textStyle="bold"
                        android:textColor="#000000" android:backgroundTint="#BB86FC"/>
                </LinearLayout>
            </FrameLayout>
            <TextView android:id="@+id/trending_title"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Trending" android:textColor="#FFFFFF"
                android:textSize="15sp" android:textStyle="bold" android:padding="14dp"/>
            <androidx.recyclerview.widget.RecyclerView android:id="@+id/trending_rv"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:paddingStart="8dp" android:paddingEnd="8dp" android:nestedScrollingEnabled="false"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Most Popular" android:textColor="#FFFFFF"
                android:textSize="15sp" android:textStyle="bold" android:padding="14dp"/>
            <androidx.recyclerview.widget.RecyclerView android:id="@+id/popular_rv"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:paddingStart="8dp" android:paddingEnd="8dp" android:nestedScrollingEnabled="false"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="This Season" android:textColor="#FFFFFF"
                android:textSize="15sp" android:textStyle="bold" android:padding="14dp"/>
            <androidx.recyclerview.widget.RecyclerView android:id="@+id/season_rv"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:paddingStart="8dp" android:paddingEnd="8dp" android:paddingBottom="24dp"
                android:nestedScrollingEnabled="false"/>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
""")

w("app/src/main/res/layout/activity_watch.xml", """<?xml version="1.0" encoding="utf-8"?>
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
                    android:src="@drawable/ic_close" android:tint="#FFFFFF"
                    android:background="#88000000" android:contentDescription="Back"/>
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
                    <TextView android:id="@+id/meta_text"
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
                            <ImageButton android:id="@+id/download_btn" android:layout_width="36dp" android:layout_height="36dp"
                                android:src="@drawable/ic_download" android:tint="#FFFFFF"
                                android:background="?attr/selectableItemBackground"/>
                            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                                android:text="Save" android:textColor="#888888" android:textSize="10sp"/>
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
                android:orientation="vertical" android:paddingStart="12dp" android:paddingEnd="12dp">
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="horizontal" android:paddingBottom="8dp">
                    <Button android:id="@+id/play_btn"
                        android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="1"
                        android:text="PLAY" android:textSize="14sp" android:textStyle="bold"
                        android:textColor="#000000" android:backgroundTint="#BB86FC" android:layout_marginEnd="8dp"/>
                    <Button android:id="@+id/resume_btn"
                        android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="1"
                        android:text="RESUME" android:textSize="13sp" android:textColor="#BB86FC"
                        android:backgroundTint="#22BB86FC" android:visibility="gone"/>
                </LinearLayout>
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="horizontal" android:paddingBottom="12dp">
                    <Spinner android:id="@+id/source_spinner"
                        android:layout_width="0dp" android:layout_height="44dp" android:layout_weight="1"
                        android:background="@drawable/spinner_bg" android:layout_marginEnd="8dp"/>
                    <Button android:id="@+id/episode_menu_btn"
                        android:layout_width="wrap_content" android:layout_height="44dp"
                        android:text="Episodes" android:textSize="12sp" android:textColor="#BB86FC"
                        android:backgroundTint="#22BB86FC" android:visibility="gone"/>
                </LinearLayout>
            </LinearLayout>
            <TextView android:id="@+id/overview_text"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="#BBBBBB" android:textSize="13sp" android:lineSpacingExtra="3dp"
                android:paddingStart="14dp" android:paddingEnd="14dp" android:paddingBottom="12dp" android:visibility="gone"/>
            <LinearLayout android:id="@+id/tv_section"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:visibility="gone">
                <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:scrollbars="none" android:paddingStart="12dp" android:paddingEnd="12dp" android:paddingBottom="8dp">
                    <LinearLayout android:id="@+id/season_buttons_container"
                        android:layout_width="wrap_content" android:layout_height="wrap_content" android:orientation="horizontal"/>
                </HorizontalScrollView>
                <EditText android:id="@+id/ep_search_bar"
                    android:layout_width="match_parent" android:layout_height="44dp"
                    android:hint="Search episode number or name..."
                    android:textColorHint="#555555" android:textColor="#FFFFFF"
                    android:background="#1A1A1A" android:paddingStart="14dp" android:paddingEnd="14dp"
                    android:inputType="text" android:imeOptions="actionSearch"
                    android:layout_marginStart="12dp" android:layout_marginEnd="12dp"
                    android:layout_marginBottom="8dp" android:visibility="gone"/>
                <androidx.recyclerview.widget.RecyclerView android:id="@+id/episode_list_rv"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:nestedScrollingEnabled="false"/>
            </LinearLayout>
            <LinearLayout android:id="@+id/similar_section"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:visibility="gone">
                <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:text="More Like This" android:textColor="#FFFFFF"
                    android:textSize="15sp" android:textStyle="bold" android:padding="14dp"/>
                <androidx.recyclerview.widget.RecyclerView android:id="@+id/similar_rv"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:paddingStart="8dp" android:paddingEnd="8dp" android:paddingBottom="24dp"
                    android:nestedScrollingEnabled="false"/>
            </LinearLayout>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
    <ProgressBar android:id="@+id/loading_view"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center"/>
</FrameLayout>
""")

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
    <View android:id="@+id/touch_interceptor"
        android:layout_width="match_parent" android:layout_height="match_parent"
        android:clickable="true" android:focusable="true"/>
    <LinearLayout android:id="@+id/top_controls"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:layout_gravity="top"
        android:padding="8dp" android:gravity="end" android:background="#44000000">
        <ImageButton android:id="@+id/pip_btn"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@drawable/ic_pip" android:background="?attr/selectableItemBackground"
            android:tint="#FFFFFF" android:padding="6dp" android:layout_marginEnd="8dp" android:contentDescription="PiP"/>
        <ImageButton android:id="@+id/share_btn"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@drawable/ic_share" android:background="?attr/selectableItemBackground"
            android:tint="#FFFFFF" android:padding="6dp" android:layout_marginEnd="8dp" android:contentDescription="Share"/>
        <ImageButton android:id="@+id/fullscreen_btn"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@drawable/ic_fullscreen" android:background="?attr/selectableItemBackground"
            android:tint="#FFFFFF" android:padding="6dp" android:contentDescription="Fullscreen"/>
    </LinearLayout>
    <LinearLayout android:id="@+id/controls_bar"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:layout_gravity="bottom" android:background="#CC000000">
        <LinearLayout android:layout_width="match_parent" android:layout_height="52dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingStart="8dp" android:paddingEnd="8dp">
            <ImageButton android:id="@+id/back_btn"
                android:layout_width="40dp" android:layout_height="40dp"
                android:src="@drawable/ic_close" android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackground" android:contentDescription="Back"/>
            <TextView android:id="@+id/title_text"
                android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                android:textColor="#EEEEEE" android:textSize="13sp" android:maxLines="1"
                android:ellipsize="end" android:layout_marginStart="8dp" android:layout_marginEnd="8dp"/>
            <TextView android:id="@+id/episode_label"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:textColor="#BB86FC" android:textSize="12sp" android:textStyle="bold"
                android:layout_marginEnd="8dp" android:visibility="gone"/>
            <Spinner android:id="@+id/source_spinner"
                android:layout_width="120dp" android:layout_height="38dp" android:background="@drawable/spinner_bg"/>
        </LinearLayout>
        <LinearLayout android:id="@+id/tv_controls"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:paddingStart="8dp" android:paddingEnd="8dp"
            android:paddingBottom="10dp" android:visibility="gone">
            <Button android:id="@+id/prev_btn"
                android:layout_width="0dp" android:layout_height="40dp" android:layout_weight="1"
                android:text="Prev" android:textSize="12sp" android:layout_marginEnd="6dp" android:backgroundTint="#1F1F1F"/>
            <Button android:id="@+id/next_btn"
                android:layout_width="0dp" android:layout_height="40dp" android:layout_weight="1"
                android:text="Next" android:textSize="12sp" android:layout_marginStart="6dp"
                android:backgroundTint="#BB86FC" android:textColor="#000000"/>
        </LinearLayout>
    </LinearLayout>
</FrameLayout>
""")

w("app/src/main/res/layout/activity_search.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="60dp"
        android:gravity="center_vertical" android:background="#141414"
        android:paddingStart="4dp" android:paddingEnd="8dp">
        <ImageButton android:id="@+id/back_btn"
            android:layout_width="44dp" android:layout_height="44dp"
            android:src="@drawable/ic_close" android:tint="#FFFFFF"
            android:background="?attr/selectableItemBackground" android:contentDescription="Back"/>
        <SearchView android:id="@+id/search_input"
            android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="1"
            android:queryHint="Search movies, TV shows, anime..."
            android:iconifiedByDefault="false" android:background="#1A1A1A"/>
    </LinearLayout>
    <TextView android:id="@+id/empty_text"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="Type to search..." android:textColor="#555555" android:textSize="14sp"
        android:gravity="center" android:padding="40dp"/>
    <androidx.recyclerview.widget.RecyclerView android:id="@+id/results_rv"
        android:layout_width="match_parent" android:layout_height="match_parent" android:padding="4dp"/>
</LinearLayout>
""")

w("app/src/main/res/layout/activity_anime_detail.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
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
    <androidx.core.widget.NestedScrollView android:layout_width="match_parent" android:layout_height="match_parent">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:padding="12dp">
                <ImageView android:id="@+id/poster_image"
                    android:layout_width="90dp" android:layout_height="130dp"
                    android:scaleType="centerCrop" android:background="#1E1E1E" android:layout_marginEnd="12dp"/>
                <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
                    android:layout_weight="1" android:orientation="vertical">
                    <TextView android:id="@+id/title_text"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:textColor="#FFFFFF" android:textSize="18sp" android:textStyle="bold"/>
                    <TextView android:id="@+id/rating_text"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:textColor="#888888" android:textSize="12sp" android:layout_marginTop="6dp"/>
                    <Button android:id="@+id/watch_btn"
                        android:layout_width="match_parent" android:layout_height="48dp"
                        android:layout_marginTop="12dp" android:text="WATCH NOW"
                        android:textColor="#000000" android:textStyle="bold" android:backgroundTint="#BB86FC"/>
                </LinearLayout>
            </LinearLayout>
            <TextView android:id="@+id/desc_text"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="#BBBBBB" android:textSize="13sp" android:lineSpacingExtra="3dp"
                android:paddingStart="14dp" android:paddingEnd="14dp" android:paddingBottom="24dp"/>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</LinearLayout>
""")

w("app/src/main/res/layout/fragment_more.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414" android:paddingStart="16dp">
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Menu" android:textSize="22sp" android:textStyle="bold" android:textColor="#FFFFFF"/>
    </LinearLayout>
    <ScrollView android:layout_width="match_parent" android:layout_height="match_parent" android:overScrollMode="never">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" android:paddingBottom="32dp">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:padding="16dp" android:background="#141414" android:layout_marginBottom="8dp">
                <TextView android:layout_width="56dp" android:layout_height="56dp"
                    android:background="@drawable/circle_bg" android:gravity="center"
                    android:text="F" android:textColor="#000000" android:textSize="22sp"
                    android:textStyle="bold" android:layout_marginEnd="16dp"/>
                <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:orientation="vertical" android:gravity="center_vertical">
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:text="Fountain Home" android:textColor="#BB86FC" android:textSize="17sp" android:textStyle="bold"/>
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:text="v1.24 — Premium Streaming" android:textColor="#888888" android:textSize="12sp"/>
                </LinearLayout>
            </LinearLayout>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="LIBRARY" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="8dp" android:paddingBottom="4dp"/>
            <LinearLayout android:id="@+id/nav_watchlist_page"
                android:layout_width="match_parent" android:layout_height="56dp"
                android:gravity="center_vertical" android:background="#141414"
                android:paddingStart="16dp" android:paddingEnd="16dp">
                <ImageView android:layout_width="24dp" android:layout_height="24dp"
                    android:src="@drawable/ic_watch_later" android:tint="#BB86FC" android:layout_marginEnd="16dp"/>
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="Watch List" android:textColor="#FFFFFF" android:textSize="15sp"/>
                <ImageView android:layout_width="20dp" android:layout_height="20dp"
                    android:src="@drawable/ic_arrow_right" android:tint="#555555"/>
            </LinearLayout>
            <View android:layout_width="match_parent" android:layout_height="0.5dp"
                android:background="#2A2A2A" android:layout_marginStart="56dp"/>
            <LinearLayout android:id="@+id/nav_downloads_page"
                android:layout_width="match_parent" android:layout_height="56dp"
                android:gravity="center_vertical" android:background="#141414"
                android:paddingStart="16dp" android:paddingEnd="16dp">
                <ImageView android:layout_width="24dp" android:layout_height="24dp"
                    android:src="@drawable/ic_download" android:tint="#BB86FC" android:layout_marginEnd="16dp"/>
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="Downloads" android:textColor="#FFFFFF" android:textSize="15sp"/>
                <ImageView android:layout_width="20dp" android:layout_height="20dp"
                    android:src="@drawable/ic_arrow_right" android:tint="#555555"/>
            </LinearLayout>
            <View android:layout_width="match_parent" android:layout_height="0.5dp"
                android:background="#2A2A2A" android:layout_marginStart="56dp"/>
            <LinearLayout android:id="@+id/nav_history_page"
                android:layout_width="match_parent" android:layout_height="56dp"
                android:gravity="center_vertical" android:background="#141414"
                android:paddingStart="16dp" android:paddingEnd="16dp">
                <ImageView android:layout_width="24dp" android:layout_height="24dp"
                    android:src="@drawable/ic_star" android:tint="#BB86FC" android:layout_marginEnd="16dp"/>
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="History" android:textColor="#FFFFFF" android:textSize="15sp"/>
                <ImageView android:layout_width="20dp" android:layout_height="20dp"
                    android:src="@drawable/ic_arrow_right" android:tint="#555555"/>
            </LinearLayout>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="SETTINGS" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="4dp"/>
            <LinearLayout android:id="@+id/nav_settings_page"
                android:layout_width="match_parent" android:layout_height="56dp"
                android:gravity="center_vertical" android:background="#141414"
                android:paddingStart="16dp" android:paddingEnd="16dp">
                <ImageView android:layout_width="24dp" android:layout_height="24dp"
                    android:src="@drawable/ic_filter" android:tint="#BB86FC" android:layout_marginEnd="16dp"/>
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="Settings" android:textColor="#FFFFFF" android:textSize="15sp"/>
                <ImageView android:layout_width="20dp" android:layout_height="20dp"
                    android:src="@drawable/ic_arrow_right" android:tint="#555555"/>
            </LinearLayout>
            <View android:layout_width="match_parent" android:layout_height="0.5dp"
                android:background="#2A2A2A" android:layout_marginStart="56dp"/>
            <LinearLayout android:id="@+id/nav_about_page"
                android:layout_width="match_parent" android:layout_height="56dp"
                android:gravity="center_vertical" android:background="#141414"
                android:paddingStart="16dp" android:paddingEnd="16dp">
                <ImageView android:layout_width="24dp" android:layout_height="24dp"
                    android:src="@drawable/ic_info" android:tint="#BB86FC" android:layout_marginEnd="16dp"/>
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="About" android:textColor="#FFFFFF" android:textSize="15sp"/>
                <ImageView android:layout_width="20dp" android:layout_height="20dp"
                    android:src="@drawable/ic_arrow_right" android:tint="#555555"/>
            </LinearLayout>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
""")

w("app/src/main/res/layout/fragment_settings.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414"
        android:paddingStart="4dp" android:paddingEnd="8dp">
        <ImageButton android:id="@+id/back_btn"
            android:layout_width="44dp" android:layout_height="44dp"
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
                    <Spinner android:id="@+id/source_spinner"
                        android:layout_width="150dp" android:layout_height="40dp" android:background="@drawable/spinner_bg"/>
                </LinearLayout>
                <View android:layout_width="match_parent" android:layout_height="0.5dp"
                    android:background="#2A2A2A" android:layout_marginStart="16dp"/>
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
                <View android:layout_width="match_parent" android:layout_height="0.5dp"
                    android:background="#2A2A2A" android:layout_marginStart="16dp"/>
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
                <View android:layout_width="match_parent" android:layout_height="0.5dp"
                    android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Subtitle Language" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/sub_lang_spinner"
                        android:layout_width="160dp" android:layout_height="40dp" android:background="@drawable/spinner_bg"/>
                </LinearLayout>
            </LinearLayout>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="ANIME" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                android:gravity="center_vertical" android:background="#141414"
                android:paddingStart="16dp" android:paddingEnd="16dp">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                    android:text="Sub / Dub Default" android:textColor="#FFFFFF" android:textSize="15sp"/>
                <Spinner android:id="@+id/dub_sub_spinner"
                    android:layout_width="150dp" android:layout_height="40dp" android:background="@drawable/spinner_bg"/>
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
                <View android:layout_width="match_parent" android:layout_height="0.5dp"
                    android:background="#2A2A2A" android:layout_marginStart="16dp"/>
                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
                    android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="16dp">
                    <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                        android:text="Download Quality" android:textColor="#FFFFFF" android:textSize="15sp"/>
                    <Spinner android:id="@+id/quality_spinner"
                        android:layout_width="120dp" android:layout_height="40dp" android:background="@drawable/spinner_bg"/>
                </LinearLayout>
            </LinearLayout>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="STORAGE" android:textColor="#BB86FC" android:textSize="11sp"
                android:paddingStart="16dp" android:paddingTop="16dp" android:paddingBottom="6dp"/>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="vertical" android:background="#141414">
                <TextView android:id="@+id/storage_text"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:text="Cache: calculating..." android:textColor="#888888"
                    android:textSize="12sp" android:paddingStart="16dp" android:paddingTop="12dp" android:paddingBottom="4dp"/>
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

w("app/src/main/res/layout/fragment_about.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414"
        android:paddingStart="4dp" android:paddingEnd="8dp">
        <ImageButton android:id="@+id/back_btn"
            android:layout_width="44dp" android:layout_height="44dp"
            android:src="@drawable/ic_close" android:tint="#FFFFFF"
            android:background="?attr/selectableItemBackground" android:contentDescription="Back"/>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="About" android:textSize="18sp" android:textStyle="bold"
            android:textColor="#FFFFFF" android:layout_marginStart="8dp"/>
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="match_parent"
        android:orientation="vertical" android:gravity="center" android:padding="32dp">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="FOUNTAIN HOME" android:textColor="#BB86FC"
            android:textSize="28sp" android:textStyle="bold" android:gravity="center" android:layout_marginBottom="8dp"/>
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Version 1.24" android:textColor="#888888"
            android:textSize="14sp" android:gravity="center" android:layout_marginBottom="32dp"/>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Premium streaming — Movies, TV Series and Anime.\n\nSources: VidSrc, 2Embed, AutoEmbed, SuperEmbed\nAnime: AniList API\nData: TMDB API"
            android:textColor="#AAAAAA" android:textSize="14sp"
            android:gravity="center" android:lineSpacingExtra="4dp" android:layout_marginBottom="32dp"/>
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Built with Java + Android SDK"
            android:textColor="#555555" android:textSize="12sp" android:gravity="center"/>
    </LinearLayout>
</LinearLayout>
""")

w("app/src/main/res/layout/fragment_downloads.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414"
        android:paddingStart="4dp" android:paddingEnd="8dp">
        <ImageButton android:id="@+id/back_btn"
            android:layout_width="44dp" android:layout_height="44dp"
            android:src="@drawable/ic_close" android:tint="#FFFFFF"
            android:background="?attr/selectableItemBackground" android:contentDescription="Back"/>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Downloads" android:textSize="18sp" android:textStyle="bold"
            android:textColor="#FFFFFF" android:layout_marginStart="8dp"/>
    </LinearLayout>
    <TextView android:id="@+id/empty_text"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="No downloads yet." android:textColor="#555555" android:textSize="14sp"
        android:gravity="center" android:padding="48dp"/>
    <androidx.recyclerview.widget.RecyclerView android:id="@+id/downloads_rv"
        android:layout_width="match_parent" android:layout_height="match_parent" android:padding="8dp"/>
</LinearLayout>
""")

w("app/src/main/res/layout/item_poster.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="115dp" android:layout_height="wrap_content"
    android:orientation="vertical" android:padding="4dp">
    <FrameLayout android:layout_width="match_parent" android:layout_height="165dp">
        <ImageView android:id="@+id/poster_image"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:scaleType="centerCrop" android:background="#1A1A1A"/>
        <TextView android:id="@+id/rating_text"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_gravity="top|end" android:layout_margin="4dp"
            android:background="#CC000000" android:textColor="#FFD600" android:textSize="10sp"
            android:textStyle="bold" android:paddingStart="5dp" android:paddingEnd="5dp"
            android:paddingTop="2dp" android:paddingBottom="2dp"/>
    </FrameLayout>
    <TextView android:id="@+id/title_text"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:textColor="#DDDDDD" android:textSize="11sp" android:maxLines="2"
        android:ellipsize="end" android:paddingTop="4dp" android:paddingStart="2dp" android:paddingEnd="2dp"/>
</LinearLayout>
""")

w("app/src/main/res/layout/item_anime.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="115dp" android:layout_height="wrap_content"
    android:orientation="vertical" android:padding="4dp">
    <FrameLayout android:layout_width="match_parent" android:layout_height="165dp">
        <ImageView android:id="@+id/poster_image"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:scaleType="centerCrop" android:background="#1A1A1A"/>
        <TextView android:id="@+id/rating_text"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_gravity="top|end" android:layout_margin="4dp"
            android:background="#CC000000" android:textColor="#FFD600" android:textSize="10sp"
            android:textStyle="bold" android:paddingStart="5dp" android:paddingEnd="5dp"
            android:paddingTop="2dp" android:paddingBottom="2dp"/>
        <TextView android:id="@+id/episodes_text"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_gravity="bottom|start" android:layout_margin="4dp"
            android:background="#CC000000" android:textColor="#AAAAAA" android:textSize="9sp"
            android:paddingStart="4dp" android:paddingEnd="4dp"
            android:paddingTop="2dp" android:paddingBottom="2dp"/>
    </FrameLayout>
    <TextView android:id="@+id/title_text"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:textColor="#DDDDDD" android:textSize="11sp" android:maxLines="2"
        android:ellipsize="end" android:paddingTop="4dp" android:paddingStart="2dp" android:paddingEnd="2dp"/>
</LinearLayout>
""")

w("app/src/main/res/layout/item_episode.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:padding="10dp"
    android:background="?attr/selectableItemBackground">
    <FrameLayout android:layout_width="120dp" android:layout_height="70dp" android:layout_marginEnd="12dp">
        <ImageView android:id="@+id/ep_thumbnail"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:scaleType="centerCrop" android:background="#1E1E1E"/>
        <View android:id="@+id/ep_progress"
            android:layout_width="60dp" android:layout_height="3dp"
            android:layout_gravity="bottom|start" android:background="#BB86FC" android:visibility="gone"/>
        <TextView android:id="@+id/ep_number"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_gravity="bottom|end" android:background="#CC000000"
            android:textColor="#FFFFFF" android:textSize="10sp" android:padding="3dp"/>
    </FrameLayout>
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical" android:gravity="center_vertical">
        <TextView android:id="@+id/ep_title"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="#EEEEEE" android:textSize="13sp" android:textStyle="bold"
            android:maxLines="1" android:ellipsize="end"/>
        <TextView android:id="@+id/ep_duration"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textColor="#888888" android:textSize="11sp" android:visibility="gone"/>
        <TextView android:id="@+id/ep_overview"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="#AAAAAA" android:textSize="11sp" android:maxLines="2" android:ellipsize="end"/>
    </LinearLayout>
    <ImageView android:id="@+id/download_icon"
        android:layout_width="24dp" android:layout_height="24dp"
        android:layout_gravity="center_vertical" android:layout_marginStart="8dp"
        android:src="@drawable/ic_download" android:tint="#FFFFFF"/>
</LinearLayout>
""")

w("app/src/main/res/layout/dialog_episode_menu.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="#0A0A0A">
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:gravity="center_vertical" android:background="#141414" android:paddingStart="16dp">
        <TextView android:id="@+id/dialog_title"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="#FFFFFF" android:textSize="16sp" android:textStyle="bold"/>
    </LinearLayout>
    <androidx.recyclerview.widget.RecyclerView android:id="@+id/episodes_rv"
        android:layout_width="match_parent" android:layout_height="match_parent" android:padding="8dp"/>
</LinearLayout>
""")

print("All layout files written!")
