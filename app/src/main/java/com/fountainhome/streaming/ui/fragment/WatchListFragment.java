package com.fountainhome.streaming.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fountainhome.streaming.databinding.FragmentWatchlistBinding;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.util.List;

public class WatchListFragment extends Fragment {

    private FragmentWatchlistBinding binding;
    private ContentAdapter adapter;
    private String currentList = LibraryManager.WATCHLIST;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWatchlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ContentAdapter(item -> {
            Intent i = new Intent(getContext(), WatchActivity.class);
            i.putExtra("type", item.mediaType); i.putExtra("id", item.id);
            startActivity(i);
        });
        binding.contentRv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.contentRv.setAdapter(adapter);

        // Tab click handlers
        binding.tabMovie.setOnClickListener(v  -> loadList(LibraryManager.WATCHLIST,  "Movie"));
        binding.tabTv.setOnClickListener(v     -> loadList(LibraryManager.FAVORITES,  "Favorites ♥"));
        binding.tabContinue.setOnClickListener(v -> loadList(LibraryManager.CONTINUE, "Continue"));

        // Rename tabs
        binding.tabMovie.setText("Watch List");
        binding.tabTv.setText("♥ Favorites");
        binding.tabContinue.setText("Continue");

        loadList(LibraryManager.WATCHLIST, "Watch List");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh current list on return
        String title = binding.pageTitle.getText().toString();
        loadList(currentList, title);
    }

    private void loadList(String list, String title) {
        currentList = list;
        binding.pageTitle.setText(title);

        int active = 0xFFBB86FC, grey = 0xFF888888;
        binding.tabMovie.setTextColor(list.equals(LibraryManager.WATCHLIST)  ? active : grey);
        binding.tabTv.setTextColor(list.equals(LibraryManager.FAVORITES)     ? active : grey);
        binding.tabContinue.setTextColor(list.equals(LibraryManager.CONTINUE)? active : grey);

        List<ContentItem> items = LibraryManager.get(requireContext(), list);
        adapter.submitList(items);

        binding.emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.emptyText.setText(items.isEmpty() ? "Nothing in " + title + " yet" : "");
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
