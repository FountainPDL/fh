package com.fountainhome.streaming.ui.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.fountainhome.streaming.databinding.FragmentWatchlistBinding;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import java.util.List;
public class HistoryFragment extends Fragment {
    private FragmentWatchlistBinding b;private ContentAdapter adapter;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentWatchlistBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){super.onViewCreated(v,s);b.pageTitle.setText("Watch History");b.tabWatchlist.setVisibility(View.GONE);b.tabFavorites.setVisibility(View.GONE);b.tabContinue.setVisibility(View.GONE);adapter=new ContentAdapter(item->{if("anime".equals(item.mediaType)){Intent i=new Intent(getContext(),com.fountainhome.streaming.ui.AnimeDetailActivity.class);i.putExtra("anime_id",item.id);i.putExtra("title",item.displayTitle());i.putExtra("cover",item.posterPath);i.putExtra("banner",item.backdropPath);i.putExtra("rating",item.rating);startActivity(i);}else{Intent i=new Intent(getContext(),WatchActivity.class);i.putExtra("type",item.mediaType);i.putExtra("id",item.id);startActivity(i);}});b.contentRv.setLayoutManager(new GridLayoutManager(getContext(),com.fountainhome.streaming.service.AppPreferences.getGridColumns(getContext())));b.contentRv.setAdapter(adapter);load();}
    @Override public void onResume(){super.onResume();load();}
    private void load(){if(b==null||getContext()==null)return;List<ContentItem>items=LibraryManager.get(requireContext(),LibraryManager.CONTINUE);adapter.submitList(items);b.emptyText.setVisibility(items.isEmpty()?View.VISIBLE:View.GONE);b.emptyText.setText("No watch history yet.");}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
