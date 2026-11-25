package com.fountainhome.streaming.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.fountainhome.streaming.anime.*;
import com.fountainhome.streaming.databinding.ActivityAnimeListBinding;
import java.util.*;
import androidx.annotation.NonNull;
public class AnimeListActivity extends BaseActivity {
    private ActivityAnimeListBinding b;
    private AnimeAdapter adapter;
    private final List<AniListClient.AnimeItem> allItems = new ArrayList<>();
    private String category;
    private boolean loading = false, hasMore = true;
    private int page = 1;
    private String query;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAnimeListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        category = getIntent().getStringExtra("category"); // trending/popular/season/search
        String title = getIntent().getStringExtra("title");
        query = getIntent().getStringExtra("query");
        b.pageTitle.setText(title != null ? title : "Anime");
        b.backBtn.setOnClickListener(v -> finish());
        adapter = new AnimeAdapter(item -> {
            Intent i = new Intent(this, AnimeDetailActivity.class);
            i.putExtra("anime_id", item.id); i.putExtra("title", item.displayTitle());
            i.putExtra("cover", item.coverImage); i.putExtra("banner", item.bannerImage);
            i.putExtra("description", item.description); i.putExtra("episodes", item.episodes);
            i.putExtra("rating", item.averageScore); i.putExtra("status", item.status);
            i.putExtra("format", item.format);
            i.putExtra("romaji", item.titleRomaji); i.putExtra("english", item.titleEnglish);
            startActivity(i);
        });
        GridLayoutManager lm = new GridLayoutManager(this, 3);
        b.contentRv.setLayoutManager(lm);
        b.contentRv.setAdapter(adapter);
        // Infinite scroll — AniList supports pagination
        b.contentRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                int total = lm.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (!loading && hasMore && last >= total - 6) load();
            }
        });
        load();
    }
    private void load() {
        loading = true; b.progressBar.setVisibility(View.VISIBLE);
        final int p = page;
        AniListClient.Callback<List<AniListClient.AnimeItem>> cb = new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) {
                loading = false;
                if (b == null) return;
                b.progressBar.setVisibility(View.GONE);
                if (r == null || r.isEmpty()) { hasMore = false; return; }
                page = p + 1;
                allItems.addAll(r);
                adapter.submitList(new ArrayList<>(allItems));
                if (r.size() < 20) hasMore = false;
            }
            public void onError(String e) {
                loading = false;
                if (b != null) b.progressBar.setVisibility(View.GONE);
                Toast.makeText(AnimeListActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
            }
        };
        switch (category != null ? category : "trending") {
            case "popular": AniListClient.getPopular(p, cb); break;
            case "season":  AniListClient.getThisSeason(p, cb); break;
            case "search":  AniListClient.search(query != null ? query : "", p, cb); break;
            default:        AniListClient.getTrending(p, cb); break;
        }
    }
}
