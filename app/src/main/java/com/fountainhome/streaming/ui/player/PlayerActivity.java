package com.fountainhome.streaming.ui.player;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fountainhome.streaming.databinding.ActivityPlayerBinding;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.viewmodel.PlayerViewModel;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private PlayerViewModel vm;
    private List<SourceGenerator.Source> sources = new ArrayList<>();
    private String imdbId = "";
    private int tmdbId;
    private String type;
    private int season  = 1;
    private int episode = 1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        type   = getIntent().getStringExtra("type");
        tmdbId = getIntent().getIntExtra("id", 0);
        String title = getIntent().getStringExtra("title");
        binding.titleText.setText(title);

        // Configure WebView
        WebSettings ws = binding.playerWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        binding.playerWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                binding.loadingBar.setProgress(progress);
                binding.loadingBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
            }
        });

        vm = new ViewModelProvider(this).get(PlayerViewModel.class);
        vm.getContent().observe(this, item -> {
            imdbId = item.imdbId != null ? item.imdbId : "";
            buildSources();
        });

        if ("movie".equals(type)) {
            vm.loadMovie(tmdbId);
            binding.tvControls.setVisibility(View.GONE);
        } else {
            vm.loadTV(tmdbId);
            binding.tvControls.setVisibility(View.VISIBLE);
            binding.nextBtn.setOnClickListener(v -> { episode++; buildSources(); });
            binding.prevBtn.setOnClickListener(v -> {
                if (episode > 1) episode--; else if (season > 1) { season--; episode = 1; }
                buildSources();
            });
            binding.seasonBtn.setOnClickListener(v -> { season++; episode = 1; buildSources(); });
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

        binding.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                loadSource(pos);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });

        if (!sources.isEmpty()) loadSource(0);
    }

    private void loadSource(int index) {
        if (index < 0 || index >= sources.size()) return;
        String url = sources.get(index).url;
        // Wrap in HTML so it fills the WebView as an iframe
        String html = "<!DOCTYPE html><html><head>"
            + "<style>*{margin:0;padding:0;background:#000}"
            + "iframe{width:100vw;height:100vh;border:0}</style></head>"
            + "<body><iframe src='" + url + "' allowfullscreen "
            + "allow='autoplay; fullscreen; encrypted-media'></iframe></body></html>";
        binding.playerWebView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    @Override
    public void onBackPressed() {
        if (binding.playerWebView.canGoBack()) binding.playerWebView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause()  { super.onPause();  binding.playerWebView.onPause(); }
    @Override
    protected void onResume() { super.onResume(); binding.playerWebView.onResume(); }
    @Override
    protected void onDestroy() {
        binding.playerWebView.destroy();
        super.onDestroy();
    }
}
