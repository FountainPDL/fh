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
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.FragmentHomeBinding;
import com.fountainhome.streaming.service.AppPreferences;
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

        // Hero banner — crossfades between top movies
        vm.getFeatured().observe(getViewLifecycleOwner(), item -> {
            if (item == null || binding == null) return;
            Glide.with(this)
                .load(SourceGenerator.imageUrl(item.backdropPath, "w780"))
                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(600))
                .centerCrop()
                .into(binding.featuredBanner);
            binding.featuredTitle.setText(item.displayTitle());
            binding.featuredGenre.setText(
                "★ " + String.format("%.1f", item.rating)
                + "  ·  " + ("movie".equals(item.mediaType) ? "Movie" : "TV")
                + (item.year().isEmpty() ? "" : "  ·  " + item.year()));
            binding.watchNowBtn.setOnClickListener(v -> openWatch(item));
            binding.featuredBanner.setOnClickListener(v -> openWatch(item));
            binding.detailBtn.setOnClickListener(v -> openWatch(item));
            binding.featuredAdd.setOnClickListener(v -> {
                boolean inWl = LibraryManager.isIn(requireContext(),
                    item.id, item.mediaType, LibraryManager.WATCHLIST);
                if (inWl) LibraryManager.remove(requireContext(),
                    item.id, item.mediaType, LibraryManager.WATCHLIST);
                else LibraryManager.add(requireContext(), item, LibraryManager.WATCHLIST);
                binding.featuredAdd.setText(inWl ? "+" : "✓");
            });
        });

        // Trending tab toggle
        binding.tabTrending.setOnClickListener(v -> {
            setTab(true);
            vm.getPopularMovies().observe(getViewLifecycleOwner(), items -> {
                if (binding != null && binding.trendingRv.getAdapter() != null)
                    ((ContentAdapter) binding.trendingRv.getAdapter()).submitList(items);
            });
        });
        binding.tabPopular.setOnClickListener(v -> {
            setTab(false);
            vm.getTopRated().observe(getViewLifecycleOwner(), items -> {
                if (binding != null && binding.trendingRv.getAdapter() != null)
                    ((ContentAdapter) binding.trendingRv.getAdapter()).submitList(items);
            });
        });

        // Rows
        setupRow(binding.trendingRv,    vm.getPopularMovies());
        setupRow(binding.latestMoviesRv, vm.getNowPlaying());
        setupRow(binding.latestTvRv,    vm.getPopularTV());

        // Continue watching
        refreshContinue();

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

        // View all
        binding.viewAllMovies.setOnClickListener(v -> switchNav(R.id.nav_movies));
        binding.viewAllTv.setOnClickListener(v -> switchNav(R.id.nav_tv));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContinue();
    }

    private void setupRow(androidx.recyclerview.widget.RecyclerView rv,
                          androidx.lifecycle.LiveData<List<ContentItem>> data) {
        ContentAdapter adapter = new ContentAdapter(this::openWatch);
        rv.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
        data.observe(getViewLifecycleOwner(), items -> {
            if (items != null) adapter.submitList(items);
        });
    }

    private void refreshContinue() {
        if (binding == null || getContext() == null) return;
        List<ContentItem> list = LibraryManager.get(requireContext(), LibraryManager.CONTINUE);
        boolean show = !list.isEmpty()
            && AppPreferences.getShowContinue(requireContext());
        binding.continueSection.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            ContentAdapter ca = new ContentAdapter(this::openWatch);
            binding.continueRv.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.continueRv.setAdapter(ca);
            ca.submitList(list);
        }
    }

    private void setTab(boolean trending) {
        if (binding == null) return;
        int accent = AppPreferences.getAccentColor(requireContext());
        binding.tabTrending.setTextColor(trending ? accent : 0xFF888888);
        binding.tabPopular.setTextColor(!trending ? accent : 0xFF888888);
    }

    private void openWatch(ContentItem item) {
        Intent i = new Intent(getContext(), WatchActivity.class);
        i.putExtra("type", item.mediaType);
        i.putExtra("id",   item.id);
        startActivity(i);
    }

    private void switchNav(int navItemId) {
        if (getActivity() == null) return;
        com.google.android.material.bottomnavigation.BottomNavigationView nav =
            getActivity().findViewById(R.id.bottom_nav);
        if (nav != null) nav.setSelectedItemId(navItemId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
