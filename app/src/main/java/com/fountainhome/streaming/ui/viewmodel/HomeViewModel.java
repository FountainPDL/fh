package com.fountainhome.streaming.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.api.TMDBClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<List<ContentItem>> popularMovies = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> popularTV     = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> topRated      = new MutableLiveData<>();
    private final MutableLiveData<String>            error         = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application app) {
        super(app);
        loadAll();
    }

    public void loadAll() {
        loadPopularMovies();
        loadPopularTV();
        loadTopRated();
    }

    private void loadPopularMovies() {
        TMDBClient.get().getPopularMovies(1).enqueue(new Callback<Models.PagedResponse<Models.MovieResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    popularMovies.postValue(movieItemsFrom(r.body().results));
                }
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                  @NonNull Throwable t) {
                error.postValue(t.getMessage());
            }
        });
    }

    private void loadPopularTV() {
        TMDBClient.get().getPopularTV(1).enqueue(new Callback<Models.PagedResponse<Models.TVResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.TVResult>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    popularTV.postValue(tvItemsFrom(r.body().results));
                }
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                  @NonNull Throwable t) {
                error.postValue(t.getMessage());
            }
        });
    }

    private void loadTopRated() {
        TMDBClient.get().getTopRatedMovies(1).enqueue(new Callback<Models.PagedResponse<Models.MovieResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    topRated.postValue(movieItemsFrom(r.body().results));
                }
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                  @NonNull Throwable t) {}
        });
    }

    private List<ContentItem> movieItemsFrom(List<Models.MovieResult> results) {
        List<ContentItem> items = new ArrayList<>();
        for (Models.MovieResult r : results) {
            ContentItem ci = new ContentItem();
            ci.id = r.id;
            ci.title = r.title;
            ci.posterPath = r.poster_path;
            ci.backdropPath = r.backdrop_path;
            ci.mediaType = "movie";
            ci.rating = r.vote_average;
            items.add(ci);
        }
        return items;
    }

    private List<ContentItem> tvItemsFrom(List<Models.TVResult> results) {
        List<ContentItem> items = new ArrayList<>();
        for (Models.TVResult r : results) {
            ContentItem ci = new ContentItem();
            ci.id = r.id;
            ci.name = r.name;
            ci.posterPath = r.poster_path;
            ci.backdropPath = r.backdrop_path;
            ci.mediaType = "tv";
            ci.rating = r.vote_average;
            items.add(ci);
        }
        return items;
    }

    public LiveData<List<ContentItem>> getPopularMovies() { return popularMovies; }
    public LiveData<List<ContentItem>> getPopularTV()     { return popularTV; }
    public LiveData<List<ContentItem>> getTopRated()      { return topRated; }
    public LiveData<String>            getError()         { return error; }
}
