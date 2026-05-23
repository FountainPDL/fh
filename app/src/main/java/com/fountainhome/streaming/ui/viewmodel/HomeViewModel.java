package com.fountainhome.streaming.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

    private static final String TAG = "HomeViewModel";

    private final MutableLiveData<ContentItem>       featured      = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> popularMovies = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> popularTV     = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> topRated      = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> nowPlaying    = new MutableLiveData<>();
    private final MutableLiveData<String>            error         = new MutableLiveData<>();

    private final Handler heroHandler = new Handler(Looper.getMainLooper());
    private final List<ContentItem> heroItems = new ArrayList<>();
    private int heroIndex = 0;
    private Runnable heroRunnable;
    private boolean cleared = false;

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
        try {
            TMDBClient.get().getPopularMovies(1).enqueue(
                new Callback<Models.PagedResponse<Models.MovieResult>>() {
                    public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                           @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                        if (cleared) return;
                        try {
                            if (r.isSuccessful() && r.body() != null && r.body().results != null) {
                                List<ContentItem> items = movieItems(r.body().results);
                                popularMovies.postValue(items);
                                if (!items.isEmpty()) {
                                    heroItems.clear();
                                    int limit = Math.min(10, items.size());
                                    heroItems.addAll(items.subList(0, limit));
                                    featured.postValue(heroItems.get(0));
                                    startHeroRotation();
                                }
                            } else {
                                error.postValue("API error " + r.code());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "onResponse: " + e);
                        }
                    }
                    public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                           @NonNull Throwable t) {
                        if (!cleared) error.postValue(t.getMessage());
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "loadPopularMovies: " + e);
        }
    }

    private void startHeroRotation() {
        if (heroRunnable != null) heroHandler.removeCallbacks(heroRunnable);
        heroRunnable = new Runnable() {
            @Override public void run() {
                if (cleared || heroItems.isEmpty()) return;
                heroIndex = (heroIndex + 1) % heroItems.size();
                try { featured.postValue(heroItems.get(heroIndex)); }
                catch (Exception e) { Log.e(TAG, "hero rotate: " + e); }
                heroHandler.postDelayed(this, 5000);
            }
        };
        heroHandler.postDelayed(heroRunnable, 5000);
    }

    private void loadPopularTV() {
        try {
            TMDBClient.get().getPopularTV(1).enqueue(
                new Callback<Models.PagedResponse<Models.TVResult>>() {
                    public void onResponse(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                           @NonNull Response<Models.PagedResponse<Models.TVResult>> r) {
                        if (!cleared && r.isSuccessful() && r.body() != null)
                            popularTV.postValue(tvItems(r.body().results));
                    }
                    public void onFailure(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                          @NonNull Throwable t) {}
                });
        } catch (Exception e) { Log.e(TAG, "loadPopularTV: " + e); }
    }

    private void loadTopRated() {
        try {
            TMDBClient.get().getTopRatedMovies(1).enqueue(
                new Callback<Models.PagedResponse<Models.MovieResult>>() {
                    public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                           @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                        if (!cleared && r.isSuccessful() && r.body() != null)
                            topRated.postValue(movieItems(r.body().results));
                    }
                    public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                          @NonNull Throwable t) {}
                });
        } catch (Exception e) { Log.e(TAG, "loadTopRated: " + e); }
    }

    private void loadNowPlaying() {
        try {
            TMDBClient.get().getNowPlaying(1).enqueue(
                new Callback<Models.PagedResponse<Models.MovieResult>>() {
                    public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                           @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                        if (!cleared && r.isSuccessful() && r.body() != null)
                            nowPlaying.postValue(movieItems(r.body().results));
                    }
                    public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                          @NonNull Throwable t) {}
                });
        } catch (Exception e) { Log.e(TAG, "loadNowPlaying: " + e); }
    }

    private List<ContentItem> movieItems(List<Models.MovieResult> results) {
        List<ContentItem> items = new ArrayList<>();
        if (results == null) return items;
        for (Models.MovieResult r : results) {
            try {
                ContentItem ci = new ContentItem();
                ci.id = r.id; ci.title = r.title;
                ci.posterPath = r.poster_path; ci.backdropPath = r.backdrop_path;
                ci.rating = r.vote_average; ci.mediaType = "movie";
                ci.releaseDate = r.release_date;
                items.add(ci);
            } catch (Exception ignored) {}
        }
        return items;
    }

    private List<ContentItem> tvItems(List<Models.TVResult> results) {
        List<ContentItem> items = new ArrayList<>();
        if (results == null) return items;
        for (Models.TVResult r : results) {
            try {
                ContentItem ci = new ContentItem();
                ci.id = r.id; ci.name = r.name;
                ci.posterPath = r.poster_path; ci.backdropPath = r.backdrop_path;
                ci.rating = r.vote_average; ci.mediaType = "tv";
                items.add(ci);
            } catch (Exception ignored) {}
        }
        return items;
    }

    @Override
    protected void onCleared() {
        cleared = true;
        if (heroRunnable != null) heroHandler.removeCallbacks(heroRunnable);
        super.onCleared();
    }

    public LiveData<ContentItem>       getFeatured()      { return featured; }
    public LiveData<List<ContentItem>> getPopularMovies() { return popularMovies; }
    public LiveData<List<ContentItem>> getPopularTV()     { return popularTV; }
    public LiveData<List<ContentItem>> getTopRated()      { return topRated; }
    public LiveData<List<ContentItem>> getNowPlaying()    { return nowPlaying; }
    public LiveData<String>            getError()         { return error; }
}
