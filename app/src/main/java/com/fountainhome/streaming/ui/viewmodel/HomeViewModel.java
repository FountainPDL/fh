package com.fountainhome.streaming.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

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

    private final MutableLiveData<ContentItem>       featured      = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> popularMovies = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> popularTV     = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> topRated      = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> nowPlaying    = new MutableLiveData<>();
    private final MutableLiveData<String>            error         = new MutableLiveData<>();

    // Hero rotation
    private final Handler heroHandler = new Handler(Looper.getMainLooper());
    private List<ContentItem> heroItems = new ArrayList<>();
    private int heroIndex = 0;
    private Runnable heroRunnable;

    public HomeViewModel(@NonNull Application app) {
        super(app);
        loadAll();
    }

    public void loadAll() {
        loadPopularMovies();
        loadPopularTV();
        loadTopRated();
        loadNowPlaying();
    }

    private void loadPopularMovies() {
        TMDBClient.get().getPopularMovies(1).enqueue(
            new Callback<Models.PagedResponse<Models.MovieResult>>() {
                public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                       @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                    if (r.isSuccessful() && r.body() != null && r.body().results != null) {
                        List<ContentItem> items = movieItems(r.body().results);
                        popularMovies.postValue(items);
                        // Start hero carousel with top 10
                        heroItems = items.size() > 10 ? items.subList(0, 10) : items;
                        startHeroRotation();
                    } else { error.postValue("Failed to load: " + r.code()); }
                }
                public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                      @NonNull Throwable t) { error.postValue(t.getMessage()); }
            });
    }

    private void startHeroRotation() {
        if (heroItems.isEmpty()) return;
        featured.postValue(heroItems.get(0));
        heroRunnable = new Runnable() {
            @Override public void run() {
                if (!heroItems.isEmpty()) {
                    heroIndex = (heroIndex + 1) % heroItems.size();
                    featured.postValue(heroItems.get(heroIndex));
                }
                heroHandler.postDelayed(this, 5000); // rotate every 5s
            }
        };
        heroHandler.postDelayed(heroRunnable, 5000);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (heroRunnable != null) heroHandler.removeCallbacks(heroRunnable);
    }

    private void loadPopularTV() {
        TMDBClient.get().getPopularTV(1).enqueue(
            new Callback<Models.PagedResponse<Models.TVResult>>() {
                public void onResponse(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                       @NonNull Response<Models.PagedResponse<Models.TVResult>> r) {
                    if (r.isSuccessful() && r.body() != null) popularTV.postValue(tvItems(r.body().results));
                }
                public void onFailure(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                      @NonNull Throwable t) {}
            });
    }

    private void loadTopRated() {
        TMDBClient.get().getTopRatedMovies(1).enqueue(
            new Callback<Models.PagedResponse<Models.MovieResult>>() {
                public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                       @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                    if (r.isSuccessful() && r.body() != null) topRated.postValue(movieItems(r.body().results));
                }
                public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                      @NonNull Throwable t) {}
            });
    }

    private void loadNowPlaying() {
        TMDBClient.get().getNowPlaying(1).enqueue(
            new Callback<Models.PagedResponse<Models.MovieResult>>() {
                public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                       @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                    if (r.isSuccessful() && r.body() != null) nowPlaying.postValue(movieItems(r.body().results));
                }
                public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                      @NonNull Throwable t) {}
            });
    }

    private List<ContentItem> movieItems(List<Models.MovieResult> results) {
        List<ContentItem> items = new ArrayList<>();
        if (results == null) return items;
        for (Models.MovieResult r : results) {
            ContentItem ci = new ContentItem();
            ci.id = r.id; ci.title = r.title; ci.posterPath = r.poster_path;
            ci.backdropPath = r.backdrop_path; ci.rating = r.vote_average;
            ci.mediaType = "movie"; ci.releaseDate = r.release_date;
            items.add(ci);
        }
        return items;
    }

    private List<ContentItem> tvItems(List<Models.TVResult> results) {
        List<ContentItem> items = new ArrayList<>();
        if (results == null) return items;
        for (Models.TVResult r : results) {
            ContentItem ci = new ContentItem();
            ci.id = r.id; ci.name = r.name; ci.posterPath = r.poster_path;
            ci.backdropPath = r.backdrop_path; ci.rating = r.vote_average;
            ci.mediaType = "tv";
            items.add(ci);
        }
        return items;
    }

    public LiveData<ContentItem>       getFeatured()      { return featured; }
    public LiveData<List<ContentItem>> getPopularMovies() { return popularMovies; }
    public LiveData<List<ContentItem>> getPopularTV()     { return popularTV; }
    public LiveData<List<ContentItem>> getTopRated()      { return topRated; }
    public LiveData<List<ContentItem>> getNowPlaying()    { return nowPlaying; }
    public LiveData<String>            getError()         { return error; }
}
