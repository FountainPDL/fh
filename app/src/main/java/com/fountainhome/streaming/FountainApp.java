package com.fountainhome.streaming;

import android.app.Application;
import com.fountainhome.streaming.ui.theme.ThemeManager;
import com.fountainhome.streaming.download.AppDatabase;

public class FountainApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ThemeManager.applyNightMode(this);
        com.fountainhome.streaming.api.TMDBClient.init(this);
        com.fountainhome.streaming.anime.AniListClient.init(this);
        // Was never called before, so the in-app crash report screen could not work.
        CrashLogger.init(this);
        // A download that was running when the process died is not running any more.
        new Thread(() -> {
            try { AppDatabase.get(this).downloadDao().failInterrupted(); } catch (Throwable ignored) {}
        }, "fh-dl-cleanup").start();
    }
}
