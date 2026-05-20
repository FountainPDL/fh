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

import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.api.TMDBClient;
import com.fountainhome.streaming.databinding.FragmentBrowseBinding;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TVFragment extends Fragment {

    private FragmentBrowseBinding binding;
    private ContentAdapter adapter;
    private String activeTab = "latest";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBrowseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.pageTitle.setText("TV Series");
        binding.tabLatest.setText("Latest");
        binding.tabTrending.setText("Trending");
        binding.tabPopular.setText("Popular");
        binding.backBtn.setVisibility(View.GONE);

        adapter = new ContentAdapter(item -> {
            Intent i = new Intent(getContext(), WatchActivity.class);
            i.putExtra("type", "tv"); i.putExtra("id", item.id);
            startActivity(i);
        });
        binding.contentRv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.contentRv.setAdapter(adapter);

        binding.tabLatest.setOnClickListener(v ->   { activeTab = "latest";   loadTV(); setTab(0); });
        binding.tabTrending.setOnClickListener(v -> { activeTab = "trending"; loadTV(); setTab(1); });
        binding.tabPopular.setOnClickListener(v ->  { activeTab = "popular";  loadTV(); setTab(2); });

        setTab(0);
        loadTV();
    }

    private void setTab(int a) {
        int g = 0xFFCFFF04, gr = 0xFF888888;
        binding.tabLatest.setTextColor(a == 0 ? g : gr);
        binding.tabTrending.setTextColor(a == 1 ? g : gr);
        binding.tabPopular.setTextColor(a == 2 ? g : gr);
    }

    private void loadTV() {
        binding.progressBar.setVisibility(View.VISIBLE);
        Call<Models.PagedResponse<Models.TVResult>> call;
        switch (activeTab) {
            case "trending": call = TMDBClient.get().getTopRatedTV(1);  break;
            case "popular":  call = TMDBClient.get().getPopularTV(1);   break;
            default:         call = TMDBClient.get().getOnAirTV(1);     break;
        }
        call.enqueue(new Callback<Models.PagedResponse<Models.TVResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.TVResult>> r) {
                binding.progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null && r.body().results != null) {
                    List<ContentItem> items = new ArrayList<>();
                    for (Models.TVResult t : r.body().results) {
                        ContentItem ci = new ContentItem();
                        ci.id = t.id; ci.name = t.name; ci.posterPath = t.poster_path;
                        ci.backdropPath = t.backdrop_path; ci.rating = t.vote_average;
                        ci.mediaType = "tv";
                        items.add(ci);
                    }
                    adapter.submitList(items);
                }
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                  @NonNull Throwable t) { binding.progressBar.setVisibility(View.GONE); }
        });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
