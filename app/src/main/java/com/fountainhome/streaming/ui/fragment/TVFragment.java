package com.fountainhome.streaming.ui.fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.fountainhome.streaming.api.*;
import com.fountainhome.streaming.databinding.FragmentBrowseBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.SearchActivity;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.ContentAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import java.util.*;
import retrofit2.*;
public class TVFragment extends Fragment {
    private FragmentBrowseBinding b;
    private ContentAdapter adapter;
    private String tab = "latest";
    private int currentPage = 1;
    private boolean loading = false, hasMore = true;
    private final List<ContentItem> allItems = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentBrowseBinding.inflate(i, c, false); return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        b.pageTitle.setText("TV Series"); b.backBtn.setVisibility(View.GONE);
        adapter = new ContentAdapter(item -> {
            Intent i = new Intent(getContext(), WatchActivity.class);
            i.putExtra("type", "tv"); i.putExtra("id", item.id); startActivity(i);
        });
        GridLayoutManager lm = new GridLayoutManager(getContext(), 3);
        b.contentRv.setLayoutManager(lm);
        b.contentRv.setAdapter(adapter);
        b.contentRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                int total = lm.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (!loading && hasMore && last >= total - 6) loadMore();
            }
        });
        b.tabLatest.setOnClickListener(x -> { resetAndLoad("latest"); setTab(0); });
        b.tabTrending.setOnClickListener(x -> { resetAndLoad("trending"); setTab(1); });
        b.tabPopular.setOnClickListener(x -> { resetAndLoad("popular"); setTab(2); });
        b.searchBtn.setOnClickListener(x -> startActivity(new Intent(getContext(), SearchActivity.class)));
        b.filterBtn.setOnClickListener(x -> Toast.makeText(getContext(), "Filter coming soon", Toast.LENGTH_SHORT).show());
        setTab(0); resetAndLoad("latest");
    }
    private void resetAndLoad(String t) {
        tab = t; currentPage = 1; hasMore = true; loading = false;
        allItems.clear(); adapter.submitList(null); load(1);
    }
    private void loadMore() { if (!loading && hasMore) load(currentPage + 1); }
    private void load(int page) {
        loading = true; b.progressBar.setVisibility(View.VISIBLE);
        Call<Models.PagedResponse<Models.TVResult>> call;
        switch (tab) {
            case "trending": call = TMDBClient.get().getTopRatedTV(page); break;
            case "popular":  call = TMDBClient.get().getPopularTV(page); break;
            default:         call = TMDBClient.get().getOnAirTV(page); break;
        }
        call.enqueue(new Callback<Models.PagedResponse<Models.TVResult>>() {
            public void onResponse(@NonNull Call<Models.PagedResponse<Models.TVResult>> c,
                                   @NonNull Response<Models.PagedResponse<Models.TVResult>> r) {
                loading = false;
                if (b == null) return;
                b.progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null && r.body().results != null) {
                    List<Models.TVResult> results = r.body().results;
                    if (results.isEmpty()) { hasMore = false; return; }
                    currentPage = page;
                    for (Models.TVResult t : results) {
                        ContentItem ci = new ContentItem();
                        ci.id = t.id; ci.name = t.name; ci.posterPath = t.poster_path;
                        ci.backdropPath = t.backdrop_path; ci.rating = t.vote_average;
                        ci.mediaType = "tv"; ci.releaseDate = t.first_air_date;
                        allItems.add(ci);
                    }
                    adapter.submitList(new ArrayList<>(allItems));
                    if (results.size() < 20) hasMore = false;
                } else hasMore = false;
            }
            public void onFailure(@NonNull Call<Models.PagedResponse<Models.TVResult>> c, @NonNull Throwable t) {
                loading = false; if (b != null) b.progressBar.setVisibility(View.GONE);
            }
        });
    }
    private void setTab(int a) {
        if (b == null) return;
        int g = AppPreferences.getAccentColor(requireContext()), gr = 0xFF888888;
        b.tabLatest.setTextColor(a == 0 ? g : gr);
        b.tabTrending.setTextColor(a == 1 ? g : gr);
        b.tabPopular.setTextColor(a == 2 ? g : gr);
    }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
