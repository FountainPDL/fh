package com.fountainhome.streaming.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.api.TMDBClient;
import com.fountainhome.streaming.service.OfflineStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * Details page data. Network first; if the network is unavailable the saved offline copy is used,
 * so a page the user saved is complete (info, images, seasons, episodes, similar titles) without internet.
 */
public class WatchViewModel extends AndroidViewModel {
    public static final int SAVE_NONE = 0, SAVE_BUSY = 1, SAVE_DONE = 2, SAVE_FAILED = 3;

    private final MutableLiveData<ContentItem> content = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> similar = new MutableLiveData<>();
    private final MutableLiveData<Models.SeasonDetail> season = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> offlineMode = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> saveState = new MutableLiveData<>(SAVE_NONE);
    private final ExecutorService io = Executors.newFixedThreadPool(2);

    private String type;
    private int id;
    private OfflineStore.Saved saved;           // loaded copy, if any
    private List<ContentItem> lastSimilar = new ArrayList<>();

    public WatchViewModel(@NonNull Application app) { super(app); }

    // ------------------------------------------------------------------ loading

    public void load(String type, int id) {
        this.type = type;
        this.id = id;
        loading.postValue(true);
        io.execute(() -> {
            saved = OfflineStore.load(getApplication(), type, id);
            saveState.postValue(saved != null ? SAVE_DONE : SAVE_NONE);
            ContentItem ci = null;
            List<ContentItem> sim = new ArrayList<>();
            try {
                ci = "movie".equals(type) ? fetchMovie(id) : fetchTV(id);
                sim = fetchSimilar(type, id);
            } catch (Exception ignored) { }

            if (ci != null) {
                offlineMode.postValue(false);
                lastSimilar = sim;
                content.postValue(ci);
                similar.postValue(sim);
            } else if (saved != null) {
                offlineMode.postValue(true);
                lastSimilar = saved.similar;
                content.postValue(saved.item);
                similar.postValue(saved.similar);
            }
            loading.postValue(false);
        });
    }

    public void loadSeason(int tvId, int sn) {
        io.execute(() -> {
            Models.SeasonDetail sd = null;
            try {
                Response<Models.SeasonDetail> r = TMDBClient.get().getSeason(tvId, sn).execute();
                if (r.isSuccessful()) sd = r.body();
            } catch (Exception ignored) { }
            if (sd == null && saved != null) sd = saved.seasons.get(sn);
            if (sd != null) season.postValue(sd);
        });
    }

    // ------------------------------------------------------------------ offline copy

    /** Save the whole details page (info, images, every season and episode, similar titles). */
    public void saveOffline() {
        final ContentItem ci = content.getValue();
        if (ci == null || Integer.valueOf(SAVE_BUSY).equals(saveState.getValue())) return;
        saveState.postValue(SAVE_BUSY);
        io.execute(() -> {
            try {
                OfflineStore.Saved s = new OfflineStore.Saved();
                s.item = ci;
                s.similar = new ArrayList<>(lastSimilar);
                s.seasons = new TreeMap<>();
                if ("tv".equals(ci.mediaType)) {
                    int n = Math.max(1, ci.numberOfSeasons);
                    for (int sn = 1; sn <= n; sn++) {
                        Response<Models.SeasonDetail> r = TMDBClient.get().getSeason(ci.id, sn).execute();
                        if (r.isSuccessful() && r.body() != null) s.seasons.put(sn, r.body());
                    }
                    if (s.seasons.isEmpty()) throw new java.io.IOException("No season data");
                }
                s.savedAt = System.currentTimeMillis();
                OfflineStore.save(getApplication(), s);
                saved = s;
                saveState.postValue(SAVE_DONE);
            } catch (Exception e) {
                saveState.postValue(SAVE_FAILED);
            }
        });
    }

    public void removeOffline() {
        io.execute(() -> {
            OfflineStore.remove(getApplication(), type, id);
            saved = null;
            saveState.postValue(SAVE_NONE);
        });
    }

    // ------------------------------------------------------------------ network mapping

    private ContentItem fetchMovie(int id) throws Exception {
        Response<Models.MovieDetail> r = TMDBClient.get().getMovieDetail(id, "credits").execute();
        if (!r.isSuccessful() || r.body() == null) return null;
        Models.MovieDetail d = r.body();
        ContentItem ci = new ContentItem();
        ci.id = d.id; ci.title = d.title; ci.imdbId = d.imdb_id;
        ci.posterPath = d.poster_path; ci.backdropPath = d.backdrop_path;
        ci.rating = d.vote_average; ci.runtime = d.runtime; ci.overview = d.overview;
        ci.releaseDate = d.release_date; ci.mediaType = "movie";
        return ci;
    }

    private ContentItem fetchTV(int id) throws Exception {
        Response<Models.TVDetail> r = TMDBClient.get().getTVDetail(id, "credits,external_ids").execute();
        if (!r.isSuccessful() || r.body() == null) return null;
        Models.TVDetail d = r.body();
        ContentItem ci = new ContentItem();
        ci.id = d.id; ci.name = d.name;
        ci.posterPath = d.poster_path; ci.backdropPath = d.backdrop_path;
        ci.rating = d.vote_average; ci.overview = d.overview;
        ci.releaseDate = d.first_air_date; ci.numberOfSeasons = d.number_of_seasons;
        ci.imdbId = d.external_ids != null ? d.external_ids.imdb_id : null;
        ci.mediaType = "tv";
        return ci;
    }

    private List<ContentItem> fetchSimilar(String type, int id) throws Exception {
        List<ContentItem> out = new ArrayList<>();
        if ("movie".equals(type)) {
            Response<Models.PagedResponse<Models.MovieResult>> r = TMDBClient.get().getSimilarMovies(id).execute();
            if (r.isSuccessful() && r.body() != null && r.body().results != null)
                for (Models.MovieResult m : r.body().results) {
                    ContentItem c = new ContentItem();
                    c.id = m.id; c.title = m.title; c.posterPath = m.poster_path;
                    c.backdropPath = m.backdrop_path; c.rating = m.vote_average; c.mediaType = "movie";
                    out.add(c);
                }
        } else {
            Response<Models.PagedResponse<Models.TVResult>> r = TMDBClient.get().getSimilarTV(id).execute();
            if (r.isSuccessful() && r.body() != null && r.body().results != null)
                for (Models.TVResult m : r.body().results) {
                    ContentItem c = new ContentItem();
                    c.id = m.id; c.name = m.name; c.posterPath = m.poster_path;
                    c.backdropPath = m.backdrop_path; c.rating = m.vote_average; c.mediaType = "tv";
                    out.add(c);
                }
        }
        return out;
    }

    // ------------------------------------------------------------------ LiveData

    public LiveData<ContentItem> getContent() { return content; }
    public LiveData<List<ContentItem>> getSimilar() { return similar; }
    public LiveData<Models.SeasonDetail> getSeason() { return season; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getOfflineMode() { return offlineMode; }
    public LiveData<Integer> getSaveState() { return saveState; }

    @Override protected void onCleared() { io.shutdown(); }
}
