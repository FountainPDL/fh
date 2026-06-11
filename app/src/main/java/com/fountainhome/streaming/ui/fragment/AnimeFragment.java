package com.fountainhome.streaming.ui.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.SearchView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fountainhome.streaming.anime.*;
import com.fountainhome.streaming.databinding.FragmentAnimeBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.AnimeDetailActivity;
import com.fountainhome.streaming.ui.AnimeListActivity;
import java.util.List;
public class AnimeFragment extends Fragment {
    private FragmentAnimeBinding b;
    private AnimeAdapter tA, pA, sA;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentAnimeBinding.inflate(i, c, false); return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        tA = new AnimeAdapter(this::open);
        b.trendingRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        b.trendingRv.setAdapter(tA);
        pA = new AnimeAdapter(this::open);
        b.popularRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        b.popularRv.setAdapter(pA);
        sA = new AnimeAdapter(this::open);
        b.seasonRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        b.seasonRv.setAdapter(sA);
        loadAll();
        b.tabTrending.setOnClickListener(x -> { setTab(0); AniListClient.getTrending(new AniListClient.Callback<List<AniListClient.AnimeItem>>() { public void onSuccess(List<AniListClient.AnimeItem> r) { if (b != null) { b.trendingTitle.setText("Trending"); tA.submitList(r); if (!r.isEmpty()) loadHero(r.get(0)); } } public void onError(String e) {} }); });
        b.tabPopular.setOnClickListener(x -> { setTab(1); AniListClient.getPopular(new AniListClient.Callback<List<AniListClient.AnimeItem>>() { public void onSuccess(List<AniListClient.AnimeItem> r) { if (b != null) { tA.submitList(r); if (!r.isEmpty()) loadHero(r.get(0)); } } public void onError(String e) {} }); });
        b.tabSeason.setOnClickListener(x -> { setTab(2); AniListClient.getThisSeason(new AniListClient.Callback<List<AniListClient.AnimeItem>>() { public void onSuccess(List<AniListClient.AnimeItem> r) { if (b != null) { tA.submitList(r); if (!r.isEmpty()) loadHero(r.get(0)); } } public void onError(String e) {} }); });
        b.searchBtn.setOnClickListener(x -> b.searchBar.setVisibility(b.searchBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        b.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String q) { search(q); return true; }
            public boolean onQueryTextChange(String q) { if (q != null && q.length() >= 2) search(q); return false; }
        });
        b.animeHeroBanner.setOnClickListener(x -> { Object tag = b.animeHeroBanner.getTag(); if (tag instanceof AniListClient.AnimeItem) open((AniListClient.AnimeItem) tag); });
        b.animeHeroPlay.setOnClickListener(x -> { Object tag = b.animeHeroBanner.getTag(); if (tag instanceof AniListClient.AnimeItem) open((AniListClient.AnimeItem) tag); });
        // View All buttons
        b.viewAllTrending.setOnClickListener(x -> viewAll("trending", "Trending Anime"));
        b.viewAllPopular.setOnClickListener(x -> viewAll("popular", "Most Popular"));
        b.viewAllSeason.setOnClickListener(x -> viewAll("season", "This Season"));
    }
    private void viewAll(String cat, String title) {
        Intent i = new Intent(getContext(), AnimeListActivity.class);
        i.putExtra("category", cat); i.putExtra("title", title);
        startActivity(i);
    }
    private void loadHero(AniListClient.AnimeItem item) {
        if (b == null) return;
        b.animeHeroBanner.setTag(item);
        b.animeHeroSection.setVisibility(View.VISIBLE);
        Glide.with(this).load(item.bannerImage != null && !item.bannerImage.isEmpty() ? item.bannerImage : item.coverImage)
            .transition(DrawableTransitionOptions.withCrossFade(600)).centerCrop().into(b.animeHeroBanner);
        b.animeHeroTitle.setText(item.displayTitle());
        b.animeHeroMeta.setText((item.averageScore > 0 ? String.format("\u2605%.1f", item.averageScore) + "  \u00b7  " : "")
            + (item.episodes > 0 ? item.episodes + " eps  \u00b7  " : "")
            + (item.status != null ? item.status : ""));
    }
    private void loadAll() {
        b.progressBar.setVisibility(View.VISIBLE);
        AniListClient.getTrending(new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) { if (b != null) { b.progressBar.setVisibility(View.GONE); tA.submitList(r); if (!r.isEmpty()) loadHero(r.get(0)); } }
            public void onError(String e) { if (b != null) b.progressBar.setVisibility(View.GONE); }
        });
        AniListClient.getPopular(new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) { if (b != null) pA.submitList(r); }
            public void onError(String e) {}
        });
        AniListClient.getThisSeason(new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) { if (b != null) sA.submitList(r); }
            public void onError(String e) {}
        });
    }
    private void search(String q) {
        b.progressBar.setVisibility(View.VISIBLE);
        AniListClient.search(q, new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) {
                if (b != null) { b.progressBar.setVisibility(View.GONE); b.trendingTitle.setText("Results: " + q); tA.submitList(r); if (!r.isEmpty()) loadHero(r.get(0)); }
            }
            public void onError(String e) { if (b != null) b.progressBar.setVisibility(View.GONE); }
        });
    }
    private void setTab(int a) {
        if (b == null) return;
        int g = AppPreferences.getAccentColor(requireContext()), gr = 0xFF888888;
        b.tabTrending.setTextColor(a == 0 ? g : gr);
        b.tabPopular.setTextColor(a == 1 ? g : gr);
        b.tabSeason.setTextColor(a == 2 ? g : gr);
    }
    private void open(AniListClient.AnimeItem item) {
        Intent i = new Intent(getContext(), AnimeDetailActivity.class);
        i.putExtra("anime_id", item.id); i.putExtra("title", item.displayTitle());
        i.putExtra("cover", item.coverImage); i.putExtra("banner", item.bannerImage);
        i.putExtra("description", item.description); i.putExtra("episodes", item.episodes);
        i.putExtra("rating", item.averageScore); i.putExtra("status", item.status);
        i.putExtra("format", item.format);
        startActivity(i);
    }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
