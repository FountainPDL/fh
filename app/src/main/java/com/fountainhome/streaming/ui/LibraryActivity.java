package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import com.fountainhome.streaming.databinding.ActivityLibraryBinding;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;

public class LibraryActivity extends AppCompatActivity {

    private ActivityLibraryBinding binding;
    private ContentAdapter adapter;
    private String currentList = LibraryManager.FAVORITES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backBtn.setOnClickListener(v -> finish());

        adapter = new ContentAdapter(item -> {
            Intent i = new Intent(this, WatchActivity.class);
            i.putExtra("type", item.mediaType);
            i.putExtra("id",   item.id);
            startActivity(i);
        });

        binding.contentRv.setLayoutManager(new GridLayoutManager(this, 3));
        binding.contentRv.setAdapter(adapter);

        // Tab buttons
        binding.tabFavorites.setOnClickListener(v  -> loadList(LibraryManager.FAVORITES,  "Favorites"));
        binding.tabWatchlist.setOnClickListener(v  -> loadList(LibraryManager.WATCHLIST,  "Watchlist"));
        binding.tabContinue.setOnClickListener(v   -> loadList(LibraryManager.CONTINUE,   "Continue Watching"));
        binding.tabDownloads.setOnClickListener(v  -> loadList(LibraryManager.DOWNLOADED, "Downloads"));

        loadList(LibraryManager.FAVORITES, "Favorites");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadList(currentList, binding.sectionTitle.getText().toString());
    }

    private void loadList(String list, String title) {
        currentList = list;
        binding.sectionTitle.setText(title);

        // Highlight active tab
        int normal   = 0x40FFFFFF;
        int active   = 0xFFBB86FC;
        binding.tabFavorites.setBackgroundColor(list.equals(LibraryManager.FAVORITES)  ? active : normal);
        binding.tabWatchlist.setBackgroundColor(list.equals(LibraryManager.WATCHLIST)  ? active : normal);
        binding.tabContinue.setBackgroundColor(list.equals(LibraryManager.CONTINUE)    ? active : normal);
        binding.tabDownloads.setBackgroundColor(list.equals(LibraryManager.DOWNLOADED) ? active : normal);

        java.util.List<com.fountainhome.streaming.ui.viewmodel.ContentItem> items =
            LibraryManager.get(this, list);
        adapter.submitList(items);

        binding.emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        String emptyMsg = "No " + title.toLowerCase() + " yet";
        binding.emptyText.setText(emptyMsg);
    }
}
