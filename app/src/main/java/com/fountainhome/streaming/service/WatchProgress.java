package com.fountainhome.streaming.service;

import android.content.Context;
import android.content.SharedPreferences;

public class WatchProgress {
    private static final String PREFS = "fh_watch_progress";

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Save position in milliseconds */
    public static void save(Context c, int tmdbId, String type,
                            int season, int episode, long positionMs) {
        String key = key(tmdbId, type, season, episode);
        p(c).edit().putLong(key, positionMs).apply();
    }

    /** Get saved position in milliseconds (0 if none) */
    public static long get(Context c, int tmdbId, String type, int season, int episode) {
        return p(c).getLong(key(tmdbId, type, season, episode), 0L);
    }

    public static void clear(Context c, int tmdbId, String type, int season, int episode) {
        p(c).edit().remove(key(tmdbId, type, season, episode)).apply();
    }

    private static String key(int tmdbId, String type, int season, int episode) {
        return type + "_" + tmdbId + "_s" + season + "_e" + episode;
    }
}
