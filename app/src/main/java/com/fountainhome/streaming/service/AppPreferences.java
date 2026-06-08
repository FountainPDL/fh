package com.fountainhome.streaming.service;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
public class AppPreferences {
    private static final String PREFS = "fh_prefs";
    public static final String COLOR_PURPLE="#BB86FC", COLOR_BLUE="#2196F3", COLOR_RED="#CF6679";
    public static final String COLOR_GREEN="#4CAF50", COLOR_ORANGE="#FF9800", COLOR_PINK="#E91E8C", COLOR_TEAL="#03DAC6";
    public static final String STATUS_NONE="none", STATUS_PLAN="planning", STATUS_WATCH="watching",
        STATUS_DONE="watched", STATUS_DROP="dropped";
    private static SharedPreferences p(Context c) { return c.getSharedPreferences(PREFS, 0); }
    public static String  getAccent(Context c)               { return p(c).getString("accent", COLOR_PURPLE); }
    public static void    setAccent(Context c, String v)     { p(c).edit().putString("accent", v).apply(); }
    public static int     getAccentColor(Context c)          { try { return Color.parseColor(getAccent(c)); } catch (Exception e) { return Color.parseColor(COLOR_PURPLE); } }
    public static String  getSource(Context c)               { return p(c).getString("source", "VidSrc"); }
    public static void    setSource(Context c, String v)     { p(c).edit().putString("source", v).apply(); }
    public static String  getAnimeDubSub(Context c)          { return p(c).getString("dub_sub", "sub"); }
    public static void    setAnimeDubSub(Context c, String v){ p(c).edit().putString("dub_sub", v).apply(); }
    public static boolean getAutoplay(Context c)             { return p(c).getBoolean("autoplay", true); }
    public static void    setAutoplay(Context c, boolean v)  { p(c).edit().putBoolean("autoplay", v).apply(); }
    public static boolean getPiP(Context c)                  { return p(c).getBoolean("pip", true); }
    public static void    setPiP(Context c, boolean v)       { p(c).edit().putBoolean("pip", v).apply(); }
    public static float   getPlaybackSpeed(Context c)        { return p(c).getFloat("speed", 1.0f); }
    public static void    setPlaybackSpeed(Context c, float v){ p(c).edit().putFloat("speed", v).apply(); }
    public static boolean getHwAccel(Context c)              { return p(c).getBoolean("hw_accel", true); }
    public static void    setHwAccel(Context c, boolean v)   { p(c).edit().putBoolean("hw_accel", v).apply(); }
    public static boolean getAutoSkipIntro(Context c)        { return p(c).getBoolean("skip_intro", false); }
    public static void    setAutoSkipIntro(Context c, boolean v){ p(c).edit().putBoolean("skip_intro", v).apply(); }
    public static boolean getKeepScreenOn(Context c)         { return p(c).getBoolean("screen_on", true); }
    public static void    setKeepScreenOn(Context c, boolean v){ p(c).edit().putBoolean("screen_on", v).apply(); }
    public static String  getSubLang(Context c)              { return p(c).getString("sub_lang", "en"); }
    public static void    setSubLang(Context c, String v)    { p(c).edit().putString("sub_lang", v).apply(); }
    public static boolean getSubEnabled(Context c)           { return p(c).getBoolean("sub_on", true); }
    public static void    setSubEnabled(Context c, boolean v){ p(c).edit().putBoolean("sub_on", v).apply(); }
    public static boolean getWifiOnly(Context c)             { return p(c).getBoolean("wifi_only", false); }
    public static void    setWifiOnly(Context c, boolean v)  { p(c).edit().putBoolean("wifi_only", v).apply(); }
    public static String  getDlQuality(Context c)            { return p(c).getString("dl_quality", "720p"); }
    public static void    setDlQuality(Context c, String v)  { p(c).edit().putString("dl_quality", v).apply(); }
    public static boolean getSubWithDownload(Context c)      { return p(c).getBoolean("dl_sub", true); }
    public static void    setSubWithDownload(Context c, boolean v){ p(c).edit().putBoolean("dl_sub", v).apply(); }
    public static boolean getShowContinue(Context c)         { return p(c).getBoolean("show_continue", true); }
    public static void    setShowContinue(Context c, boolean v){ p(c).edit().putBoolean("show_continue", v).apply(); }
    public static boolean getShowRating(Context c)           { return p(c).getBoolean("show_rating", true); }
    public static void    setShowRating(Context c, boolean v){ p(c).edit().putBoolean("show_rating", v).apply(); }
    public static int     getGridColumns(Context c)          { return p(c).getInt("grid_cols", 3); }
    public static void    setGridColumns(Context c, int v)   { p(c).edit().putInt("grid_cols", v).apply(); }
    public static String  getItemStatus(Context c, int id, String type) { return p(c).getString("status_" + type + "_" + id, STATUS_NONE); }
    public static void    setItemStatus(Context c, int id, String type, String status) { p(c).edit().putString("status_" + type + "_" + id, status).apply(); }
    public static void    clearAll(Context c) { p(c).edit().clear().apply(); }
    public static int     getSurfaceColor(Context c) { return Color.parseColor("#141414"); }
}
