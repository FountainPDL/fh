package com.fountainhome.streaming;
import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
public class FountainApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
