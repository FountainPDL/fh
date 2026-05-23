package com.fountainhome.streaming.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fountainhome.streaming.databinding.ActivitySettingsBinding;
import com.fountainhome.streaming.service.AppPreferences;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backBtn.setOnClickListener(v -> finish());

        String t = AppPreferences.getTheme(this);
        updateThemeBtns(t);

        binding.themeDark.setOnClickListener(v -> {
            AppPreferences.setTheme(this, AppPreferences.THEME_DARK);
            updateThemeBtns(AppPreferences.THEME_DARK);
            Toast.makeText(this, "Dark theme applied", Toast.LENGTH_SHORT).show();
            recreate();
        });

        binding.themeLight.setOnClickListener(v -> {
            AppPreferences.setTheme(this, AppPreferences.THEME_LIGHT);
            updateThemeBtns(AppPreferences.THEME_LIGHT);
            Toast.makeText(this, "Light theme applied", Toast.LENGTH_SHORT).show();
            recreate();
        });
    }

    private void updateThemeBtns(String theme) {
        binding.themeDark.setAlpha(AppPreferences.THEME_DARK.equals(theme) ? 1f : 0.4f);
        binding.themeLight.setAlpha(AppPreferences.THEME_LIGHT.equals(theme) ? 1f : 0.4f);
    }
}
