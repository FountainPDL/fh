package com.fountainhome.streaming.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ItemPosterBinding;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

public class ContentAdapter extends ListAdapter<ContentItem, ContentAdapter.VH> {

    public interface OnClick { void onClick(ContentItem item); }
    private final OnClick listener;

    public ContentAdapter(@NonNull OnClick listener) {
        super(new DiffUtil.ItemCallback<ContentItem>() {
            public boolean areItemsTheSame(@NonNull ContentItem a, @NonNull ContentItem b) {
                return a.id == b.id;
            }
            public boolean areContentsTheSame(@NonNull ContentItem a, @NonNull ContentItem b) {
                return a.id == b.id;
            }
        });
        this.listener = listener;
    }

    @NonNull
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemPosterBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false));
    }

    public void onBindViewHolder(@NonNull VH holder, int position) {
        ContentItem item = getItem(position);
        holder.binding.titleText.setText(item.displayTitle());
        holder.binding.ratingText.setText(String.format("★%.1f", item.rating));
        Glide.with(holder.itemView.getContext())
            .load(SourceGenerator.imageUrl(item.posterPath, "w342"))
            .placeholder(com.fountainhome.streaming.R.color.surface)
            .error(com.fountainhome.streaming.R.color.surface)
            .centerCrop()
            .into(holder.binding.posterImage);
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemPosterBinding binding;
        VH(@NonNull ItemPosterBinding b) { super(b.getRoot()); binding = b; }
    }
}
