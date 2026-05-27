package com.fountainhome.streaming;

import android.app.Application;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.fountainhome.streaming.service.AppPreferences;

public class FountainApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (AppPreferences.THEME_LIGHT.equals(AppPreferences.getTheme(this)))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } catch (Exception e) {
            Log.e("FountainApp", "Theme error: " + e.getMessage());
        }
    }
}
