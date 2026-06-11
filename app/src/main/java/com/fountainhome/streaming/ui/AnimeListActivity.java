package com.fountainhome.streaming.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.fountainhome.streaming.anime.*;
import com.fountainhome.streaming.databinding.ActivityAnimeListBinding;
import java.util.*;
import androidx.annotation.NonNull;
public class AnimeListActivity extends AppCompatActivity {
    private ActivityAnimeListBinding b;
    private AnimeAdapter adapter;
    private final List<AniListClient.AnimeItem> allItems = new ArrayList<>();
    private String category;
    private boolean loading = false;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAnimeListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        category = getIntent().getStringExtra("category"); // trending/popular/season/search
        String title = getIntent().getStringExtra("title");
        String query = getIntent().getStringExtra("query");
        b.pageTitle.setText(title != null ? title : "Anime");
        b.backBtn.setOnClickListener(v -> finish());
        adapter = new AnimeAdapter(item -> {
            Intent i = new Intent(this, AnimeDetailActivity.class);
            i.putExtra("anime_id", item.id); i.putExtra("title", item.displayTitle());
            i.putExtra("cover", item.coverImage); i.putExtra("banner", item.bannerImage);
            i.putExtra("description", item.description); i.putExtra("episodes", item.episodes);
            i.putExtra("rating", item.averageScore); i.putExtra("status", item.status);
            i.putExtra("format", item.format);
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
                if (!loading && last >= total - 6) loadMore();
            }
        });
        load();
    }
    private void load() {
        loading = true; b.progressBar.setVisibility(View.VISIBLE);
        AniListClient.Callback<List<AniListClient.AnimeItem>> cb = new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) {
                loading = false;
                if (b == null) return;
                b.progressBar.setVisibility(View.GONE);
                if (r != null) {
                    allItems.addAll(r);
                    adapter.submitList(new ArrayList<>(allItems));
                }
            }
            public void onError(String e) {
                loading = false;
                if (b != null) b.progressBar.setVisibility(View.GONE);
                Toast.makeText(AnimeListActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
            }
        };
        switch (category != null ? category : "trending") {
            case "popular": AniListClient.getPopular(cb); break;
            case "season":  AniListClient.getThisSeason(cb); break;
            default:        AniListClient.getTrending(cb); break;
        }
    }
    private void loadMore() {
        // AniList pagination — reload with next batch
        // For now load same endpoint again (AniList returns different results with page param)
        load();
    }
}
