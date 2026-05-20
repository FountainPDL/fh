package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.fountainhome.streaming.databinding.ActivitySearchBinding;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.SearchViewModel;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private SearchViewModel vm;
    private ContentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vm = new ViewModelProvider(this).get(SearchViewModel.class);
        adapter = new ContentAdapter(item -> {
            Intent i = new Intent(this, WatchActivity.class);
            i.putExtra("type", item.mediaType);
            i.putExtra("id",   item.id);
            startActivity(i);
        });

        binding.resultsRv.setLayoutManager(new GridLayoutManager(this, 3));
        binding.resultsRv.setAdapter(adapter);
        binding.backBtn.setOnClickListener(v -> finish());

        vm.getResults().observe(this, items -> {
            adapter.submitList(items);
            if (items == null || items.isEmpty()) {
                binding.emptyText.setVisibility(View.VISIBLE);
                binding.emptyText.setText("No results found");
            } else {
                binding.emptyText.setVisibility(View.GONE);
            }
        });

        binding.searchInput.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String q) { doSearch(q); return true; }
            public boolean onQueryTextChange(String q) {
                if (q != null && q.length() >= 2) doSearch(q);
                return false;
            }
        });

        String q = getIntent().getStringExtra("query");
        if (q != null && !q.isEmpty()) {
            binding.searchInput.setQuery(q, false);
            doSearch(q);
        }
    }

    private void doSearch(String q) {
        if (q == null || q.trim().isEmpty()) return;
        binding.emptyText.setText("Searching...");
        binding.emptyText.setVisibility(View.VISIBLE);
        vm.search(q.trim());
    }
}
