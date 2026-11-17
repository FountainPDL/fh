package com.fountainhome.streaming.player;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fountainhome.streaming.anime.AniListClient;
import com.fountainhome.streaming.databinding.ActivityEmbedPlayerBinding;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.service.WatchProgress;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Streaming player screen. It shows the chosen provider's own player (see {@link EmbedWebPlayer})
 * and adds server switching, episode navigation, resume/continue-watching and offline download.
 *
 * Lifecycle contract (this is what fixes "audio keeps playing after I close it"):
 *  - finish() / onDestroy() destroy the WebView immediately.
 *  - onStop() unloads the page unless the user turned on "Background playback".
 */
public class EmbedPlayerActivity extends AppCompatActivity implements EmbedWebPlayer.Listener {

    private static final int MENU_RELOAD = 1, MENU_NEXT_SERVER = 2, MENU_DIRECT = 3,
        MENU_PIP = 4, MENU_DOWNLOAD = 5, MENU_SHARE = 6;

    private ActivityEmbedPlayerBinding b;
    private EmbedWebPlayer player;
    private final Handler h = new Handler(Looper.getMainLooper());

    private List<SourceGenerator.Source> sources = new ArrayList<>();
    private int currentIdx = 0;
    private String type, titleStr, imdbId = "";
    private int tmdbId, season = 1, episode = 1, totalEpisodes = 0, totalSeasons = 0;
    private ContentItem cur;

    private long lastPosMs = 0, lastSavedPosMs = 0;
    private boolean suspended = false, frameLoaded = false, webFullscreen = false;
    private long buildToken = 0;   // invalidates stale async work (anime id lookup, episode change)
    private int crashReloads = 0;

    private final Runnable slowHintRunnable = () -> { if (b != null && !frameLoaded) b.slowHint.setVisibility(View.VISIBLE); };
    private final Runnable hideFloatRunnable = () -> { if (b != null) b.floatBar.setVisibility(View.GONE); };

    // ------------------------------------------------------------------ lifecycle

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityEmbedPlayerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (AppPreferences.getKeepScreenOn(this)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent in = getIntent();
        type = in.getStringExtra("type");
        if (type == null) type = "movie";
        tmdbId = in.getIntExtra("id", 0);
        titleStr = in.getStringExtra("title");
        if (titleStr == null) titleStr = "";
        String ii = in.getStringExtra("imdbId");
        imdbId = ii != null ? ii : "";
        season = Math.max(1, in.getIntExtra("season", 1));
        episode = Math.max(1, in.getIntExtra("episode", 1));
        totalEpisodes = in.getIntExtra("total_episodes", 0);
        totalSeasons = in.getIntExtra("total_seasons", 0);

        cur = new ContentItem();
        cur.id = tmdbId;
        cur.mediaType = type;
        cur.title = titleStr;
        cur.imdbId = imdbId;
        cur.posterPath = in.getStringExtra("posterPath");
        cur.backdropPath = in.getStringExtra("backdropPath");
        cur.rating = in.getDoubleExtra("rating", 0);
        cur.numberOfSeasons = totalSeasons;

        player = new EmbedWebPlayer(this, b.webContainer, b.fullscreenHost, this);

        b.backBtn.setOnClickListener(v -> onBackPressed());
        b.floatBack.setOnClickListener(v -> onBackPressed());
        b.serverBtn.setOnClickListener(v -> showServerMenu(b.serverBtn));
        b.floatServer.setOnClickListener(v -> { showServerMenu(b.floatServer); showFloatBar(); });
        b.moreBtn.setOnClickListener(v -> showMoreMenu(b.moreBtn));
        b.floatMore.setOnClickListener(v -> { showMoreMenu(b.floatMore); showFloatBar(); });
        b.revealZone.setOnClickListener(v -> showFloatBar());
        b.slowHintBtn.setOnClickListener(v -> tryNextServer());
        b.prevBtn.setOnClickListener(v -> changeEpisode(-1));
        b.nextBtn.setOnClickListener(v -> changeEpisode(1));
        b.panelDownloadBtn.setOnClickListener(v -> startDownload());

        resetEpisodeState();
        updateTexts();
        applyLayout();
        loadSourcesAndPlay();
    }

    @Override protected void onStart() {
        super.onStart();
        if (suspended) {
            suspended = false;
            player.prepareResume();
            startPlayback();
        }
    }

