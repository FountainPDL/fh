package com.fountainhome.streaming.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.fountainhome.streaming.anime.AniListClient;
import com.fountainhome.streaming.anime.AnimeAdapter;
import com.fountainhome.streaming.databinding.FragmentAnimeBinding;
import com.fountainhome.streaming.ui.AnimeDetailActivity;
import java.util.List;

public class AnimeFragment extends Fragment {

    private FragmentAnimeBinding binding;
    private AnimeAdapter trendingAdapter, popularAdapter, seasonAdapter;
    private String activeFilter = "trending";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnimeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRows();
        loadAll();

        binding.tabTrending.setOnClickListener(v -> { setTab("trending"); loadTrending(); });
        binding.tabPopular.setOnClickListener(v  -> { setTab("popular");  loadPopular();  });
        binding.tabSeason.setOnClickListener(v   -> { setTab("season");   loadSeason();   });

        binding.searchBtn.setOnClickListener(v -> {
            binding.searchBar.setVisibility(
                binding.searchBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (binding.searchBar.getVisibility() == View.VISIBLE)
                binding.searchBar.requestFocus();
        });

        binding.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String q) {
                doSearch(q); return true;
            }
            public boolean onQueryTextChange(String q) {
                if (q.length() >= 2) doSearch(q);
                return false;
            }
        });
    }

    private void setupRows() {
        trendingAdapter = new AnimeAdapter(this::openDetail);
        binding.trendingRv.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.trendingRv.setAdapter(trendingAdapter);

        popularAdapter = new AnimeAdapter(this::openDetail);
        binding.popularRv.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.popularRv.setAdapter(popularAdapter);

        seasonAdapter = new AnimeAdapter(this::openDetail);
        binding.seasonRv.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.seasonRv.setAdapter(seasonAdapter);
    }

    private void loadAll() {
        loadTrending();
        loadPopular();
        loadSeason();
    }

    private void loadTrending() {
        binding.progressBar.setVisibility(View.VISIBLE);
        AniListClient.getTrending(new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> result) {
                binding.progressBar.setVisibility(View.GONE);
                trendingAdapter.submitList(result);
            }
            public void onError(String error) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadPopular() {
        AniListClient.getPopular(new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) { popularAdapter.submitList(r); }
            public void onError(String e) {}
        });
    }

    private void loadSeason() {
        AniListClient.getThisSeason(new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) { seasonAdapter.submitList(r); }
            public void onError(String e) {}
        });
    }

    private void doSearch(String q) {
        binding.progressBar.setVisibility(View.VISIBLE);
        AniListClient.search(q, new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) {
                binding.progressBar.setVisibility(View.GONE);
                trendingAdapter.submitList(r);
                binding.trendingTitle.setText("Results: " + q);
            }
            public void onError(String e) { binding.progressBar.setVisibility(View.GONE); }
        });
    }

    private void setTab(String tab) {
        activeFilter = tab;
        int active = 0xFFCFFF04, grey = 0xFF888888;
        binding.tabTrending.setTextColor("trending".equals(tab) ? active : grey);
        binding.tabPopular.setTextColor("popular".equals(tab)   ? active : grey);
        binding.tabSeason.setTextColor("season".equals(tab)     ? active : grey);
    }

    private void openDetail(AniListClient.AnimeItem item) {
        Intent i = new Intent(getContext(), AnimeDetailActivity.class);
        i.putExtra("anime_id", item.id);
        i.putExtra("title", item.displayTitle());
        i.putExtra("cover", item.coverImage);
        i.putExtra("banner", item.bannerImage);
        i.putExtra("description", item.description);
        i.putExtra("episodes", item.episodes);
        i.putExtra("rating", item.averageScore);
        startActivity(i);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
