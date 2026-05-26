package com.fountainhome.streaming.ui.adapter;
import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.*;
import java.util.List;
public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.VH> {
    public interface OnEpClick{void onClick(Models.Episode ep,int num);}
    private final List<Models.Episode>eps;private final OnEpClick l;private final int tmdbId;private final String type;private final int season;private final Context ctx;
    public EpisodeAdapter(List<Models.Episode>eps,OnEpClick l,int tmdbId,String type,int season,Context ctx){this.eps=eps;this.l=l;this.tmdbId=tmdbId;this.type=type;this.season=season;this.ctx=ctx;}
    @NonNull public VH onCreateViewHolder(@NonNull ViewGroup p,int t){return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_episode,p,false));}
    public void onBindViewHolder(@NonNull VH h,int pos){Models.Episode ep=eps.get(pos);int num=ep.episode_number>0?ep.episode_number:pos+1;h.num.setText("E"+num);h.title.setText(ep.name!=null?ep.name:"Episode "+num);if(ep.overview!=null&&!ep.overview.isEmpty()){h.overview.setText(ep.overview);h.overview.setVisibility(View.VISIBLE);}else h.overview.setVisibility(View.GONE);if(ep.runtime>0){h.duration.setText(ep.runtime+"m");h.duration.setVisibility(View.VISIBLE);}Glide.with(ctx).load(SourceGenerator.imageUrl(ep.still_path,"w300")).placeholder(android.R.color.darker_gray).into(h.thumb);boolean dl=DownloadManager2.isDownloaded(ctx,tmdbId,type,season,num);h.dlIcon.setImageResource(dl?R.drawable.ic_check:R.drawable.ic_download);h.dlIcon.setAlpha(dl?1f:0.5f);long prog=WatchProgress.get(ctx,tmdbId,type,season,num);h.progress.setVisibility(prog>0?View.VISIBLE:View.GONE);h.itemView.setOnClickListener(v->l.onClick(ep,num));}
    public int getItemCount(){return eps.size();}
    static class VH extends RecyclerView.ViewHolder{TextView num,title,overview,duration;ImageView thumb,dlIcon;View progress;VH(View v){super(v);num=v.findViewById(R.id.ep_number);title=v.findViewById(R.id.ep_title);overview=v.findViewById(R.id.ep_overview);duration=v.findViewById(R.id.ep_duration);thumb=v.findViewById(R.id.ep_thumbnail);dlIcon=v.findViewById(R.id.download_icon);progress=v.findViewById(R.id.ep_progress);}}
}
