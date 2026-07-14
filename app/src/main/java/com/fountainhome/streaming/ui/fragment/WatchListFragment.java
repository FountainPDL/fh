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
public class WatchListFragment extends Fragment {
    private FragmentWatchlistBinding b;private ContentAdapter adapter;private String curList=LibraryManager.WATCHLIST;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentWatchlistBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){super.onViewCreated(v,s);adapter=new ContentAdapter(item->{if("anime".equals(item.mediaType)){Intent i=new Intent(getContext(),com.fountainhome.streaming.ui.AnimeDetailActivity.class);i.putExtra("anime_id",item.id);i.putExtra("title",item.displayTitle());i.putExtra("cover",item.posterPath);i.putExtra("banner",item.backdropPath);i.putExtra("rating",item.rating);startActivity(i);}else{Intent i=new Intent(getContext(),WatchActivity.class);i.putExtra("type",item.mediaType);i.putExtra("id",item.id);startActivity(i);}});b.contentRv.setLayoutManager(new GridLayoutManager(getContext(),com.fountainhome.streaming.service.AppPreferences.getGridColumns(getContext())));b.contentRv.setAdapter(adapter);b.tabWatchlist.setOnClickListener(x->load(LibraryManager.WATCHLIST,"Watch List"));b.tabFavorites.setOnClickListener(x->load(LibraryManager.FAVORITES,"Favorites"));b.tabContinue.setOnClickListener(x->load(LibraryManager.CONTINUE,"Continue"));load(LibraryManager.WATCHLIST,"Watch List");}
    @Override public void onResume(){super.onResume();if(b!=null)load(curList,b.pageTitle.getText().toString());}
    private void load(String list,String title){if(b==null)return;curList=list;b.pageTitle.setText(title);int ac=0xFFBB86FC,gr=0xFF888888;b.tabWatchlist.setTextColor(list.equals(LibraryManager.WATCHLIST)?ac:gr);b.tabFavorites.setTextColor(list.equals(LibraryManager.FAVORITES)?ac:gr);b.tabContinue.setTextColor(list.equals(LibraryManager.CONTINUE)?ac:gr);List<ContentItem>items=LibraryManager.get(requireContext(),list);adapter.submitList(items);b.emptyText.setVisibility(items.isEmpty()?View.VISIBLE:View.GONE);b.emptyText.setText(items.isEmpty()?"Nothing here yet":"");}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
