package com.fountainhome.streaming.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fountainhome.streaming.databinding.ActivitySettingsBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backBtn.setOnClickListener(v -> finish());

        setupTheme();
        setupAccentColors();
        setupSource();
        setupPlayer();
        setupDownloads();
        setupLibrary();
        setupPrivacy();
    }

    private void setupTheme() {
        String t = AppPreferences.getTheme(this);
        binding.themeDark.setAlpha(AppPreferences.THEME_DARK.equals(t)   ? 1f : 0.4f);
        binding.themeAmoled.setAlpha(AppPreferences.THEME_AMOLED.equals(t) ? 1f : 0.4f);
        binding.themeLight.setAlpha(AppPreferences.THEME_LIGHT.equals(t)  ? 1f : 0.4f);

        binding.themeDark.setOnClickListener(v -> applyTheme(AppPreferences.THEME_DARK));
        binding.themeAmoled.setOnClickListener(v -> applyTheme(AppPreferences.THEME_AMOLED));
        binding.themeLight.setOnClickListener(v -> applyTheme(AppPreferences.THEME_LIGHT));
    }

    private void applyTheme(String theme) {
        AppPreferences.setTheme(this, theme);
        Toast.makeText(this, "Theme changed. Restart app to apply.", Toast.LENGTH_SHORT).show();
        binding.themeDark.setAlpha(AppPreferences.THEME_DARK.equals(theme)   ? 1f : 0.4f);
        binding.themeAmoled.setAlpha(AppPreferences.THEME_AMOLED.equals(theme) ? 1f : 0.4f);
        binding.themeLight.setAlpha(AppPreferences.THEME_LIGHT.equals(theme)  ? 1f : 0.4f);
    }

    private void setupAccentColors() {
        String[][] colors = {
            {AppPreferences.COLOR_PURPLE, "purple"},
            {AppPreferences.COLOR_BLUE,   "blue"},
            {AppPreferences.COLOR_RED,    "red"},
            {AppPreferences.COLOR_GREEN,  "green"},
            {AppPreferences.COLOR_ORANGE, "orange"},
            {AppPreferences.COLOR_PINK,   "pink"},
            {AppPreferences.COLOR_TEAL,   "teal"},
        };
        android.view.View[] swatches = {
            binding.colorPurple, binding.colorBlue, binding.colorRed,
            binding.colorGreen,  binding.colorOrange, binding.colorPink, binding.colorTeal
        };
        String cur = AppPreferences.getAccent(this);
        for (int i = 0; i < swatches.length; i++) {
            final String hex = colors[i][0];
            swatches[i].setBackgroundColor(Color.parseColor(hex));
            swatches[i].setAlpha(hex.equals(cur) ? 1f : 0.5f);
            swatches[i].setOnClickListener(v -> {
                AppPreferences.setAccent(this, hex);
                for (android.view.View sw : swatches) sw.setAlpha(0.5f);
                v.setAlpha(1f);
                Toast.makeText(this, "Accent color set", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupSource() {
        String[] sources = {"VidSrc", "2Embed", "AutoEmbed", "MultiEmbed"};
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, sources);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sourceSpinner.setAdapter(a);

        String cur = AppPreferences.getSource(this);
        for (int i = 0; i < sources.length; i++)
            if (sources[i].equals(cur)) { binding.sourceSpinner.setSelection(i); break; }

        binding.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, android.view.View v, int pos, long id) {
                AppPreferences.setSource(SettingsActivity.this, sources[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupPlayer() {
        binding.autoplaySwitch.setChecked(AppPreferences.getAutoplay(this));
        binding.autoplaySwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setAutoplay(this, c));

        binding.hwAccelSwitch.setChecked(AppPreferences.getHwAccel(this));
        binding.hwAccelSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setHwAccel(this, c));

        String[] langs = {"English (en)", "Spanish (es)", "French (fr)", "German (de)",
                          "Japanese (ja)", "Korean (ko)", "Portuguese (pt)", "Arabic (ar)"};
        String[] codes = {"en","es","fr","de","ja","ko","pt","ar"};
        ArrayAdapter<String> la = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, langs);
        la.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.subLangSpinner.setAdapter(la);

        String curLang = AppPreferences.getSubLang(this);
        for (int i = 0; i < codes.length; i++)
            if (codes[i].equals(curLang)) { binding.subLangSpinner.setSelection(i); break; }

        binding.subLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, android.view.View v, int pos, long id) {
                AppPreferences.setSubLang(SettingsActivity.this, codes[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupDownloads() {
        binding.wifiOnlySwitch.setChecked(AppPreferences.getWifiOnly(this));
        binding.wifiOnlySwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setWifiOnly(this, c));

        String[] quals = {"480p", "720p", "1080p"};
        ArrayAdapter<String> qa = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, quals);
        qa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.qualitySpinner.setAdapter(qa);

        String curQ = AppPreferences.getDlQuality(this);
        for (int i = 0; i < quals.length; i++)
            if (quals[i].equals(curQ)) { binding.qualitySpinner.setSelection(i); break; }

        binding.qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, android.view.View v, int pos, long id) {
                AppPreferences.setDlQuality(SettingsActivity.this, quals[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupLibrary() {
        binding.showContinueSwitch.setChecked(AppPreferences.getShowContinue(this));
        binding.showContinueSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setShowContinue(this, c));
    }

    private void setupPrivacy() {
        binding.clearCacheBtn.setOnClickListener(v -> {
            try {
                clearDir(getCacheDir());
                Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        binding.clearHistoryBtn.setOnClickListener(v -> {
            LibraryManager.clearList(this, LibraryManager.CONTINUE);
            Toast.makeText(this, "Watch history cleared", Toast.LENGTH_SHORT).show();
        });

        binding.clearAllBtn.setOnClickListener(v -> {
            LibraryManager.clearList(this, LibraryManager.FAVORITES);
            LibraryManager.clearList(this, LibraryManager.WATCHLIST);
            LibraryManager.clearList(this, LibraryManager.CONTINUE);
            AppPreferences.clearAll(this);
            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
        });

        // Storage info
        long cacheSize = getDirSize(getCacheDir());
        binding.storageText.setText("Cache: " + formatSize(cacheSize));
    }

    private void clearDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) for (String f : files) clearDir(new java.io.File(dir, f));
        }
        if (dir != null) dir.delete();
    }

    private long getDirSize(java.io.File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) for (java.io.File f : files) size += f.length();
        }
        return size;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
