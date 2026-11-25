package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.anime.AniListClient;
import com.fountainhome.streaming.anime.source.AnimeLookup;
import com.fountainhome.streaming.player.OfflinePlayerActivity;
import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.databinding.ActivityWatchBinding;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.player.EmbedPlayerActivity;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.OfflineStore;
import com.fountainhome.streaming.service.WatchProgress;
import com.fountainhome.streaming.ui.adapter.EpisodeAdapter;
import com.fountainhome.streaming.ui.theme.ThemeManager;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.util.ArrayList;
import java.util.List;

/** Anime details. It uses the very same layout as the movie and TV details page. */
public class AnimeDetailActivity extends BaseActivity {
    private static final String[] SERVERS = {"VidLink", "VIDEASY", "SuperFlix", "AutoEmbed", "2Anime", "Yugen"};

    private ActivityWatchBinding b;
    private int anilistId, totalEpisodes, selectedEp = 1;
    private String title, cover, banner;
    private double rating;
    private ContentItem item;
    private String desc, status, format, romaji;
    private AnimeLookup.Ref ref;
    private boolean userSaved;
    private final List<Models.Episode> eps = new ArrayList<>();
    private final List<Models.Episode> shown = new ArrayList<>();
    private EpisodeAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityWatchBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        anilistId = getIntent().getIntExtra("anime_id", 0);
        title = getIntent().getStringExtra("title");
        cover = getIntent().getStringExtra("cover");
        banner = getIntent().getStringExtra("banner");
        desc = getIntent().getStringExtra("description");
        totalEpisodes = getIntent().getIntExtra("episodes", 0);
        rating = getIntent().getDoubleExtra("rating", 0);
        status = getIntent().getStringExtra("status");
        format = getIntent().getStringExtra("format");

        item = new ContentItem();
        item.id = anilistId; item.mediaType = "anime"; item.title = title;
        item.posterPath = cover; item.backdropPath = banner != null && !banner.isEmpty() ? banner : cover;
        item.rating = rating;
        item.isAnime = true;
        item.overview = desc; item.episodeCount = totalEpisodes; item.animeStatus = status; item.animeFormat = format;
        romaji = getIntent().getStringExtra("romaji");
        item.romajiTitle = romaji;
        ref = new AnimeLookup.Ref(anilistId, getIntent().getStringExtra("english") != null ? getIntent().getStringExtra("english") : title, romaji, totalEpisodes);
        AnimeLookup.Ref old = AnimeLookup.recall(this, anilistId, title);
        if (ref.romaji == null) ref.romaji = old.romaji;
        AnimeLookup.remember(this, ref);

