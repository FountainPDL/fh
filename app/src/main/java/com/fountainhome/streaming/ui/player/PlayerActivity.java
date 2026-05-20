package com.fountainhome.streaming.ui.player;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.fountainhome.streaming.databinding.ActivityPlayerBinding;
import com.fountainhome.streaming.service.SourceGenerator;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private List<SourceGenerator.Source> sources = new ArrayList<>();
    private String imdbId = "";
    private int    tmdbId;
    private String type;
    private int    season  = 1;
    private int    episode = 1;
    private String startSource;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        type        = getIntent().getStringExtra("type");
        tmdbId      = getIntent().getIntExtra("id", 0);
        imdbId      = getIntent().getStringExtra("imdbId");
        String title = getIntent().getStringExtra("title");
        season      = getIntent().getIntExtra("season",  1);
        episode     = getIntent().getIntExtra("episode", 1);
        startSource = getIntent().getStringExtra("source");
        if (imdbId == null) imdbId = "";

        binding.titleText.setText(title != null ? title : "");
        updateEpLabel();

        // WebView config
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

        binding.playerWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                binding.loadingBar.setProgress(progress);
                binding.loadingBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
            }
        });

        buildSources();

        // TV controls
        if ("tv".equals(type)) {
            binding.tvControls.setVisibility(View.VISIBLE);
            binding.prevBtn.setOnClickListener(v -> {
                if (episode > 1) { episode--; updateAndPlay(); }
            });
            binding.nextBtn.setOnClickListener(v -> {
                episode++;
                updateAndPlay();
            });
        } else {
            binding.tvControls.setVisibility(View.GONE);
        }

        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void buildSources() {
        sources = "movie".equals(type)
            ? SourceGenerator.getMovieSources(imdbId, tmdbId)
            : SourceGenerator.getTVSources(imdbId, tmdbId, season, episode);

        List<String> labels = new ArrayList<>();
        for (SourceGenerator.Source s : sources) labels.add(s.label);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sourceSpinner.setAdapter(adapter);

        // Select preferred source
        int defaultIdx = 0;
        if (startSource != null) {
            for (int i = 0; i < sources.size(); i++) {
                if (sources.get(i).label.startsWith(startSource)) { defaultIdx = i; break; }
            }
        }
        final int startIdx = defaultIdx;
        binding.sourceSpinner.setSelection(startIdx);
        binding.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (first && pos == startIdx) { first = false; loadSource(pos); return; }
                first = false;
                loadSource(pos);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });

        loadSource(defaultIdx);
    }

    private void updateAndPlay() {
        updateEpLabel();
        buildSources();
    }

    private void updateEpLabel() {
        if ("tv".equals(type)) {
            binding.episodeLabel.setVisibility(View.VISIBLE);
            binding.episodeLabel.setText("S" + season + " · E" + episode);
        } else {
            binding.episodeLabel.setVisibility(View.GONE);
        }
    }

    private void loadSource(int index) {
        if (index < 0 || index >= sources.size()) return;
        String url = sources.get(index).url;
        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width'>"
            + "<style>*{margin:0;padding:0;background:#000;overflow:hidden}"
            + "iframe{width:100vw;height:100vh;border:none;display:block}</style></head>"
            + "<body><iframe src='" + url + "' "
            + "allowfullscreen allow='autoplay;fullscreen;encrypted-media;picture-in-picture;xr-spatial-tracking'>"
            + "</iframe></body></html>";
        binding.playerWebView.loadDataWithBaseURL(
            "https://www.google.com", html, "text/html", "utf-8", null);
    }

    @Override
    public void onBackPressed() {
        if (binding.playerWebView.canGoBack()) binding.playerWebView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onPause()  { super.onPause();  binding.playerWebView.onPause(); }
    @Override protected void onResume() { super.onResume(); binding.playerWebView.onResume(); }
    @Override protected void onDestroy() { binding.playerWebView.destroy(); super.onDestroy(); }
}
