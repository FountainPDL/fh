package com.fountainhome.streaming.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.fountainhome.streaming.databinding.ActivitySearchBinding;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.SearchViewModel;
public class SearchActivity extends AppCompatActivity {
    private ActivitySearchBinding b;private SearchViewModel vm;private ContentAdapter adapter;
    @Override protected void onCreate(Bundle s){super.onCreate(s);b=ActivitySearchBinding.inflate(getLayoutInflater());setContentView(b.getRoot());vm=new ViewModelProvider(this).get(SearchViewModel.class);adapter=new ContentAdapter(item->{if("anime".equals(item.mediaType)){Intent i=new Intent(this,com.fountainhome.streaming.ui.AnimeDetailActivity.class);i.putExtra("anime_id",item.id);i.putExtra("title",item.displayTitle());i.putExtra("cover",item.posterPath);i.putExtra("banner",item.backdropPath);i.putExtra("rating",item.rating);startActivity(i);}else{Intent i=new Intent(this,WatchActivity.class);i.putExtra("type",item.mediaType);i.putExtra("id",item.id);startActivity(i);}});b.resultsRv.setLayoutManager(new GridLayoutManager(this,com.fountainhome.streaming.service.AppPreferences.getGridColumns(this)));b.resultsRv.setAdapter(adapter);b.backBtn.setOnClickListener(v->finish());vm.getResults().observe(this,items->{adapter.submitList(items);b.emptyText.setVisibility(items==null||items.isEmpty()?View.VISIBLE:View.GONE);});b.searchInput.setOnQueryTextListener(new SearchView.OnQueryTextListener(){public boolean onQueryTextSubmit(String q){doSearch(q);return true;}public boolean onQueryTextChange(String q){if(q!=null&&q.length()>=2)doSearch(q);return false;}});String q=getIntent().getStringExtra("query");if(q!=null&&!q.isEmpty()){b.searchInput.setQuery(q,false);doSearch(q);}}
    private void doSearch(String q){if(q==null||q.trim().isEmpty())return;b.emptyText.setText("Searching...");b.emptyText.setVisibility(View.VISIBLE);vm.search(q.trim());}
}
