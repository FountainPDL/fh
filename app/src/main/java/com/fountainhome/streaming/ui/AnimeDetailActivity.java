package com.fountainhome.streaming.ui;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fountainhome.streaming.databinding.ActivityAnimeDetailBinding;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.player.PlayerActivity;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
public class AnimeDetailActivity extends AppCompatActivity {
    private ActivityAnimeDetailBinding b;
    private int anilistId, totalEpisodes, selectedEp = 1;
    private String title, cover;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAnimeDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        anilistId     = getIntent().getIntExtra("anime_id", 0);
        title         = getIntent().getStringExtra("title");
        cover         = getIntent().getStringExtra("cover");
        String banner = getIntent().getStringExtra("banner");
        String desc   = getIntent().getStringExtra("description");
        totalEpisodes = getIntent().getIntExtra("episodes", 0);
        double rating = getIntent().getDoubleExtra("rating", 0);
        String status = getIntent().getStringExtra("status");
        String format = getIntent().getStringExtra("format");
        int year      = getIntent().getIntExtra("year", 0);

        // Images
        Glide.with(this).load(banner != null && !banner.isEmpty() ? banner : cover)
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)).centerCrop().into(b.backdropImage);
        Glide.with(this).load(cover)
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)).centerCrop().into(b.posterImage);

        b.titleText.setText(title != null ? title : "");
        StringBuilder meta = new StringBuilder();
        if (year > 0) meta.append(year);
        if (rating > 0) meta.append(meta.length() > 0 ? "  \u00b7  " : "").append(String.format("\u2605%.1f", rating));
        if (totalEpisodes > 0) meta.append(meta.length() > 0 ? "  \u00b7  " : "").append(totalEpisodes).append(" eps");
        if (status != null && !status.isEmpty()) meta.append(meta.length() > 0 ? "  \u00b7  " : "").append(status);
        if (format != null && !format.isEmpty()) meta.append(meta.length() > 0 ? "  \u00b7  " : "").append(format);
        b.ratingText.setText(meta);

        if (desc != null && !desc.isEmpty()) {
            b.descText.setText(Html.fromHtml(desc, Html.FROM_HTML_MODE_COMPACT));
            b.descText.setVisibility(View.VISIBLE);
        }

        updateStatusBadge();
        b.statusBtn.setOnClickListener(v -> showStatusDialog());

        ContentItem ci = new ContentItem();
        ci.id = anilistId; ci.mediaType = "anime"; ci.title = title; ci.posterPath = cover;
        refreshButtons(ci);
        b.favoriteBtn.setOnClickListener(v -> { LibraryManager.toggleFavorite(this, ci); refreshButtons(ci); });
        b.watchlistBtn.setOnClickListener(v -> {
            boolean in = LibraryManager.isIn(this, anilistId, "anime", LibraryManager.WATCHLIST);
            if (in) LibraryManager.remove(this, anilistId, "anime", LibraryManager.WATCHLIST);
            else LibraryManager.add(this, ci, LibraryManager.WATCHLIST);
            refreshButtons(ci);
        });
        b.shareBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, (title != null ? title : "") + " \u2014 Fountain Home");
            startActivity(Intent.createChooser(i, "Share via"));
        });

        b.watchBtn.setOnClickListener(v -> openPlayer(false));
        b.subBtn.setOnClickListener(v -> { AppPreferences.setAnimeDubSub(this, "sub"); openPlayer(false); });
        b.dubBtn.setOnClickListener(v -> { AppPreferences.setAnimeDubSub(this, "dub"); openPlayer(true); });
        b.backBtn.setOnClickListener(v -> finish());

        // Source spinner
        String[] srcs = {"AutoEmbed Sub", "AutoEmbed Dub", "2Anime", "Yugen"};
        ArrayAdapter<String> sa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, srcs);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.sourceSpinner.setAdapter(sa);

        // Episode list
        if (totalEpisodes > 0) {
            b.episodeSection.setVisibility(View.VISIBLE);
            b.episodeRv.setLayoutManager(new LinearLayoutManager(this));
            b.episodeRv.setNestedScrollingEnabled(false);
            b.episodeRv.setAdapter(new EpAdapter());
        }
    }

    private void refreshButtons(ContentItem ci) {
        b.favoriteBtn.setAlpha(LibraryManager.isIn(this, anilistId, "anime", LibraryManager.FAVORITES) ? 1f : 0.4f);
        b.watchlistBtn.setAlpha(LibraryManager.isIn(this, anilistId, "anime", LibraryManager.WATCHLIST) ? 1f : 0.4f);
    }
    private void showStatusDialog() {
        String[] opts = {"Planning to Watch","Watching","Watched","Dropped","Remove Status"};
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
        String label; int color;
        switch (st) {
            case AppPreferences.STATUS_PLAN:  label="Planning"; color=0xFF2196F3; break;
            case AppPreferences.STATUS_WATCH: label="Watching"; color=0xFF4CAF50; break;
            case AppPreferences.STATUS_DONE:  label="Watched";  color=0xFFBB86FC; break;
            case AppPreferences.STATUS_DROP:  label="Dropped";  color=0xFFCF6679; break;
            default: label="Set Status"; color=0xFF888888;
        }
        b.statusBtn.setText(label); b.statusBtn.setTextColor(color);
    }
    private void openPlayer(boolean dub) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("type", "anime"); i.putExtra("id", anilistId);
        i.putExtra("title", title); i.putExtra("imdbId", "");
        i.putExtra("season", 1); i.putExtra("episode", selectedEp);
        startActivity(i);
        AppPreferences.setItemStatus(this, anilistId, "anime", AppPreferences.STATUS_WATCH);
    }

    class EpAdapter extends RecyclerView.Adapter<EpAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            LinearLayout row = new LinearLayout(p.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(32, 28, 32, 28);
            row.setBackgroundColor(0xFF141414);
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackground(getDrawable(android.R.drawable.list_selector_background));
            TextView num = new TextView(p.getContext());
            num.setTextColor(0xFFBB86FC); num.setTextSize(13); num.setMinWidth(80);
            num.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            TextView title = new TextView(p.getContext());
            title.setTextColor(0xFFEEEEEE); title.setTextSize(14);
            title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(num); row.addView(title);
            return new VH(row, num, title);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            int ep = pos + 1;
            h.num.setText("E" + ep);
            h.title.setText("Episode " + ep);
            h.row.setOnClickListener(v -> { selectedEp = ep; openPlayer("dub".equals(AppPreferences.getAnimeDubSub(AnimeDetailActivity.this))); });
        }
        @Override public int getItemCount() { return totalEpisodes; }
        class VH extends RecyclerView.ViewHolder {
            LinearLayout row; TextView num, title;
            VH(LinearLayout r, TextView n, TextView t) { super(r); row=r; num=n; title=t; }
        }
    }
}
