package com.fountainhome.streaming.ui.viewmodel;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.fountainhome.streaming.api.*;
import java.util.*;
import retrofit2.*;
public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<ContentItem> featured=new MutableLiveData<>();
    private final MutableLiveData<List<ContentItem>> popularMovies=new MutableLiveData<>(),popularTV=new MutableLiveData<>(),topRated=new MutableLiveData<>(),nowPlaying=new MutableLiveData<>();
    private final MutableLiveData<String> error=new MutableLiveData<>();
    private final Handler hero=new Handler(Looper.getMainLooper());
    private final List<ContentItem> heroItems=new ArrayList<>();
    private int heroIdx=0;private boolean cleared=false;private Runnable heroR;
    public HomeViewModel(@NonNull Application app){super(app);load();}
    public void load(){loadMovies();loadTV();loadTopRated();loadNowPlaying();}
    private void loadMovies(){TMDBClient.get().getPopularMovies(1).enqueue(new Callback<Models.PagedResponse<Models.MovieResult>>(){public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>>c,@NonNull Response<Models.PagedResponse<Models.MovieResult>>r){if(cleared)return;if(r.isSuccessful()&&r.body()!=null&&r.body().results!=null){List<ContentItem>items=toMovie(r.body().results);popularMovies.postValue(items);if(!items.isEmpty()){heroItems.clear();heroItems.addAll(items.subList(0,Math.min(10,items.size())));featured.postValue(heroItems.get(0));startHero();}}}public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>>c,@NonNull Throwable t){if(!cleared)error.postValue(t.getMessage());}});}
    private void startHero(){if(heroR!=null)hero.removeCallbacks(heroR);heroR=new Runnable(){@Override public void run(){if(cleared||heroItems.isEmpty())return;heroIdx=(heroIdx+1)%heroItems.size();featured.postValue(heroItems.get(heroIdx));hero.postDelayed(this,5000);}};hero.postDelayed(heroR,5000);}
    private void loadTV(){TMDBClient.get().getPopularTV(1).enqueue(new Callback<Models.PagedResponse<Models.TVResult>>(){public void onResponse(@NonNull Call<Models.PagedResponse<Models.TVResult>>c,@NonNull Response<Models.PagedResponse<Models.TVResult>>r){if(!cleared&&r.isSuccessful()&&r.body()!=null)popularTV.postValue(toTV(r.body().results));}public void onFailure(@NonNull Call<Models.PagedResponse<Models.TVResult>>c,@NonNull Throwable t){}});}
    private void loadTopRated(){TMDBClient.get().getTopRatedMovies(1).enqueue(new Callback<Models.PagedResponse<Models.MovieResult>>(){public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>>c,@NonNull Response<Models.PagedResponse<Models.MovieResult>>r){if(!cleared&&r.isSuccessful()&&r.body()!=null)topRated.postValue(toMovie(r.body().results));}public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>>c,@NonNull Throwable t){}});}
    private void loadNowPlaying(){TMDBClient.get().getNowPlaying(1).enqueue(new Callback<Models.PagedResponse<Models.MovieResult>>(){public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>>c,@NonNull Response<Models.PagedResponse<Models.MovieResult>>r){if(!cleared&&r.isSuccessful()&&r.body()!=null)nowPlaying.postValue(toMovie(r.body().results));}public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>>c,@NonNull Throwable t){}});}
    private List<ContentItem>toMovie(List<Models.MovieResult>rs){List<ContentItem>l=new ArrayList<>();if(rs==null)return l;for(Models.MovieResult r:rs){ContentItem ci=new ContentItem();ci.id=r.id;ci.title=r.title;ci.posterPath=r.poster_path;ci.backdropPath=r.backdrop_path;ci.rating=r.vote_average;ci.mediaType="movie";ci.releaseDate=r.release_date;l.add(ci);}return l;}
    private List<ContentItem>toTV(List<Models.TVResult>rs){List<ContentItem>l=new ArrayList<>();if(rs==null)return l;for(Models.TVResult r:rs){ContentItem ci=new ContentItem();ci.id=r.id;ci.name=r.name;ci.posterPath=r.poster_path;ci.backdropPath=r.backdrop_path;ci.rating=r.vote_average;ci.mediaType="tv";l.add(ci);}return l;}
    @Override protected void onCleared(){cleared=true;if(heroR!=null)hero.removeCallbacks(heroR);super.onCleared();}
    public LiveData<ContentItem>getFeatured(){return featured;}
    public LiveData<List<ContentItem>>getPopularMovies(){return popularMovies;}
    public LiveData<List<ContentItem>>getPopularTV(){return popularTV;}
    public LiveData<List<ContentItem>>getTopRated(){return topRated;}
    public LiveData<List<ContentItem>>getNowPlaying(){return nowPlaying;}
    public LiveData<String>getError(){return error;}
}
