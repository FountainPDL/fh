package com.fountainhome.streaming.ui;

import com.fountainhome.streaming.ui.theme.ThemeManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.databinding.ActivityWatchBinding;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.adapter.*;
import com.fountainhome.streaming.player.EmbedPlayerActivity;
import com.fountainhome.streaming.ui.viewmodel.*;
import java.util.ArrayList;
import java.util.List;
public class WatchActivity extends BaseActivity {
    private ActivityWatchBinding b;private WatchViewModel vm;private ContentItem item;
    private boolean offline=false,userSaved=false;private String type;private int tmdbId,selSeason=1,selEpisode=1;
    private List<Models.Episode> eps=new ArrayList<>(),filteredEps=new ArrayList<>();
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);b=ActivityWatchBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        type=getIntent().getStringExtra("type");tmdbId=getIntent().getIntExtra("id",0);
        vm=new ViewModelProvider(this).get(WatchViewModel.class);
        vm.getLoading().observe(this,l->b.loadingView.setVisibility(l?View.VISIBLE:View.GONE));
        vm.getContent().observe(this,ci->{if(ci==null)return;item=ci;populate(ci);});
        vm.getSeason().observe(this,sd->{if(sd==null)return;eps=sd.episodes!=null?sd.episodes:new ArrayList<>();filteredEps=new ArrayList<>(eps);populateEps(filteredEps);});
        vm.getSimilar().observe(this,items->{if(items==null||items.isEmpty()){b.similarSection.setVisibility(View.GONE);return;}b.similarSection.setVisibility(View.VISIBLE);ContentAdapter a=new ContentAdapter(sim->{Intent i=new Intent(this,WatchActivity.class);i.putExtra("type",sim.mediaType);i.putExtra("id",sim.id);startActivity(i);});b.similarRv.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));b.similarRv.setAdapter(a);a.submitList(items);});
        vm.getOfflineMode().observe(this,off->{offline=off!=null&&off;if(item!=null)populateMeta(item);});
        vm.getSaveState().observe(this,st->{
            if(st==null)return;
            switch(st){
                case WatchViewModel.SAVE_BUSY:b.downloadBtn.setImageResource(R.drawable.ic_refresh);b.downloadBtn.setAlpha(0.6f);break;
                case WatchViewModel.SAVE_DONE:b.downloadBtn.setImageResource(R.drawable.ic_check);b.downloadBtn.setAlpha(1f);if(userSaved){userSaved=false;Toast.makeText(this,"Saved for offline \u2014 find it under More > Offline",Toast.LENGTH_LONG).show();}break;
                case WatchViewModel.SAVE_FAILED:b.downloadBtn.setImageResource(R.drawable.ic_download);b.downloadBtn.setAlpha(1f);if(userSaved){userSaved=false;Toast.makeText(this,"Could not save. Check your connection and try again.",Toast.LENGTH_LONG).show();}break;
                default:b.downloadBtn.setImageResource(R.drawable.ic_download);b.downloadBtn.setAlpha(1f);
            }
        });
        b.backBtn.setOnClickListener(v->finish());vm.load(type,tmdbId);
    }
    private void populateMeta(ContentItem ci){
        StringBuilder m=new StringBuilder();if(!ci.year().isEmpty())m.append(ci.year());if(ci.runtime>0)m.append(" \u00b7 ").append(ci.runtime).append("m");if(ci.rating>0)m.append(" \u00b7 \u2605").append(String.format("%.1f",ci.rating));if("tv".equals(type)&&ci.numberOfSeasons>0)m.append(" \u00b7 ").append(ci.numberOfSeasons).append(" seasons");
        if(offline)m.append(" \u00b7 Offline copy");
        b.metaText.setText(m);
    }
    private void populate(ContentItem ci){
        Glide.with(this).load(OfflineStore.image(this,ci.backdropPath,"w780")).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)).centerCrop().into(b.backdropImage);
        Glide.with(this).load(OfflineStore.image(this,ci.posterPath,"w500")).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)).centerCrop().into(b.posterImage);
        b.titleText.setText(ci.displayTitle());
        populateMeta(ci);
        if(ci.overview!=null){b.overviewText.setText(ci.overview);b.overviewText.setVisibility(View.VISIBLE);}
        if("tv".equals(type)){b.tvSection.setVisibility(View.VISIBLE);setupSeasons(ci.numberOfSeasons);b.epSearchBar.setVisibility(View.VISIBLE);b.epSearchBar.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int t){}public void onTextChanged(CharSequence s,int a,int b,int c){filterEps(s.toString());}public void afterTextChanged(Editable s){}});}else b.tvSection.setVisibility(View.GONE);
        String[]srcs={"VidSrc","VidLink","VidRock","VIDEASY","VidFast","VidKing","111Movies","Peachify","SuperFlix","VidNest","2Embed","AutoEmbed","SuperEmbed VIP","SuperEmbed"};
        ArrayAdapter<String>sa=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,srcs);sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);b.sourceSpinner.setAdapter(sa);
        String pref=AppPreferences.getSource(this);for(int i=0;i<srcs.length;i++)if(srcs[i].equals(pref)){b.sourceSpinner.setSelection(i);break;}
        b.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?>p,View v,int pos,long id){AppPreferences.setSource(WatchActivity.this,srcs[pos]);}public void onNothingSelected(AdapterView<?>p){}});
        refreshBtns(ci);updateStatusBadge(ci);
        b.statusBtn.setOnClickListener(v->showStatusDialog(ci));
        b.favoriteBtn.setOnClickListener(v->{LibraryManager.toggleFavorite(this,ci);refreshBtns(ci);Toast.makeText(this,LibraryManager.isIn(this,ci.id,ci.mediaType,LibraryManager.FAVORITES)?"Added to favorites":"Removed",Toast.LENGTH_SHORT).show();});
        b.watchlistBtn.setOnClickListener(v->{boolean in=LibraryManager.isIn(this,ci.id,ci.mediaType,LibraryManager.WATCHLIST);if(in)LibraryManager.remove(this,ci.id,ci.mediaType,LibraryManager.WATCHLIST);else LibraryManager.add(this,ci,LibraryManager.WATCHLIST);refreshBtns(ci);});
        b.downloadBtn.setOnClickListener(v->{Integer st=vm.getSaveState().getValue();if(st!=null&&st==WatchViewModel.SAVE_DONE){new AlertDialog.Builder(this).setMessage("Remove the saved offline copy of this page?").setPositiveButton("Remove",(d,w)->vm.removeOffline()).setNegativeButton("Cancel",null).show();}else{userSaved=true;Toast.makeText(this,"Saving page for offline\u2026",Toast.LENGTH_SHORT).show();vm.saveOffline();}});
        b.shareBtn.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,ci.displayTitle()+" — Fountain Home");startActivity(Intent.createChooser(i,"Share via"));});
        b.playBtn.setOnClickListener(v->openPlayer(ci));
        long pos=WatchProgress.get(this,tmdbId,type,selSeason,selEpisode);
        if(pos>10000){b.resumeBtn.setVisibility(View.VISIBLE);b.resumeBtn.setOnClickListener(v->openPlayer(ci));}
        b.downloadMovieBtn.setOnClickListener(v->startDownload(ci,selSeason,selEpisode));
        if("tv".equals(type)){b.episodeMenuBtn.setVisibility(View.VISIBLE);b.episodeMenuBtn.setOnClickListener(v->showMenu(ci));}
    }
    private void filterEps(String q){filteredEps.clear();if(q.isEmpty()){filteredEps.addAll(eps);}else{try{int n=Integer.parseInt(q.trim());for(Models.Episode ep:eps)if(ep.episode_number==n||String.valueOf(ep.episode_number).contains(q.trim()))filteredEps.add(ep);}catch(Exception e){for(Models.Episode ep:eps)if(ep.name!=null&&ep.name.toLowerCase().contains(q.toLowerCase()))filteredEps.add(ep);}}populateEps(filteredEps);}
    private void showStatusDialog(ContentItem ci){String[]opts={"Planning to Watch","Watching","Watched","Dropped","Remove Status"};new AlertDialog.Builder(this).setTitle("Set Status").setItems(opts,(d,w)->{String[]keys={AppPreferences.STATUS_PLAN,AppPreferences.STATUS_WATCH,AppPreferences.STATUS_DONE,AppPreferences.STATUS_DROP,AppPreferences.STATUS_NONE};AppPreferences.setItemStatus(this,ci.id,ci.mediaType,keys[w]);updateStatusBadge(ci);if(w==0)LibraryManager.add(this,ci,LibraryManager.WATCHLIST);Toast.makeText(this,"Status: "+opts[w],Toast.LENGTH_SHORT).show();}).show();}
    private void updateStatusBadge(ContentItem ci){String status=AppPreferences.getItemStatus(this,ci.id,ci.mediaType);b.statusBtn.setText(ThemeManager.statusLabel(status));b.statusBtn.setTextColor(ThemeManager.statusColor(this,status));}
    private void setupSeasons(int count){final int total=count<=0?1:count;b.seasonButtonsContainer.removeAllViews();int ac=AppPreferences.getAccentColor(this);for(int i=1;i<=total;i++){final int sn=i;TextView btn=new TextView(this);btn.setText("S"+i);btn.setPadding(28,14,28,14);btn.setTextColor(i==selSeason?ac:ThemeManager.color(this, R.attr.fhTextDim));btn.setTextSize(13);btn.setOnClickListener(v->{selSeason=sn;selEpisode=1;vm.loadSeason(tmdbId,sn);setupSeasons(total);});b.seasonButtonsContainer.addView(btn);}}
    private void populateEps(List<Models.Episode>list){EpisodeAdapter a=new EpisodeAdapter(list,(ep,num)->{selEpisode=num;openPlayer(item);},(ep,num)->startDownload(item,selSeason,num),tmdbId,type,selSeason,this);b.episodeListRv.setLayoutManager(new LinearLayoutManager(this));b.episodeListRv.setAdapter(a);}
    private void showMenu(ContentItem ci){Dialog d=new Dialog(this,R.style.Theme_FountainHome);d.requestWindowFeature(Window.FEATURE_NO_TITLE);d.setContentView(R.layout.dialog_episode_menu);if(d.getWindow()!=null)d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);RecyclerView rv=d.findViewById(R.id.episodes_rv);rv.setLayoutManager(new LinearLayoutManager(this));EpisodeAdapter a=new EpisodeAdapter(eps,(ep,num)->{selEpisode=num;openPlayer(ci);d.dismiss();},(ep,num)->startDownload(ci,selSeason,num),tmdbId,type,selSeason,this);rv.setAdapter(a);((TextView)d.findViewById(R.id.dialog_title)).setText(ci.displayTitle()+" — S"+selSeason);d.show();}
    private void startDownload(ContentItem ci,int season,int episode){
        if(ci==null)return;
        DownloadManager2.enqueue(this,ci,season,episode);
        Toast.makeText(this,"Download queued \u2014 see More > Offline",Toast.LENGTH_LONG).show();
    }
    private void openPlayer(ContentItem ci){Intent i=new Intent(this,EmbedPlayerActivity.class);i.putExtra("type",type);i.putExtra("id",tmdbId);i.putExtra("imdbId",ci.imdbId!=null?ci.imdbId:"");i.putExtra("title",ci.displayTitle());i.putExtra("posterPath",ci.posterPath!=null?ci.posterPath:"");i.putExtra("backdropPath",ci.backdropPath!=null?ci.backdropPath:"");i.putExtra("rating",ci.rating);i.putExtra("season",selSeason);i.putExtra("episode",selEpisode);i.putExtra("total_episodes",eps.size());i.putExtra("total_seasons",ci.numberOfSeasons);startActivity(i);LibraryManager.updateProgress(this,ci,selSeason,selEpisode,WatchProgress.get(this,tmdbId,type,selSeason,selEpisode));if(AppPreferences.STATUS_NONE.equals(AppPreferences.getItemStatus(this,ci.id,ci.mediaType)))AppPreferences.setItemStatus(this,ci.id,ci.mediaType,AppPreferences.STATUS_WATCH);}
    private void refreshBtns(ContentItem ci){boolean fav=LibraryManager.isIn(this,ci.id,ci.mediaType,LibraryManager.FAVORITES);boolean wl=LibraryManager.isIn(this,ci.id,ci.mediaType,LibraryManager.WATCHLIST);b.favoriteBtn.setAlpha(fav?1f:0.4f);b.watchlistBtn.setAlpha(wl?1f:0.4f);}
}
