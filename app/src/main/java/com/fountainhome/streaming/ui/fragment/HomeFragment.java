package com.fountainhome.streaming.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.FragmentHomeBinding;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.SearchActivity;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.fountainhome.streaming.ui.viewmodel.HomeViewModel;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private static final String TAG = "HomeFragment";
    private boolean searchOpen = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            Log.e(TAG, "onCreateView error: " + e.getMessage());
            return new View(requireContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) return;

        try {
            HomeViewModel vm = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

            // Featured banner
            vm.getFeatured().observe(getViewLifecycleOwner(), item -> {
                if (item == null || binding == null) return;
                try {
                    Glide.with(this)
                        .load(SourceGenerator.imageUrl(item.backdropPath, "w780"))
                        .centerCrop().into(binding.featuredBanner);
                    binding.featuredTitle.setText(item.displayTitle());
                    binding.featuredGenre.setText("★ " + String.format("%.1f", item.rating)
                        + "  ·  " + ("movie".equals(item.mediaType) ? "Movie" : "TV Series"));
                    binding.watchNowBtn.setOnClickListener(v -> openWatch(item));
                    binding.featuredBanner.setOnClickListener(v -> openWatch(item));
                    binding.detailBtn.setOnClickListener(v -> openWatch(item));
                    binding.featuredAdd.setOnClickListener(v -> toggleWatchlist(item));
                } catch (Exception e) {
                    Log.e(TAG, "Featured error: " + e.getMessage());
                }
            });

            // Continue watching
            refreshContinue();

            // Tab switching
            binding.tabTrending.setOnClickListener(v -> {
                setTabActive(true);
                vm.getPopularMovies().observe(getViewLifecycleOwner(), items -> {
                    if (binding != null && binding.trendingRv.getAdapter() != null)
                        ((ContentAdapter) binding.trendingRv.getAdapter()).submitList(items);
                });
            });
            binding.tabPopular.setOnClickListener(v -> {
                setTabActive(false);
                vm.getTopRated().observe(getViewLifecycleOwner(), items -> {
                    if (binding != null && binding.trendingRv.getAdapter() != null)
                        ((ContentAdapter) binding.trendingRv.getAdapter()).submitList(items);
                });
            });

            // Trending
            ContentAdapter trendingAdapter = new ContentAdapter(this::openWatch);
            binding.trendingRv.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.trendingRv.setAdapter(trendingAdapter);
            vm.getPopularMovies().observe(getViewLifecycleOwner(), trendingAdapter::submitList);

            // Latest Movies
            ContentAdapter latestMoviesAdapter = new ContentAdapter(this::openWatch);
            binding.latestMoviesRv.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.latestMoviesRv.setAdapter(latestMoviesAdapter);
            vm.getNowPlaying().observe(getViewLifecycleOwner(), latestMoviesAdapter::submitList);

            // Latest TV
            ContentAdapter latestTvAdapter = new ContentAdapter(this::openWatch);
            binding.latestTvRv.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.latestTvRv.setAdapter(latestTvAdapter);
            vm.getPopularTV().observe(getViewLifecycleOwner(), latestTvAdapter::submitList);

            // Search
            binding.searchBtn.setOnClickListener(v -> {
                searchOpen = !searchOpen;
                binding.searchBar.setVisibility(searchOpen ? View.VISIBLE : View.GONE);
                if (searchOpen) binding.searchBar.requestFocus();
            });
            binding.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                public boolean onQueryTextSubmit(String q) {
                    if (q != null && !q.trim().isEmpty()) {
                        Intent i = new Intent(getContext(), SearchActivity.class);
                        i.putExtra("query", q.trim());
                        startActivity(i);
                        binding.searchBar.setVisibility(View.GONE);
                        searchOpen = false;
                    }
                    return true;
                }
                public boolean onQueryTextChange(String q) { return false; }
            });

            binding.viewAllMovies.setOnClickListener(v -> switchTab(R.id.nav_movies));
            binding.viewAllTv.setOnClickListener(v -> switchTab(R.id.nav_tv));

            vm.getError().observe(getViewLifecycleOwner(), err -> {
                if (err != null) Log.e(TAG, "VM error: " + err);
            });

        } catch (Exception e) {
            Log.e(TAG, "onViewCreated error: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContinue();
    }

    private void refreshContinue() {
        if (binding == null || getContext() == null) return;
        try {
            List<ContentItem> list = LibraryManager.get(requireContext(), LibraryManager.CONTINUE);
            binding.continueSection.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
            if (!list.isEmpty()) {
                ContentAdapter ca = new ContentAdapter(this::openWatch);
                binding.continueRv.setLayoutManager(
                    new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                binding.continueRv.setAdapter(ca);
                ca.submitList(list);
            }
        } catch (Exception e) {
            Log.e(TAG, "refreshContinue error: " + e.getMessage());
        }
    }

    private void toggleWatchlist(ContentItem item) {
        if (getContext() == null) return;
        boolean inWl = LibraryManager.isIn(requireContext(), item.id,
            item.mediaType, LibraryManager.WATCHLIST);
        if (inWl) {
            LibraryManager.remove(requireContext(), item.id, item.mediaType, LibraryManager.WATCHLIST);
        } else {
            LibraryManager.add(requireContext(), item, LibraryManager.WATCHLIST);
        }
    }

    private void setTabActive(boolean trendingActive) {
        if (binding == null) return;
        binding.tabTrending.setTextColor(trendingActive ? 0xFFCFFF04 : 0xFF888888);
        binding.tabPopular.setTextColor(!trendingActive ? 0xFFCFFF04 : 0xFF888888);
    }

    private void openWatch(ContentItem item) {
        Intent i = new Intent(getContext(), WatchActivity.class);
        i.putExtra("type", item.mediaType);
        i.putExtra("id",   item.id);
        startActivity(i);
    }

    private void switchTab(int navId) {
        if (getActivity() != null) {
            com.fountainhome.streaming.databinding.ActivityMainBinding mainBinding =
                com.fountainhome.streaming.databinding.ActivityMainBinding.bind(
                    requireActivity().findViewById(android.R.id.content).getRootView());
        }
        // Use bottom nav directly
        requireActivity().runOnUiThread(() -> {
            View nav = requireActivity().findViewById(com.fountainhome.streaming.R.id.bottom_nav);
            if (nav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                ((com.google.android.material.bottomnavigation.BottomNavigationView) nav)
                    .setSelectedItemId(navId);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
