package com.fountainhome.streaming.ui.adapter;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fountainhome.streaming.databinding.ItemPosterBinding;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
public class ContentAdapter extends ListAdapter<ContentItem,ContentAdapter.VH> {
    public interface OnClick{void onClick(ContentItem item);}
    private final OnClick l;
    public ContentAdapter(@NonNull OnClick l){super(new DiffUtil.ItemCallback<ContentItem>(){public boolean areItemsTheSame(@NonNull ContentItem a,@NonNull ContentItem b){return a.id==b.id;}public boolean areContentsTheSame(@NonNull ContentItem a,@NonNull ContentItem b){return a.id==b.id;}});this.l=l;}
    @NonNull public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(ItemPosterBinding.inflate(LayoutInflater.from(p.getContext()),p,false));}
    public void onBindViewHolder(@NonNull VH h,int pos){
        ContentItem item=getItem(pos);
        h.b.titleText.setText(item.displayTitle());
        h.b.ratingText.setText(item.rating>0?String.format("★%.1f",item.rating):"");
        h.b.ratingText.setVisibility(item.rating>0?View.VISIBLE:View.GONE);
        Glide.with(h.itemView).load(SourceGenerator.imageUrl(item.posterPath,"w342"))
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(android.R.color.darker_gray))
            .centerCrop().into(h.b.posterImage);
        h.itemView.setOnClickListener(v->l.onClick(item));
        h.itemView.setOnLongClickListener(v->{
            android.content.Context ctx=v.getContext();
            boolean fav=LibraryManager.isIn(ctx,item.id,item.mediaType,LibraryManager.FAVORITES);
            boolean wl=LibraryManager.isIn(ctx,item.id,item.mediaType,LibraryManager.WATCHLIST);
            String[]opts={"Open",wl?"Remove from Watchlist":"Add to Watchlist",fav?"Remove from Favorites":"Add to Favorites","Share"};
            new AlertDialog.Builder(ctx).setTitle(item.displayTitle()).setItems(opts,(d,w)->{switch(w){case 0:l.onClick(item);break;case 1:if(wl)LibraryManager.remove(ctx,item.id,item.mediaType,LibraryManager.WATCHLIST);else LibraryManager.add(ctx,item,LibraryManager.WATCHLIST);Toast.makeText(ctx,wl?"Removed":"Added to Watchlist",Toast.LENGTH_SHORT).show();break;case 2:LibraryManager.toggleFavorite(ctx,item);Toast.makeText(ctx,fav?"Removed":"Added to Favorites",Toast.LENGTH_SHORT).show();break;case 3:android.content.Intent si=new android.content.Intent(android.content.Intent.ACTION_SEND);si.setType("text/plain");si.putExtra(android.content.Intent.EXTRA_TEXT,item.displayTitle()+" — Fountain Home");ctx.startActivity(android.content.Intent.createChooser(si,"Share via"));break;}}).show();
            return true;
        });
    }
    static class VH extends RecyclerView.ViewHolder{final ItemPosterBinding b;VH(@NonNull ItemPosterBinding b){super(b.getRoot());this.b=b;}}
}
