package com.fountainhome.streaming.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fountainhome.streaming.R;
import com.fountainhome.streaming.anime.AniListClient;
import com.fountainhome.streaming.anime.AnimeAdapter;
import com.fountainhome.streaming.databinding.FragmentBrowseBinding;
import com.fountainhome.streaming.ui.AnimeDetailActivity;
import com.fountainhome.streaming.ui.AnimeListActivity;
import com.fountainhome.streaming.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/** Anime tab. Same layout and behaviour as the Movies and TV tabs: tabs on top, endless poster grid. */
public class AnimeFragment extends Fragment {
    private FragmentBrowseBinding b;
    private AnimeAdapter adapter;
    private String tab = "trending";
    private int page = 1;
    private boolean loading = false, hasMore = true;
    private final List<AniListClient.AnimeItem> all = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentBrowseBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        b.pageTitle.setText("Anime");
        b.backBtn.setVisibility(View.GONE);
        b.filterBtn.setVisibility(View.GONE);
        b.tabLatest.setText("Trending");
        b.tabTrending.setText("Popular");
        b.tabPopular.setText("This Season");

        adapter = new AnimeAdapter(this::open);
        GridLayoutManager lm = new GridLayoutManager(getContext(), 3);
        b.contentRv.setLayoutManager(lm);
        b.contentRv.setAdapter(adapter);
        b.contentRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!loading && hasMore && lm.findLastVisibleItemPosition() >= lm.getItemCount() - 6) load(page + 1);
            }
        });
        b.tabLatest.setOnClickListener(x -> { setTab(0); reset("trending"); });
        b.tabTrending.setOnClickListener(x -> { setTab(1); reset("popular"); });
        b.tabPopular.setOnClickListener(x -> { setTab(2); reset("season"); });
        b.searchBtn.setOnClickListener(x -> askSearch());
        setTab(0);
        reset("trending");
    }

    private void askSearch() {
        final EditText input = new EditText(requireContext());
        input.setHint("Search anime");
        input.setSingleLine(true);
        new AlertDialog.Builder(requireContext()).setTitle("Search anime").setView(input)
            .setPositiveButton("Search", (d, w) -> {
                String q = input.getText().toString().trim();
                if (q.length() < 2) return;
                Intent i = new Intent(requireContext(), AnimeListActivity.class);
                i.putExtra("category", "search");
                i.putExtra("query", q);
                i.putExtra("title", "Results: " + q);
                startActivity(i);
            }).setNegativeButton("Cancel", null).show();
    }

    private void reset(String t) {
        tab = t; page = 1; hasMore = true; loading = false;
        all.clear();
        adapter.submitList(null);
        load(1);
    }

    private void load(int p) {
        loading = true;
        b.progressBar.setVisibility(View.VISIBLE);
        AniListClient.Callback<List<AniListClient.AnimeItem>> cb = new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) {
                loading = false;
                if (b == null) return;
                b.progressBar.setVisibility(View.GONE);
                if (r == null || r.isEmpty()) { hasMore = false; return; }
                page = p;
                all.addAll(r);
                adapter.submitList(new ArrayList<>(all));
                if (r.size() < 20) hasMore = false;
            }
            public void onError(String e) {
                loading = false;
                if (b != null) b.progressBar.setVisibility(View.GONE);
                if (getContext() != null && p == 1) Toast.makeText(getContext(), "Could not load anime", Toast.LENGTH_SHORT).show();
            }
        };
        switch (tab) {
            case "popular": AniListClient.getPopular(p, cb); break;
            case "season": AniListClient.getThisSeason(p, cb); break;
            default: AniListClient.getTrending(p, cb); break;
        }
    }

    private void setTab(int a) {
        if (b == null) return;
        int g = ThemeManager.accent(requireContext()), gr = ThemeManager.color(requireContext(), R.attr.fhTextDim);
        b.tabLatest.setTextColor(a == 0 ? g : gr);
        b.tabTrending.setTextColor(a == 1 ? g : gr);
        b.tabPopular.setTextColor(a == 2 ? g : gr);
    }

    private void open(AniListClient.AnimeItem item) {
        Intent i = new Intent(getContext(), AnimeDetailActivity.class);
        i.putExtra("anime_id", item.id); i.putExtra("title", item.displayTitle());
        i.putExtra("cover", item.coverImage); i.putExtra("banner", item.bannerImage);
        i.putExtra("description", item.description); i.putExtra("episodes", item.episodes);
        i.putExtra("rating", item.averageScore); i.putExtra("status", item.status);
        i.putExtra("format", item.format);
        startActivity(i);
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
