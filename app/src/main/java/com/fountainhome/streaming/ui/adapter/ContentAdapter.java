package com.fountainhome.streaming.ui.adapter;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ItemPosterBinding;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
public class ContentAdapter extends ListAdapter<ContentItem,ContentAdapter.VH> {
    public interface OnClick{void onClick(ContentItem item);}
    private final OnClick l;
    public ContentAdapter(@NonNull OnClick l){super(new DiffUtil.ItemCallback<ContentItem>(){public boolean areItemsTheSame(@NonNull ContentItem a,@NonNull ContentItem b){return a.id==b.id;}public boolean areContentsTheSame(@NonNull ContentItem a,@NonNull ContentItem b){return a.id==b.id;}});this.l=l;}
    @NonNull public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(ItemPosterBinding.inflate(LayoutInflater.from(p.getContext()),p,false));}
    public void onBindViewHolder(@NonNull VH h,int pos){ContentItem item=getItem(pos);h.b.titleText.setText(item.displayTitle());h.b.ratingText.setText(item.rating>0?String.format("\u2605%.1f",item.rating):"");Glide.with(h.itemView).load(SourceGenerator.imageUrl(item.posterPath,"w342")).placeholder(android.R.color.darker_gray).centerCrop().into(h.b.posterImage);h.itemView.setOnClickListener(v->l.onClick(item));}
    static class VH extends RecyclerView.ViewHolder{final ItemPosterBinding b;VH(@NonNull ItemPosterBinding b){super(b.getRoot());this.b=b;}}
}
