package com.fountainhome.streaming.ui;

import android.content.res.ColorStateList;
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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            AppPreferences.applyTheme(this);
        } catch (Exception e) {
            Log.e(TAG, "Theme error: " + e.getMessage());
        }

        super.onCreate(savedInstanceState);

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Exception e) {
            Log.e(TAG, "Layout inflate error: " + e.getMessage());
            return;
        }

        try {
            applyAccentToNav();
        } catch (Exception e) {
            Log.e(TAG, "Accent error: " + e.getMessage());
        }

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "home");
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)   { loadFragment(new HomeFragment(),   "home");   return true; }
            if (id == R.id.nav_movies) { loadFragment(new MoviesFragment(), "movies"); return true; }
            if (id == R.id.nav_tv)     { loadFragment(new TVFragment(),     "tv");     return true; }
            if (id == R.id.nav_anime)  { loadFragment(new AnimeFragment(),  "anime");  return true; }
            if (id == R.id.nav_more)   { loadFragment(new MoreFragment(),   "more");   return true; }
            return false;
        });
    }

    public void applyAccentToNav() {
        if (binding == null) return;
        int accent = AppPreferences.getAccentColor(this);
        ColorStateList csl = new ColorStateList(
            new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
            new int[]{ accent, 0xFF666666 });
        binding.bottomNav.setItemIconTintList(csl);
        binding.bottomNav.setItemTextColor(csl);
        binding.bottomNav.setBackgroundColor(AppPreferences.getSurfaceColor(this));
    }

    private void loadFragment(Fragment fragment, String tag) {
        try {
            getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
        } catch (Exception e) {
            Log.e(TAG, "Fragment load error: " + e.getMessage());
        }
    }
}
