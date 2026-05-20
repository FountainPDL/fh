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

public class WatchViewModel extends AndroidViewModel {

    private final MutableLiveData<ContentItem>       content      = new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> similar      = new MutableLiveData<>();
    private final MutableLiveData<Models.SeasonDetail> season     = new MutableLiveData<>();
    private final MutableLiveData<Boolean>           loading      = new MutableLiveData<>(true);

    public WatchViewModel(@NonNull Application app) { super(app); }

    public void load(String type, int id) {
        loading.postValue(true);
        if ("movie".equals(type)) loadMovie(id);
        else loadTV(id);
    }

    private void loadMovie(int id) {
        TMDBClient.get().getMovieDetail(id, "credits").enqueue(new Callback<Models.MovieDetail>() {
            public void onResponse(@NonNull Call<Models.MovieDetail> c,
                                   @NonNull Response<Models.MovieDetail> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Models.MovieDetail d = r.body();
                    ContentItem ci = new ContentItem();
                    ci.id = d.id; ci.title = d.title; ci.imdbId = d.imdb_id;
                    ci.posterPath = d.poster_path; ci.backdropPath = d.backdrop_path;
                    ci.rating = d.vote_average; ci.runtime = d.runtime;
                    ci.overview = d.overview; ci.releaseDate = d.release_date;
                    ci.mediaType = "movie";
                    content.postValue(ci);
                }
                loading.postValue(false);
            }
            public void onFailure(@NonNull Call<Models.MovieDetail> c, @NonNull Throwable t) {
                loading.postValue(false);
            }
        });

        TMDBClient.get().getSimilarMovies(id).enqueue(new Callback<Models.PagedResponse<Models.MovieResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.MovieResult>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    List<ContentItem> items = new ArrayList<>();
                    if (r.body().results != null) {
                        for (Models.MovieResult mr : r.body().results) {
                            ContentItem ci = new ContentItem();
                            ci.id = mr.id; ci.title = mr.title; ci.posterPath = mr.poster_path;
                            ci.backdropPath = mr.backdrop_path; ci.rating = mr.vote_average;
                            ci.mediaType = "movie";
                            items.add(ci);
                        }
                    }
                    similar.postValue(items);
                }
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>> c, @NonNull Throwable t) {}
        });
    }

    private void loadTV(int id) {
        TMDBClient.get().getTVDetail(id, "credits").enqueue(new Callback<Models.TVDetail>() {
            public void onResponse(@NonNull Call<Models.TVDetail> c,
                                   @NonNull Response<Models.TVDetail> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Models.TVDetail d = r.body();
                    ContentItem ci = new ContentItem();
                    ci.id = d.id; ci.name = d.name;
                    ci.posterPath = d.poster_path; ci.backdropPath = d.backdrop_path;
                    ci.rating = d.vote_average; ci.numberOfSeasons = d.number_of_seasons;
                    ci.overview = d.overview; ci.releaseDate = d.first_air_date;
                    ci.mediaType = "tv"; ci.seasons = d.seasons;
                    content.postValue(ci);
                    // Auto-load season 1
                    loadSeason(id, 1);
                }
                loading.postValue(false);
            }
            public void onFailure(@NonNull Call<Models.TVDetail> c, @NonNull Throwable t) {
                loading.postValue(false);
            }
        });

        TMDBClient.get().getSimilarTV(id).enqueue(new Callback<Models.PagedResponse<Models.TVResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.TVResult>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    List<ContentItem> items = new ArrayList<>();
                    if (r.body().results != null) {
                        for (Models.TVResult tr : r.body().results) {
                            ContentItem ci = new ContentItem();
                            ci.id = tr.id; ci.name = tr.name; ci.posterPath = tr.poster_path;
                            ci.backdropPath = tr.backdrop_path; ci.rating = tr.vote_average;
                            ci.mediaType = "tv";
                            items.add(ci);
                        }
                    }
                    similar.postValue(items);
                }
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.TVResult>> c, @NonNull Throwable t) {}
        });
    }

    public void loadSeason(int tvId, int seasonNum) {
        TMDBClient.get().getSeason(tvId, seasonNum).enqueue(new Callback<Models.SeasonDetail>() {
            public void onResponse(@NonNull Call<Models.SeasonDetail> c,
                                   @NonNull Response<Models.SeasonDetail> r) {
                if (r.isSuccessful() && r.body() != null) season.postValue(r.body());
            }
            public void onFailure(@NonNull Call<Models.SeasonDetail> c, @NonNull Throwable t) {}
        });
    }

    public LiveData<ContentItem>         getContent()  { return content; }
    public LiveData<List<ContentItem>>   getSimilar()  { return similar; }
    public LiveData<Models.SeasonDetail> getSeason()   { return season; }
    public LiveData<Boolean>             getLoading()  { return loading; }
}
