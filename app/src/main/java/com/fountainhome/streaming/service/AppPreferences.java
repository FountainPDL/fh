package com.fountainhome.streaming.service;
import android.content.Context;
import android.content.SharedPreferences;
public class AppPreferences {
    private static final String PREFS = "fh_prefs";
    public static final String STATUS_NONE="none", STATUS_PLAN="planning", STATUS_WATCH="watching",
        STATUS_DONE="watched", STATUS_DROP="dropped";
    private static SharedPreferences p(Context c) { return c.getSharedPreferences(PREFS, 0); }
    public static String  getThemeMode(Context c)             { return p(c).getString("theme_mode", "system"); }
    public static void    setThemeMode(Context c, String v)   { p(c).edit().putString("theme_mode", v).apply(); }
    public static String  getThemeAccent(Context c)           { return p(c).getString("theme_accent", "purple"); }
    public static void    setThemeAccent(Context c, String v) { p(c).edit().putString("theme_accent", v).apply(); }
    public static int     getAccentColor(Context c)           { return com.fountainhome.streaming.ui.theme.ThemeManager.accent(c); }
    public static String  getSource(Context c)                { return p(c).getString("source", "VidSrc"); }
    public static void    setSource(Context c, String v)      { p(c).edit().putString("source", v).apply(); }
    public static String  getAnimeSource(Context c)           { return p(c).getString("anime_source", "VidLink"); }
    public static void    setAnimeSource(Context c, String v) { p(c).edit().putString("anime_source", v).apply(); }
    /** Which anime website supplies episodes and streams: "auto" or a site name. */
    public static String  getAnimeSite(Context c)             { return p(c).getString("anime_site", "auto"); }
    public static void    setAnimeSite(Context c, String v)   { p(c).edit().putString("anime_site", v).apply(); }
    /** Play anime in the built-in player from a direct stream (falls back to the web player). */
    public static boolean getAnimeDirect(Context c)           { return p(c).getBoolean("anime_direct", true); }
    public static void    setAnimeDirect(Context c, boolean v){ p(c).edit().putBoolean("anime_direct", v).apply(); }
    public static String  getAnimeDubSub(Context c)           { return p(c).getString("dub_sub", "sub"); }
    public static void    setAnimeDubSub(Context c, String v) { p(c).edit().putString("dub_sub", v).apply(); }
    public static boolean getAutoplay(Context c)              { return p(c).getBoolean("autoplay", true); }
    public static void    setAutoplay(Context c, boolean v)   { p(c).edit().putBoolean("autoplay", v).apply(); }
    public static boolean getPiP(Context c)                   { return p(c).getBoolean("pip", true); }
    public static void    setPiP(Context c, boolean v)        { p(c).edit().putBoolean("pip", v).apply(); }
    public static float   getPlaybackSpeed(Context c)         { return p(c).getFloat("speed", 1.0f); }
    public static void    setPlaybackSpeed(Context c, float v){ p(c).edit().putFloat("speed", v).apply(); }
    public static boolean getHwAccel(Context c)               { return p(c).getBoolean("hw_accel", true); }
    public static void    setHwAccel(Context c, boolean v)    { p(c).edit().putBoolean("hw_accel", v).apply(); }
    public static boolean getAutoSkipIntro(Context c)         { return p(c).getBoolean("skip_intro", false); }
    public static void    setAutoSkipIntro(Context c, boolean v){ p(c).edit().putBoolean("skip_intro", v).apply(); }
    public static boolean getKeepScreenOn(Context c)          { return p(c).getBoolean("screen_on", true); }
    public static void    setKeepScreenOn(Context c, boolean v){ p(c).edit().putBoolean("screen_on", v).apply(); }
    public static String  getSubLang(Context c)               { return p(c).getString("sub_lang", "en"); }
    public static void    setSubLang(Context c, String v)     { p(c).edit().putString("sub_lang", v).apply(); }
    public static boolean getSubEnabled(Context c)            { return p(c).getBoolean("sub_on", true); }
    public static void    setSubEnabled(Context c, boolean v) { p(c).edit().putBoolean("sub_on", v).apply(); }
    public static int     getParallelDownloads(Context c)     { return p(c).getInt("parallel_dl", 3); }
    public static void    setParallelDownloads(Context c, int v) { p(c).edit().putInt("parallel_dl", v).apply(); }
    public static boolean getWifiOnly(Context c)              { return p(c).getBoolean("wifi_only", false); }
    public static void    setWifiOnly(Context c, boolean v)   { p(c).edit().putBoolean("wifi_only", v).apply(); }
    public static String  getDlQuality(Context c)             { return p(c).getString("dl_quality", "ask"); }
    public static void    setDlQuality(Context c, String v)   { p(c).edit().putString("dl_quality", v).apply(); }
    /** Quality picked in the download dialog for one queued item (falls back to the setting). */
    public static void    setDlOverride(Context c, int id, String q) { p(c).edit().putString("dlq_" + id, q).apply(); }
    public static String  getDlOverride(Context c, int id)    { return p(c).getString("dlq_" + id, null); }
    public static String  effectiveQuality(Context c, int id) {
        String o = getDlOverride(c, id);
        if (o != null && !o.isEmpty()) return o;
        String g = getDlQuality(c);
        return "ask".equals(g) ? "best" : g;
    }

