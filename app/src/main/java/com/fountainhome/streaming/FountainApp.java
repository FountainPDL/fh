package com.fountainhome.streaming;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.fountainhome.streaming.download.AppDatabase;

public class FountainApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        // Was never called before, so the in-app crash report screen could not work.
        CrashLogger.init(this);
        // A download that was running when the process died is not running any more.
        new Thread(() -> {
            try { AppDatabase.get(this).downloadDao().failInterrupted(); } catch (Throwable ignored) {}
        }, "fh-dl-cleanup").start();
    }
}
