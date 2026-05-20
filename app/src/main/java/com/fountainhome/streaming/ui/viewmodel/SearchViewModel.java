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

public class SearchViewModel extends AndroidViewModel {

    private final MutableLiveData<List<ContentItem>> results = new MutableLiveData<>();

    public SearchViewModel(@NonNull Application app) { super(app); }

    public void search(String query) {
        TMDBClient.get().search(query, 1).enqueue(new Callback<Models.PagedResponse<Models.SearchResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.SearchResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.SearchResult>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    List<ContentItem> items = new ArrayList<>();
                    for (Models.SearchResult sr : r.body().results) {
                        if ("person".equals(sr.media_type)) continue;
                        ContentItem ci = new ContentItem();
                        ci.id = sr.id;
                        ci.title = sr.title;
                        ci.name = sr.name;
                        ci.posterPath = sr.poster_path;
                        ci.mediaType = sr.media_type != null ? sr.media_type : "movie";
                        ci.rating = sr.vote_average;
                        items.add(ci);
                    }
                    results.postValue(items);
                }
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.SearchResult>> c,
                                  @NonNull Throwable t) {}
        });
    }

    public LiveData<List<ContentItem>> getResults() { return results; }
}