    @Override protected void onStop() {
        super.onStop();
        save();
        if (isFinishing()) return;
        if (inPip()) {          // the PiP window was dismissed: close the player completely
            finish();
            return;
        }
        if (!AppPreferences.getBackgroundPlayback(this)) {
            // Unload the page so its video/audio cannot keep running while we are in the background.
            player.unload();
            suspended = true;
            frameLoaded = false;
        }
    }

    @Override public void finish() {
        if (player != null) player.release();   // destroys the WebView right now, not "later"
        super.finish();
    }

    @Override protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        save();
        if (player != null) player.release();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (player != null && player.isFullscreen()) { player.exitFullscreen(); return; }
        save();
        super.onBackPressed();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applyLayout();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyLayout();
    }

    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (canPip() && frameLoaded && !isFinishing()) enterPip();
    }

    @Override public void onPictureInPictureModeChanged(boolean isInPip) {
        super.onPictureInPictureModeChanged(isInPip);
        applyLayout();
    }

    // ------------------------------------------------------------------ sources + playback

    private void loadSourcesAndPlay() {
        final long token = ++buildToken;
        frameLoaded = false;
        showLoading("Finding servers…");
        if ("anime".equals(type)) {
            AniListClient.getMalId(tmdbId, malId -> {
                if (token != buildToken || isFinishing() || isDestroyed()) return;
                sources = ordered(SourceGenerator.getAnimeSources(tmdbId, malId, season, episode));
                afterSources();
            });
        } else {
            List<SourceGenerator.Source> list = "movie".equals(type)
                ? SourceGenerator.getMovieSources(imdbId, tmdbId)
                : SourceGenerator.getTVSources(imdbId, tmdbId, season, episode);
            sources = ordered(list);
            afterSources();
        }
    }

    /**
     * Preferred server first, the rest in their normal order. For anime the dub/sub choice is
     * applied first (labels carry "Sub" / "Dub"), then the preferred anime server.
     */
    private List<SourceGenerator.Source> ordered(List<SourceGenerator.Source> in) {
        final boolean anime = "anime".equals(type);
        final String pref = anime ? AppPreferences.getAnimeSource(this) : AppPreferences.getSource(this);
        final String want = "dub".equals(AppPreferences.getAnimeDubSub(this)) ? "Dub" : "Sub";
        final String other = "Dub".equals(want) ? "Sub" : "Dub";
        List<SourceGenerator.Source> out = new ArrayList<>(in);
        // Stable insertion sort by score (lower = earlier); lists are tiny.
        for (int i = 1; i < out.size(); i++) {
            SourceGenerator.Source x = out.get(i);
            int sx = score(x, anime, pref, want, other);
            int j = i - 1;
            while (j >= 0 && score(out.get(j), anime, pref, want, other) > sx) {
                out.set(j + 1, out.get(j));
                j--;
            }
            out.set(j + 1, x);
        }
        return out;
    }

    private static int score(SourceGenerator.Source s, boolean anime, String pref, String want, String other) {
        int lang = 0;
        if (anime) {
            if (s.label.endsWith(want)) lang = 0;
            else if (s.label.endsWith(other)) lang = 2;
            else lang = 1;
        }
        int server = s.label.startsWith(pref) ? 0 : 1;
        return lang * 2 + server;
    }

    private void afterSources() {
        currentIdx = 0;
        buildChips();
        save();   // puts the title in "Continue watching" straight away
        if (sources.isEmpty()) {
            showLoading("No servers available for this title.");
            b.loadBar.setVisibility(View.GONE);
            return;
        }
        startPlayback();
    }

    private void startPlayback() {
        if (sources.isEmpty() || isFinishing()) return;
        if (currentIdx < 0 || currentIdx >= sources.size()) currentIdx = 0;
        SourceGenerator.Source src = sources.get(currentIdx);
        frameLoaded = false;
        showLoading("Loading " + src.label + "…");
        h.removeCallbacks(slowHintRunnable);
        b.slowHint.setVisibility(View.GONE);
        h.postDelayed(slowHintRunnable, 12000);
        int startSec = lastPosMs > 5000 ? (int) (lastPosMs / 1000) : 0;
        player.load(SourceGenerator.urlFor(src, startSec), AppPreferences.getEmbedDirect(this));
        highlightChips();
    }

    private void selectServer(int idx) {
        if (idx < 0 || idx >= sources.size()) return;
        currentIdx = idx;
        String base = sources.get(idx).label.replace(" Sub", "").replace(" Dub", "");
        if ("anime".equals(type)) AppPreferences.setAnimeSource(this, base);
        else AppPreferences.setSource(this, base);
        startPlayback();
    }

    private void tryNextServer() {
        if (sources.isEmpty()) return;
        selectServer((currentIdx + 1) % sources.size());
    }

    private void showLoading(String msg) {
        b.loadingOverlay.setVisibility(View.VISIBLE);
        b.loadBar.setVisibility(View.VISIBLE);
        b.loadText.setText(msg);
    }

    // ------------------------------------------------------------------ EmbedWebPlayer.Listener

    @Override public void onFrameLoaded() {
        if (b == null) return;
        frameLoaded = true;
        h.removeCallbacks(slowHintRunnable);
        b.slowHint.setVisibility(View.GONE);
        b.loadingOverlay.setVisibility(View.GONE);
        crashReloads = 0;
    }

    @Override public void onProgress(long positionMs, long durationMs) {
        lastPosMs = positionMs;
        if (cur != null) cur.durationMs = durationMs;
        if (Math.abs(positionMs - lastSavedPosMs) > 10000) {
            lastSavedPosMs = positionMs;
            save();
        }
    }

    @Override public void onEnded() {
        if (isSeries() && AppPreferences.getAutoplay(this)) changeEpisode(1);
    }

    @Override public void onFullscreenChanged(boolean fullscreen) {
        webFullscreen = fullscreen;
        setRequestedOrientation(fullscreen
            ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        applyLayout();
    }

    @Override public void onRenderGone() {
        if (isFinishing() || crashReloads++ >= 2) {
            showLoading("The player stopped unexpectedly. Pick a server to try again.");
            b.loadBar.setVisibility(View.GONE);
            return;
        }
        Toast.makeText(this, "Player restarted", Toast.LENGTH_SHORT).show();
        startPlayback();
    }

    // ------------------------------------------------------------------ episodes

    private boolean isSeries() { return "tv".equals(type) || "anime".equals(type); }

    private void resetEpisodeState() {
        lastPosMs = WatchProgress.get(this, tmdbId, type, season, episode);
        lastSavedPosMs = lastPosMs;
    }

    private void changeEpisode(int delta) {
        if (!isSeries()) return;
        save();
        int newSeason = season, newEp = episode + delta;
        if (delta > 0) {
            if (totalEpisodes > 0 && episode >= totalEpisodes) {
                if (totalSeasons > 0 && season < totalSeasons) { newSeason = season + 1; newEp = 1; totalEpisodes = 0; }
                else { Toast.makeText(this, "That was the last episode", Toast.LENGTH_SHORT).show(); return; }
            }
        } else if (newEp < 1) {
            if (season > 1) { newSeason = season - 1; newEp = 1; totalEpisodes = 0; }
            else return;
        }
        season = newSeason;
        episode = newEp;
        resetEpisodeState();
        updateTexts();
        loadSourcesAndPlay();
    }

    private void updateTexts() {
        b.titleText.setText(titleStr);
        if (isSeries()) {
            String label = "anime".equals(type) ? "Episode " + episode : "Season " + season + " · Episode " + episode;
            b.subtitleText.setText(label);
            b.epLabel.setText(label);
            b.epRow.setVisibility(View.VISIBLE);
        } else {
            b.subtitleText.setText("Movie");
            b.epRow.setVisibility(View.GONE);
        }
    }

    // ------------------------------------------------------------------ server chips + menus

    private void buildChips() {
        b.serverChips.removeAllViews();
        for (int i = 0; i < sources.size(); i++) {
            final int idx = i;
            TextView t = new TextView(this);
            t.setText(sources.get(i).label);
            t.setTextSize(13);
            t.setPadding(dp(16), dp(9), dp(16), dp(9));
            t.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            t.setLayoutParams(lp);
            t.setOnClickListener(v -> selectServer(idx));
            b.serverChips.addView(t);
        }
        highlightChips();
    }

    private void highlightChips() {
        int accent = AppPreferences.getAccentColor(this);
        for (int i = 0; i < b.serverChips.getChildCount(); i++) {
            TextView t = (TextView) b.serverChips.getChildAt(i);
            boolean sel = i == currentIdx;
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(20));
            g.setColor(sel ? accent : 0xFF1E1E1E);
            t.setBackground(g);
            t.setTextColor(sel ? Color.BLACK : Color.WHITE);
        }
    }

    private void showServerMenu(View anchor) {
        if (sources.isEmpty()) return;
        PopupMenu m = new PopupMenu(this, anchor);
        for (int i = 0; i < sources.size(); i++) {
            m.getMenu().add(0, i, i, (i == currentIdx ? "●  " : "     ") + sources.get(i).label);
        }
        m.setOnMenuItemClickListener(item -> { selectServer(item.getItemId()); return true; });
        m.show();
    }

    private void showMoreMenu(View anchor) {
        PopupMenu m = new PopupMenu(this, anchor);
        m.getMenu().add(0, MENU_RELOAD, 0, "Reload");
        m.getMenu().add(0, MENU_NEXT_SERVER, 1, "Try next server");
        m.getMenu().add(0, MENU_DIRECT, 2, AppPreferences.getEmbedDirect(this)
            ? "Compatibility mode: ON" : "Compatibility mode: off");
        if (canPip()) m.getMenu().add(0, MENU_PIP, 3, "Picture-in-picture");
        m.getMenu().add(0, MENU_DOWNLOAD, 4, "Download for offline");
        m.getMenu().add(0, MENU_SHARE, 5, "Share");
        m.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case MENU_RELOAD: startPlayback(); break;
                case MENU_NEXT_SERVER: tryNextServer(); break;
                case MENU_DIRECT:
                    boolean on = !AppPreferences.getEmbedDirect(this);
                    AppPreferences.setEmbedDirect(this, on);
                    Toast.makeText(this, on ? "Compatibility mode on: the server page loads directly"
                        : "Compatibility mode off", Toast.LENGTH_SHORT).show();
                    startPlayback();
                    break;
                case MENU_PIP: enterPip(); break;
                case MENU_DOWNLOAD: startDownload(); break;
                case MENU_SHARE:
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, titleStr + " — Fountain Home");
                    startActivity(Intent.createChooser(i, "Share"));
                    break;
                default: break;
            }
            return true;
        });
        m.show();
    }

    private void startDownload() {
        DownloadManager2.enqueue(this, cur, season, episode);
    }

    // ------------------------------------------------------------------ layout / immersive / PiP

    private boolean inPip() {
        return Build.VERSION.SDK_INT >= 24 && isInPictureInPictureMode();
    }

    private boolean canPip() {
        return Build.VERSION.SDK_INT >= 26 && AppPreferences.getPiP(this)
            && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    private void enterPip() {
        if (!canPip() || !frameLoaded) return;
        try {
            enterPictureInPictureMode(new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9)).build());
        } catch (Throwable ignored) {}
    }

    private void applyLayout() {
        if (b == null) return;
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean pip = inPip();
        boolean immersive = land || webFullscreen || pip;
        ViewGroup.LayoutParams lp = b.playerHolder.getLayoutParams();
        if (immersive) {
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            b.topBar.setVisibility(View.GONE);
            b.infoPanel.setVisibility(View.GONE);
            b.revealZone.setVisibility(pip ? View.GONE : View.VISIBLE);
            if (pip) b.floatBar.setVisibility(View.GONE);
        } else {
            int w = b.playerRoot.getWidth() > 0 ? b.playerRoot.getWidth() : getResources().getDisplayMetrics().widthPixels;
            lp.height = w * 9 / 16;
            b.topBar.setVisibility(View.VISIBLE);
            b.infoPanel.setVisibility(View.VISIBLE);
            b.revealZone.setVisibility(View.GONE);
            b.floatBar.setVisibility(View.GONE);
        }
        b.playerHolder.setLayoutParams(lp);
        setImmersive(immersive);
    }

    @SuppressWarnings("deprecation")
    private void setImmersive(boolean on) {
        View d = getWindow().getDecorView();
        if (on) {
            d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void showFloatBar() {
        if (b == null) return;
        b.floatBar.setVisibility(View.VISIBLE);
        h.removeCallbacks(hideFloatRunnable);
        h.postDelayed(hideFloatRunnable, 3500);
    }

    // ------------------------------------------------------------------ misc

    private void save() {
        try {
            if (cur != null) LibraryManager.updateProgress(this, cur, season, episode, lastPosMs);
        } catch (Exception ignored) {}
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
