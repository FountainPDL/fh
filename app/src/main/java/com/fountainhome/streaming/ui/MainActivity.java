package com.fountainhome.streaming.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.ActivityMainBinding;
import com.fountainhome.streaming.ui.fragment.HomeFragment;
import com.fountainhome.streaming.ui.fragment.MoviesFragment;
import com.fountainhome.streaming.ui.fragment.TVFragment;
import com.fountainhome.streaming.ui.fragment.WatchListFragment;
import com.fountainhome.streaming.ui.fragment.MoreFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "home");
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)      { loadFragment(new HomeFragment(),      "home");      return true; }
            if (id == R.id.nav_movies)    { loadFragment(new MoviesFragment(),    "movies");    return true; }
            if (id == R.id.nav_tv)        { loadFragment(new TVFragment(),        "tv");        return true; }
            if (id == R.id.nav_watchlist) { loadFragment(new WatchListFragment(), "watchlist"); return true; }
            if (id == R.id.nav_more)      { loadFragment(new MoreFragment(),      "more");      return true; }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit();
    }
}
