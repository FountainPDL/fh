package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ActivityMainBinding;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.player.PlayerActivity;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.fountainhome.streaming.ui.viewmodel.HomeViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean searchVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        HomeViewModel vm = new ViewModelProvider(this).get(HomeViewModel.class);

        // Popular Movies — horizontal scroll
        ContentAdapter moviesAdapter = new ContentAdapter(this::openContent);
        binding.popularMoviesRv.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.popularMoviesRv.setAdapter(moviesAdapter);
        vm.getPopularMovies().observe(this, items -> {
            moviesAdapter.submitList(items);
            // Use first item's backdrop as featured banner
            if (items != null && !items.isEmpty() && items.get(0).backdropPath != null) {
                Glide.with(this)
                    .load(SourceGenerator.imageUrl(items.get(0).backdropPath, "w780"))
                    .centerCrop()
                    .into(binding.featuredBanner);
                binding.featuredBanner.setOnClickListener(v -> openContent(items.get(0)));
            }
        });

        // Popular TV — horizontal scroll
        ContentAdapter tvAdapter = new ContentAdapter(this::openContent);
        binding.popularTvRv.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.popularTvRv.setAdapter(tvAdapter);
        vm.getPopularTV().observe(this, tvAdapter::submitList);

        // Top Rated — horizontal scroll
        ContentAdapter topAdapter = new ContentAdapter(this::openContent);
        binding.topRatedRv.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.topRatedRv.setAdapter(topAdapter);
        vm.getTopRated().observe(this, topAdapter::submitList);

        // Toggle search bar
        binding.searchIconBtn.setOnClickListener(v -> {
            searchVisible = !searchVisible;
            binding.searchView.setVisibility(searchVisible ? View.VISIBLE : View.GONE);
            if (searchVisible) binding.searchView.requestFocus();
        });

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String q) {
                if (q != null && !q.trim().isEmpty()) {
                    Intent i = new Intent(MainActivity.this, SearchActivity.class);
                    i.putExtra("query", q.trim());
                    startActivity(i);
                    binding.searchView.setVisibility(View.GONE);
                    searchVisible = false;
                }
                return true;
            }
            public boolean onQueryTextChange(String q) { return false; }
        });

        binding.libraryBtn.setOnClickListener(v ->
            startActivity(new Intent(this, LibraryActivity.class)));
        binding.settingsBtn.setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void openContent(ContentItem item) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("type",  item.mediaType);
        i.putExtra("id",    item.id);
        i.putExtra("title", item.displayTitle());
        startActivity(i);
    }
}
