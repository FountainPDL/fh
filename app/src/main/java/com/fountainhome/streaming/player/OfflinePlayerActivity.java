package com.fountainhome.streaming.player;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.ui.TrackSelectionDialogBuilder;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.exoplayer.ExoPlayer;

import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.ActivityOfflinePlayerBinding;
import com.fountainhome.streaming.download.AppDatabase;
import com.fountainhome.streaming.download.DownloadDao;
import com.fountainhome.streaming.download.DownloadItem;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.download.Remuxer;
import com.fountainhome.streaming.download.ShareHelper;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.WatchProgress;
import com.fountainhome.streaming.ui.BaseActivity;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.io.File;
import java.util.Locale;

/**
 * Plays a downloaded video from the Offline folder with a full set of controls:
 * seek bar, +/- skip, brightness (left swipe), volume (right swipe), swipe to scrub, double-tap to skip,
 * hold for fast speed, speed picker, aspect ratio, rotation, screen lock, previous/next episode, share.
 */
public class OfflinePlayerActivity extends BaseActivity {
    public static final String EXTRA_DOWNLOAD_ID = "download_id";
    /** Stream mode (anime): the player finds a direct stream itself and plays it with the same controls. */
    public static final String EXTRA_STREAM_ANIME_ID = "stream_anime_id";
    public static final String EXTRA_STREAM_EPISODE = "stream_episode";
    public static final String EXTRA_STREAM_TOTAL = "stream_total";
    public static final String EXTRA_STREAM_TITLE = "stream_title";
    public static final String EXTRA_STREAM_ROMAJI = "stream_romaji";
    public static final String EXTRA_STREAM_POSTER = "stream_poster";
    public static final String EXTRA_STREAM_BACKDROP = "stream_backdrop";
    public static final String EXTRA_STREAM_RATING = "stream_rating";

