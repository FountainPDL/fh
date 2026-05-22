package com.fountainhome.streaming.anime;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ItemAnimeBinding;

public class AnimeAdapter extends ListAdapter<AniListClient.AnimeItem, AnimeAdapter.VH> {

    public interface OnClick { void onClick(AniListClient.AnimeItem item); }
    private final OnClick listener;

    public AnimeAdapter(@NonNull OnClick listener) {
        super(new DiffUtil.ItemCallback<AniListClient.AnimeItem>() {
            public boolean areItemsTheSame(@NonNull AniListClient.AnimeItem a,
                                           @NonNull AniListClient.AnimeItem b) {
                return a.id == b.id;
            }
            public boolean areContentsTheSame(@NonNull AniListClient.AnimeItem a,
                                              @NonNull AniListClient.AnimeItem b) {
                return a.id == b.id;
            }
        });
        this.listener = listener;
    }

    @NonNull
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemAnimeBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false));
    }

    public void onBindViewHolder(@NonNull VH holder, int position) {
        AniListClient.AnimeItem item = getItem(position);
        holder.binding.titleText.setText(item.displayTitle());
        holder.binding.ratingText.setText(item.averageScore > 0
            ? String.format("★%.1f", item.averageScore) : "");
        holder.binding.episodesText.setText(item.episodes > 0
            ? item.episodes + " eps" : item.status);

        Glide.with(holder.itemView)
            .load(item.coverImage)
            .placeholder(com.fountainhome.streaming.R.color.surface)
            .centerCrop()
            .into(holder.binding.posterImage);

        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemAnimeBinding binding;
        VH(@NonNull ItemAnimeBinding b) { super(b.getRoot()); binding = b; }
    }
}
