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

    @Nullable
    @Override
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

        binding.tabMovie.setOnClickListener(v ->   { currentList = LibraryManager.WATCHLIST;  loadList("Movie"); setTab(0); });
        binding.tabTv.setOnClickListener(v ->      { currentList = LibraryManager.FAVORITES;  loadList("TV Series"); setTab(1); });
        binding.tabContinue.setOnClickListener(v ->{ currentList = LibraryManager.CONTINUE;   loadList("Continue"); setTab(2); });

        setTab(0);
        loadList("Movie");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadList(binding.pageTitle.getText().toString());
    }

    private void loadList(String title) {
        binding.pageTitle.setText("Watch List");
        List<ContentItem> items = LibraryManager.get(requireContext(), currentList);
        adapter.submitList(items);
        binding.emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.emptyText.setText("No " + title.toLowerCase() + " saved yet");
    }

    private void setTab(int a) {
        int g = 0xFFCFFF04, gr = 0xFF888888;
        binding.tabMovie.setTextColor(a == 0 ? g : gr);
        binding.tabTv.setTextColor(a == 1 ? g : gr);
        binding.tabContinue.setTextColor(a == 2 ? g : gr);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
