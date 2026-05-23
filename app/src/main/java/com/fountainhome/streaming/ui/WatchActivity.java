package com.fountainhome.streaming.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.databinding.ActivityWatchBinding;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.service.WatchProgress;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.adapter.EpisodeAdapter;
import com.fountainhome.streaming.ui.player.PlayerActivity;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.fountainhome.streaming.ui.viewmodel.WatchViewModel;

import java.util.ArrayList;
import java.util.List;

public class WatchActivity extends AppCompatActivity {

    private ActivityWatchBinding binding;
    private WatchViewModel vm;
    private ContentItem currentItem;
    private String type;
    private int tmdbId, selSeason = 1, selEpisode = 1;
    private List<Models.Episode> currentEpisodes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityWatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        type   = getIntent().getStringExtra("type");
        tmdbId = getIntent().getIntExtra("id", 0);

        vm = new ViewModelProvider(this).get(WatchViewModel.class);

        vm.getLoading().observe(this, loading ->
            binding.loadingView.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.getContent().observe(this, item -> {
            if (item == null) return;
            currentItem = item;
            populateUI(item);
        });

        vm.getSeason().observe(this, sd -> {
            if (sd == null) return;
            currentEpisodes = sd.episodes != null ? sd.episodes : new ArrayList<>();
            populateEpisodeList();
        });

        vm.getSimilar().observe(this, items -> {
            if (items == null || items.isEmpty()) {
                binding.similarSection.setVisibility(View.GONE);
                return;
            }
            binding.similarSection.setVisibility(View.VISIBLE);
            ContentAdapter adapter = new ContentAdapter(sim -> {
                Intent i = new Intent(this, WatchActivity.class);
                i.putExtra("type", sim.mediaType); i.putExtra("id", sim.id);
                startActivity(i);
            });
            binding.similarRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.similarRv.setAdapter(adapter);
            adapter.submitList(items);
        });

        binding.backBtn.setOnClickListener(v -> finish());
        vm.load(type, tmdbId);
    }

