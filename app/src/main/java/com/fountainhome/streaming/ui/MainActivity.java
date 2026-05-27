package com.fountainhome.streaming.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.ActivityMainBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.fragment.*;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Global crash handler — shows error ON SCREEN instead of silent close
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            runOnUiThread(() -> {
                try {
                    ScrollView sv = new ScrollView(this);
                    TextView tv = new TextView(this);
                    tv.setPadding(32, 80, 32, 32);
                    tv.setTextSize(12);
                    tv.setTextColor(0xFFFF4444);
                    tv.setBackgroundColor(0xFF0A0A0A);

                    StringBuilder sb = new StringBuilder();
                    sb.append("CRASH REPORT — copy this:\n\n");
                    sb.append(throwable.toString()).append("\n\n");
                    for (StackTraceElement e : throwable.getStackTrace()) {
                        sb.append("  at ").append(e.toString()).append("\n");
                        if (sb.length() > 3000) { sb.append("..."); break; }
                    }
                    Throwable cause = throwable.getCause();
                    if (cause != null) {
                        sb.append("\nCaused by: ").append(cause.toString()).append("\n");
                        for (StackTraceElement e : cause.getStackTrace()) {
                            sb.append("  at ").append(e.toString()).append("\n");
                            if (sb.length() > 5000) break;
                        }
                    }

                    tv.setText(sb.toString());
                    sv.addView(tv);
                    setContentView(sv);
                } catch (Exception ignored) {}
            });
        });

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyAccent();

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

    public void applyAccent() {
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
        getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment, tag)
            .commitAllowingStateLoss();
    }
}
