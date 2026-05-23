package com.fountainhome.streaming.ui.player;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.fountainhome.streaming.databinding.ActivityPlayerBinding;
import com.fountainhome.streaming.service.AdBlockWebViewClient;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.service.StreamExtractor;
import com.fountainhome.streaming.service.WatchProgress;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private ExoPlayer exoPlayer;
    private List<SourceGenerator.Source> sources = new ArrayList<>();
    private String imdbId = "", type;
    private int tmdbId, season = 1, episode = 1, totalEpisodes = 0;
    private boolean isFullscreen = false, usingExoPlayer = false;
    private ContentItem currentItem;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        type          = getIntent().getStringExtra("type");
        tmdbId        = getIntent().getIntExtra("id", 0);
        imdbId        = getIntent().getStringExtra("imdbId");
        String title  = getIntent().getStringExtra("title");
        season        = getIntent().getIntExtra("season", 1);
        episode       = getIntent().getIntExtra("episode", 1);
        totalEpisodes = getIntent().getIntExtra("total_episodes", 0);
        if (imdbId == null) imdbId = "";

        // Restore saved item for progress tracking
        currentItem = new ContentItem();
        currentItem.id = tmdbId;
        currentItem.mediaType = type;
        currentItem.title = title;
        currentItem.imdbId = imdbId;

        binding.titleText.setText(title != null ? title : "");
        updateEpLabel();

        // WebView setup
        WebSettings ws = binding.playerWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36");

        binding.playerWebView.setWebViewClient(new AdBlockWebViewClient());
        binding.playerWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                binding.loadingBar.setProgress(progress);
                binding.loadingBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onShowCustomView(View v, CustomViewCallback cb) {
                enterFullscreen();
                binding.fullscreenContainer.addView(v);
                binding.fullscreenContainer.setVisibility(View.VISIBLE);
            }
            @Override
            public void onHideCustomView() {
                binding.fullscreenContainer.removeAllViews();
                binding.fullscreenContainer.setVisibility(View.GONE);
                exitFullscreen();
            }
        });

        // Fullscreen toggle
        binding.fullscreenBtn.setOnClickListener(v -> {
            if (isFullscreen) exitFullscreen(); else enterFullscreen();
        });

        // Share button
        binding.shareBtn.setOnClickListener(v -> shareContent());

        // PiP button
        binding.pipBtn.setOnClickListener(v -> enterPiP());

        // TV controls
        if ("tv".equals(type)) {
            binding.tvControls.setVisibility(View.VISIBLE);
            binding.prevBtn.setOnClickListener(v -> {
                saveProgress();
                if (episode > 1) episode--;
                else if (season > 1) { season--; episode = 1; }
                updateEpLabel(); buildSources();
            });
            binding.nextBtn.setOnClickListener(v -> {
                saveProgress();
                episode++;
                updateEpLabel(); buildSources();
            });
        } else {
            binding.tvControls.setVisibility(View.GONE);
        }

        binding.backBtn.setOnClickListener(v -> { saveProgress(); finish(); });

        buildSources();
        startProgressSaving();
    }

    private void startProgressSaving() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                saveProgress();
                progressHandler.postDelayed(this, 10000); // save every 10 seconds
            }
        };
        progressHandler.postDelayed(progressRunnable, 10000);
    }

    private void saveProgress() {
        try {
            long pos = 0;
            if (usingExoPlayer && exoPlayer != null) {
                pos = exoPlayer.getCurrentPosition();
            }
            LibraryManager.updateProgress(this, currentItem, season, episode, pos);
        } catch (Exception e) { /* ignore */ }
    }

    private void buildSources() {
        sources = "movie".equals(type)
            ? SourceGenerator.getMovieSources(imdbId, tmdbId)
            : SourceGenerator.getTVSources(imdbId, tmdbId, season, episode);

        // Preferred source first
        String pref = AppPreferences.getSource(this);
        List<SourceGenerator.Source> reordered = new ArrayList<>();
        for (SourceGenerator.Source s : sources) if (s.label.startsWith(pref)) reordered.add(0, s);
        for (SourceGenerator.Source s : sources) if (!s.label.startsWith(pref)) reordered.add(s);
        sources = reordered;

        List<String> labels = new ArrayList<>();
        for (SourceGenerator.Source s : sources) labels.add(s.label);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sourceSpinner.setAdapter(adapter);

        binding.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean init = true;
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (init) { init = false; tryPlay(pos); return; }
                tryPlay(pos);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });

        // Seek to saved position
        long savedPos = WatchProgress.get(this, tmdbId, type, season, episode);
        if (!sources.isEmpty()) tryPlay(0);
        if (savedPos > 5000) {
            binding.playerWebView.postDelayed(() -> {
                if (usingExoPlayer && exoPlayer != null) exoPlayer.seekTo(savedPos);
            }, 3000);
        }
    }

    private void tryPlay(int index) {
        if (index < 0 || index >= sources.size()) return;
        String embedUrl = sources.get(index).url;
        showLoading(true);
        releaseExoPlayer();
        binding.playerWebView.setVisibility(View.GONE);
        binding.playerView.setVisibility(View.GONE);

        new StreamExtractor().extract(this, embedUrl, 12000, new StreamExtractor.Callback() {
            @Override public void onFound(String url, Map<String, String> headers) {
                runOnUiThread(() -> playExo(url, headers));
            }
            @Override public void onFailed() {
                runOnUiThread(() -> playWebView(embedUrl));
            }
        });
    }

    private void playExo(String url, Map<String, String> headers) {
        showLoading(false);
        usingExoPlayer = true;
        binding.playerView.setVisibility(View.VISIBLE);
        binding.playerWebView.setVisibility(View.GONE);

        DefaultHttpDataSource.Factory dsf = new DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 Chrome/120.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true);

        exoPlayer = new ExoPlayer.Builder(this).build();
        binding.playerView.setPlayer(exoPlayer);

        MediaItem mi = MediaItem.fromUri(url);
        if (url.contains(".m3u8") || url.contains("/hls/")) {
            exoPlayer.setMediaSource(new HlsMediaSource.Factory(dsf).createMediaSource(mi));
        } else {
            exoPlayer.setMediaSource(new ProgressiveMediaSource.Factory(dsf).createMediaSource(mi));
        }

        // Auto-play next episode
        if ("tv".equals(type) && AppPreferences.getAutoplay(this)) {
            exoPlayer.addListener(new androidx.media3.common.Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == androidx.media3.common.Player.STATE_ENDED) {
                        saveProgress();
                        episode++;
                        updateEpLabel();
                        buildSources();
                    }
                }
            });
        }

        exoPlayer.prepare();
        exoPlayer.setPlaybackSpeed(AppPreferences.getPlaybackSpeed(this));
        exoPlayer.play();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void playWebView(String embedUrl) {
        showLoading(false);
        usingExoPlayer = false;
        releaseExoPlayer();
        binding.playerWebView.setVisibility(View.VISIBLE);
        binding.playerView.setVisibility(View.GONE);

        String html = "<!DOCTYPE html><html><head>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>*{margin:0;padding:0;background:#000}"
            + "iframe,video{width:100vw;height:100vh;border:none}</style></head>"
            + "<body><iframe src='" + embedUrl + "' "
            + "allowfullscreen allow='autoplay;fullscreen;encrypted-media;picture-in-picture'>"
            + "</iframe></body></html>";
        binding.playerWebView.loadDataWithBaseURL(
            "https://www.google.com", html, "text/html", "utf-8", null);
    }

    private void enterFullscreen() {
        isFullscreen = true;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        binding.controlsBar.setVisibility(View.GONE);
    }

    private void exitFullscreen() {
        isFullscreen = false;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        binding.controlsBar.setVisibility(View.VISIBLE);
    }

    private void enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppPreferences.getPiP(this)) {
            try {
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                Toast.makeText(this, "PiP not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareContent() {
        String text = currentItem.displayTitle() + " — watch on Fountain Home";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "Share via"));
    }

    private void showLoading(boolean show) {
        binding.loadingBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.extractingText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateEpLabel() {
        if ("tv".equals(type) || "anime".equals(type)) {
            binding.episodeLabel.setVisibility(View.VISIBLE);
            binding.episodeLabel.setText("S" + season + "·E" + episode);
        } else {
            binding.episodeLabel.setVisibility(View.GONE);
        }
    }

    private void releaseExoPlayer() {
        if (exoPlayer != null) { exoPlayer.release(); exoPlayer = null; }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (AppPreferences.getPiP(this)) enterPiP();
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) { exitFullscreen(); return; }
        if (!usingExoPlayer && binding.playerWebView.canGoBack())
            binding.playerWebView.goBack();
        else { saveProgress(); super.onBackPressed(); }
    }

    @Override protected void onPause() {
        super.onPause();
        saveProgress();
        if (exoPlayer != null) exoPlayer.pause();
        else binding.playerWebView.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (exoPlayer != null) exoPlayer.play();
        else binding.playerWebView.onResume();
    }
    @Override protected void onDestroy() {
        saveProgress();
        progressHandler.removeCallbacksAndMessages(null);
        releaseExoPlayer();
        binding.playerWebView.destroy();
        super.onDestroy();
    }
}
