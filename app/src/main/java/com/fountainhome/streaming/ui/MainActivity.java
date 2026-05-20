package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ActivityMainBinding;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.fountainhome.streaming.ui.viewmodel.HomeViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean searchOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        HomeViewModel vm = new ViewModelProvider(this).get(HomeViewModel.class);

        // Featured banner
        vm.getFeatured().observe(this, item -> {
            if (item == null || item.backdropPath == null) return;
            Glide.with(this)
                .load(SourceGenerator.imageUrl(item.backdropPath, "w780"))
                .centerCrop().into(binding.featuredBanner);
            binding.featuredTitle.setText(item.displayTitle());
            binding.featuredMeta.setText("★ " + String.format("%.1f", item.rating) + "  ·  " + item.year());
            binding.featuredPlayBtn.setOnClickListener(v -> openWatch(item));
            binding.featuredBanner.setOnClickListener(v -> openWatch(item));
        });

        // Rows
        setupRow(binding.popularMoviesRv, vm, "movies");
        vm.getPopularMovies().observe(this, items -> {
            ((ContentAdapter) binding.popularMoviesRv.getAdapter()).submitList(items);
        });

        setupRow(binding.popularTvRv, vm, "tv");
        vm.getPopularTV().observe(this, items ->
            ((ContentAdapter) binding.popularTvRv.getAdapter()).submitList(items));

        setupRow(binding.topRatedRv, vm, "tr");
        vm.getTopRated().observe(this, items ->
            ((ContentAdapter) binding.topRatedRv.getAdapter()).submitList(items));

        setupRow(binding.nowPlayingRv, vm, "np");
        vm.getNowPlaying().observe(this, items ->
            ((ContentAdapter) binding.nowPlayingRv.getAdapter()).submitList(items));

        // Error
        vm.getError().observe(this, err -> {
            if (err != null) Toast.makeText(this, "Error: " + err, Toast.LENGTH_LONG).show();
        });

        // Search toggle
        binding.searchBtn.setOnClickListener(v -> {
            searchOpen = !searchOpen;
            binding.searchBar.setVisibility(searchOpen ? View.VISIBLE : View.GONE);
            if (searchOpen) binding.searchBar.requestFocus();
        });

        binding.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String q) {
                if (q != null && !q.trim().isEmpty()) {
                    Intent i = new Intent(MainActivity.this, SearchActivity.class);
                    i.putExtra("query", q.trim());
                    startActivity(i);
                    binding.searchBar.setVisibility(View.GONE);
                    searchOpen = false;
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

    private void setupRow(androidx.recyclerview.widget.RecyclerView rv,
                          HomeViewModel vm, String tag) {
        ContentAdapter adapter = new ContentAdapter(this::openWatch);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }

    private void openWatch(ContentItem item) {
        Intent i = new Intent(this, WatchActivity.class);
        i.putExtra("type", item.mediaType);
        i.putExtra("id",   item.id);
        startActivity(i);
    }
}
