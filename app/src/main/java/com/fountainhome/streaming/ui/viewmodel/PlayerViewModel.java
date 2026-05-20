package com.fountainhome.streaming.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.api.TMDBClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerViewModel extends AndroidViewModel {

    private final MutableLiveData<ContentItem> content = new MutableLiveData<>();
    private final MutableLiveData<Models.SeasonDetail> seasonDetail = new MutableLiveData<>();

    public PlayerViewModel(@NonNull Application app) { super(app); }

    public void loadMovie(int id) {
        TMDBClient.get().getMovieDetail(id, "credits").enqueue(new Callback<Models.MovieDetail>() {
            public void onResponse(@NonNull Call<Models.MovieDetail> c,
                                   @NonNull Response<Models.MovieDetail> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Models.MovieDetail d = r.body();
                    ContentItem ci = new ContentItem();
                    ci.id = d.id;
                    ci.title = d.title;
                    ci.posterPath = d.poster_path;
                    ci.backdropPath = d.backdrop_path;
                    ci.imdbId = d.imdb_id;
                    ci.mediaType = "movie";
                    ci.rating = d.vote_average;
                    content.postValue(ci);
                }
            }
            public void onFailure(@NonNull Call<Models.MovieDetail> c, @NonNull Throwable t) {}
        });
    }

    public void loadTV(int id) {
        TMDBClient.get().getTVDetail(id, "credits").enqueue(new Callback<Models.TVDetail>() {
            public void onResponse(@NonNull Call<Models.TVDetail> c,
                                   @NonNull Response<Models.TVDetail> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Models.TVDetail d = r.body();
                    ContentItem ci = new ContentItem();
                    ci.id = d.id;
                    ci.name = d.name;
                    ci.posterPath = d.poster_path;
                    ci.backdropPath = d.backdrop_path;
                    ci.mediaType = "tv";
                    ci.rating = d.vote_average;
                    ci.numberOfSeasons = d.number_of_seasons;
                    ci.seasons = d.seasons;
                    content.postValue(ci);
                }
            }
            public void onFailure(@NonNull Call<Models.TVDetail> c, @NonNull Throwable t) {}
        });
    }

    public void loadSeason(int tvId, int seasonNumber) {
        TMDBClient.get().getSeason(tvId, seasonNumber).enqueue(new Callback<Models.SeasonDetail>() {
            public void onResponse(@NonNull Call<Models.SeasonDetail> c,
                                   @NonNull Response<Models.SeasonDetail> r) {
                if (r.isSuccessful() && r.body() != null) seasonDetail.postValue(r.body());
            }
            public void onFailure(@NonNull Call<Models.SeasonDetail> c, @NonNull Throwable t) {}
        });
    }

    public LiveData<ContentItem>          getContent()      { return content; }
    public LiveData<Models.SeasonDetail>  getSeasonDetail() { return seasonDetail; }
}