    private static final int MODE_NONE = 0, MODE_BRIGHTNESS = 1, MODE_VOLUME = 2, MODE_SEEK = 3, MODE_IGNORE = 4;
    private static final float[] SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};
    private static final String[] RESIZE = {"fit", "fill", "zoom"};

    private ActivityOfflinePlayerBinding b;
    private ExoPlayer player;
    private DownloadItem item, prevItem, nextItem;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private AudioManager audio;

    private boolean controlsVisible = true, locked = false, scrubbing = false, ended = false;
    private boolean remuxing = false;
    private float speed = 1f;
    private int resizeIdx = 0;
    private int seekStepMs = 10000;
    private long lastSaved = 0;

    // gesture state
    private GestureDetector detector;
    private int mode = MODE_NONE;
    private float startBrightness, startVolume;
    private long startPos, pendingSeek = -1;
    private boolean holdActive = false;
    private float speedBeforeHold = 1f;
    private int tapAccum = 0;

    private Runnable sleepRunnable;

    // stream mode
    private boolean streamMode = false;
    private int streamTotal = 0;
    private com.fountainhome.streaming.anime.source.AnimeLookup.Ref streamRef;
    private com.fountainhome.streaming.anime.source.AnimeSource.Stream streamData;
    private int streamDataEp = -1, streamToken = 0;
    private final ActivityResultLauncher<String[]> subtitlePicker =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) attachSubtitle(uri); });

    // ------------------------------------------------------------------ lifecycle

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        Remuxer.configure(this);
        b = ActivityOfflinePlayerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (AppPreferences.getKeepScreenOn(this)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        seekStepMs = Math.max(5, AppPreferences.getDoubleTapSeek(this)) * 1000;
        speed = AppPreferences.getPlaybackSpeed(this);
        resizeIdx = Math.max(0, java.util.Arrays.asList(RESIZE).indexOf(AppPreferences.getResizeMode(this)));
        b.rewLabel.setText(String.valueOf(seekStepMs / 1000));
        b.fwdLabel.setText(String.valueOf(seekStepMs / 1000));
        applyResize();
        updateSpeedLabel();

        streamMode = getIntent().hasExtra(EXTRA_STREAM_ANIME_ID);
        wireControls();
        if (streamMode) b.shareBtn.setVisibility(View.GONE);
        wireGestures();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (locked) { showUnlock(); return; }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (streamMode) {
            Intent in = getIntent();
            DownloadItem si = new DownloadItem();
            si.type = "anime";
            si.tmdbId = in.getIntExtra(EXTRA_STREAM_ANIME_ID, 0);
            si.season = 1;
            si.episode = Math.max(1, in.getIntExtra(EXTRA_STREAM_EPISODE, 1));
            si.title = in.getStringExtra(EXTRA_STREAM_TITLE) != null ? in.getStringExtra(EXTRA_STREAM_TITLE) : "Anime";
            si.posterPath = in.getStringExtra(EXTRA_STREAM_POSTER);
            si.backdropPath = in.getStringExtra(EXTRA_STREAM_BACKDROP);
            si.rating = in.getDoubleExtra(EXTRA_STREAM_RATING, 0);
            streamTotal = in.getIntExtra(EXTRA_STREAM_TOTAL, 0);
            streamRef = com.fountainhome.streaming.anime.source.AnimeLookup.recall(this, si.tmdbId, si.title);
            if (streamRef.romaji == null && in.getStringExtra(EXTRA_STREAM_ROMAJI) != null)
                streamRef.romaji = in.getStringExtra(EXTRA_STREAM_ROMAJI);
            item = si;
            return;
        }
        int id = getIntent().getIntExtra(EXTRA_DOWNLOAD_ID, -1);
        DownloadItem d = AppDatabase.get(this).downloadDao().byId(id);
        if (!isPlayable(d)) {
            Toast.makeText(this, "This download is missing. Delete it and download again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        item = d;
    }

    @Override protected void onStart() {
        super.onStart();
        hideSystemBars();
        if (item != null && player == null && !remuxing) {
            if (streamMode) {
                if (streamData != null && streamDataEp == item.episode) play(); else openStream(item.episode);
            } else openItem(item);
        }
    }

    @Override protected void onStop() {
        super.onStop();
        // The floating window was closed with its X: end playback.
        boolean pipClosed = android.os.Build.VERSION.SDK_INT >= 26 && isInPictureInPictureMode();
        releasePlayer();
        if (pipClosed) finishAndRemoveTask();
    }

    @Override protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(lp);
        super.onDestroy();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        hideSystemBars();
    }

    // ------------------------------------------------------------------ opening a file

    private static boolean isPlayable(DownloadItem d) {
        return d != null && d.localPath != null && new File(d.localPath).isFile();
    }

    /** Load a download. Old raw .ts files are converted to .mp4 first so that seeking works. */
    private void openItem(DownloadItem d) {
        item = d;
        b.titleText.setText(label(d));
        findNeighbours();
        File f = new File(d.localPath);
        if (Remuxer.needsRemux(f)) {
            remuxing = true;
            b.preparePanel.setVisibility(View.VISIBLE);
            b.prepareBar.setIndeterminate(false);
            b.prepareBar.setProgress(0);
            b.prepareText.setText("Preparing this video for smooth seeking (first time only). Tap to play now.");
            final java.util.concurrent.atomic.AtomicBoolean cancel = new java.util.concurrent.atomic.AtomicBoolean(false);
            b.preparePanel.setOnClickListener(v -> cancel.set(true));
            // If the conversion makes no progress at all, stop waiting and just play the file as it is.
            final int[] seen = {0};
            ui.postDelayed(new Runnable() {
                @Override public void run() {
                    if (!remuxing || cancel.get() || isDestroyed()) return;
                    if (b.prepareBar.getProgress() <= seen[0]) { Remuxer.markFailed(f); cancel.set(true); }
                    else { seen[0] = b.prepareBar.getProgress(); ui.postDelayed(this, 25000); }
                }
            }, 25000);
            final DownloadItem target = d;
            new Thread(() -> {
                File out = Remuxer.makeSeekable(f, pct -> ui.post(() -> { if (b != null) b.prepareBar.setProgress(pct); }), cancel);
                if (!out.equals(f)) {
                    target.localPath = out.getAbsolutePath();
                    target.totalBytes = out.length();
                    try { AppDatabase.get(getApplicationContext()).downloadDao().update(target); } catch (Throwable ignored) {}
                }
                ui.post(() -> {
                    remuxing = false;
                    if (isDestroyed() || isFinishing()) return;
                    b.preparePanel.setVisibility(View.GONE);
                    if (item == target) play();
                });
            }, "fh-remux").start();
        } else {
            play();
        }
    }

    // ------------------------------------------------------------------ stream mode (anime)

    private void openStream(final int ep) {
        item.episode = ep;
        streamData = null;
        final int token = ++streamToken;
        releasePlayer();
        b.titleText.setText(label(item));
        b.prevEpBtn.setVisibility(ep > 1 ? View.VISIBLE : View.GONE);
        b.nextEpBtn.setVisibility(streamTotal <= 0 || ep < streamTotal ? View.VISIBLE : View.GONE);
        b.preparePanel.setVisibility(View.VISIBLE);
        b.prepareBar.setIndeterminate(true);
        b.prepareText.setText("Finding a stream for episode " + ep + "\u2026");
        final boolean dub = "dub".equals(AppPreferences.getAnimeDubSub(this));
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            com.fountainhome.streaming.anime.source.AnimeLookup.Result r = null;
            String err = null;
            try { r = com.fountainhome.streaming.anime.source.AnimeLookup.stream(app, streamRef, ep, dub); }
            catch (Throwable t) { err = t.getMessage() != null ? t.getMessage() : "No stream found"; }
            final com.fountainhome.streaming.anime.source.AnimeLookup.Result res = r;
            final String fail = err;
            ui.post(() -> {
                if (isDestroyed() || isFinishing() || token != streamToken) return;
                b.preparePanel.setVisibility(View.GONE);
                if (res == null) { streamFailed(fail); return; }
                streamData = res.stream;
                streamDataEp = ep;
                play();
            });
        }, "fh-stream").start();
    }

    private void streamFailed(String why) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this).setTitle("Couldn't get a stream")
            .setMessage((why != null ? why + "\n\n" : "") + "You can try again, or pick another site in the anime's page.")
            .setPositiveButton("Try again", (x, w) -> openStream(item.episode))
            .setNegativeButton("Close", (x, w) -> finish())
            .show();
    }

    private androidx.media3.datasource.DataSource.Factory streamFactory() {
        androidx.media3.datasource.DefaultHttpDataSource.Factory http = new androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent(com.fountainhome.streaming.anime.source.AnimeNet.UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000).setReadTimeoutMs(20000);
        java.util.Map<String, String> h = new java.util.HashMap<>();
        if (streamData != null) for (java.util.Map.Entry<String, String> e : streamData.headers.entrySet()) {
            if ("user-agent".equalsIgnoreCase(e.getKey())) http.setUserAgent(e.getValue()); else h.put(e.getKey(), e.getValue());
        }
        http.setDefaultRequestProperties(h);
        return new androidx.media3.datasource.DefaultDataSource.Factory(this, http);
    }

    /** A stand-in file used only to name subtitle files that belong to a streamed episode. */
    private File streamStub() {
        return new File(getCacheDir(), "stream_" + item.tmdbId + "_e" + item.episode + ".vid");
    }

    private Uri baseUri() {
        return streamMode && streamData != null ? Uri.parse(streamData.url) : Uri.fromFile(new File(item.localPath));
    }

    private void findNeighbours() {
        prevItem = nextItem = null;
        if (!"movie".equals(item.type)) {
            DownloadDao dao = AppDatabase.get(this).downloadDao();
            DownloadItem p = dao.find(item.tmdbId, item.type, item.season, item.episode - 1);
            DownloadItem n = dao.find(item.tmdbId, item.type, item.season, item.episode + 1);
            if (isDone(p)) prevItem = p;
            if (isDone(n)) nextItem = n;
        }
        b.prevEpBtn.setVisibility(prevItem != null ? View.VISIBLE : View.GONE);
        b.nextEpBtn.setVisibility(nextItem != null ? View.VISIBLE : View.GONE);
    }

    private static boolean isDone(DownloadItem d) {
        return d != null && DownloadItem.COMPLETED.equals(d.status) && isPlayable(d);
    }

    private void play() {
        ended = false;
        if (player == null) {
            // Broad format support: any container/codec the phone can decode, with fallback decoders,
            // and TS quirks (streams without clean keyframes) tolerated.
            androidx.media3.extractor.DefaultExtractorsFactory extractors = new androidx.media3.extractor.DefaultExtractorsFactory()
                .setTsExtractorFlags(androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
                    | androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
                .setConstantBitrateSeekingEnabled(true);
            androidx.media3.exoplayer.DefaultRenderersFactory renderers = new androidx.media3.exoplayer.DefaultRenderersFactory(this)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory msf = streamMode
                ? new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(streamFactory(), extractors)
                : new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this, extractors);
            player = new ExoPlayer.Builder(this, renderers, msf)
                .setSeekBackIncrementMs(seekStepMs)
                .setSeekForwardIncrementMs(seekStepMs)
                .setHandleAudioBecomingNoisy(true)
                .build();
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
            b.playerView.setPlayer(player);
            applySubtitleStyle();
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    b.buffering.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                    ui.removeCallbacks(stallCheck);
                    if (state == Player.STATE_BUFFERING && !streamMode) ui.postDelayed(stallCheck, 12000);
                    if (state == Player.STATE_READY) {
                        long dur = player.getDuration();
                        if (dur > 0 && dur < Integer.MAX_VALUE) b.seek.setMax((int) dur);
                        b.durText.setText(fmt(dur));
                    }
                    if (state == Player.STATE_ENDED) onEnded();
                    refreshPlayIcon();
                }
                @Override public void onIsPlayingChanged(boolean playing) {
                    refreshPlayIcon();
                    if (playing) { scheduleTick(); scheduleHide(); } else ui.removeCallbacks(hideRunnable);
                }
                @Override public void onPlayerError(PlaybackException e) { onError(e); }
            });
        }
        player.setPlaybackParameters(new androidx.media3.common.PlaybackParameters(speed));
        MediaItem.Builder mi = new MediaItem.Builder().setUri(baseUri());
        if (streamMode && streamData != null && streamData.url.toLowerCase().contains(".m3u8")) mi.setMimeType(MimeTypes.APPLICATION_M3U8);
        File sub = com.fountainhome.streaming.download.SubtitleFetcher.find(streamMode ? streamStub() : new File(item.localPath));
        java.util.List<MediaItem.SubtitleConfiguration> subs = new java.util.ArrayList<>();
        if (sub != null) subs.add(subtitleConfig(Uri.fromFile(sub), sub.getName(), true));
        if (streamMode && streamData != null) {
            String lang = AppPreferences.getSubLang(this);
            int k = 0;
            for (com.fountainhome.streaming.download.SubtitleFetcher.Track t : streamData.subs) {
                if (k >= 8) break;
                String low = t.url.toLowerCase(Locale.ROOT);
                String mime = low.contains(".vtt") ? MimeTypes.TEXT_VTT : low.contains(".ass") ? MimeTypes.TEXT_SSA : MimeTypes.APPLICATION_SUBRIP;
                boolean want = sub == null && com.fountainhome.streaming.download.SubtitleFetcher.matchesLang(t.label + " " + t.url, lang);
                subs.add(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(t.url)).setMimeType(mime)
                    .setLabel(t.label.isEmpty() ? "Subtitle " + (k + 1) : t.label)
                    .setLanguage(want ? lang : null)
                    .setSelectionFlags(want ? C.SELECTION_FLAG_DEFAULT : 0).build());
                k++;
            }
        }
        if (!subs.isEmpty()) mi.setSubtitleConfigurations(subs);
        player.setMediaItem(mi.build());
        player.prepare();
        long resume = AppPreferences.getResumePlayback(this)
            ? WatchProgress.get(this, item.tmdbId, item.type, item.season, item.episode) : 0;
        if (resume > 5000) player.seekTo(resume);
        player.setPlayWhenReady(true);
        showControls(true);
    }

    /** A local file that sits on "loading" is not going to start by itself: offer a way out. */
    private final Runnable stallCheck = new Runnable() {
        @Override public void run() {
            if (isFinishing() || isDestroyed() || player == null || player.getPlaybackState() != Player.STATE_BUFFERING) return;
            new AlertDialog.Builder(OfflinePlayerActivity.this)
                .setTitle("This video won't start")
                .setMessage("The file seems damaged or in a format this player can't handle. Open it in another app, or delete it and download it again.")
                .setPositiveButton("Open in another app", (d, w) -> playElsewhere())
                .setNeutralButton("Keep waiting", null)
                .setNegativeButton("Close", (d, w) -> finish())
                .show();
        }
    };

    private void releasePlayer() {
        ui.removeCallbacks(stallCheck);
        if (player == null) return;
        saveProgress();
        ui.removeCallbacks(tick);
        b.playerView.setPlayer(null);
        player.release();
        player = null;
    }

    private void onError(PlaybackException e) {
        if (isFinishing() || isDestroyed()) return;
        if (streamMode) { streamData = null; streamFailed("The stream stopped working (" + e.getErrorCodeName() + ")."); return; }
        new AlertDialog.Builder(this)
            .setTitle("Can't play this video")
            .setMessage("The file could not be played (" + e.getErrorCodeName() + "). "
                + "It may be incomplete. You can try again, or delete it and download it again.")
            .setPositiveButton("Try again", (d, w) -> { if (player != null) { player.prepare(); player.play(); } })
            .setNeutralButton("Open in another app", (d, w) -> playElsewhere())
            .setNegativeButton("Close", (d, w) -> finish())
            .show();
    }

    private void onEnded() {
        ended = true;
        saveProgressAs(0);
        if (streamMode) {
            if (AppPreferences.getAutoPlayNext(this) && (streamTotal <= 0 || item.episode < streamTotal)) openStream(item.episode + 1);
            else showControls(false);
            return;
        }
        if (nextItem != null && AppPreferences.getAutoPlayNext(this)) {
            DownloadItem n = nextItem;
            releasePlayer();
            openItem(n);
        } else {
            showControls(false);
        }
    }

    // ------------------------------------------------------------------ progress

    private void saveProgress() {
        if (player == null || item == null || ended) return;
        long pos = player.getCurrentPosition(), dur = player.getDuration();
        if (dur > 0 && pos > dur - 15000) pos = 0;   // finished: start again next time
        saveProgressAs(pos);
    }

    private void saveProgressAs(long pos) {
        if (item == null) return;
        ContentItem ci = new ContentItem();
        ci.id = item.tmdbId;
        ci.mediaType = item.type;
        ci.title = "movie".equals(item.type) ? item.title : null;
        ci.name = "movie".equals(item.type) ? null : item.title;
        ci.posterPath = item.posterPath;
        ci.backdropPath = item.backdropPath;
        ci.rating = item.rating;
        ci.imdbId = item.imdbId;
        if (player != null && player.getDuration() > 0) ci.durationMs = player.getDuration();
        LibraryManager.updateProgress(this, ci, item.season, item.episode, pos);
    }

    // ------------------------------------------------------------------ controls

    private void wireControls() {
        b.backBtn.setOnClickListener(v -> finish());
        b.shareBtn.setOnClickListener(v -> ShareHelper.shareVideo(this, item));
        b.moreBtn.setOnClickListener(v -> { showControls(false); showMoreMenu(); });
        b.playBtn.setOnClickListener(v -> togglePlay());
        b.rewBtn.setOnClickListener(v -> seekBy(-seekStepMs));
        b.fwdBtn.setOnClickListener(v -> seekBy(seekStepMs));
        b.prevEpBtn.setOnClickListener(v -> { if (streamMode) { if (item.episode > 1) openStream(item.episode - 1); } else if (prevItem != null) { DownloadItem p = prevItem; releasePlayer(); openItem(p); } });
        b.nextEpBtn.setOnClickListener(v -> { if (streamMode) { if (streamTotal <= 0 || item.episode < streamTotal) openStream(item.episode + 1); } else if (nextItem != null) { DownloadItem n = nextItem; releasePlayer(); openItem(n); } });
        b.speedBtn.setOnClickListener(v -> showSpeedMenu());
        b.aspectBtn.setOnClickListener(v -> { resizeIdx = (resizeIdx + 1) % RESIZE.length; applyResize();
            AppPreferences.setResizeMode(this, RESIZE[resizeIdx]); scheduleHide(); });
        b.rotateBtn.setOnClickListener(v -> {
            boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            setRequestedOrientation(land ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                         : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        });
        b.lockBtn.setOnClickListener(v -> lock());
        b.unlockBtn.setOnClickListener(v -> unlock());

        b.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) b.posText.setText(fmt(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { scrubbing = true; ui.removeCallbacks(hideRunnable); }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                scrubbing = false;
                if (player != null) { player.seekTo(sb.getProgress()); ended = false; }
                scheduleHide();
            }
        });
    }

    // ------------------------------------------------------------------ more menu

    private void showMoreMenu() {
        if (player == null) return;
        final boolean loop = player.getRepeatMode() == Player.REPEAT_MODE_ONE;
        final java.util.List<String> names = new java.util.ArrayList<>();
        final java.util.List<Runnable> acts = new java.util.ArrayList<>();
        names.add("Subtitles"); acts.add(this::showSubtitleMenu);
        names.add("Audio track"); acts.add(() -> showTrackDialog(C.TRACK_TYPE_AUDIO, "Audio track"));
        if (streamMode) { names.add("Video quality"); acts.add(() -> showTrackDialog(C.TRACK_TYPE_VIDEO, "Video quality")); }
        names.add("Sleep timer"); acts.add(this::showSleepMenu);
        names.add(loop ? "Loop: on (tap to turn off)" : "Loop this video");
        acts.add(() -> {
            player.setRepeatMode(loop ? Player.REPEAT_MODE_OFF : Player.REPEAT_MODE_ONE);
            Toast.makeText(this, loop ? "Loop off" : "Loop on", Toast.LENGTH_SHORT).show();
        });
        names.add("Picture-in-picture"); acts.add(this::enterPip);
        if (!streamMode) {
            names.add("Play in another app"); acts.add(this::playElsewhere);
            names.add("File info"); acts.add(this::showFileInfo);
        }
        new AlertDialog.Builder(this).setTitle("Options").setItems(names.toArray(new String[0]), (d, i) -> {
            if (player != null) acts.get(i).run();
        }).show();
    }

    private void showSubtitleMenu() {
        final String[] opts = {"Choose subtitle track", "Load subtitle file (.srt .vtt .ass)", "Turn subtitles off"};
        new AlertDialog.Builder(this).setTitle("Subtitles").setItems(opts, (d, i) -> {
            if (player == null) return;
            if (i == 0) showTrackDialog(C.TRACK_TYPE_TEXT, "Subtitles");
            else if (i == 1) subtitlePicker.launch(new String[]{"*/*"});
            else {
                player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build());
                Toast.makeText(this, "Subtitles off", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    private void showTrackDialog(int type, String title) {
        boolean has = false;
        for (Tracks.Group g : player.getCurrentTracks().getGroups()) if (g.getType() == type) { has = true; break; }
        if (!has) { Toast.makeText(this, "No " + title.toLowerCase(Locale.ROOT) + " in this video", Toast.LENGTH_SHORT).show(); return; }
        if (type == C.TRACK_TYPE_TEXT)
            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build());
        new TrackSelectionDialogBuilder(this, title, player, type).setAllowAdaptiveSelections(false).build().show();
    }

    /** Detects the subtitle format from the file's own text, not from its (often missing) extension. */
    private static String sniffMime(File f, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            byte[] head = new byte[2048];
            int n = in.read(head);
            String t = n > 0 ? new String(head, 0, n, java.nio.charset.StandardCharsets.UTF_8) : "";
            if (t.contains("WEBVTT")) return MimeTypes.TEXT_VTT;
            if (t.contains("[Script Info]") || t.contains("[V4+ Styles]") || t.contains("[V4 Styles]")) return MimeTypes.TEXT_SSA;
            if (t.contains("<tt")) return MimeTypes.APPLICATION_TTML;
        } catch (Exception ignored) { }
        if (lower.endsWith(".vtt")) return MimeTypes.TEXT_VTT;
        if (lower.endsWith(".ass") || lower.endsWith(".ssa")) return MimeTypes.TEXT_SSA;
        return MimeTypes.APPLICATION_SUBRIP;
    }

    private static String langName(String code) {
        for (String[] l : com.fountainhome.streaming.download.SubtitleFetcher.LANGS) if (l[0].equals(code)) return l[1];
        return code;
    }

    /** Size, colour, background, edge and position from Settings > Subtitles. */
    private void applySubtitleStyle() {
        androidx.media3.ui.SubtitleView sv = b.playerView.getSubtitleView();
        if (sv == null) return;
        int bg = AppPreferences.getSubBg(this);
        int edge = AppPreferences.getSubEdge(this);
        int edgeType = edge == 1 ? androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE
            : edge == 2 ? androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
            : androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE;
        sv.setStyle(new androidx.media3.ui.CaptionStyleCompat(
            AppPreferences.getSubColor(this),
            bg <= 0 ? android.graphics.Color.TRANSPARENT : android.graphics.Color.argb(Math.round(bg * 2.55f), 0, 0, 0),
            android.graphics.Color.TRANSPARENT, edgeType, android.graphics.Color.BLACK, null));
        sv.setApplyEmbeddedStyles(false);
        sv.setApplyEmbeddedFontSizes(false);
        sv.setFractionalTextSize(androidx.media3.ui.SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * AppPreferences.getSubSize(this) / 100f);
        sv.setBottomPaddingFraction(AppPreferences.getSubBottom(this) / 100f);
    }

    private MediaItem.SubtitleConfiguration subtitleConfig(Uri uri, String name, boolean makeDefault) {
        String mime = sniffMime(new File(uri.getPath()), name);
        return new MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mime).setLanguage(AppPreferences.getSubLang(this)).setLabel(langName(AppPreferences.getSubLang(this)))
            .setSelectionFlags(makeDefault ? C.SELECTION_FLAG_DEFAULT : 0).build();
    }

    /** Copies the chosen file next to the video (so it is remembered), then plays it with the video. */
    private void attachSubtitle(Uri picked) {
        if (player == null || item == null) return;
        File video = streamMode ? streamStub() : new File(item.localPath);
        String stem = video.getName().contains(".") ? video.getName().substring(0, video.getName().lastIndexOf('.')) : video.getName();
        File tmp = new File(video.getParentFile(), stem + ".user.tmp");
        try (java.io.InputStream in = getContentResolver().openInputStream(picked);
             java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) {
            if (in == null) throw new java.io.IOException("unreadable");
            byte[] buf = new byte[16 * 1024]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            Toast.makeText(this, "Couldn't read that subtitle file", Toast.LENGTH_SHORT).show();
            return;
        }
        String mime = sniffMime(tmp, "");
        String ext = MimeTypes.TEXT_VTT.equals(mime) ? ".vtt" : MimeTypes.TEXT_SSA.equals(mime) ? ".ass" : ".srt";
        if (MimeTypes.APPLICATION_TTML.equals(mime)) ext = ".srt";   // keep the sidecar names predictable
        File dest = new File(video.getParentFile(), stem + ".user" + ext);
        for (String e : new String[]{".user.srt", ".user.vtt", ".user.ass"}) //noinspection ResultOfMethodCallIgnored
            new File(video.getParentFile(), stem + e).delete();
        if (!tmp.renameTo(dest)) { Toast.makeText(this, "Couldn't save the subtitle", Toast.LENGTH_SHORT).show(); return; }

        long pos = player.getCurrentPosition();
        boolean wasPlaying = player.getPlayWhenReady();
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).setPreferredTextLanguage(AppPreferences.getSubLang(this)).build());
        MediaItem next = new MediaItem.Builder().setUri(baseUri())
            .setSubtitleConfigurations(java.util.Collections.singletonList(subtitleConfig(Uri.fromFile(dest), dest.getName(), true))).build();
        player.setMediaItem(next, pos);
        player.prepare();
        player.setPlayWhenReady(wasPlaying);
        Toast.makeText(this, "Subtitles loaded", Toast.LENGTH_SHORT).show();
    }

    private void showSleepMenu() {
        final String[] opts = {"5 min", "10 min", "15 min", "30 min", "1 hour", "Cancel timer"};
        final long[] ms = {5 * 60000L, 10 * 60000L, 15 * 60000L, 30 * 60000L, 60 * 60000L, 0};
        new AlertDialog.Builder(this).setTitle("Sleep timer").setItems(opts, (d, i) -> {
            if (sleepRunnable != null) ui.removeCallbacks(sleepRunnable);
            sleepRunnable = null;
            if (ms[i] > 0) {
                sleepRunnable = () -> { if (player != null) player.pause(); sleepRunnable = null; };
                ui.postDelayed(sleepRunnable, ms[i]);
                Toast.makeText(this, "Pausing in " + opts[i], Toast.LENGTH_SHORT).show();
            } else Toast.makeText(this, "Timer cancelled", Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void showFileInfo() {
        VideoSize vs = player.getVideoSize();
        File f = new File(item.localPath);
        String msg = "Name: " + label(item)
            + "\nDuration: " + fmt(player.getDuration())
            + "\nResolution: " + (vs.width > 0 ? vs.width + " \u00d7 " + vs.height : "unknown")
            + "\nSize: " + (f.length() / 1024 / 1024) + " MB"
            + "\nPath: " + item.localPath;
        new AlertDialog.Builder(this).setTitle("File info").setMessage(msg).setPositiveButton("OK", null).show();
    }

    /** Hand the current video to the external player chosen in Settings (or the system list if none). */
    private void playElsewhere() {
        long pos = player != null ? player.getCurrentPosition() : 0;
        boolean ok = com.fountainhome.streaming.download.ExternalPlayers.openPreferred(this, item);
        if (!ok) Toast.makeText(this, "No other video player found", Toast.LENGTH_SHORT).show();
        else { releasePlayerSaving(pos); finish(); }
    }

    private void releasePlayerSaving(long pos) { saveProgressAs(pos); releasePlayer(); }

    // ------------------------------------------------------------------ picture in picture

    private void enterPip() {
        if (android.os.Build.VERSION.SDK_INT < 26 || player == null) return;
        if (!getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            Toast.makeText(this, "Picture-in-picture isn't supported on this phone", Toast.LENGTH_SHORT).show();
            return;
        }
        int w = 16, h = 9;
        VideoSize vs = player.getVideoSize();
        if (vs.width > 0 && vs.height > 0) { w = vs.width; h = vs.height; }
        float r = (float) w / h;
        if (r > 2.39f) { w = 239; h = 100; } else if (r < 1f / 2.39f) { w = 100; h = 239; }
        try {
            enterPictureInPictureMode(new android.app.PictureInPictureParams.Builder()
                .setAspectRatio(new android.util.Rational(w, h)).build());
        } catch (IllegalStateException ignored) { }
    }

    @Override public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (player != null && player.isPlaying() && AppPreferences.getAutoPip(this)) enterPip();
    }

    @Override public void onPictureInPictureModeChanged(boolean inPip, Configuration cfg) {
        super.onPictureInPictureModeChanged(inPip, cfg);
        if (inPip) { b.controls.setVisibility(View.GONE); b.unlockBtn.setVisibility(View.GONE); }
        else { b.controls.setVisibility(View.VISIBLE); showControls(true); }
    }

    private void togglePlay() {
        if (player == null) return;
        if (ended || player.getPlaybackState() == Player.STATE_ENDED) {
            ended = false;
            player.seekTo(0);
            player.play();
        } else if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        scheduleHide();
    }

    private void seekBy(long delta) {
        if (player == null) return;
        long dur = player.getDuration();
        long target = player.getCurrentPosition() + delta;
        if (dur > 0) target = Math.min(target, Math.max(0, dur - 500));
        player.seekTo(Math.max(0, target));
        ended = false;
        updateTimeUi();
        scheduleHide();
    }

    private void showSpeedMenu() {
        PopupMenu m = new PopupMenu(this, b.speedBtn);
        for (int i = 0; i < SPEEDS.length; i++) {
            String t = (SPEEDS[i] == 1f ? "Normal" : trimSpeed(SPEEDS[i]) + "x");
            m.getMenu().add(0, i, i, t).setCheckable(true).setChecked(SPEEDS[i] == speed);
        }
        m.setOnMenuItemClickListener(it -> {
            speed = SPEEDS[it.getItemId()];
            if (player != null) player.setPlaybackSpeed(speed);
            AppPreferences.setPlaybackSpeed(this, speed);
            updateSpeedLabel();
            return true;
        });
        ui.removeCallbacks(hideRunnable);
        m.setOnDismissListener(x -> scheduleHide());
        m.show();
    }

    private void updateSpeedLabel() { b.speedBtn.setText(trimSpeed(speed) + "x"); }

    private static String trimSpeed(float s) {
        return s == (int) s ? String.valueOf((int) s) : String.valueOf(s);
    }

    private void applyResize() {
        int mode = resizeIdx == 1 ? AspectRatioFrameLayout.RESIZE_MODE_FILL
                 : resizeIdx == 2 ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                 : AspectRatioFrameLayout.RESIZE_MODE_FIT;
        b.playerView.setResizeMode(mode);
        b.aspectBtn.setText(resizeIdx == 1 ? "Stretch" : resizeIdx == 2 ? "Crop" : "Fit");
    }

    @DrawableRes private int playIcon() {
        if (ended || (player != null && player.getPlaybackState() == Player.STATE_ENDED)) return R.drawable.ic_replay;
        return player != null && player.getPlayWhenReady() ? R.drawable.ic_pause : R.drawable.ic_play;
    }

    private void refreshPlayIcon() { b.playBtn.setImageResource(playIcon()); }

    // ------------------------------------------------------------------ show / hide controls

    private final Runnable hideRunnable = () -> { if (player != null && player.isPlaying() && !scrubbing) hideControls(); };

    private void scheduleHide() {
        ui.removeCallbacks(hideRunnable);
        if (controlsVisible && !locked) ui.postDelayed(hideRunnable, AppPreferences.getControlsTimeout(this) * 1000L);
    }

    private void showControls(boolean autoHide) {
        if (locked) return;
        controlsVisible = true;
        b.controls.setVisibility(View.VISIBLE);
        b.controls.animate().alpha(1f).setDuration(150).start();
        refreshPlayIcon();
        updateTimeUi();
        scheduleTick();
        if (autoHide) scheduleHide(); else ui.removeCallbacks(hideRunnable);
    }

    private void hideControls() {
        controlsVisible = false;
        b.controls.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            if (!controlsVisible && b != null) b.controls.setVisibility(View.GONE);
        }).start();
    }

    private void lock() {
        locked = true;
        hideControls();
        showUnlock();
    }

    private void unlock() {
        locked = false;
        ui.removeCallbacks(hideUnlock);
        b.unlockBtn.setVisibility(View.GONE);
        showControls(true);
    }

    private final Runnable hideUnlock = () -> { if (b != null) b.unlockBtn.setVisibility(View.GONE); };

    private void showUnlock() {
        b.unlockBtn.setVisibility(View.VISIBLE);
        ui.removeCallbacks(hideUnlock);
        ui.postDelayed(hideUnlock, 2500);
    }

    // ------------------------------------------------------------------ seek bar ticker

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (player == null) return;
            if (controlsVisible && !scrubbing) updateTimeUi();
            long now = System.currentTimeMillis();
            if (player.isPlaying() && now - lastSaved > 5000) { lastSaved = now; saveProgress(); }
            if (player.isPlaying() || controlsVisible) ui.postDelayed(this, 500);
        }
    };

    private void scheduleTick() { ui.removeCallbacks(tick); ui.post(tick); }

    private void updateTimeUi() {
        if (player == null) return;
        long pos = player.getCurrentPosition(), dur = player.getDuration();
        if (dur > 0 && dur < Integer.MAX_VALUE && b.seek.getMax() != (int) dur) b.seek.setMax((int) dur);
        if (!scrubbing) b.seek.setProgress((int) Math.min(pos, Integer.MAX_VALUE));
        b.seek.setSecondaryProgress((int) Math.min(player.getBufferedPosition(), Integer.MAX_VALUE));
        b.posText.setText(fmt(pos));
        b.durText.setText(fmt(dur));
    }

    // ------------------------------------------------------------------ gestures

    private void wireGestures() {
        final float density = getResources().getDisplayMetrics().density;
        final int edge = (int) (28 * density);
        detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) {
                mode = MODE_NONE;
                pendingSeek = -1;
                startPos = player != null ? player.getCurrentPosition() : 0;
                startBrightness = currentBrightness();
                startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                return true;
            }

            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                if (controlsVisible) hideControls(); else showControls(true);
                return true;
            }

            @Override public boolean onDoubleTap(MotionEvent e) {
                if (player == null) return true;
                float w = b.root.getWidth();
                if (e.getX() < w / 3f) tapSkip(false);
                else if (e.getX() > w * 2f / 3f) tapSkip(true);
                else togglePlay();
                return true;
            }

            @Override public void onLongPress(MotionEvent e) {
                if (player == null || mode != MODE_NONE || !AppPreferences.getGestureControls(OfflinePlayerActivity.this)) return;
                holdActive = true;
                speedBeforeHold = speed;
                float hs = AppPreferences.getHoldSpeed(OfflinePlayerActivity.this);
                player.setPlaybackSpeed(hs);
                showHud(R.drawable.ic_speed, trimSpeed(hs) + "x speed", 0);
            }

            @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (e1 == null || player == null || holdActive) return false;
                if (!AppPreferences.getGestureControls(OfflinePlayerActivity.this)) return false;
                float tx = e2.getX() - e1.getX(), ty = e2.getY() - e1.getY();
                int w = b.root.getWidth(), h = b.root.getHeight();
                if (mode == MODE_NONE) {
                    if (Math.abs(tx) < ViewConfiguration.get(OfflinePlayerActivity.this).getScaledTouchSlop()
                        && Math.abs(ty) < ViewConfiguration.get(OfflinePlayerActivity.this).getScaledTouchSlop()) return false;
                    if (e1.getX() < edge || e1.getX() > w - edge) { mode = MODE_IGNORE; return false; }
                    if (Math.abs(ty) > Math.abs(tx)) mode = e1.getX() < w / 2f ? MODE_BRIGHTNESS : MODE_VOLUME;
                    else mode = AppPreferences.getSwipeSeek(OfflinePlayerActivity.this) ? MODE_SEEK : MODE_IGNORE;
                }
                float frac = -ty / (h * 0.8f);   // swipe up = increase
                switch (mode) {
                    case MODE_BRIGHTNESS: setBrightness(startBrightness + frac); break;
                    case MODE_VOLUME: setVolume(startVolume + frac * audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)); break;
                    case MODE_SEEK: {
                        long dur = player.getDuration();
                        long target = startPos + (long) (tx / (float) w * 120000f);
                        target = Math.max(0, dur > 0 ? Math.min(target, dur) : target);
                        pendingSeek = target;
                        long diff = target - startPos;
                        showHud(diff >= 0 ? R.drawable.ic_skip_next : R.drawable.ic_skip_prev,
                            (diff >= 0 ? "+" : "-") + fmt(Math.abs(diff)) + "  (" + fmt(target) + ")", 0);
                        break;
                    }
                    default: break;
                }
                return true;
            }
        });
        b.root.setOnTouchListener((v, e) -> {
            if (locked) {
                if (e.getActionMasked() == MotionEvent.ACTION_UP) showUnlock();
                return true;
            }
            detector.onTouchEvent(e);
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) endGesture();
            return true;
        });
    }

    private void endGesture() {
        if (mode == MODE_SEEK && pendingSeek >= 0 && player != null) { player.seekTo(pendingSeek); ended = false; }
        if (holdActive && player != null) { player.setPlaybackSpeed(speedBeforeHold); holdActive = false; }
        pendingSeek = -1;
        mode = MODE_NONE;
        hideHudLater(600);
    }

    private void tapSkip(boolean forward) {
        tapAccum = (forwardWas == forward && tapRecent) ? tapAccum + 1 : 1;
        forwardWas = forward;
        tapRecent = true;
        ui.removeCallbacks(clearTap);
        ui.postDelayed(clearTap, 800);
        seekBy(forward ? seekStepMs : -seekStepMs);
        String txt = (forward ? "+" : "-") + (tapAccum * seekStepMs / 1000) + "s";
        android.widget.TextView t = forward ? b.tapRight : b.tapLeft;
        (forward ? b.tapLeft : b.tapRight).setVisibility(View.GONE);
        t.setText(txt);
        t.setVisibility(View.VISIBLE);
    }

    private boolean forwardWas = false, tapRecent = false;
    private final Runnable clearTap = () -> {
        tapRecent = false;
        tapAccum = 0;
        if (b != null) { b.tapLeft.setVisibility(View.GONE); b.tapRight.setVisibility(View.GONE); }
    };

    private float currentBrightness() {
        float v = getWindow().getAttributes().screenBrightness;
        if (v >= 0) return v;
        try { return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / 255f; }
        catch (Exception e) { return 0.5f; }
    }

    private void setBrightness(float v) {
        v = Math.max(0.02f, Math.min(1f, v));
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = v;
        getWindow().setAttributes(lp);
        showHud(R.drawable.ic_brightness, "Brightness  " + Math.round(v * 100) + "%", 0);
    }

    private void setVolume(float v) {
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int vol = Math.round(Math.max(0, Math.min(max, v)));
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
        showHud(R.drawable.ic_volume, "Volume  " + Math.round(vol * 100f / max) + "%", 0);
    }

    private final Runnable hideHud = () -> { if (b != null) b.hud.setVisibility(View.GONE); };

    private void showHud(@DrawableRes int icon, String text, long autoHideMs) {
        b.hudIcon.setImageResource(icon);
        b.hudText.setText(text);
        b.hud.setVisibility(View.VISIBLE);
        ui.removeCallbacks(hideHud);
        if (autoHideMs > 0) ui.postDelayed(hideHud, autoHideMs);
    }

    private void hideHudLater(long ms) { ui.removeCallbacks(hideHud); ui.postDelayed(hideHud, ms); }

    // ------------------------------------------------------------------ helpers

    private static String label(DownloadItem d) {
        return "movie".equals(d.type) ? d.title : d.title + "  ·  S" + d.season + " E" + d.episode;
    }

    private static String fmt(long ms) {
        if (ms < 0 || ms == C.TIME_UNSET) return "0:00";
        long s = ms / 1000, h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, sec) : String.format(Locale.US, "%d:%02d", m, sec);
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
