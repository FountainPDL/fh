package com.fountainhome.streaming.ui.player;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.fountainhome.streaming.databinding.ActivityPlayerBinding;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.fountainhome.streaming.ui.viewmodel.PlayerViewModel;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private PlayerViewModel vm;
    private ExoPlayer player;

    private List<SourceGenerator.Source> sources = new ArrayList<>();
    private String imdbId = "";
    private int tmdbId;
    private String type;
    private int season  = 1;
    private int episode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        type   = getIntent().getStringExtra("type");
        tmdbId = getIntent().getIntExtra("id", 0);
        String title = getIntent().getStringExtra("title");

        binding.titleText.setText(title);

        // ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        binding.playerView.setPlayer(player);

        vm = new ViewModelProvider(this).get(PlayerViewModel.class);

        // Load content details to get imdbId then build sources
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
            setupTVControls();
        }

        // Source spinner changes
        binding.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                playSource(pos);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void buildSources() {
        if ("movie".equals(type)) {
            sources = SourceGenerator.getMovieSources(imdbId, tmdbId);
        } else {
            sources = SourceGenerator.getTVSources(imdbId, tmdbId, season, episode);
        }

        List<String> labels = new ArrayList<>();
        for (SourceGenerator.Source s : sources) labels.add(s.label);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sourceSpinner.setAdapter(adapter);

        if (!sources.isEmpty()) playSource(0);
    }

    private void playSource(int index) {
        if (index < 0 || index >= sources.size()) return;
        try {
            player.setMediaItem(MediaItem.fromUri(sources.get(index).url));
            player.prepare();
            player.play();
        } catch (Exception e) {
            Toast.makeText(this, "Source failed, try another", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTVControls() {
        binding.nextBtn.setOnClickListener(v -> {
            episode++;
            buildSources();
        });
        binding.prevBtn.setOnClickListener(v -> {
            if (episode > 1) { episode--; buildSources(); }
            else if (season > 1) { season--; episode = 1; buildSources(); }
        });
        binding.seasonBtn.setOnClickListener(v -> {
            season++;
            episode = 1;
            buildSources();
        });
    }

    @Override
    protected void onPause()   { super.onPause();   if (player != null) player.pause(); }
    @Override
    protected void onResume()  { super.onResume();  if (player != null) player.play(); }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }
}
