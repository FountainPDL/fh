package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fountainhome.streaming.databinding.ActivityMainBinding;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.player.PlayerActivity;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.fountainhome.streaming.ui.viewmodel.HomeViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        HomeViewModel vm = new ViewModelProvider(this).get(HomeViewModel.class);

        // Popular Movies
        ContentAdapter moviesAdapter = new ContentAdapter(this::openContent);
        binding.popularMoviesRv.setLayoutManager(new GridLayoutManager(this, 3));
        binding.popularMoviesRv.setAdapter(moviesAdapter);
        vm.getPopularMovies().observe(this, moviesAdapter::submitList);

        // Popular TV
        ContentAdapter tvAdapter = new ContentAdapter(this::openContent);
        binding.popularTvRv.setLayoutManager(new GridLayoutManager(this, 3));
        binding.popularTvRv.setAdapter(tvAdapter);
        vm.getPopularTV().observe(this, tvAdapter::submitList);

        // Top Rated
        ContentAdapter topAdapter = new ContentAdapter(this::openContent);
        binding.topRatedRv.setLayoutManager(new GridLayoutManager(this, 3));
        binding.topRatedRv.setAdapter(topAdapter);
        vm.getTopRated().observe(this, topAdapter::submitList);

        // Search
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String q) {
                if (q != null && !q.trim().isEmpty()) {
                    Intent i = new Intent(MainActivity.this, SearchActivity.class);
                    i.putExtra("query", q.trim());
                    startActivity(i);
                }
                return true;
            }
            public boolean onQueryTextChange(String q) { return false; }
        });

        // Nav buttons
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
