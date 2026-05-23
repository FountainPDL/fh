package com.fountainhome.streaming.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.ActivityMainBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.fragment.AnimeFragment;
import com.fountainhome.streaming.ui.fragment.HomeFragment;
import com.fountainhome.streaming.ui.fragment.MoreFragment;
import com.fountainhome.streaming.ui.fragment.MoviesFragment;
import com.fountainhome.streaming.ui.fragment.TVFragment;
import com.fountainhome.streaming.ui.fragment.WatchListFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyAccentToNav();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "home");
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)      { loadFragment(new HomeFragment(),      "home");      return true; }
            if (id == R.id.nav_movies)    { loadFragment(new MoviesFragment(),    "movies");    return true; }
            if (id == R.id.nav_tv)        { loadFragment(new TVFragment(),        "tv");        return true; }
            if (id == R.id.nav_anime)     { loadFragment(new AnimeFragment(),     "anime");     return true; }
            if (id == R.id.nav_more)      { loadFragment(new MoreFragment(),      "more");      return true; }
            return false;
        });
    }

    public void applyAccentToNav() {
        if (binding == null) return;
        try {
            int accent = AppPreferences.getAccentColor(this);
            ColorStateList csl = new ColorStateList(
                new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
                new int[]{ accent, 0xFF666666 });
            binding.bottomNav.setItemIconTintList(csl);
            binding.bottomNav.setItemTextColor(csl);
            binding.bottomNav.setBackgroundColor(AppPreferences.getSurfaceColor(this));
        } catch (Exception e) {
            Log.e(TAG, "applyAccentToNav: " + e);
        }
    }

    private void loadFragment(Fragment fragment, String tag) {
        try {
            getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment, tag)
                .commitAllowingStateLoss();
        } catch (Exception e) {
            Log.e(TAG, "loadFragment " + tag + ": " + e);
        }
    }
}
