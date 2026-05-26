package com.fountainhome.streaming.ui.viewmodel;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.fountainhome.streaming.api.*;
import java.util.*;
import retrofit2.*;
public class SearchViewModel extends AndroidViewModel {
    private final MutableLiveData<List<ContentItem>>results=new MutableLiveData<>();
    public SearchViewModel(@NonNull Application app){super(app);}
    public void search(String q){TMDBClient.get().search(q,1).enqueue(new Callback<Models.PagedResponse<Models.SearchResult>>(){public void onResponse(@NonNull Call<Models.PagedResponse<Models.SearchResult>>c,@NonNull Response<Models.PagedResponse<Models.SearchResult>>r){if(r.isSuccessful()&&r.body()!=null){List<ContentItem>l=new ArrayList<>();if(r.body().results!=null)for(Models.SearchResult sr:r.body().results){if("person".equals(sr.media_type))continue;ContentItem ci=new ContentItem();ci.id=sr.id;ci.title=sr.title;ci.name=sr.name;ci.posterPath=sr.poster_path;ci.mediaType=sr.media_type!=null?sr.media_type:"movie";ci.rating=sr.vote_average;l.add(ci);}results.postValue(l);}}public void onFailure(@NonNull Call<Models.PagedResponse<Models.SearchResult>>c,@NonNull Throwable t){}});}
    public LiveData<List<ContentItem>>getResults(){return results;}
}
