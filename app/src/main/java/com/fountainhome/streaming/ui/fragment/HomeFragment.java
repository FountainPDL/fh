package com.fountainhome.streaming.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

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
    private boolean searchOpen = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HomeViewModel vm = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // Featured hero
        vm.getFeatured().observe(getViewLifecycleOwner(), item -> {
            if (item == null) return;
            Glide.with(this).load(SourceGenerator.imageUrl(item.backdropPath, "w780"))
                .centerCrop().into(binding.featuredBanner);
            binding.featuredTitle.setText(item.displayTitle());
            binding.featuredGenre.setText("★ " + String.format("%.1f", item.rating)
                + "  ·  " + (item.mediaType.equals("movie") ? "Movie" : "TV Series"));
            binding.watchNowBtn.setOnClickListener(v -> openWatch(item));
            binding.detailBtn.setOnClickListener(v -> openWatch(item));
            binding.featuredAdd.setOnClickListener(v -> {
                boolean inWl = LibraryManager.isIn(requireContext(), item.id,
                    item.mediaType, LibraryManager.WATCHLIST);
                if (inWl) {
                    LibraryManager.remove(requireContext(), item.id,
                        item.mediaType, LibraryManager.WATCHLIST);
                    binding.featuredAdd.setText("+ Add");
                    Toast.makeText(getContext(), "Removed from watchlist", Toast.LENGTH_SHORT).show();
                } else {
                    LibraryManager.add(requireContext(), item, LibraryManager.WATCHLIST);
                    binding.featuredAdd.setText("✓ Added");
                    Toast.makeText(getContext(), "Added to watchlist", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Continue watching
        refreshContinue();

        // Trending / Popular tabs
        binding.tabTrending.setOnClickListener(v -> {
            setTabActive(true);
            vm.getPopularMovies().observe(getViewLifecycleOwner(), items ->
                ((ContentAdapter) binding.trendingRv.getAdapter()).submitList(items));
        });
        binding.tabPopular.setOnClickListener(v -> {
            setTabActive(false);
            vm.getTopRated().observe(getViewLifecycleOwner(), items ->
                ((ContentAdapter) binding.trendingRv.getAdapter()).submitList(items));
        });

        // Trending row (default)
        ContentAdapter trendingAdapter = new ContentAdapter(this::openWatch);
        binding.trendingRv.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.trendingRv.setAdapter(trendingAdapter);
        vm.getPopularMovies().observe(getViewLifecycleOwner(), trendingAdapter::submitList);

        // Latest movies row
        ContentAdapter latestMoviesAdapter = new ContentAdapter(this::openWatch);
        binding.latestMoviesRv.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.latestMoviesRv.setAdapter(latestMoviesAdapter);
        vm.getNowPlaying().observe(getViewLifecycleOwner(), latestMoviesAdapter::submitList);

        // Latest TV row
        ContentAdapter latestTvAdapter = new ContentAdapter(this::openWatch);
        binding.latestTvRv.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.latestTvRv.setAdapter(latestTvAdapter);
        vm.getPopularTV().observe(getViewLifecycleOwner(), latestTvAdapter::submitList);

        // Search toggle
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

        // View all buttons
        binding.viewAllMovies.setOnClickListener(v -> switchToMovies());
        binding.viewAllTv.setOnClickListener(v -> switchToTV());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContinue();
    }

    private void refreshContinue() {
        List<ContentItem> continueList = LibraryManager.get(requireContext(), LibraryManager.CONTINUE);
        if (continueList.isEmpty()) {
            binding.continueSection.setVisibility(View.GONE);
        } else {
            binding.continueSection.setVisibility(View.VISIBLE);
            ContentAdapter continueAdapter = new ContentAdapter(item -> {
                // Resume from last position
                openWatch(item);
            });
            binding.continueRv.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.continueRv.setAdapter(continueAdapter);
            continueAdapter.submitList(continueList);
        }
    }

    private void setTabActive(boolean trendingActive) {
        binding.tabTrending.setTextColor(trendingActive ? 0xFFCFFF04 : 0xFF888888);
        binding.tabTrending.setTypeface(null, trendingActive ?
            android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        binding.tabPopular.setTextColor(!trendingActive ? 0xFFCFFF04 : 0xFF888888);
        binding.tabPopular.setTypeface(null, !trendingActive ?
            android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void openWatch(ContentItem item) {
        Intent i = new Intent(getContext(), WatchActivity.class);
        i.putExtra("type", item.mediaType);
        i.putExtra("id",   item.id);
        startActivity(i);
    }

    private void switchToMovies() {
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(com.fountainhome.streaming.R.id.fragment_container, new MoviesFragment())
            .commit();
    }

    private void switchToTV() {
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(com.fountainhome.streaming.R.id.fragment_container, new TVFragment())
            .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
