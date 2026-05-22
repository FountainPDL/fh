package com.fountainhome.streaming.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Window;

import androidx.appcompat.app.AppCompatDelegate;

public class AppPreferences {
    private static final String PREFS = "fh_prefs";

    public static final String THEME_DARK   = "dark";
    public static final String THEME_AMOLED = "amoled";
    public static final String THEME_LIGHT  = "light";

    public static final String COLOR_GREEN  = "#CFFF04";
    public static final String COLOR_PURPLE = "#BB86FC";
    public static final String COLOR_BLUE   = "#2196F3";
    public static final String COLOR_RED    = "#CF6679";
    public static final String COLOR_ORANGE = "#FF9800";
    public static final String COLOR_PINK   = "#E91E8C";
    public static final String COLOR_TEAL   = "#03DAC6";

    private static SharedPreferences p(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // Theme
    public static String  getTheme(Context c)              { return p(c).getString("theme", THEME_DARK); }
    public static void    setTheme(Context c, String v)    { p(c).edit().putString("theme", v).apply(); }

    // Accent color
    public static String  getAccent(Context c)             { return p(c).getString("accent", COLOR_GREEN); }
    public static void    setAccent(Context c, String v)   { p(c).edit().putString("accent", v).apply(); }

    // Source
    public static String  getSource(Context c)             { return p(c).getString("source", "VSEmbed"); }
    public static void    setSource(Context c, String v)   { p(c).edit().putString("source", v).apply(); }

    // Player
    public static boolean getAutoplay(Context c)           { return p(c).getBoolean("autoplay", true); }
    public static void    setAutoplay(Context c, boolean v){ p(c).edit().putBoolean("autoplay", v).apply(); }
    public static boolean getHwAccel(Context c)            { return p(c).getBoolean("hw_accel", true); }
    public static void    setHwAccel(Context c, boolean v) { p(c).edit().putBoolean("hw_accel", v).apply(); }
    public static String  getSubLang(Context c)            { return p(c).getString("sub_lang", "en"); }
    public static void    setSubLang(Context c, String v)  { p(c).edit().putString("sub_lang", v).apply(); }

    // Downloads
    public static boolean getWifiOnly(Context c)           { return p(c).getBoolean("wifi_only", false); }
    public static void    setWifiOnly(Context c, boolean v){ p(c).edit().putBoolean("wifi_only", v).apply(); }
    public static String  getDlQuality(Context c)          { return p(c).getString("dl_quality", "720p"); }
    public static void    setDlQuality(Context c, String v){ p(c).edit().putString("dl_quality", v).apply(); }

    // Library
    public static boolean getShowContinue(Context c)       { return p(c).getBoolean("show_continue", true); }
    public static void    setShowContinue(Context c, boolean v){ p(c).edit().putBoolean("show_continue", v).apply(); }

    // Grid columns
    public static int     getGridCols(Context c)           { return p(c).getInt("grid_cols", 3); }
    public static void    setGridCols(Context c, int v)    { p(c).edit().putInt("grid_cols", v).apply(); }

    public static void clearAll(Context c) { p(c).edit().clear().apply(); }

    /**
     * Apply theme immediately — call this in every Activity.onCreate BEFORE setContentView
     */
    public static void applyTheme(Context c) {
        String theme = getTheme(c);
        switch (theme) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_AMOLED:
            case THEME_DARK:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    /**
     * Get background color for current theme
     */
    public static int getBackgroundColor(Context c) {
        switch (getTheme(c)) {
            case THEME_AMOLED: return Color.BLACK;
            case THEME_LIGHT:  return Color.WHITE;
            default:           return Color.parseColor("#070707");
        }
    }

    /**
     * Get text color for current theme
     */
    public static int getTextColor(Context c) {
        return THEME_LIGHT.equals(getTheme(c)) ? Color.BLACK : Color.WHITE;
    }

    /**
     * Get surface color for current theme
     */
    public static int getSurfaceColor(Context c) {
        switch (getTheme(c)) {
            case THEME_AMOLED: return Color.parseColor("#050505");
            case THEME_LIGHT:  return Color.parseColor("#F5F5F5");
            default:           return Color.parseColor("#0E0E0E");
        }
    }

    /**
     * Get accent as int color
     */
    public static int getAccentColor(Context c) {
        try { return Color.parseColor(getAccent(c)); }
        catch (Exception e) { return Color.parseColor(COLOR_GREEN); }
    }
}