    // ---- 18+ (anime)
    public static boolean getAdultEnabled(Context c)          { return p(c).getBoolean("adult_enabled", false); }
    public static void    setAdultEnabled(Context c, boolean v) { p(c).edit().putBoolean("adult_enabled", v).apply(); }
    public static boolean getAdultMode(Context c)             { return getAdultEnabled(c) && p(c).getBoolean("adult_mode", false); }
    public static void    setAdultMode(Context c, boolean v)  { p(c).edit().putBoolean("adult_mode", v).apply(); }

    // ---- subtitles
    public static boolean getConvertTs(Context c)             { return p(c).getBoolean("convert_ts", false); }
    public static void    setConvertTs(Context c, boolean v)  { p(c).edit().putBoolean("convert_ts", v).apply(); }
    public static boolean getSubDownload(Context c)           { return p(c).getBoolean("sub_dl", true); }
    public static void    setSubDownload(Context c, boolean v) { p(c).edit().putBoolean("sub_dl", v).apply(); }
    public static int     getSubSize(Context c)               { return p(c).getInt("sub_size", 100); }       // percent
    public static void    setSubSize(Context c, int v)        { p(c).edit().putInt("sub_size", v).apply(); }
    public static int     getSubColor(Context c)              { return p(c).getInt("sub_color", 0xFFFFFFFF); }
    public static void    setSubColor(Context c, int v)       { p(c).edit().putInt("sub_color", v).apply(); }
    public static int     getSubBg(Context c)                 { return p(c).getInt("sub_bg", 0); }          // 0 none, else 0-100 % black
    public static void    setSubBg(Context c, int v)          { p(c).edit().putInt("sub_bg", v).apply(); }
    public static int     getSubEdge(Context c)               { return p(c).getInt("sub_edge", 1); }        // 0 none 1 outline 2 shadow
    public static void    setSubEdge(Context c, int v)        { p(c).edit().putInt("sub_edge", v).apply(); }
    public static int     getSubBottom(Context c)             { return p(c).getInt("sub_bottom", 8); }      // % from bottom
    public static void    setSubBottom(Context c, int v)      { p(c).edit().putInt("sub_bottom", v).apply(); }
    /** Absolute path of the folder downloads are saved in; empty = the app's own folder. */
    public static String  getDownloadDir(Context c)           { return p(c).getString("dl_dir", ""); }
    public static void    setDownloadDir(Context c, String v) { p(c).edit().putString("dl_dir", v == null ? "" : v).apply(); }
    public static int     getImageCacheMb(Context c)          { return p(c).getInt("img_cache_mb", 250); }
    public static void    setImageCacheMb(Context c, int v)   { p(c).edit().putInt("img_cache_mb", v).apply(); }
    public static boolean getSubWithDownload(Context c)       { return p(c).getBoolean("dl_sub", true); }
    public static void    setSubWithDownload(Context c, boolean v){ p(c).edit().putBoolean("dl_sub", v).apply(); }
    public static boolean getShowContinue(Context c)          { return p(c).getBoolean("show_continue", true); }
    public static void    setShowContinue(Context c, boolean v){ p(c).edit().putBoolean("show_continue", v).apply(); }
    public static boolean getShowRating(Context c)            { return p(c).getBoolean("show_rating", true); }
    public static void    setShowRating(Context c, boolean v) { p(c).edit().putBoolean("show_rating", v).apply(); }
    public static int     getGridColumns(Context c)           { return p(c).getInt("grid_cols", 3); }
    public static void    setGridColumns(Context c, int v)    { p(c).edit().putInt("grid_cols", v).apply(); }
    // --- Player gestures (new in v1.26) ---
    public static boolean getGestureControls(Context c)       { return p(c).getBoolean("gestures", true); }
    public static void    setGestureControls(Context c, boolean v){ p(c).edit().putBoolean("gestures", v).apply(); }
    public static int     getDoubleTapSeek(Context c)         { return p(c).getInt("dt_seek", 10); }
    public static void    setDoubleTapSeek(Context c, int v)  { p(c).edit().putInt("dt_seek", v).apply(); }
    public static boolean getShowSkipIntro(Context c)         { return p(c).getBoolean("show_skip_intro", true); }
    public static void    setShowSkipIntro(Context c, boolean v){ p(c).edit().putBoolean("show_skip_intro", v).apply(); }
    public static boolean getBackgroundPlayback(Context c)    { return p(c).getBoolean("bg_playback", false); }
    public static void    setBackgroundPlayback(Context c, boolean v){ p(c).edit().putBoolean("bg_playback", v).apply(); }
    // --- Embed player: load the provider page directly instead of inside the wrapper iframe ---
    public static boolean getEmbedDirect(Context c)           { return p(c).getBoolean("embed_direct", false); }
    public static void    setEmbedDirect(Context c, boolean v){ p(c).edit().putBoolean("embed_direct", v).apply(); }
    // --- Offline player ---
    public static boolean getAutoPlayNext(Context c)          { return p(c).getBoolean("auto_next", true); }
    public static void    setAutoPlayNext(Context c, boolean v){ p(c).edit().putBoolean("auto_next", v).apply(); }
    public static String  getResizeMode(Context c)            { return p(c).getString("resize_mode", "fit"); }
    public static void    setResizeMode(Context c, String v)  { p(c).edit().putString("resize_mode", v).apply(); }
    public static boolean getSwipeSeek(Context c)             { return p(c).getBoolean("swipe_seek", true); }
    public static void    setSwipeSeek(Context c, boolean v)  { p(c).edit().putBoolean("swipe_seek", v).apply(); }
    public static float   getHoldSpeed(Context c)             { return p(c).getFloat("hold_speed", 2.0f); }
    public static void    setHoldSpeed(Context c, float v)    { p(c).edit().putFloat("hold_speed", v).apply(); }
    public static int     getControlsTimeout(Context c)       { return p(c).getInt("controls_timeout", 4); }
    public static void    setControlsTimeout(Context c, int v){ p(c).edit().putInt("controls_timeout", v).apply(); }
    /** Where offline videos open: "builtin", "external" (one chosen app) or "ask" (system list). */
    public static String  getOfflinePlayer(Context c) {
        String v = p(c).getString("offline_player", "builtin");
        if ("fountainplay".equals(v)) return "external";   // older builds
        if ("chooser".equals(v)) return "ask";
        return v;
    }
    /** Package of the external video app chosen in Settings (empty = none chosen yet). */
    public static String  getExternalPlayerPkg(Context c) {
        String v = p(c).getString("external_player_pkg", "");
        if (v.isEmpty() && "fountainplay".equals(p(c).getString("offline_player", ""))) return "com.fountainpdl.fountainplay";
        return v;
    }
    public static void    setExternalPlayerPkg(Context c, String v){ p(c).edit().putString("external_player_pkg", v).apply(); }
    public static void    setOfflinePlayer(Context c, String v){ p(c).edit().putString("offline_player", v).apply(); }
    public static boolean getAutoPip(Context c)                { return p(c).getBoolean("auto_pip", false); }
    public static void    setAutoPip(Context c, boolean v)     { p(c).edit().putBoolean("auto_pip", v).apply(); }
    public static boolean getResumePlayback(Context c)        { return p(c).getBoolean("resume_playback", true); }
    public static void    setResumePlayback(Context c, boolean v){ p(c).edit().putBoolean("resume_playback", v).apply(); }
    // --- Home / general ---
    public static int     getStartTab(Context c)              { return p(c).getInt("start_tab", 3); }
    public static void    setStartTab(Context c, int v)       { p(c).edit().putInt("start_tab", v).apply(); }
    public static boolean getShowTop10(Context c)             { return p(c).getBoolean("show_top10", true); }
    public static void    setShowTop10(Context c, boolean v)  { p(c).edit().putBoolean("show_top10", v).apply(); }
    public static boolean getShowMyList(Context c)            { return p(c).getBoolean("show_mylist", true); }
    public static void    setShowMyList(Context c, boolean v) { p(c).edit().putBoolean("show_mylist", v).apply(); }
    // --- Status ---
    public static String  getItemStatus(Context c, int id, String type) { return p(c).getString("status_" + type + "_" + id, STATUS_NONE); }
    public static void    setItemStatus(Context c, int id, String type, String status) { p(c).edit().putString("status_" + type + "_" + id, status).apply(); }
    public static void    clearAll(Context c) { p(c).edit().clear().apply(); }
}
