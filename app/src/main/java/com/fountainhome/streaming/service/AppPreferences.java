package com.fountainhome.streaming.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatDelegate;

public class AppPreferences {
    private static final String PREFS = "fh_prefs";

    public static final String THEME_DARK  = "dark";
    public static final String THEME_LIGHT = "light";

    // Accent colors — PURPLE is default
    public static final String COLOR_PURPLE = "#BB86FC";
    public static final String COLOR_BLUE   = "#2196F3";
    public static final String COLOR_RED    = "#CF6679";
    public static final String COLOR_GREEN  = "#4CAF50";
    public static final String COLOR_ORANGE = "#FF9800";
    public static final String COLOR_PINK   = "#E91E8C";
    public static final String COLOR_TEAL   = "#03DAC6";

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // Theme
    public static String  getTheme(Context c)               { return p(c).getString("theme", THEME_DARK); }
    public static void    setTheme(Context c, String v)     { p(c).edit().putString("theme", v).apply(); }

    // Accent — PURPLE default
    public static String  getAccent(Context c)              { return p(c).getString("accent", COLOR_PURPLE); }
    public static void    setAccent(Context c, String v)    { p(c).edit().putString("accent", v).apply(); }
    public static int     getAccentColor(Context c)         {
        try { return Color.parseColor(getAccent(c)); }
        catch (Exception e) { return Color.parseColor(COLOR_PURPLE); }
    }

    // Source
    public static String  getSource(Context c)              { return p(c).getString("source", "VidSrc"); }
    public static void    setSource(Context c, String v)    { p(c).edit().putString("source", v).apply(); }

    // Anime source
    public static String  getAnimeSource(Context c)         { return p(c).getString("anime_source", "Gogoanime"); }
    public static void    setAnimeSource(Context c, String v){ p(c).edit().putString("anime_source", v).apply(); }
    public static String  getAnimeDubSub(Context c)         { return p(c).getString("dub_sub", "sub"); }
    public static void    setAnimeDubSub(Context c, String v){ p(c).edit().putString("dub_sub", v).apply(); }

    // Player
    public static boolean getAutoplay(Context c)            { return p(c).getBoolean("autoplay", true); }
    public static void    setAutoplay(Context c, boolean v) { p(c).edit().putBoolean("autoplay", v).apply(); }
    public static boolean getHwAccel(Context c)             { return p(c).getBoolean("hw_accel", true); }
    public static void    setHwAccel(Context c, boolean v)  { p(c).edit().putBoolean("hw_accel", v).apply(); }
    public static boolean getPiP(Context c)                 { return p(c).getBoolean("pip", true); }
    public static void    setPiP(Context c, boolean v)      { p(c).edit().putBoolean("pip", v).apply(); }
    public static String  getSubLang(Context c)             { return p(c).getString("sub_lang", "en"); }
    public static void    setSubLang(Context c, String v)   { p(c).edit().putString("sub_lang", v).apply(); }
    public static float   getPlaybackSpeed(Context c)       { return p(c).getFloat("speed", 1.0f); }
    public static void    setPlaybackSpeed(Context c, float v){ p(c).edit().putFloat("speed", v).apply(); }
    public static boolean getSkipIntro(Context c)           { return p(c).getBoolean("skip_intro", false); }
    public static void    setSkipIntro(Context c, boolean v){ p(c).edit().putBoolean("skip_intro", v).apply(); }

    // Downloads
    public static boolean getWifiOnly(Context c)            { return p(c).getBoolean("wifi_only", false); }
    public static void    setWifiOnly(Context c, boolean v) { p(c).edit().putBoolean("wifi_only", v).apply(); }
    public static String  getDlQuality(Context c)           { return p(c).getString("dl_quality", "720p"); }
    public static void    setDlQuality(Context c, String v) { p(c).edit().putString("dl_quality", v).apply(); }

    // Library
    public static boolean getShowContinue(Context c)        { return p(c).getBoolean("show_continue", true); }
    public static void    setShowContinue(Context c, boolean v){ p(c).edit().putBoolean("show_continue", v).apply(); }

    // UI
    public static boolean isGridView(Context c)             { return p(c).getBoolean("grid_view", true); }
    public static void    setGridView(Context c, boolean v) { p(c).edit().putBoolean("grid_view", v).apply(); }
    public static String  getAppFont(Context c)             { return p(c).getString("app_font", "default"); }
    public static void    setAppFont(Context c, String v)   { p(c).edit().putString("app_font", v).apply(); }

    // Notifications
    public static boolean getEpNotify(Context c)            { return p(c).getBoolean("ep_notify", true); }
    public static void    setEpNotify(Context c, boolean v) { p(c).edit().putBoolean("ep_notify", v).apply(); }

    public static void clearAll(Context c) { p(c).edit().clear().apply(); }

    /** Apply theme — call before setContentView in every Activity */
    public static void applyTheme(Context c) {
        if (THEME_LIGHT.equals(getTheme(c))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public static int getBackgroundColor(Context c) {
        return THEME_LIGHT.equals(getTheme(c))
            ? Color.parseColor("#F5F5F5") : Color.parseColor("#0A0A0A");
    }

    public static int getSurfaceColor(Context c) {
        return THEME_LIGHT.equals(getTheme(c))
            ? Color.parseColor("#FFFFFF") : Color.parseColor("#141414");
    }

    public static int getTextColor(Context c) {
        return THEME_LIGHT.equals(getTheme(c))
            ? Color.parseColor("#111111") : Color.WHITE;
    }
}
