package com.fountainhome.streaming.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.api.TMDBClient;
import com.fountainhome.streaming.databinding.FragmentBrowseBinding;
import com.fountainhome.streaming.ui.SearchActivity;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoviesFragment extends Fragment {

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

        binding.pageTitle.setText("Movies");
        binding.tabLatest.setText("Latest");
        binding.tabTrending.setText("Trending");
        binding.tabPopular.setText("Popular");

        adapter = new ContentAdapter(item -> {
            Intent i = new Intent(getContext(), WatchActivity.class);
            i.putExtra("type", "movie");
            i.putExtra("id",   item.id);
            startActivity(i);
        });
        binding.contentRv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.contentRv.setAdapter(adapter);

        binding.tabLatest.setOnClickListener(v ->   { activeTab = "latest";   loadMovies(); setTab(0); });
        binding.tabTrending.setOnClickListener(v -> { activeTab = "trending"; loadMovies(); setTab(1); });
        binding.tabPopular.setOnClickListener(v ->  { activeTab = "popular";  loadMovies(); setTab(2); });

        binding.searchBtn.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), SearchActivity.class);
            startActivity(i);
        });

        binding.backBtn.setVisibility(View.GONE);

        setTab(0);
        loadMovies();
    }

    private void setTab(int active) {
        int green = 0xFFCFFF04, grey = 0xFF888888;
        binding.tabLatest.setTextColor(active == 0 ? green : grey);
        binding.tabTrending.setTextColor(active == 1 ? green : grey);
        binding.tabPopular.setTextColor(active == 2 ? green : grey);
    }

    private void loadMovies() {
        binding.progressBar.setVisibility(View.VISIBLE);
        Call<Models.PagedResponse<Models.MovieResult>> call;
        switch (activeTab) {
            case "trending": call = TMDBClient.get().getTopRatedMovies(1); break;
            case "popular":  call = TMDBClient.get().getPopularMovies(1);  break;
            default:         call = TMDBClient.get().getNowPlaying(1);     break;
        }
        call.enqueue(new Callback<Models.PagedResponse<Models.MovieResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                binding.progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null && r.body().results != null) {
                    List<ContentItem> items = new ArrayList<>();
                    for (Models.MovieResult m : r.body().results) {
                        ContentItem ci = new ContentItem();
                        ci.id = m.id; ci.title = m.title; ci.posterPath = m.poster_path;
                        ci.backdropPath = m.backdrop_path; ci.rating = m.vote_average;
                        ci.mediaType = "movie"; ci.releaseDate = m.release_date;
                        items.add(ci);
                    }
                    adapter.submitList(items);
                }
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                  @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
