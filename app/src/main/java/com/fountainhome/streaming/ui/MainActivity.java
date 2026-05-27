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

        // MUST be before super.onCreate to catch all crashes
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                final String msg = buildCrashReport(throwable);
                runOnUiThread(() -> {
                    try {
                        ScrollView sv = new ScrollView(MainActivity.this);
                        TextView tv = new TextView(MainActivity.this);
                        tv.setPadding(24, 80, 24, 24);
                        tv.setTextSize(11);
                        tv.setTextColor(0xFFFF6666);
                        tv.setBackgroundColor(0xFF000000);
                        tv.setText(msg);
                        sv.addView(tv);
                        setContentView(sv);
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        });

        try {
            super.onCreate(savedInstanceState);
        } catch (Throwable t) {
            showCrash(t);
            return;
        }

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Throwable t) {
            showCrash(t);
            return;
        }

        try {
            applyAccent();
        } catch (Throwable ignored) {}

        try {
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
        } catch (Throwable t) {
            showCrash(t);
        }
    }

    private void showCrash(Throwable t) {
        try {
            ScrollView sv = new ScrollView(this);
            TextView tv = new TextView(this);
            tv.setPadding(24, 80, 24, 24);
            tv.setTextSize(11);
            tv.setTextColor(0xFFFF6666);
            tv.setBackgroundColor(0xFF000000);
            tv.setText(buildCrashReport(t));
            sv.addView(tv);
            setContentView(sv);
        } catch (Exception ignored) {}
    }

    private String buildCrashReport(Throwable t) {
        StringBuilder sb = new StringBuilder("=== CRASH REPORT ===\n\n");
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n\n");
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append("  ").append(e).append("\n");
            if (sb.length() > 4000) { sb.append("...truncated"); break; }
        }
        if (t.getCause() != null) {
            sb.append("\nCAUSED BY: ").append(t.getCause()).append("\n");
            for (StackTraceElement e : t.getCause().getStackTrace()) {
                sb.append("  ").append(e).append("\n");
                if (sb.length() > 7000) break;
            }
        }
        return sb.toString();
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
