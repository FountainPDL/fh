package com.fountainhome.streaming.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.fountainhome.streaming.R;
import com.fountainhome.streaming.service.AppPreferences;

/** One place for light/dark mode, AMOLED black and the accent palettes. */
public final class ThemeManager {
    private ThemeManager() {}

    public static final String MODE_SYSTEM = "system", MODE_LIGHT = "light", MODE_DARK = "dark", MODE_AMOLED = "amoled";
    public static final String[] MODES = {MODE_SYSTEM, MODE_LIGHT, MODE_DARK, MODE_AMOLED};
    public static final String[] MODE_LABELS = {"Follow system", "Light", "Dark", "AMOLED black"};

    public static final String[] ACCENTS = {"purple", "blue", "red", "green", "orange", "pink", "teal",
        "gold", "crimson", "mint", "slate", "dynamic"};
    public static final String[] ACCENT_LABELS = {"Purple", "Blue", "Rose", "Green", "Orange", "Pink", "Teal",
        "Gold", "Crimson", "Mint", "Slate", "Material You"};

    @StyleRes public static int overlayFor(String accent) {
        switch (accent) {
            case "blue": return R.style.ThemeOverlay_Accent_Blue;
            case "red": return R.style.ThemeOverlay_Accent_Red;
            case "green": return R.style.ThemeOverlay_Accent_Green;
            case "orange": return R.style.ThemeOverlay_Accent_Orange;
            case "pink": return R.style.ThemeOverlay_Accent_Pink;
            case "teal": return R.style.ThemeOverlay_Accent_Teal;
            case "gold": return R.style.ThemeOverlay_Accent_Gold;
            case "crimson": return R.style.ThemeOverlay_Accent_Crimson;
            case "mint": return R.style.ThemeOverlay_Accent_Mint;
            case "slate": return R.style.ThemeOverlay_Accent_Slate;
            case "dynamic": return R.style.ThemeOverlay_Accent_Dynamic;
            default: return R.style.ThemeOverlay_Accent_Purple;
        }
    }

    @ColorRes public static int swatchFor(String accent) {
        switch (accent) {
            case "blue": return R.color.accent_blue;
            case "red": return R.color.accent_red;
            case "green": return R.color.accent_green;
            case "orange": return R.color.accent_orange;
            case "pink": return R.color.accent_pink;
            case "teal": return R.color.accent_teal;
            case "gold": return R.color.accent_gold;
            case "crimson": return R.color.accent_crimson;
            case "mint": return R.color.accent_mint;
            case "slate": return R.color.accent_slate;
            case "dynamic": return R.color.accent_dynamic;
            default: return R.color.accent_purple;
        }
    }

    /** Call once at startup and whenever the mode changes. */
    public static void applyNightMode(Context c) {
        String m = AppPreferences.getThemeMode(c);
        AppCompatDelegate.setDefaultNightMode(
            MODE_LIGHT.equals(m) ? AppCompatDelegate.MODE_NIGHT_NO
            : MODE_SYSTEM.equals(m) ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            : AppCompatDelegate.MODE_NIGHT_YES);
    }

    /** Call at the very start of every activity's onCreate, before super.onCreate. */
    public static void apply(Activity a) {
        a.getTheme().applyStyle(overlayFor(AppPreferences.getThemeAccent(a)), true);
        if (MODE_AMOLED.equals(AppPreferences.getThemeMode(a)) && isNight(a))
            a.getTheme().applyStyle(R.style.ThemeOverlay_Amoled, true);
    }

    /** Status and navigation bar icons must be dark on a light theme and light on a dark one. */
    public static void styleBars(Activity a) {
        View decor = a.getWindow().getDecorView();
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(a.getWindow(), decor);
        boolean light = !isNight(a);
        c.setAppearanceLightStatusBars(light);
        c.setAppearanceLightNavigationBars(light);
    }

    public static boolean isNight(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Resolve a theme attribute (for example R.attr.fhAccent) to a color for this context's theme. */
    @ColorInt public static int color(Context c, @AttrRes int attr) {
        TypedValue tv = new TypedValue();
        if (!c.getTheme().resolveAttribute(attr, tv, true)) return 0;
        if (tv.resourceId != 0) return ContextCompat.getColor(c, tv.resourceId);
        return tv.data;
    }

    public static String statusLabel(String status) {
        switch (status) {
            case AppPreferences.STATUS_PLAN: return "Planning";
            case AppPreferences.STATUS_WATCH: return "Watching";
            case AppPreferences.STATUS_DONE: return "Watched";
            case AppPreferences.STATUS_DROP: return "Dropped";
            default: return "Set Status";
        }
    }

    @ColorInt public static int statusColor(Context c, String status) {
        switch (status) {
            case AppPreferences.STATUS_PLAN: return color(c, R.attr.fhInfo);
            case AppPreferences.STATUS_WATCH: return color(c, R.attr.fhSuccess);
            case AppPreferences.STATUS_DONE: return color(c, R.attr.fhAccent);
            case AppPreferences.STATUS_DROP: return color(c, R.attr.fhDanger);
            default: return color(c, R.attr.fhTextDim);
        }
    }

    @ColorInt public static int accent(Context c) { return color(c, R.attr.fhAccent); }
}
