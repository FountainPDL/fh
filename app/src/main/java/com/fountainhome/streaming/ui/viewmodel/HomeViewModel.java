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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/** Data for the Netflix-style home: a hero title and several rows, each loaded independently. */
public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<List<ContentItem>> heroes = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> top10 = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> trending = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> popularTV = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> newReleases = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> topRated = new MutableLiveData<>();
    private final MutableLiveData<Boolean> failed = new MutableLiveData<>(false);
    private final ExecutorService io = Executors.newFixedThreadPool(3);

    public HomeViewModel(@NonNull Application app) { super(app); load(); }

    public void load() {
        failed.postValue(false);
        io.execute(() -> {
            try {
                Response<Models.PagedResponse<Models.SearchResult>> r =
                    TMDBClient.get().getTrending("all", "week").execute();
                List<ContentItem> all = new ArrayList<>();
                if (r.isSuccessful() && r.body() != null && r.body().results != null)
                    for (Models.SearchResult m : r.body().results) {
                        if (!"movie".equals(m.media_type) && !"tv".equals(m.media_type)) continue;
                        all.add(fromSearch(m));
                    }
                if (all.isEmpty()) { failed.postValue(true); return; }
                List<ContentItem> ten = new ArrayList<>(all.subList(0, Math.min(10, all.size())));
                top10.postValue(ten);
                List<ContentItem> hs = new ArrayList<>();
                for (ContentItem c : all) {
                    if (c.backdropPath != null && c.overview != null && !c.overview.isEmpty()) hs.add(c);
                    if (hs.size() >= 6) break;
                }
                if (!hs.isEmpty()) heroes.postValue(hs);
                trending.postValue(all);
            } catch (Exception e) { failed.postValue(true); }
        });
        io.execute(() -> {
            try {
                Response<Models.PagedResponse<Models.TVResult>> r = TMDBClient.get().getPopularTV(1).execute();
                if (r.isSuccessful() && r.body() != null) popularTV.postValue(tv(r.body().results));
            } catch (Exception ignored) { }
        });
        io.execute(() -> {
            try {
                Response<Models.PagedResponse<Models.MovieResult>> r = TMDBClient.get().getNowPlaying(1).execute();
                if (r.isSuccessful() && r.body() != null) newReleases.postValue(movies(r.body().results));
                Response<Models.PagedResponse<Models.MovieResult>> t = TMDBClient.get().getTopRatedMovies(1).execute();
                if (t.isSuccessful() && t.body() != null) topRated.postValue(movies(t.body().results));
            } catch (Exception ignored) { }
        });
    }

    private static ContentItem fromSearch(Models.SearchResult m) {
        ContentItem c = new ContentItem();
        c.id = m.id; c.mediaType = m.media_type;
        c.title = m.title; c.name = m.name;
        c.posterPath = m.poster_path; c.backdropPath = m.backdrop_path;
        c.overview = m.overview; c.rating = m.vote_average;
        c.releaseDate = "movie".equals(m.media_type) ? m.release_date : m.first_air_date;
        return c;
    }

    private static List<ContentItem> movies(List<Models.MovieResult> rs) {
        List<ContentItem> l = new ArrayList<>();
        if (rs == null) return l;
        for (Models.MovieResult r : rs) {
            ContentItem c = new ContentItem();
            c.id = r.id; c.title = r.title; c.posterPath = r.poster_path; c.backdropPath = r.backdrop_path;
            c.rating = r.vote_average; c.overview = r.overview; c.releaseDate = r.release_date; c.mediaType = "movie";
            l.add(c);
        }
        return l;
    }

    private static List<ContentItem> tv(List<Models.TVResult> rs) {
        List<ContentItem> l = new ArrayList<>();
        if (rs == null) return l;
        for (Models.TVResult r : rs) {
            ContentItem c = new ContentItem();
            c.id = r.id; c.name = r.name; c.posterPath = r.poster_path; c.backdropPath = r.backdrop_path;
            c.rating = r.vote_average; c.overview = r.overview; c.releaseDate = r.first_air_date; c.mediaType = "tv";
            l.add(c);
        }
        return l;
    }

    @Override protected void onCleared() { io.shutdown(); }

    public LiveData<List<ContentItem>> getHeroes() { return heroes; }
    public LiveData<List<ContentItem>> getTop10() { return top10; }
    public LiveData<List<ContentItem>> getTrending() { return trending; }
    public LiveData<List<ContentItem>> getPopularTV() { return popularTV; }
    public LiveData<List<ContentItem>> getNewReleases() { return newReleases; }
    public LiveData<List<ContentItem>> getTopRated() { return topRated; }
    public LiveData<Boolean> getFailed() { return failed; }
}
