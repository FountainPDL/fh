package com.fountainhome.streaming.ui.player;
import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.anime.AniListClient;
import com.fountainhome.streaming.databinding.ActivityPlayerBinding;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import java.util.*;

public class PlayerActivity extends AppCompatActivity {
    private ActivityPlayerBinding b;
    private ExoPlayer exo;
    private List<SourceGenerator.Source> sources = new ArrayList<>();
    private int currentSourceIdx = 0;
    private String imdbId = "", type, titleStr;
    private int tmdbId, season = 1, episode = 1;
    private boolean fs = false, ctrlVisible = true, locked = false, seekTracking = false;
    private ContentItem cur;
    private final Handler ph = new Handler(Looper.getMainLooper());
    private Runnable pr, hideCtrl, upNextCountdown;
    private AudioManager audioManager;
    private GestureDetector gestureDetector;
    private float dragStartY, dragStartX, startBrightness, startVolume;
    private boolean isDragging = false, isBrightnessZone = false;
    private boolean introHandledThisEp = false, upNextShown = false, upNextCancelled = false;
    private static final float[] SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};

    private final Runnable hideIndicatorRunnable = () -> { if (b != null) b.bvIndicator.setVisibility(View.GONE); };

    private final Runnable posUpdater = new Runnable() {
        @Override public void run() {
            if (b != null && exo != null) {
                long pos = exo.getCurrentPosition(), dur = exo.getDuration();
                if (dur > 0) {
                    b.seekBar.setMax(1000);
                    if (!seekTracking) b.seekBar.setProgress((int) (pos * 1000 / dur));
                    b.currentTimeText.setText(fmtTime(pos));
                    b.durationText.setText(fmtTime(dur));
                }
                checkSkipIntro(pos);
                checkUpNext(pos, dur);
            }
            ph.postDelayed(this, 500);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (AppPreferences.getKeepScreenOn(this)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        type = getIntent().getStringExtra("type");
        tmdbId = getIntent().getIntExtra("id", 0);
        imdbId = getIntent().getStringExtra("imdbId");
        titleStr = getIntent().getStringExtra("title");
        season = getIntent().getIntExtra("season", 1);
        episode = getIntent().getIntExtra("episode", 1);
        if (imdbId == null) imdbId = "";
        cur = new ContentItem();
        cur.id = tmdbId; cur.mediaType = type; cur.title = titleStr; cur.imdbId = imdbId;
        cur.posterPath = getIntent().getStringExtra("posterPath");
        cur.rating = getIntent().getDoubleExtra("rating", 0);
        b.titleText.setText(titleStr != null ? titleStr : "");
        updateLabel();

        b.playerView.setUseController(false);
        try {
            b.seekBar.getProgressDrawable().setTint(AppPreferences.getAccentColor(this));
            b.seekBar.getThumb().setTint(AppPreferences.getAccentColor(this));
        } catch (Exception ignored) {}
        b.speedBtn.setText(fmtSpeed(AppPreferences.getPlaybackSpeed(this)));

        hideCtrl = this::hideControls;
        setupGestures();

        b.backBtn.setOnClickListener(v -> { save(); finish(); });
        b.fullscreenBtn.setOnClickListener(v -> { if (fs) exitFs(); else enterFs(); resetHide(); });
        b.lockBtn.setOnClickListener(v -> setLocked(true));
        b.lockIndicator.setOnClickListener(v -> setLocked(false));
        b.moreBtn.setOnClickListener(v -> showMoreMenu());
        b.speedBtn.setOnClickListener(v -> showSpeedMenu());
        b.playPauseBtn.setOnClickListener(v -> { if (exo == null) return; if (exo.isPlaying()) exo.pause(); else exo.play(); updatePlayPauseIcon(); resetHide(); });
        b.skipBackBtn.setOnClickListener(v -> { if (exo != null) exo.seekTo(Math.max(0, exo.getCurrentPosition() - 10000)); resetHide(); });
        b.skipForwardBtn.setOnClickListener(v -> { if (exo != null) exo.seekTo(exo.getCurrentPosition() + 10000); resetHide(); });
        b.skipIntroPill.setOnClickListener(v -> { if (exo != null) exo.seekTo(90000); b.skipIntroPill.setVisibility(View.GONE); });
        b.retrySourcesBtn.setOnClickListener(v -> { b.noStreamCard.setVisibility(View.GONE); build(); });

        b.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser && exo != null && exo.getDuration() > 0) b.currentTimeText.setText(fmtTime((long) p * exo.getDuration() / 1000));
            }
            public void onStartTrackingTouch(SeekBar sb) { seekTracking = true; ph.removeCallbacks(hideCtrl); }
            public void onStopTrackingTouch(SeekBar sb) {
                if (exo != null && exo.getDuration() > 0) exo.seekTo((long) sb.getProgress() * exo.getDuration() / 1000);
                seekTracking = false; resetHide();
            }
        });

        if ("tv".equals(type) || "anime".equals(type)) {
            b.tvControls.setVisibility(View.VISIBLE);
            b.prevBtn.setOnClickListener(v -> {
                save();
                if (episode > 1) episode--; else if (season > 1) { season--; episode = 1; }
                updateLabel(); resetEpisodeState(); build(); resetHide();
            });
            b.nextBtn.setOnClickListener(v -> { save(); episode++; updateLabel(); resetEpisodeState(); build(); resetHide(); });
        } else b.tvControls.setVisibility(View.GONE);

        build(); startSaving(); enterFs();
        ph.post(posUpdater);
        showControls();
    }

    // ---------- Gestures: tap / double-tap seek / brightness & volume swipe ----------
    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapConfirmed(MotionEvent e) { toggleControls(); return true; }
            @Override public boolean onDoubleTap(MotionEvent e) {
                if (exo == null) return false;
                int seekMs = AppPreferences.getDoubleTapSeek(PlayerActivity.this) * 1000;
                boolean isLeft = e.getX() < b.touchInterceptor.getWidth() / 2f;
                long pos = exo.getCurrentPosition();
                exo.seekTo(isLeft ? Math.max(0, pos - seekMs) : pos + seekMs);
                showSeekFlash(isLeft);
                return true;
            }
        });
        b.touchInterceptor.setOnTouchListener((v, event) -> {
            if (locked) return true;
            gestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: onTouchDown(event); break;
                case MotionEvent.ACTION_MOVE: onTouchMove(event); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: onTouchUp(); break;
            }
            return true;
        });
    }
    private void onTouchDown(MotionEvent e) {
        dragStartY = e.getY(); dragStartX = e.getX(); isDragging = false;
        isBrightnessZone = e.getX() < b.touchInterceptor.getWidth() / 2f;
        float br = getWindow().getAttributes().screenBrightness;
        startBrightness = br < 0 ? 0.5f : br;
        startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }
    private void onTouchMove(MotionEvent e) {
        if (!AppPreferences.getGestureControls(this)) return;
        float dy = dragStartY - e.getY();
        float dx = Math.abs(e.getX() - dragStartX);
        if (Math.abs(dy) < 24 || dx > 120) return;
        isDragging = true;
        float screenH = Math.max(b.touchInterceptor.getHeight(), 1);
        float delta = dy / screenH;
        if (isBrightnessZone) {
            float newB = clamp(startBrightness + delta, 0.02f, 1f);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = newB;
            getWindow().setAttributes(lp);
            showIndicator(true, newB);
        } else {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int newVol = (int) clamp(startVolume + delta * maxVol, 0, maxVol);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
            showIndicator(false, maxVol > 0 ? newVol / (float) maxVol : 0);
        }
    }
    private void onTouchUp() {
        if (isDragging) { ph.removeCallbacks(hideIndicatorRunnable); ph.postDelayed(hideIndicatorRunnable, 600); }
        isDragging = false;
    }
    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private void showIndicator(boolean isBrightness, float level) {
        b.bvIndicator.setVisibility(View.VISIBLE);
        b.bvIcon.setImageResource(isBrightness ? R.drawable.ic_brightness : R.drawable.ic_volume);
        b.bvLevelBar.setProgress((int) (level * 100));
        b.bvText.setText(((int) (level * 100)) + "%");
    }
    private void showSeekFlash(boolean isLeft) {
        TextView flash = isLeft ? b.flashLeft : b.flashRight;
        flash.setText((isLeft ? "-" : "+") + AppPreferences.getDoubleTapSeek(this) + "s");
        flash.setAlpha(1f); flash.setVisibility(View.VISIBLE);
        flash.animate().alpha(0f).setDuration(450).setStartDelay(150)
            .withEndAction(() -> flash.setVisibility(View.GONE)).start();
    }

    // ---------- Lock ----------
    private void setLocked(boolean l) {
        locked = l;
        b.lockIndicator.setVisibility(l ? View.VISIBLE : View.GONE);
        if (l) {
            ph.removeCallbacks(hideCtrl);
            b.topControls.setVisibility(View.GONE);
            b.controlsBar.setVisibility(View.GONE);
            b.centerControls.setVisibility(View.GONE);
            b.skipIntroPill.setVisibility(View.GONE);
            b.upNextCard.setVisibility(View.GONE);
        } else {
            showControls();
        }
    }

    // ---------- Menus ----------
    private void showMoreMenu() {
        PopupMenu menu = new PopupMenu(this, b.moreBtn);
        menu.getMenu().add("Picture-in-Picture");
        menu.getMenu().add("Download");
        menu.getMenu().add("Share");
        menu.setOnMenuItemClickListener(item -> {
            String t = item.getTitle().toString();
            if ("Picture-in-Picture".equals(t)) pip();
            else if ("Download".equals(t)) {
                DownloadManager2.downloadVideo(this, cur, season, episode);
                Toast.makeText(this, "Download started \u2014 check the notification for progress", Toast.LENGTH_LONG).show();
            } else if ("Share".equals(t)) {
                Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, (titleStr != null ? titleStr : "") + " \u2014 Fountain Home");
                startActivity(Intent.createChooser(i, "Share"));
            }
            resetHide();
            return true;
        });
        menu.show();
    }
    private void showSpeedMenu() {
        PopupMenu menu = new PopupMenu(this, b.speedBtn);
        for (float sp : SPEEDS) menu.getMenu().add(fmtSpeed(sp));
        menu.setOnMenuItemClickListener(item -> {
            String label = item.getTitle().toString();
            float speed = Float.parseFloat(label.replace("x", ""));
            AppPreferences.setPlaybackSpeed(this, speed);
            if (exo != null) exo.setPlaybackSpeed(speed);
            b.speedBtn.setText(label);
            resetHide();
            return true;
        });
        menu.show();
    }
    private String fmtSpeed(float sp) {
        String num = (sp == Math.floor(sp)) ? String.valueOf((int) sp) : String.valueOf(sp);
        return num + "x";
    }
    private String fmtTime(long ms) {
        long totalSec = Math.max(0, ms) / 1000;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, sec = totalSec % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, sec) : String.format(Locale.US, "%d:%02d", m, sec);
    }
    private void updatePlayPauseIcon() {
        if (exo == null) return;
        b.playPauseBtn.setImageResource(exo.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    // ---------- Skip intro / Up next ----------
    private void checkSkipIntro(long pos) {
        if (!("tv".equals(type) || "anime".equals(type)) || locked) { b.skipIntroPill.setVisibility(View.GONE); return; }
        boolean show = AppPreferences.getShowSkipIntro(this);
        boolean auto = AppPreferences.getAutoSkipIntro(this);
        if (pos < 3000 || pos > 90000) { b.skipIntroPill.setVisibility(View.GONE); return; }
        if (auto && !introHandledThisEp) {
            introHandledThisEp = true;
            exo.seekTo(90000);
            b.skipIntroPill.setVisibility(View.GONE);
            return;
        }
        if (show) b.skipIntroPill.setVisibility(View.VISIBLE);
    }
    private void checkUpNext(long pos, long dur) {
        if (!("tv".equals(type) || "anime".equals(type)) || dur <= 0 || locked) return;
        if (!AppPreferences.getAutoplay(this)) return;
        if (dur - pos < 15000 && dur - pos > 500 && !upNextShown && !upNextCancelled) {
            upNextShown = true;
            showUpNext();
        }
    }
    private void showUpNext() {
        b.upNextCard.setVisibility(View.VISIBLE);
        b.upNextTitle.setText("Up Next \u2014 Episode " + (episode + 1));
        final int[] secs = {5};
        b.upNextCountdown.setText(secs[0] + "s");
        upNextCountdown = new Runnable() {
            @Override public void run() {
                if (upNextCancelled || b == null) return;
                secs[0]--;
                b.upNextCountdown.setText(Math.max(secs[0], 0) + "s");
                if (secs[0] <= 0) { advanceEpisode(); return; }
                ph.postDelayed(this, 1000);
            }
        };
        ph.postDelayed(upNextCountdown, 1000);
        b.upNextCancel.setOnClickListener(v -> {
            upNextCancelled = true;
            b.upNextCard.setVisibility(View.GONE);
            if (upNextCountdown != null) ph.removeCallbacks(upNextCountdown);
        });
        b.upNextPlayNow.setOnClickListener(v -> {
            if (upNextCountdown != null) ph.removeCallbacks(upNextCountdown);
            advanceEpisode();
        });
    }
    private void advanceEpisode() {
        if (b != null) b.upNextCard.setVisibility(View.GONE);
        save(); episode++; updateLabel(); resetEpisodeState(); build();
    }
    private void resetEpisodeState() {
        upNextShown = false; upNextCancelled = false; introHandledThisEp = false;
        if (upNextCountdown != null) { ph.removeCallbacks(upNextCountdown); upNextCountdown = null; }
    }

    // ---------- Controls visibility (cancel + state-guard so a stale fade-out
    //            callback can never re-hide controls after a later show) ----------
    private void toggleControls() { if (ctrlVisible) hideControls(); else showControls(); }
    private void showControls() {
        ctrlVisible = true;
        b.controlsBar.animate().cancel();
        b.topControls.animate().cancel();
        b.centerControls.animate().cancel();
        b.controlsBar.setVisibility(View.VISIBLE);
        b.topControls.setVisibility(View.VISIBLE);
        b.centerControls.setVisibility(View.VISIBLE);
        b.controlsBar.setAlpha(1f); b.topControls.setAlpha(1f); b.centerControls.setAlpha(1f);
        scheduleHide();
    }
    private void hideControls() {
        if (!ctrlVisible) return;
        ctrlVisible = false;
        b.controlsBar.animate().cancel();
        b.topControls.animate().cancel();
        b.centerControls.animate().cancel();
        b.controlsBar.animate().alpha(0f).setDuration(400)
            .withEndAction(() -> { if (!ctrlVisible) b.controlsBar.setVisibility(View.INVISIBLE); }).start();
        b.topControls.animate().alpha(0f).setDuration(400)
            .withEndAction(() -> { if (!ctrlVisible) b.topControls.setVisibility(View.INVISIBLE); }).start();
        b.centerControls.animate().alpha(0f).setDuration(400)
            .withEndAction(() -> { if (!ctrlVisible) b.centerControls.setVisibility(View.INVISIBLE); }).start();
    }
    private void scheduleHide() { ph.removeCallbacks(hideCtrl); ph.postDelayed(hideCtrl, 3500); }
    private void resetHide() { showControls(); }
    private void startSaving() {
        pr = new Runnable() { @Override public void run() { save(); ph.postDelayed(this, 10000); } };
        ph.postDelayed(pr, 10000);
    }
    private void save() {
        try { long pos = exo != null ? exo.getCurrentPosition() : 0; LibraryManager.updateProgress(this, cur, season, episode, pos); }
        catch (Exception ignored) {}
    }

    // ---------- Source resolution / playback — every source is just a URL resolver ----------
    private void build() {
        currentSourceIdx = 0;
        resetEpisodeState();
        b.noStreamCard.setVisibility(View.GONE);
        if ("anime".equals(type)) {
            showLoad(true);
            AniListClient.getMalId(tmdbId, malId -> {
                sources = SourceGenerator.getAnimeSources(tmdbId, malId, season, episode);
                finishBuild();
            });
        } else {
            sources = "movie".equals(type) ? SourceGenerator.getMovieSources(imdbId, tmdbId) : SourceGenerator.getTVSources(imdbId, tmdbId, season, episode);
            finishBuild();
        }
    }
    private void finishBuild() {
        String pref = AppPreferences.getSource(this);
        List<SourceGenerator.Source> ro = new ArrayList<>();
        for (SourceGenerator.Source src : sources) if (src.label.startsWith(pref)) ro.add(0, src);
        for (SourceGenerator.Source src : sources) if (!src.label.startsWith(pref)) ro.add(src);
        sources = ro;
        List<String> labels = new ArrayList<>();
        for (SourceGenerator.Source src : sources) labels.add(src.label);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.sourceSpinner.setAdapter(a);
        b.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean init = true;
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { if (init) { init = false; return; } currentSourceIdx = pos; play(pos); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        if (!sources.isEmpty()) play(0); else showNoStreamFound();
        long saved = WatchProgress.get(this, tmdbId, type, season, episode);
        if (saved > 5000) ph.postDelayed(() -> { if (exo != null) exo.seekTo(saved); }, 2000);
    }
    private void tryNextSource() {
        currentSourceIdx++;
        if (currentSourceIdx < sources.size()) { b.sourceSpinner.setSelection(currentSourceIdx); play(currentSourceIdx); }
        else showNoStreamFound();
    }
    private void play(int idx) {
        if (idx < 0 || idx >= sources.size()) { showNoStreamFound(); return; }
        String url = sources.get(idx).url;
        showLoad(true); freeExo();
        b.playerView.setVisibility(View.GONE);
        b.noStreamCard.setVisibility(View.GONE);
        // The source is used purely to resolve a playable URL. Whatever it hands back
        // plays only in our own ExoPlayer-based UI below — nothing from the source is
        // ever rendered directly.
        new StreamExtractor().extract(this, url, 8000, new StreamExtractor.Callback() {
            @Override public void onFound(String su, Map<String, String> h) { runOnUiThread(() -> playExo(su, h)); }
            @Override public void onFailed() { runOnUiThread(() -> tryNextSource()); }
        });
    }
    private void playExo(String url, Map<String, String> headers) {
        showLoad(false);
        b.playerView.setVisibility(View.VISIBLE);
        DefaultHttpDataSource.Factory dsf = new DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 Chrome/120.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(headers).setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000).setReadTimeoutMs(8000);
        exo = new ExoPlayer.Builder(this).build();
        b.playerView.setPlayer(exo);
        MediaItem mi = MediaItem.fromUri(url);
        if (url.contains(".m3u8") || url.contains("/hls/")) exo.setMediaSource(new HlsMediaSource.Factory(dsf).createMediaSource(mi));
        else exo.setMediaSource(new ProgressiveMediaSource.Factory(dsf).createMediaSource(mi));
        exo.addListener(new androidx.media3.common.Player.Listener() {
            @Override public void onPlayerError(PlaybackException e) { runOnUiThread(() -> tryNextSource()); }
            @Override public void onIsPlayingChanged(boolean isPlaying) { updatePlayPauseIcon(); }
            @Override public void onPlaybackStateChanged(int st) {
                if (st == androidx.media3.common.Player.STATE_ENDED && ("tv".equals(type) || "anime".equals(type))
                        && AppPreferences.getAutoplay(PlayerActivity.this) && !upNextShown) {
                    advanceEpisode();
                }
            }
        });
        exo.prepare();
        exo.setPlaybackSpeed(AppPreferences.getPlaybackSpeed(this));
        exo.play();
        updatePlayPauseIcon();
    }
    private void showNoStreamFound() {
        showLoad(false);
        b.noStreamCard.setVisibility(View.VISIBLE);
    }
    private void enterFs() {
        fs = true;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    private void exitFs() {
        fs = false;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
    private void pip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppPreferences.getPiP(this)) {
            try { enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16, 9)).build()); }
            catch (Exception ignored) {}
        }
    }
    @Override public void onPictureInPictureModeChanged(boolean inPip) {
        if (b == null) return;
        int vis = inPip ? View.GONE : View.VISIBLE;
        b.controlsBar.setVisibility(vis);
        b.topControls.setVisibility(vis);
        b.centerControls.setVisibility(vis);
        if (inPip && exo != null) exo.play();
    }
    @Override protected void onUserLeaveHint() { super.onUserLeaveHint(); if (AppPreferences.getPiP(this)) pip(); }
    private void showLoad(boolean show) {
        b.loadingBar.setVisibility(show ? View.VISIBLE : View.GONE);
        b.extractingText.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    private void updateLabel() {
        if ("tv".equals(type) || "anime".equals(type)) {
            b.episodeLabel.setVisibility(View.VISIBLE);
            b.episodeLabel.setText("S" + season + "\u00b7E" + episode);
        } else b.episodeLabel.setVisibility(View.GONE);
    }
    private void freeExo() { if (exo != null) { exo.release(); exo = null; } }
    @Override public void onBackPressed() { if (fs) { exitFs(); return; } save(); super.onBackPressed(); }
    @Override protected void onPause() {
        super.onPause(); save();
        if (exo != null && (!AppPreferences.getBackgroundPlayback(this) || isFinishing())) exo.pause();
    }
    @Override protected void onResume() { super.onResume(); if (exo != null) exo.play(); }
    @Override protected void onDestroy() { save(); ph.removeCallbacksAndMessages(null); freeExo(); super.onDestroy(); }
}
