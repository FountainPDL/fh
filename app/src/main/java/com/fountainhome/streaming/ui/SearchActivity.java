package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fountainhome.streaming.databinding.ActivitySearchBinding;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.player.PlayerActivity;
import com.fountainhome.streaming.ui.viewmodel.SearchViewModel;

public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySearchBinding binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String query = getIntent().getStringExtra("query");
        binding.queryText.setText("Results: " + query);
        binding.backBtn.setOnClickListener(v -> finish());

        SearchViewModel vm = new ViewModelProvider(this).get(SearchViewModel.class);
        ContentAdapter adapter = new ContentAdapter(item -> {
            Intent i = new Intent(this, PlayerActivity.class);
            i.putExtra("type",  item.mediaType);
            i.putExtra("id",    item.id);
            i.putExtra("title", item.displayTitle());
            startActivity(i);
        });

        binding.resultsRv.setLayoutManager(new GridLayoutManager(this, 3));
        binding.resultsRv.setAdapter(adapter);
        vm.getResults().observe(this, adapter::submitList);

        if (query != null) vm.search(query);
    }
}
