package com.fountainhome.streaming.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.FragmentHomeBinding;
import com.fountainhome.streaming.player.EmbedPlayerActivity;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.OfflineStore;
import com.fountainhome.streaming.ui.MainActivity;
import com.fountainhome.streaming.ui.SearchActivity;
import com.fountainhome.streaming.ui.WatchActivity;
import com.fountainhome.streaming.ui.adapter.HomeRowAdapter;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.fountainhome.streaming.ui.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

/** Netflix-style home: full-bleed hero, category pills, and horizontal rows. */
public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    private HomeViewModel vm;
    private ContentItem hero;
    private Row continueRow, top10Row, trendingRow, tvRow, newRow, ratedRow, listRow;

    /** A titled horizontal row with its own adapter; hidden until it has items. */
    private final class Row {
        final View root; final HomeRowAdapter adapter;
        Row(String title, int style, HomeRowAdapter.OnClick click) {
            root = getLayoutInflater().inflate(R.layout.item_home_row, b.rowsContainer, false);
            ((TextView) root.findViewById(R.id.row_title)).setText(title);
            adapter = new HomeRowAdapter(style, click);
            ((RecyclerView) root.findViewById(R.id.row_rv)).setAdapter(adapter);
            b.rowsContainer.addView(root);
        }
        void set(List<ContentItem> l) {
            adapter.submit(l);
            root.setVisibility(l == null || l.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentHomeBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        HomeRowAdapter.OnClick open = this::openDetails;
        continueRow = new Row("Continue Watching", HomeRowAdapter.LAND, this::resume);
        top10Row = new Row("Top 10 This Week", HomeRowAdapter.TOP10, open);
        trendingRow = new Row("Trending Now", HomeRowAdapter.POSTER, open);
        listRow = new Row("My List", HomeRowAdapter.POSTER, open);
        tvRow = new Row("Popular TV Shows", HomeRowAdapter.POSTER, open);
        newRow = new Row("New Releases", HomeRowAdapter.POSTER, open);
        ratedRow = new Row("Top Rated Movies", HomeRowAdapter.POSTER, open);

        b.searchBtn.setOnClickListener(x -> startActivity(new Intent(requireContext(), SearchActivity.class)));
        b.pillTv.setOnClickListener(x -> tab(2));
        b.pillMovies.setOnClickListener(x -> tab(1));
        b.pillAnime.setOnClickListener(x -> tab(3));
        b.pillOffline.setOnClickListener(x -> openOffline());
        b.offlineBanner.setOnClickListener(x -> openOffline());
        b.heroPlay.setOnClickListener(x -> { if (hero != null) play(hero); });
        b.heroInfo.setOnClickListener(x -> { if (hero != null) openDetails(hero); });
        b.heroMylist.setOnClickListener(x -> {
            if (hero == null) return;
            if (LibraryManager.isIn(requireContext(), hero.id, hero.mediaType, LibraryManager.WATCHLIST))
                LibraryManager.remove(requireContext(), hero.id, hero.mediaType, LibraryManager.WATCHLIST);
            else LibraryManager.add(requireContext(), hero, LibraryManager.WATCHLIST);
            refreshMyListIcon();
        });

        b.heroPager.setOffscreenPageLimit(1);
        b.heroPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) { if (pos < heroes.size()) showHero(heroes.get(pos), pos); scheduleAdvance(); }
            @Override public void onPageScrollStateChanged(int st) {
                if (st == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING) auto.removeCallbacks(advance);
                else if (st == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE) scheduleAdvance();
            }
        });
        vm.getHeroes().observe(getViewLifecycleOwner(), this::setHeroes);
        vm.getTop10().observe(getViewLifecycleOwner(), l -> top10Row.set(AppPreferences.getShowTop10(requireContext()) ? l : null));
        vm.getTrending().observe(getViewLifecycleOwner(), l -> trendingRow.set(l));
        vm.getPopularTV().observe(getViewLifecycleOwner(), l -> tvRow.set(l));
        vm.getNewReleases().observe(getViewLifecycleOwner(), l -> newRow.set(l));
        vm.getTopRated().observe(getViewLifecycleOwner(), l -> ratedRow.set(l));
        vm.getFailed().observe(getViewLifecycleOwner(), f -> {
            boolean off = Boolean.TRUE.equals(f) && !online();
            b.offlineBanner.setVisibility(off ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (b == null) return;
        // Personal rows change whenever the user watches or lists something.
        Context c = requireContext();
        if (AppPreferences.getShowContinue(c)) continueRow.set(LibraryManager.get(c, LibraryManager.CONTINUE));
        else continueRow.set(null);
        listRow.set(AppPreferences.getShowMyList(c) ? LibraryManager.get(c, LibraryManager.WATCHLIST) : null);
        refreshMyListIcon();
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }

    // ------------------------------------------------------------------

    // ---- hero carousel
    private final List<ContentItem> heroes = new ArrayList<>();
    private final android.os.Handler auto = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable advance = () -> {
        if (b == null || heroes.size() < 2 || !isResumed() || isHidden()) return;
        int next = (b.heroPager.getCurrentItem() + 1) % heroes.size();
        b.heroPager.setCurrentItem(next, true);
    };

    private void scheduleAdvance() {
        auto.removeCallbacks(advance);
        if (heroes.size() > 1 && isResumed() && !isHidden()) auto.postDelayed(advance, 6000);
    }

    @Override public void onPause() { super.onPause(); auto.removeCallbacks(advance); }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) auto.removeCallbacks(advance); else scheduleAdvance();
    }

    private void setHeroes(List<ContentItem> list) {
        if (b == null || list == null || list.isEmpty()) return;
        heroes.clear();
        heroes.addAll(list);
        final androidx.fragment.app.Fragment self = this;
        b.heroPager.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            @NonNull @Override public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                android.widget.ImageView iv = new android.widget.ImageView(p.getContext());
                iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                return new androidx.recyclerview.widget.RecyclerView.ViewHolder(iv) { };
            }
            @Override public void onBindViewHolder(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder h, int pos) {
                Glide.with(self).load(OfflineStore.image(h.itemView.getContext(), heroes.get(pos).backdropPath, "w1280"))
                    .centerCrop().into((android.widget.ImageView) h.itemView);
            }
            @Override public int getItemCount() { return heroes.size(); }
        });
        buildDots();
        showHero(heroes.get(0), 0);
        scheduleAdvance();
    }

    private void buildDots() {
        b.heroDots.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < heroes.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (8 * d), (int) (8 * d));
            lp.setMargins((int) (3 * d), 0, (int) (3 * d), 0);
            dot.setLayoutParams(lp);
            b.heroDots.addView(dot);
        }
    }

    private void paintDots(int active) {
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < b.heroDots.getChildCount(); i++) {
            View dot = b.heroDots.getChildAt(i);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(ContextCompat.getColor(requireContext(), i == active ? R.color.on_image : R.color.on_image_dim));
            dot.setBackground(g);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            lp.width = (int) ((i == active ? 20 : 8) * d);
            dot.setLayoutParams(lp);
        }
    }

    private void showHero(ContentItem c, int pos) {
        if (c == null || b == null) return;
        hero = c;
        b.heroTitle.setText(c.displayTitle());
        StringBuilder m = new StringBuilder("tv".equals(c.mediaType) ? "TV Series" : "Movie");
        if (!c.year().isEmpty()) m.append("  \u00b7  ").append(c.year());
        if (c.rating > 0) m.append("  \u00b7  \u2605 ").append(String.format("%.1f", c.rating));
        b.heroMeta.setText(m);
        paintDots(pos);
        refreshMyListIcon();
    }

    private void refreshMyListIcon() {
        if (b == null || hero == null) return;
        boolean in = LibraryManager.isIn(requireContext(), hero.id, hero.mediaType, LibraryManager.WATCHLIST);
        b.heroMylistIcon.setImageResource(in ? R.drawable.ic_check : R.drawable.ic_plus);
    }

    private void openDetails(ContentItem c) {
        Intent i;
        if ("anime".equals(c.mediaType)) {
            i = new Intent(requireContext(), com.fountainhome.streaming.ui.AnimeDetailActivity.class);
            i.putExtra("anime_id", c.id);
            i.putExtra("title", c.displayTitle());
            i.putExtra("cover", c.posterPath);
            i.putExtra("banner", c.backdropPath);
            i.putExtra("rating", c.rating);
        } else {
            i = new Intent(requireContext(), WatchActivity.class);
            i.putExtra("type", c.mediaType);
            i.putExtra("id", c.id);
        }
        startActivity(i);
    }

    /** Hero "Play": open the title's page so the user picks the episode; movies go straight to the player. */
    private void play(ContentItem c) {
        if ("movie".equals(c.mediaType)) startPlayer(c, 1, 1);
        else openDetails(c);
    }

    private void resume(ContentItem c) {
        if ("anime".equals(c.mediaType)) { openDetails(c); return; }
        startPlayer(c, Math.max(1, c.lastSeason), Math.max(1, c.lastEpisode));
    }

    private void startPlayer(ContentItem c, int season, int episode) {
        Intent i = new Intent(requireContext(), EmbedPlayerActivity.class);
        i.putExtra("type", c.mediaType);
        i.putExtra("id", c.id);
        i.putExtra("imdbId", c.imdbId != null ? c.imdbId : "");
        i.putExtra("title", c.displayTitle());
        i.putExtra("posterPath", c.posterPath != null ? c.posterPath : "");
        i.putExtra("backdropPath", c.backdropPath != null ? c.backdropPath : "");
        i.putExtra("rating", c.rating);
        i.putExtra("season", season);
        i.putExtra("episode", episode);
        startActivity(i);
    }

    private void tab(int index) {
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).selectTab(index);
    }

    private void openOffline() {
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new OfflineFragment()).addToBackStack(null).commit();
    }

    private boolean online() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
