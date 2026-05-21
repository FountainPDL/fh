package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ActivityWatchBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.player.PlayerActivity;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.fountainhome.streaming.ui.viewmodel.WatchViewModel;
import com.fountainhome.streaming.api.Models;

import java.util.ArrayList;
import java.util.List;

public class WatchActivity extends AppCompatActivity {

    private ActivityWatchBinding binding;
    private WatchViewModel vm;
    private ContentItem currentItem;
    private String type;
    private int tmdbId;
    private int selSeason  = 1;
    private int selEpisode = 1;
    private List<Models.Episode> currentEpisodes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        type   = getIntent().getStringExtra("type");
        tmdbId = getIntent().getIntExtra("id", 0);

        vm = new ViewModelProvider(this).get(WatchViewModel.class);

        // Loading state
        vm.getLoading().observe(this, loading ->
            binding.loadingView.setVisibility(loading ? View.VISIBLE : View.GONE));

        // Content loaded
        vm.getContent().observe(this, item -> {
            if (item == null) return;
            currentItem = item;
            populateUI(item);
        });

        // Season loaded (TV only)
        vm.getSeason().observe(this, seasonDetail -> {
            if (seasonDetail == null) return;
            currentEpisodes = seasonDetail.episodes != null
                ? seasonDetail.episodes : new ArrayList<>();
            populateEpisodes();
        });

        // Similar content
        vm.getSimilar().observe(this, items -> {
            if (items == null || items.isEmpty()) {
                binding.similarSection.setVisibility(View.GONE);
                return;
            }
            binding.similarSection.setVisibility(View.VISIBLE);
            ContentAdapter adapter = new ContentAdapter(sim -> openWatch(sim));
            binding.similarRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.similarRv.setAdapter(adapter);
            adapter.submitList(items);
        });

        binding.backBtn.setOnClickListener(v -> finish());
        vm.load(type, tmdbId);
    }

    private void populateUI(ContentItem item) {
        // Backdrop
        Glide.with(this)
            .load(SourceGenerator.imageUrl(item.backdropPath, "w780"))
            .placeholder(android.R.color.black).centerCrop()
            .into(binding.backdropImage);

        // Poster
        Glide.with(this)
            .load(SourceGenerator.imageUrl(item.posterPath, "w342"))
            .placeholder(R.color.surface).centerCrop()
            .into(binding.posterImage);

        binding.titleText.setText(item.displayTitle());

        // Meta line
        StringBuilder meta = new StringBuilder();
        if (!item.year().isEmpty()) meta.append(item.year());
        if (item.runtime > 0) meta.append("  ·  ").append(item.runtime).append("m");
        if (item.rating > 0)  meta.append("  ·  ★ ").append(String.format("%.1f", item.rating));
        if ("tv".equals(type) && item.numberOfSeasons > 0)
            meta.append("  ·  ").append(item.numberOfSeasons).append(" seasons");
        binding.metaText.setText(meta);

        // Overview
        if (item.overview != null && !item.overview.isEmpty()) {
            binding.overviewText.setText(item.overview);
            binding.overviewText.setVisibility(View.VISIBLE);
        }

        // TV controls
        if ("tv".equals(type)) {
            binding.tvSection.setVisibility(View.VISIBLE);
            setupSeasonSpinner(item.numberOfSeasons);
        } else {
            binding.tvSection.setVisibility(View.GONE);
        }

        // Source spinner
        String defaultSrc = AppPreferences.getSource(this);
        String[] srcs = {"VidSrc", "2Embed", "AutoEmbed", "MultiEmbed"};
        ArrayAdapter<String> srcAdapt = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, srcs);
        srcAdapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sourceSpinner.setAdapter(srcAdapt);
        for (int i = 0; i < srcs.length; i++) {
            if (srcs[i].equals(defaultSrc)) { binding.sourceSpinner.setSelection(i); break; }
        }

        // Library state
        refreshLibraryBtns(item);

        // Favorite
        binding.favoriteBtn.setOnClickListener(v -> {
            if (LibraryManager.isIn(this, item.id, item.mediaType, LibraryManager.FAVORITES)) {
                LibraryManager.remove(this, item.id, item.mediaType, LibraryManager.FAVORITES);
                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
            } else {
                LibraryManager.add(this, item, LibraryManager.FAVORITES);
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
            }
            refreshLibraryBtns(item);
        });

        // Watchlist
        binding.watchlistBtn.setOnClickListener(v -> {
            if (LibraryManager.isIn(this, item.id, item.mediaType, LibraryManager.WATCHLIST)) {
                LibraryManager.remove(this, item.id, item.mediaType, LibraryManager.WATCHLIST);
                Toast.makeText(this, "Removed from watchlist", Toast.LENGTH_SHORT).show();
            } else {
                LibraryManager.add(this, item, LibraryManager.WATCHLIST);
                Toast.makeText(this, "Added to watchlist", Toast.LENGTH_SHORT).show();
            }
            refreshLibraryBtns(item);
        });

        // Play button
        binding.playBtn.setOnClickListener(v -> openPlayer(item));
    }

    private void setupSeasonSpinner(int seasons) {
        if (seasons <= 0) seasons = 1;
        List<String> seasonList = new ArrayList<>();
        for (int i = 1; i <= seasons; i++) seasonList.add("Season " + i);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, seasonList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.seasonSpinner.setAdapter(adapter);

        binding.seasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selSeason  = pos + 1;
                selEpisode = 1;
                vm.loadSeason(tmdbId, selSeason);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void populateEpisodes() {
        List<String> eps = new ArrayList<>();
        for (Models.Episode ep : currentEpisodes) {
            String label = "E" + ep.episode_number;
            if (ep.name != null && !ep.name.isEmpty()) label += " — " + ep.name;
            eps.add(label);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, eps);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.episodeSpinner.setAdapter(adapter);
        binding.episodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos < currentEpisodes.size())
                    selEpisode = currentEpisodes.get(pos).episode_number;
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void refreshLibraryBtns(ContentItem item) {
        boolean fav = LibraryManager.isIn(this, item.id, item.mediaType, LibraryManager.FAVORITES);
        boolean wl  = LibraryManager.isIn(this, item.id, item.mediaType, LibraryManager.WATCHLIST);
        binding.favoriteBtn.setAlpha(fav ? 1.0f : 0.4f);
        binding.watchlistBtn.setAlpha(wl  ? 1.0f : 0.4f);
    }

    private void openPlayer(ContentItem item) {
        String src = (String) binding.sourceSpinner.getSelectedItem();
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("type",    type);
        i.putExtra("id",      tmdbId);
        i.putExtra("imdbId",  item.imdbId != null ? item.imdbId : "");
        i.putExtra("title",   item.displayTitle());
        i.putExtra("season",  selSeason);
        i.putExtra("episode", selEpisode);
        i.putExtra("source",  src != null ? src : "VidSrc");
        startActivity(i);
        LibraryManager.updateProgress(this, item, selSeason, selEpisode);
    }

    private void openWatch(ContentItem item) {
        Intent i = new Intent(this, WatchActivity.class);
        i.putExtra("type", item.mediaType);
        i.putExtra("id",   item.id);
        startActivity(i);
    }
}
