package com.fountainhome.streaming.ui;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.CrashLogger;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.ActivityMainBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.fragment.*;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show previous crash if any
        String prevCrash = CrashLogger.read(this);
        if (!prevCrash.equals("No crash log yet.")) {
            ScrollView sv = new ScrollView(this);
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setBackgroundColor(0xFF000000);

            Button dismiss = new Button(this);
            dismiss.setText("DISMISS & OPEN APP");
            dismiss.setOnClickListener(v -> {
                // Delete crash log and restart
                try {
                    java.io.File f = new java.io.File(getExternalFilesDir(null), "crash_log.txt");
                    f.delete();
                } catch (Exception ignored) {}
                initApp(savedInstanceState);
                sv.setVisibility(android.view.View.GONE);
            });
            ll.addView(dismiss);

            TextView tv = new TextView(this);
            tv.setPadding(24, 16, 24, 24);
            tv.setTextSize(10);
            tv.setTextColor(0xFFFF6666);
            tv.setText(prevCrash);
            ll.addView(tv);
            sv.addView(ll);
            setContentView(sv);
            return;
        }

        initApp(savedInstanceState);
    }

    private void initApp(Bundle savedInstanceState) {
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Throwable t) {
            TextView tv = new TextView(this);
            tv.setText("Layout error: " + t.getMessage());
            tv.setTextColor(0xFFFF0000);
            tv.setPadding(32, 80, 32, 32);
            setContentView(tv);
            return;
        }

        try { applyAccent(); } catch (Throwable ignored) {}

        try {
            if (savedInstanceState == null) {
                load(new HomeFragment(), "home");
                binding.bottomNav.setSelectedItemId(R.id.nav_home);
            }
            binding.bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home)   { load(new HomeFragment(),   "home");   return true; }
                if (id == R.id.nav_movies) { load(new MoviesFragment(), "movies"); return true; }
                if (id == R.id.nav_tv)     { load(new TVFragment(),     "tv");     return true; }
                if (id == R.id.nav_anime)  { load(new AnimeFragment(),  "anime");  return true; }
                if (id == R.id.nav_more)   { load(new MoreFragment(),   "more");   return true; }
                return false;
            });
        } catch (Throwable t) {
            Toast.makeText(this, "Nav error: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void applyAccent() {
        if (binding == null) return;
        int accent = AppPreferences.getAccentColor(this);
        ColorStateList csl = new ColorStateList(
            new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
            new int[]{ accent, 0xFF666666 });
        binding.bottomNav.setItemIconTintList(csl);
        binding.bottomNav.setItemTextColor(csl);
        binding.bottomNav.setBackgroundColor(0xFF141414);
    }

    private void load(Fragment f, String tag) {
        try {
            getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, f, tag)
                .commitAllowingStateLoss();
        } catch (Throwable t) {
            Toast.makeText(this, "Fragment error: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
