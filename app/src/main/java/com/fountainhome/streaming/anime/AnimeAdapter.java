package com.fountainhome.streaming.anime;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ItemAnimeBinding;
public class AnimeAdapter extends ListAdapter<AniListClient.AnimeItem,AnimeAdapter.VH> {
    public interface OnClick{void onClick(AniListClient.AnimeItem item);}
    private final OnClick l;
    public AnimeAdapter(@NonNull OnClick l){super(new DiffUtil.ItemCallback<AniListClient.AnimeItem>(){public boolean areItemsTheSame(@NonNull AniListClient.AnimeItem a,@NonNull AniListClient.AnimeItem b){return a.id==b.id;}public boolean areContentsTheSame(@NonNull AniListClient.AnimeItem a,@NonNull AniListClient.AnimeItem b){return a.id==b.id;}});this.l=l;}
    @NonNull public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(ItemAnimeBinding.inflate(LayoutInflater.from(p.getContext()),p,false));}
    public void onBindViewHolder(@NonNull VH h,int pos){AniListClient.AnimeItem item=getItem(pos);h.b.titleText.setText(item.displayTitle());h.b.ratingText.setText(item.averageScore>0?String.format("\u2605%.1f",item.averageScore):"");h.b.episodesText.setText(item.episodes>0?item.episodes+" eps":(item.status!=null?item.status:""));Glide.with(h.itemView).load(item.coverImage).placeholder(android.R.color.darker_gray).centerCrop().into(h.b.posterImage);h.itemView.setOnClickListener(v->l.onClick(item));}
    static class VH extends RecyclerView.ViewHolder{final ItemAnimeBinding b;VH(@NonNull ItemAnimeBinding b){super(b.getRoot());this.b=b;}}
}