        b.loadingView.setVisibility(View.GONE);
        b.backBtn.setOnClickListener(v -> finish());
        Glide.with(this).load(OfflineStore.image(this, item.backdropPath, "w780"))
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)).centerCrop().into(b.backdropImage);
        Glide.with(this).load(OfflineStore.image(this, cover, "w500"))
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)).centerCrop().into(b.posterImage);
        b.titleText.setText(title != null ? title : "");

        bindInfo();

        updateStatusBadge();
        b.statusBtn.setOnClickListener(v -> showStatusDialog());
        refreshButtons();
        b.favoriteBtn.setOnClickListener(v -> { LibraryManager.toggleFavorite(this, item); refreshButtons(); });
        b.watchlistBtn.setOnClickListener(v -> {
            if (LibraryManager.isIn(this, anilistId, "anime", LibraryManager.WATCHLIST))
                LibraryManager.remove(this, anilistId, "anime", LibraryManager.WATCHLIST);
            else LibraryManager.add(this, item, LibraryManager.WATCHLIST);
            refreshButtons();
        });
        b.shareBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, (title != null ? title : "") + " — Fountain Home");
            startActivity(Intent.createChooser(i, "Share via"));
        });
        b.downloadMovieBtn.setVisibility(View.GONE);
        refreshSaveButton();
        b.downloadBtn.setOnClickListener(v -> {
            if (OfflineStore.isSaved(this, "anime", anilistId)) {
                new AlertDialog.Builder(this).setMessage("Remove the saved offline copy of this page?")
                    .setPositiveButton("Remove", (d, w) -> {
                        OfflineStore.remove(getApplicationContext(), "anime", anilistId);
                        refreshSaveButton();
                    }).setNegativeButton("Cancel", null).show();
            } else saveOffline();
        });
        b.similarSection.setVisibility(View.GONE);

        b.playBtn.setOnClickListener(v -> openPlayer(selectedEp));
        long pos = WatchProgress.get(this, anilistId, "anime", 1, 1);
        long lastEp = WatchProgress.get(this, anilistId, "anime", 1, selectedEp);
        if (pos > 10000 || lastEp > 10000) {
            b.resumeBtn.setVisibility(View.VISIBLE);
            b.resumeBtn.setOnClickListener(v -> openPlayer(selectedEp));
        }

        setupServer();
        setupDubSub();
        setupEpisodes();
        if ((desc == null || desc.isEmpty() || totalEpisodes <= 0) && !getIntent().getBooleanExtra("offline", false)) fillMissing();
        loadRealEpisodes();
    }

    /** Opened from a list that only knows the cover (home, history, search): fetch the rest from AniList. */
    private void fillMissing() {
        AniListClient.getById(anilistId, new AniListClient.Callback<AniListClient.AnimeItem>() {
            @Override public void onSuccess(AniListClient.AnimeItem a) {
                if (isDestroyed()) return;
                if (desc == null || desc.isEmpty()) desc = a.description;
                if (status == null || status.isEmpty()) status = a.status;
                if (format == null || format.isEmpty()) format = a.format;
                if (rating <= 0) rating = a.averageScore;
                if (a.bannerImage != null && !a.bannerImage.isEmpty() && (banner == null || banner.isEmpty())) {
                    banner = a.bannerImage; item.backdropPath = banner;
                    Glide.with(AnimeDetailActivity.this).load(OfflineStore.image(AnimeDetailActivity.this, banner, "w780")).centerCrop().into(b.backdropImage);
                }
                romaji = a.titleRomaji; item.romajiTitle = romaji;
                ref.english = a.titleEnglish != null && !a.titleEnglish.isEmpty() ? a.titleEnglish : ref.english;
                ref.romaji = romaji;
                if (a.episodes > 0 && totalEpisodes <= 0) { totalEpisodes = a.episodes; setupEpisodes(); }
                ref.episodes = totalEpisodes;
                AnimeLookup.remember(AnimeDetailActivity.this, ref);
                item.overview = desc; item.episodeCount = totalEpisodes; item.animeStatus = status; item.animeFormat = format; item.rating = rating;
                bindInfo();
            }
            @Override public void onError(String e) {}
        });
    }

    /** Replace the synthesized 1..N list with what the streaming site really has (airing shows, unknown counts). */
    private void loadRealEpisodes() {
        final android.content.Context app = getApplicationContext();
        final AnimeLookup.Ref r = ref;
        new Thread(() -> {
            try {
                AnimeLookup.Series sr = AnimeLookup.series(app, r);
                int max = 0;
                for (com.fountainhome.streaming.anime.source.AnimeSource.Ep e : sr.episodes) max = Math.max(max, (int) e.number);
                final int n = max;
                runOnUiThread(() -> {
                    if (isDestroyed() || n <= 0 || n == totalEpisodes) return;
                    totalEpisodes = n; item.episodeCount = n;
                    eps.clear(); shown.clear();
                    for (int k = 1; k <= n; k++) {
                        Models.Episode e = new Models.Episode(); e.episode_number = k; e.name = "Episode " + k; eps.add(e);
                    }
                    shown.addAll(eps);
                    if (adapter == null) setupEpisodes(); else adapter.notifyDataSetChanged();
                    bindInfo();
                });
            } catch (Throwable ignored) {}
        }, "fh-eps").start();
    }

    private void refreshSaveButton() {
        boolean saved = OfflineStore.isSaved(this, "anime", anilistId);
        b.downloadBtn.setVisibility(View.VISIBLE);
        b.downloadBtn.setImageResource(saved ? R.drawable.ic_check : R.drawable.ic_download);
        b.downloadBtn.setAlpha(1f);
    }

    private void saveOffline() {
        Toast.makeText(this, "Saving page for offline\u2026", Toast.LENGTH_SHORT).show();
        b.downloadBtn.setImageResource(R.drawable.ic_refresh);
        b.downloadBtn.setAlpha(0.6f);
        final android.content.Context app = getApplicationContext();
        item.overview = desc; item.episodeCount = totalEpisodes; item.animeStatus = status; item.animeFormat = format; item.rating = rating;
        item.releaseDate = null;
        final OfflineStore.Saved sv = new OfflineStore.Saved();
        sv.item = item;
        Models.SeasonDetail sd = new Models.SeasonDetail();
        sd.season_number = 1; sd.name = "Episodes"; sd.episodes = new ArrayList<>(eps);
        sv.seasons.put(1, sd);
        new Thread(() -> {
            boolean ok = true;
            try { OfflineStore.save(app, sv); } catch (Throwable t) { ok = false; }
            final boolean fin = ok;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                refreshSaveButton();
                Toast.makeText(this, fin ? "Saved for offline \u2014 find it under More > Offline"
                    : "Could not save. Check your connection and try again.", Toast.LENGTH_LONG).show();
            });
        }, "fh-save").start();
    }

    private void bindInfo() {
        StringBuilder meta = new StringBuilder();
        if (rating > 0) meta.append(String.format("★%.1f", rating));
        if (totalEpisodes > 0) meta.append(meta.length() > 0 ? "  ·  " : "").append(totalEpisodes).append(" episodes");
        if (status != null && !status.isEmpty()) meta.append(meta.length() > 0 ? "  ·  " : "").append(status);
        if (format != null && !format.isEmpty()) meta.append(meta.length() > 0 ? "  ·  " : "").append(format);
        b.metaText.setText(meta);
        if (desc != null && !desc.isEmpty()) {
            b.overviewText.setText(Html.fromHtml(desc, Html.FROM_HTML_MODE_COMPACT));
            b.overviewText.setVisibility(View.VISIBLE);
        }

    }

    private void setupServer() {
        ArrayAdapter<String> sa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SERVERS);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.sourceSpinner.setAdapter(sa);
        String cur = AppPreferences.getAnimeSource(this);
        for (int k = 0; k < SERVERS.length; k++) if (SERVERS[k].equals(cur)) { b.sourceSpinner.setSelection(k); break; }
        b.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                AppPreferences.setAnimeSource(AnimeDetailActivity.this, SERVERS[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupDubSub() {
        b.dubsubRow.setVisibility(View.VISIBLE);
        b.subChip.setOnClickListener(v -> { AppPreferences.setAnimeDubSub(this, "sub"); styleDubSub(); });
        b.dubChip.setOnClickListener(v -> { AppPreferences.setAnimeDubSub(this, "dub"); styleDubSub(); });
        styleDubSub();
    }

    private void styleDubSub() {
        boolean dub = "dub".equals(AppPreferences.getAnimeDubSub(this));
        chip(b.subChip, !dub);
        chip(b.dubChip, dub);
    }

    private void chip(android.widget.TextView t, boolean on) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(getResources().getDisplayMetrics().density * 18);
        g.setColor(on ? ThemeManager.accent(this) : ThemeManager.color(this, R.attr.fhSurfaceAlt));
        t.setBackground(g);
        t.setTextColor(on ? ThemeManager.color(this, R.attr.fhOnAccent) : ThemeManager.color(this, R.attr.fhText));
    }

    private void setupEpisodes() {
        if (totalEpisodes <= 0) return;
        for (int n = 1; n <= totalEpisodes; n++) {
            Models.Episode e = new Models.Episode();
            e.episode_number = n;
            e.name = "Episode " + n;
            eps.add(e);
        }
        shown.addAll(eps);
        b.tvSection.setVisibility(View.VISIBLE);
        b.seasonButtonsContainer.setVisibility(View.GONE);
        b.episodeMenuBtn.setVisibility(View.GONE);
        b.epSearchBar.setVisibility(View.VISIBLE);
        b.epSearchBar.setHint("Jump to episode number");
        b.epSearchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { filter(s.toString().trim()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        adapter = new EpisodeAdapter(shown, (ep, num) -> { selectedEp = num; openPlayer(num); },
            (ep, num) -> startDownload(num), anilistId, "anime", 1, this);
        b.episodeListRv.setLayoutManager(new LinearLayoutManager(this));
        b.episodeListRv.setNestedScrollingEnabled(false);
        b.episodeListRv.setAdapter(adapter);
    }

    private void filter(String q) {
        shown.clear();
        for (Models.Episode e : eps)
            if (q.isEmpty() || String.valueOf(e.episode_number).contains(q)) shown.add(e);
        adapter.notifyDataSetChanged();
    }

    private void startDownload(int ep) {
        DownloadManager2.enqueue(this, item, 1, ep);
        Toast.makeText(this, "Download queued — see More > Offline", Toast.LENGTH_LONG).show();
    }

    private Intent webPlayerIntent(int ep) {
        Intent i = new Intent(this, EmbedPlayerActivity.class);
        i.putExtra("type", "anime");
        i.putExtra("id", anilistId);
        i.putExtra("imdbId", "");
        i.putExtra("title", title != null ? title : "");
        i.putExtra("posterPath", cover != null ? cover : "");
        i.putExtra("backdropPath", banner != null ? banner : "");
        i.putExtra("total_episodes", totalEpisodes);
        i.putExtra("rating", rating);
        i.putExtra("season", 1);
        i.putExtra("episode", ep);
        return i;
    }

    private void openPlayer(int ep) {
        Intent i;
        if (AppPreferences.getAnimeDirect(this)) {
            i = new Intent(this, OfflinePlayerActivity.class);
            i.putExtra(OfflinePlayerActivity.EXTRA_STREAM_ANIME_ID, anilistId);
            i.putExtra(OfflinePlayerActivity.EXTRA_STREAM_EPISODE, ep);
            i.putExtra(OfflinePlayerActivity.EXTRA_STREAM_TOTAL, totalEpisodes);
            i.putExtra(OfflinePlayerActivity.EXTRA_STREAM_TITLE, title != null ? title : "");
            i.putExtra(OfflinePlayerActivity.EXTRA_STREAM_ROMAJI, romaji);
            i.putExtra(OfflinePlayerActivity.EXTRA_STREAM_POSTER, cover != null ? cover : "");
            i.putExtra(OfflinePlayerActivity.EXTRA_STREAM_BACKDROP, banner != null ? banner : "");
            i.putExtra(OfflinePlayerActivity.EXTRA_STREAM_RATING, rating);
            i.putExtra(OfflinePlayerActivity.EXTRA_WEB_FALLBACK, webPlayerIntent(ep));
        } else i = webPlayerIntent(ep);
        startActivity(i);
        AppPreferences.setItemStatus(this, anilistId, "anime", AppPreferences.STATUS_WATCH);
        updateStatusBadge();
    }

    private void refreshButtons() {
        b.favoriteBtn.setAlpha(LibraryManager.isIn(this, anilistId, "anime", LibraryManager.FAVORITES) ? 1f : 0.4f);
        b.watchlistBtn.setAlpha(LibraryManager.isIn(this, anilistId, "anime", LibraryManager.WATCHLIST) ? 1f : 0.4f);
    }

    private void showStatusDialog() {
        String[] opts = {"Planning to Watch", "Watching", "Watched", "Dropped", "Remove Status"};
        new AlertDialog.Builder(this).setTitle("Set Status").setItems(opts, (d, w) -> {
            String[] keys = {AppPreferences.STATUS_PLAN, AppPreferences.STATUS_WATCH,
                AppPreferences.STATUS_DONE, AppPreferences.STATUS_DROP, AppPreferences.STATUS_NONE};
            AppPreferences.setItemStatus(this, anilistId, "anime", keys[w]);
            updateStatusBadge();
            Toast.makeText(this, "Status: " + opts[w], Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void updateStatusBadge() {
        String st = AppPreferences.getItemStatus(this, anilistId, "anime");
        b.statusBtn.setText(ThemeManager.statusLabel(st));
        b.statusBtn.setTextColor(ThemeManager.statusColor(this, st));
    }
}
