package com.fountainhome.streaming.ui.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.SearchView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.FragmentHomeBinding;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.SearchActivity;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.*;
import java.util.List;
public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;private boolean searchOpen=false;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentHomeBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){super.onViewCreated(v,s);HomeViewModel vm=new ViewModelProvider(requireActivity()).get(HomeViewModel.class);vm.getFeatured().observe(getViewLifecycleOwner(),item->{if(item==null||b==null)return;Glide.with(this).load(SourceGenerator.imageUrl(item.backdropPath,"w780")).transition(DrawableTransitionOptions.withCrossFade(600)).centerCrop().into(b.featuredBanner);b.featuredTitle.setText(item.displayTitle());b.featuredGenre.setText(String.format("\u2605%.1f",item.rating)+"  \u00b7  "+("movie".equals(item.mediaType)?"Movie":"TV")+(item.year().isEmpty()?"":" \u00b7 "+item.year()));b.watchNowBtn.setOnClickListener(x->open(item));b.featuredBanner.setOnClickListener(x->open(item));b.detailBtn.setOnClickListener(x->open(item));b.featuredAdd.setOnClickListener(x->{boolean in=LibraryManager.isIn(requireContext(),item.id,item.mediaType,LibraryManager.WATCHLIST);if(in)LibraryManager.remove(requireContext(),item.id,item.mediaType,LibraryManager.WATCHLIST);else LibraryManager.add(requireContext(),item,LibraryManager.WATCHLIST);b.featuredAdd.setText(in?"+":"\u2713");});});b.tabTrending.setOnClickListener(x->{setTab(true);vm.getPopularMovies().observe(getViewLifecycleOwner(),items->{if(b!=null&&b.trendingRv.getAdapter()!=null)((ContentAdapter)b.trendingRv.getAdapter()).submitList(items);});});b.tabPopular.setOnClickListener(x->{setTab(false);vm.getTopRated().observe(getViewLifecycleOwner(),items->{if(b!=null&&b.trendingRv.getAdapter()!=null)((ContentAdapter)b.trendingRv.getAdapter()).submitList(items);});});row(b.trendingRv,vm.getPopularMovies());row(b.latestMoviesRv,vm.getNowPlaying());row(b.latestTvRv,vm.getPopularTV());refreshContinue();b.searchBtn.setOnClickListener(x->{searchOpen=!searchOpen;b.searchBar.setVisibility(searchOpen?View.VISIBLE:View.GONE);if(searchOpen)b.searchBar.requestFocus();});b.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener(){public boolean onQueryTextSubmit(String q){if(q!=null&&!q.trim().isEmpty()){Intent i=new Intent(getContext(),SearchActivity.class);i.putExtra("query",q.trim());startActivity(i);b.searchBar.setVisibility(View.GONE);searchOpen=false;}return true;}public boolean onQueryTextChange(String q){return false;}});b.viewAllMovies.setOnClickListener(x->nav(R.id.nav_movies));b.viewAllTv.setOnClickListener(x->nav(R.id.nav_tv));}
    @Override public void onResume(){super.onResume();refreshContinue();}
    private void row(androidx.recyclerview.widget.RecyclerView rv,androidx.lifecycle.LiveData<List<ContentItem>>data){ContentAdapter a=new ContentAdapter(this::open);rv.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));rv.setAdapter(a);data.observe(getViewLifecycleOwner(),items->{if(items!=null)a.submitList(items);});}
    private void refreshContinue(){if(b==null||getContext()==null)return;List<ContentItem>list=LibraryManager.get(requireContext(),LibraryManager.CONTINUE);boolean show=!list.isEmpty()&&AppPreferences.getShowContinue(requireContext());b.continueSection.setVisibility(show?View.VISIBLE:View.GONE);if(show){ContentAdapter a=new ContentAdapter(this::open);b.continueRv.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));b.continueRv.setAdapter(a);a.submitList(list);}}
    private void setTab(boolean t){if(b==null)return;int ac=AppPreferences.getAccentColor(requireContext());b.tabTrending.setTextColor(t?ac:0xFF888888);b.tabPopular.setTextColor(!t?ac:0xFF888888);}
    private void open(ContentItem item){Intent i=new Intent(getContext(),WatchActivity.class);i.putExtra("type",item.mediaType);i.putExtra("id",item.id);startActivity(i);}
    private void nav(int id) {}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
