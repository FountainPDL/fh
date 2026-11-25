package com.fountainhome.streaming.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fountainhome.streaming.R;
import com.fountainhome.streaming.anime.AniListClient;
import com.fountainhome.streaming.anime.AnimeAdapter;
import com.fountainhome.streaming.databinding.FragmentAnimeBinding;
import com.fountainhome.streaming.service.OfflineStore;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.fountainhome.streaming.ui.AnimeDetailActivity;
import com.fountainhome.streaming.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/** Anime tab. Same layout and behaviour as the Movies and TV tabs: tabs on top, endless poster grid. */
public class AnimeFragment extends Fragment {
    private FragmentAnimeBinding b;
    private AnimeAdapter adapter;
    private String tab = "trending";
    private int page = 1;
    private boolean loading = false, hasMore = true;
    private final List<AniListClient.AnimeItem> all = new ArrayList<>();
    private int gen = 0;                    // bumps on every reset so late responses are ignored
    private String query = "";
    private boolean searching = false;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    // hero carousel (header row of the grid)
    private final List<AniListClient.AnimeItem> heroes = new ArrayList<>();
    private HeaderAdapter header;
    private ViewPager2 heroPager;
    private TextView heroTitle, heroMeta;
    private LinearLayout heroDots;
    private final Runnable advance = () -> {
        if (heroPager == null || heroes.size() < 2 || !isResumed() || isHidden()) return;
        heroPager.setCurrentItem((heroPager.getCurrentItem() + 1) % heroes.size(), true);
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentAnimeBinding.inflate(i, c, false);
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
        header = new HeaderAdapter();
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int pos) { return header.getItemCount() > 0 && pos == 0 ? 3 : 1; }
        });
        b.contentRv.setLayoutManager(lm);
        b.contentRv.setAdapter(new ConcatAdapter(header, adapter));
        loadHeroes();
        b.contentRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!loading && hasMore && lm.findLastVisibleItemPosition() >= lm.getItemCount() - 6) load(page + 1);
            }
        });
        b.tabLatest.setOnClickListener(x -> { leaveSearch(); setTab(0); reset("trending"); });
        b.tabTrending.setOnClickListener(x -> { leaveSearch(); setTab(1); reset("popular"); });
        b.tabPopular.setOnClickListener(x -> { leaveSearch(); setTab(2); reset("season"); });
        b.searchBtn.setOnClickListener(x -> { if (searching) closeSearch(); else openSearch(); });
        b.searchClose.setOnClickListener(x -> closeSearch());
        b.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {
                if (!searching) return;
                if (pendingSearch != null) ui.removeCallbacks(pendingSearch);
                final String q = s.toString().trim();
                pendingSearch = () -> runSearch(q);
                ui.postDelayed(pendingSearch, 450);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        b.searchInput.setOnEditorActionListener((tv, id, ev) -> {
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                if (pendingSearch != null) ui.removeCallbacks(pendingSearch);
                runSearch(tv.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });
        setTab(0);
        reset("trending");
    }

    // ------------------------------------------------------------------ search (inline)

    private void openSearch() {
        searching = true;
        b.searchRow.setVisibility(View.VISIBLE);
        b.tabRow.setVisibility(View.GONE);
        header.setShown(false);
        b.searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(b.searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    /** Close the search box and go back to the browse tabs. */
    private void closeSearch() {
        if (!searching) return;
        leaveSearch();
        b.searchInput.setText("");
        String t = "search".equals(tab) ? "trending" : tab;
        setTab(tabIndex(t));
        reset(t);
    }

    private void leaveSearch() {
        if (!searching) return;
        searching = false;
        if (pendingSearch != null) ui.removeCallbacks(pendingSearch);
        hideKeyboard();
        b.searchRow.setVisibility(View.GONE);
        b.tabRow.setVisibility(View.VISIBLE);
        header.setShown(!heroes.isEmpty());
    }

    private int tabIndex(String t) { return "popular".equals(t) ? 1 : "season".equals(t) ? 2 : 0; }

    private void runSearch(String q) {
        if (b == null || !searching || q.length() < 2) return;
        query = q;
        reset("search");
    }

    private void hideKeyboard() {
        if (b == null) return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(b.searchInput.getWindowToken(), 0);
    }

    // ------------------------------------------------------------------ hero carousel

    private void loadHeroes() {
        AniListClient.getTrending(1, new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            @Override public void onSuccess(List<AniListClient.AnimeItem> r) {
                if (b == null || r == null) return;
                heroes.clear();
                for (AniListClient.AnimeItem a : r) {
                    if (heroes.size() >= 7) break;
                    if (a.bannerImage != null && !a.bannerImage.isEmpty()) heroes.add(a);
                }
                for (AniListClient.AnimeItem a : r) {
                    if (heroes.size() >= 5) break;
                    if (!heroes.contains(a) && a.coverImage != null) heroes.add(a);
                }
                header.setShown(!searching && !heroes.isEmpty());
                if (heroPager != null) bindHeroes();
            }
            @Override public void onError(String e) {}
        });
    }

    private void bindHeroes() {
        if (heroPager == null || b == null) return;
        final Fragment self = this;
        heroPager.setOffscreenPageLimit(1);
        heroPager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                ImageView iv = new ImageView(p.getContext());
                iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return new RecyclerView.ViewHolder(iv) { };
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                final AniListClient.AnimeItem a = heroes.get(pos);
                String img = a.bannerImage != null && !a.bannerImage.isEmpty() ? a.bannerImage : a.coverImage;
                Glide.with(self).load(OfflineStore.image(h.itemView.getContext(), img, "w780")).centerCrop().into((ImageView) h.itemView);
                h.itemView.setOnClickListener(v -> open(a));
            }
            @Override public int getItemCount() { return heroes.size(); }
        });
        heroPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) { showHero(pos); scheduleAdvance(); }
            @Override public void onPageScrollStateChanged(int st) {
                if (st == ViewPager2.SCROLL_STATE_DRAGGING) ui.removeCallbacks(advance);
                else if (st == ViewPager2.SCROLL_STATE_IDLE) scheduleAdvance();
            }
        });
        buildDots();
        showHero(0);
        scheduleAdvance();
    }

    private void showHero(int pos) {
        if (heroTitle == null || pos >= heroes.size()) return;
        AniListClient.AnimeItem a = heroes.get(pos);
        heroTitle.setText(a.displayTitle());
        StringBuilder m = new StringBuilder();
        if (a.averageScore > 0) m.append(String.format(java.util.Locale.US, "\u2605%.1f", a.averageScore));
        if (a.format != null && !a.format.isEmpty()) m.append(m.length() > 0 ? "  \u00b7  " : "").append(a.format);
        if (a.episodes > 0) m.append(m.length() > 0 ? "  \u00b7  " : "").append(a.episodes).append(" eps");
        heroMeta.setText(m);
        paintDots(pos);
    }

    private void buildDots() {
        heroDots.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < heroes.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (8 * d), (int) (8 * d));
            lp.setMargins((int) (3 * d), 0, (int) (3 * d), 0);
            dot.setLayoutParams(lp);
            heroDots.addView(dot);
        }
    }

    private void paintDots(int active) {
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < heroDots.getChildCount(); i++) {
            View dot = heroDots.getChildAt(i);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(ContextCompat.getColor(requireContext(), i == active ? R.color.on_image : R.color.on_image_dim));
            dot.setBackground(g);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            lp.width = (int) ((i == active ? 20 : 8) * d);
            dot.setLayoutParams(lp);
        }
    }

    private void scheduleAdvance() {
        ui.removeCallbacks(advance);
        if (heroes.size() > 1 && isResumed() && !isHidden() && !searching) ui.postDelayed(advance, 6000);
    }

    @Override public void onResume() { super.onResume(); scheduleAdvance(); }
    @Override public void onPause() { super.onPause(); ui.removeCallbacks(advance); }
    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) ui.removeCallbacks(advance); else scheduleAdvance();
    }

    /** One full-width row on top of the poster grid holding the carousel. */
    private class HeaderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private boolean shown = false;
        void setShown(boolean s) {
            if (s == shown) return;
            shown = s;
            if (s) notifyItemInserted(0); else notifyItemRemoved(0);
        }
        @Override public int getItemCount() { return shown ? 1 : 0; }
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_anime_hero, p, false);
            heroPager = v.findViewById(R.id.anime_hero_pager);
            heroTitle = v.findViewById(R.id.anime_hero_title);
            heroMeta = v.findViewById(R.id.anime_hero_meta);
            heroDots = v.findViewById(R.id.anime_hero_dots);
            return new RecyclerView.ViewHolder(v) { };
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) { bindHeroes(); }
    }

    private void reset(String t) {
        tab = t; page = 1; hasMore = true; loading = false; gen++;
        all.clear();
        adapter.submitList(null);
        load(1);
    }

    private void load(int p) {
        final int myGen = gen;
        loading = true;
        b.progressBar.setVisibility(View.VISIBLE);
        AniListClient.Callback<List<AniListClient.AnimeItem>> cb = new AniListClient.Callback<List<AniListClient.AnimeItem>>() {
            public void onSuccess(List<AniListClient.AnimeItem> r) {
                if (myGen != gen) return;
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
                if (myGen != gen) return;
                loading = false;
                if (b != null) b.progressBar.setVisibility(View.GONE);
                if (getContext() != null && p == 1) Toast.makeText(getContext(), "Could not load anime", Toast.LENGTH_SHORT).show();
            }
        };
        switch (tab) {
            case "search": AniListClient.search(query, p, cb); break;
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
        i.putExtra("romaji", item.titleRomaji); i.putExtra("english", item.titleEnglish);
        startActivity(i);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        ui.removeCallbacksAndMessages(null);
        heroPager = null; heroTitle = null; heroMeta = null; heroDots = null;
        b = null;
    }
}
