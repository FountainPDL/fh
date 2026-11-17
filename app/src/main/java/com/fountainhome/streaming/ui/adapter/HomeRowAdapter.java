package com.fountainhome.streaming.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.service.OfflineStore;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.util.ArrayList;
import java.util.List;

/** One adapter for the home rows: portrait posters, landscape "continue watching" cards, numbered Top 10. */
public class HomeRowAdapter extends RecyclerView.Adapter<HomeRowAdapter.VH> {
    public static final int POSTER = 0, LAND = 1, TOP10 = 2;

    public interface OnClick { void onClick(ContentItem item); }

    private final int style;
    private final OnClick click;
    private List<ContentItem> items = new ArrayList<>();

    public HomeRowAdapter(int style, OnClick click) { this.style = style; this.click = click; }

    public void submit(List<ContentItem> l) {
        items = l != null ? l : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) { return style; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        int res = t == LAND ? R.layout.item_home_land : t == TOP10 ? R.layout.item_home_top10 : R.layout.item_home_poster;
        return new VH(LayoutInflater.from(p.getContext()).inflate(res, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ContentItem c = items.get(pos);
        boolean land = style == LAND;
        String path = land && c.backdropPath != null ? c.backdropPath : c.posterPath;
        Glide.with(h.img).load(OfflineStore.image(h.img.getContext(), path, land ? "w500" : "w342")).into(h.img);
        if (h.title != null) h.title.setText(c.displayTitle());
        if (h.num != null) h.num.setText(String.valueOf(pos + 1));
        if (h.progress != null) {
            int pct = c.durationMs > 0 ? (int) Math.min(100, c.lastPositionMs * 100 / c.durationMs) : 0;
            h.progress.setProgress(pct);
            h.progress.setVisibility(pct > 0 ? View.VISIBLE : View.INVISIBLE);
        }
        h.itemView.setOnClickListener(v -> click.onClick(c));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img; final TextView title, num; final ProgressBar progress;
        VH(View v) {
            super(v);
            img = v.findViewById(R.id.img);
            title = v.findViewById(R.id.title);
            num = v.findViewById(R.id.num);
            progress = v.findViewById(R.id.progress);
        }
    }
}
