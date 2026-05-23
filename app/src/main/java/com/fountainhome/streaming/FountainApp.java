package com.fountainhome.streaming;

import android.app.Application;
import android.util.Log;
import com.fountainhome.streaming.service.AppPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class FountainApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Apply saved theme on every cold start
            String theme = AppPreferences.getTheme(this);
            if (AppPreferences.THEME_LIGHT.equals(theme)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        } catch (Exception e) {
            Log.e("FountainApp", "onCreate: " + e);
        }
    }
}