    private void populateUI(ContentItem item) {
        Glide.with(this).load(SourceGenerator.imageUrl(item.backdropPath, "w780"))
            .centerCrop().into(binding.backdropImage);
        Glide.with(this).load(SourceGenerator.imageUrl(item.posterPath, "w342"))
            .centerCrop().into(binding.posterImage);

        binding.titleText.setText(item.displayTitle());

        StringBuilder meta = new StringBuilder();
        if (!item.year().isEmpty()) meta.append(item.year());
        if (item.runtime > 0) meta.append("  ·  ").append(item.runtime).append("m");
        if (item.rating > 0) meta.append("  ·  ★ ").append(String.format("%.1f", item.rating));
        if ("tv".equals(type) && item.numberOfSeasons > 0)
            meta.append("  ·  ").append(item.numberOfSeasons).append(" seasons");
        binding.metaText.setText(meta);

        if (item.overview != null) {
            binding.overviewText.setText(item.overview);
            binding.overviewText.setVisibility(View.VISIBLE);
        }

        // TV controls
        if ("tv".equals(type)) {
            binding.tvSection.setVisibility(View.VISIBLE);
            setupSeasonButtons(item.numberOfSeasons);
        } else {
            binding.tvSection.setVisibility(View.GONE);
        }

        // Source spinner
        String[] srcs = {"VidSrc", "2Embed", "AutoEmbed", "SuperEmbed VIP"};
        ArrayAdapter<String> sa = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, srcs);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sourceSpinner.setAdapter(sa);
        String pref = AppPreferences.getSource(this);
        for (int i = 0; i < srcs.length; i++)
            if (srcs[i].equals(pref)) { binding.sourceSpinner.setSelection(i); break; }
        binding.sourceSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    AppPreferences.setSource(WatchActivity.this, srcs[pos]);
                }
                public void onNothingSelected(AdapterView<?> p) {}
            });

        // Library buttons
        refreshLibraryBtns(item);

        binding.favoriteBtn.setOnClickListener(v -> {
            LibraryManager.toggleFavorite(this, item);
            refreshLibraryBtns(item);
            boolean fav = LibraryManager.isIn(this, item.id, item.mediaType, LibraryManager.FAVORITES);
            Toast.makeText(this, fav ? "Added to favorites" : "Removed from favorites",
                Toast.LENGTH_SHORT).show();
        });

        binding.watchlistBtn.setOnClickListener(v -> {
            boolean inWl = LibraryManager.isIn(this, item.id, item.mediaType, LibraryManager.WATCHLIST);
            if (inWl) LibraryManager.remove(this, item.id, item.mediaType, LibraryManager.WATCHLIST);
            else LibraryManager.add(this, item, LibraryManager.WATCHLIST);
            refreshLibraryBtns(item);
        });

        // Download button
        binding.downloadBtn.setOnClickListener(v -> {
            DownloadManager2.saveForOffline(this, item);
            Toast.makeText(this, "Saved to library for offline viewing", Toast.LENGTH_SHORT).show();
            binding.downloadBtn.setText("✓ Saved");
        });

        // Share
        binding.shareBtn.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,
                item.displayTitle() + " — watching on Fountain Home");
            startActivity(Intent.createChooser(share, "Share via"));
        });

        // Play button
        binding.playBtn.setOnClickListener(v -> openPlayer(item, selSeason, selEpisode));

        // Resume button — show if there's saved progress
        long savedPos = WatchProgress.get(this, tmdbId, type, selSeason, selEpisode);
        if (savedPos > 10000) {
            binding.resumeBtn.setVisibility(View.VISIBLE);
            binding.resumeBtn.setOnClickListener(v -> openPlayer(item, selSeason, selEpisode));
        }

        // Quick episode picker
        if ("tv".equals(type)) {
            binding.episodeMenuBtn.setVisibility(View.VISIBLE);
            binding.episodeMenuBtn.setOnClickListener(v -> showEpisodeMenu(item));
        }
    }

    private void setupSeasonButtons(int seasons) {
        if (seasons <= 0) seasons = 1;
        binding.seasonButtonsContainer.removeAllViews();
        for (int s = 1; s <= seasons; s++) {
            final int seasonNum = s;
            TextView btn = new TextView(this);
            btn.setText("S" + s);
            btn.setPadding(24, 12, 24, 12);
            btn.setTextColor(s == selSeason ? getAccentColor() : 0xFFAAAAAA);
            btn.setBackground(s == selSeason ? getHighlightDrawable() : null);
            btn.setTextSize(13);
            btn.setOnClickListener(v -> {
                selSeason = seasonNum;
                selEpisode = 1;
                vm.loadSeason(tmdbId, seasonNum);
                setupSeasonButtons(seasons);
            });
            binding.seasonButtonsContainer.addView(btn);
        }
    }

    private void populateEpisodeList() {
        EpisodeAdapter adapter = new EpisodeAdapter(currentEpisodes,
            (episode, epNum) -> {
                selEpisode = epNum;
                openPlayer(currentItem, selSeason, epNum);
            }, tmdbId, type, selSeason, this);
        binding.episodeListRv.setLayoutManager(new LinearLayoutManager(this));
        binding.episodeListRv.setAdapter(adapter);
    }

    /** Full-screen dialog showing all seasons & episodes */
    private void showEpisodeMenu(ContentItem item) {
        Dialog dialog = new Dialog(this, R.style.Theme_FountainHome);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_episode_menu);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        }

        RecyclerView rv = dialog.findViewById(R.id.episodes_rv);
        rv.setLayoutManager(new LinearLayoutManager(this));

        EpisodeAdapter adapter = new EpisodeAdapter(currentEpisodes,
            (episode, epNum) -> {
                selEpisode = epNum;
                openPlayer(item, selSeason, epNum);
                dialog.dismiss();
            }, tmdbId, type, selSeason, this);
        rv.setAdapter(adapter);

        TextView titleV = dialog.findViewById(R.id.dialog_title);
        titleV.setText(item.displayTitle() + " — S" + selSeason);

        dialog.show();
    }

    private void openPlayer(ContentItem item, int season, int episode) {
        String src = binding.sourceSpinner.getSelectedItem() != null
            ? binding.sourceSpinner.getSelectedItem().toString() : "VidSrc";
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("type",    type);
        i.putExtra("id",      tmdbId);
        i.putExtra("imdbId",  item.imdbId != null ? item.imdbId : "");
        i.putExtra("title",   item.displayTitle());
        i.putExtra("season",  season);
        i.putExtra("episode", episode);
        i.putExtra("source",  src);
        i.putExtra("total_episodes", currentEpisodes.size());
        startActivity(i);
        LibraryManager.updateProgress(this, item, season, episode,
            WatchProgress.get(this, tmdbId, type, season, episode));
    }

    private void refreshLibraryBtns(ContentItem item) {
        boolean fav = LibraryManager.isIn(this, item.id, item.mediaType, LibraryManager.FAVORITES);
        boolean wl  = LibraryManager.isIn(this, item.id, item.mediaType, LibraryManager.WATCHLIST);
        binding.favoriteBtn.setAlpha(fav ? 1.0f : 0.4f);
        binding.watchlistBtn.setAlpha(wl  ? 1.0f : 0.4f);
        if (fav) binding.favoriteBtn.setColorFilter(getAccentColor());
        else binding.favoriteBtn.clearColorFilter();
    }

    private int getAccentColor() {
        return AppPreferences.getAccentColor(this);
    }

    private android.graphics.drawable.Drawable getHighlightDrawable() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(getAccentColor());
        d.setCornerRadius(20);
        return d;
    }
}
