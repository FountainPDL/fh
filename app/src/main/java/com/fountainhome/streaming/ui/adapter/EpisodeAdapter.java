package com.fountainhome.streaming.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.service.WatchProgress;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.VH> {

    public interface OnEpisodeClick { void onClick(Models.Episode episode, int episodeNum); }

    private final List<Models.Episode> episodes;
    private final OnEpisodeClick listener;
    private final int tmdbId;
    private final String type;
    private final int season;
    private final Context ctx;

    public EpisodeAdapter(List<Models.Episode> episodes, OnEpisodeClick listener,
                          int tmdbId, String type, int season, Context ctx) {
        this.episodes = episodes;
        this.listener = listener;
        this.tmdbId = tmdbId;
        this.type = type;
        this.season = season;
        this.ctx = ctx;
    }

    @NonNull
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_episode, parent, false);
        return new VH(v);
    }

    public void onBindViewHolder(@NonNull VH holder, int position) {
        Models.Episode ep = episodes.get(position);
        int epNum = ep.episode_number > 0 ? ep.episode_number : position + 1;

        holder.epNumber.setText("E" + epNum);
        holder.epTitle.setText(ep.name != null ? ep.name : "Episode " + epNum);

        if (ep.overview != null && !ep.overview.isEmpty()) {
            holder.epOverview.setText(ep.overview);
            holder.epOverview.setVisibility(View.VISIBLE);
        } else {
            holder.epOverview.setVisibility(View.GONE);
        }

        if (ep.runtime > 0) {
            holder.epDuration.setText(ep.runtime + "m");
            holder.epDuration.setVisibility(View.VISIBLE);
        }

        Glide.with(ctx).load(SourceGenerator.imageUrl(ep.still_path, "w300"))
            .placeholder(R.color.surface).into(holder.epThumbnail);

        // Download status
        boolean downloaded = DownloadManager2.isDownloaded(ctx, tmdbId, type, season, epNum);
        holder.downloadIcon.setImageResource(downloaded
            ? R.drawable.ic_check : R.drawable.ic_download);
        holder.downloadIcon.setAlpha(downloaded ? 1.0f : 0.5f);

        // Progress bar
        long pos = WatchProgress.get(ctx, tmdbId, type, season, epNum);
        if (pos > 0) {
            holder.progressBar.setVisibility(View.VISIBLE);
            // Show some visual indicator
        } else {
            holder.progressBar.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(ep, epNum));
    }

    public int getItemCount() { return episodes.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView epNumber, epTitle, epOverview, epDuration;
        ImageView epThumbnail, downloadIcon;
        View progressBar;
        VH(View v) {
            super(v);
            epNumber   = v.findViewById(R.id.ep_number);
            epTitle    = v.findViewById(R.id.ep_title);
            epOverview = v.findViewById(R.id.ep_overview);
            epDuration = v.findViewById(R.id.ep_duration);
            epThumbnail = v.findViewById(R.id.ep_thumbnail);
            downloadIcon = v.findViewById(R.id.download_icon);
            progressBar = v.findViewById(R.id.ep_progress);
        }
    }
}
