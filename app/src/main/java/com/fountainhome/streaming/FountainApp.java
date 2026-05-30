package com.fountainhome.streaming;
import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
public class FountainApp extends Application {
    @Override
    public void onCreate() {
        CrashLogger.init(this);
        super.onCreate();
        try {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } catch (Exception ignored) {}
    }
}
