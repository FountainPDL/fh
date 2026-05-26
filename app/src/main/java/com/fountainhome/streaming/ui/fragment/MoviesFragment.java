package com.fountainhome.streaming.ui.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.fountainhome.streaming.api.*;
import com.fountainhome.streaming.databinding.FragmentBrowseBinding;
import com.fountainhome.streaming.ui.SearchActivity;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import java.util.*;import retrofit2.*;
public class MoviesFragment extends Fragment {
    private FragmentBrowseBinding b;private ContentAdapter adapter;private String tab="latest";
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentBrowseBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){super.onViewCreated(v,s);b.pageTitle.setText("Movies");b.backBtn.setVisibility(View.GONE);adapter=new ContentAdapter(item->{Intent i=new Intent(getContext(),WatchActivity.class);i.putExtra("type","movie");i.putExtra("id",item.id);startActivity(i);});b.contentRv.setLayoutManager(new GridLayoutManager(getContext(),3));b.contentRv.setAdapter(adapter);b.tabLatest.setOnClickListener(x->{tab="latest";load();setTab(0);});b.tabTrending.setOnClickListener(x->{tab="trending";load();setTab(1);});b.tabPopular.setOnClickListener(x->{tab="popular";load();setTab(2);});b.searchBtn.setOnClickListener(x->startActivity(new Intent(getContext(),SearchActivity.class)));b.filterBtn.setOnClickListener(x->Toast.makeText(getContext(),"Filter coming soon",Toast.LENGTH_SHORT).show());setTab(0);load();}
    private void setTab(int a){int g=0xFFBB86FC,gr=0xFF888888;b.tabLatest.setTextColor(a==0?g:gr);b.tabTrending.setTextColor(a==1?g:gr);b.tabPopular.setTextColor(a==2?g:gr);}
    private void load(){b.progressBar.setVisibility(View.VISIBLE);Call<Models.PagedResponse<Models.MovieResult>>call;switch(tab){case"trending":call=TMDBClient.get().getTopRatedMovies(1);break;case"popular":call=TMDBClient.get().getPopularMovies(1);break;default:call=TMDBClient.get().getNowPlaying(1);break;}call.enqueue(new Callback<Models.PagedResponse<Models.MovieResult>>(){public void onResponse(@NonNull Call<Models.PagedResponse<Models.MovieResult>>c,@NonNull Response<Models.PagedResponse<Models.MovieResult>>r){b.progressBar.setVisibility(View.GONE);if(r.isSuccessful()&&r.body()!=null&&r.body().results!=null){List<ContentItem>l=new ArrayList<>();for(Models.MovieResult m:r.body().results){ContentItem ci=new ContentItem();ci.id=m.id;ci.title=m.title;ci.posterPath=m.poster_path;ci.backdropPath=m.backdrop_path;ci.rating=m.vote_average;ci.mediaType="movie";ci.releaseDate=m.release_date;l.add(ci);}adapter.submitList(l);}}public void onFailure(@NonNull Call<Models.PagedResponse<Models.MovieResult>>c,@NonNull Throwable t){b.progressBar.setVisibility(View.GONE);}});}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
