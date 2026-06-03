package com.fountainhome.streaming.ui.player;
import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import com.fountainhome.streaming.databinding.ActivityPlayerBinding;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import java.util.*;
public class PlayerActivity extends AppCompatActivity {
    private ActivityPlayerBinding b;private ExoPlayer exo;
    private List<SourceGenerator.Source> sources=new ArrayList<>();
    private String imdbId="",type;private int tmdbId,season=1,episode=1;
    private boolean fs=false,usingExo=false,ctrlVisible=true;
    private ContentItem cur;
    private final Handler ph=new Handler(Looper.getMainLooper());
    private Runnable pr,hideCtrl;
    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);b=ActivityPlayerBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        type=getIntent().getStringExtra("type");tmdbId=getIntent().getIntExtra("id",0);
        imdbId=getIntent().getStringExtra("imdbId");String title=getIntent().getStringExtra("title");
        season=getIntent().getIntExtra("season",1);episode=getIntent().getIntExtra("episode",1);
        if(imdbId==null)imdbId="";
        cur=new ContentItem();cur.id=tmdbId;cur.mediaType=type;cur.title=title;cur.imdbId=imdbId;
        b.titleText.setText(title!=null?title:"");updateLabel();
        hideCtrl=this::hideControls;scheduleHide();
        b.touchInterceptor.setOnClickListener(v->toggleControls());
        WebSettings ws=b.playerWebView.getSettings();
        ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setLoadWithOverviewMode(true);ws.setUseWideViewPort(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");
        b.playerWebView.setWebViewClient(new AdBlockWebViewClient());
        b.playerWebView.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView v,int p){b.loadingBar.setProgress(p);b.loadingBar.setVisibility(p<100?View.VISIBLE:View.GONE);}
            @Override public void onShowCustomView(View v,CustomViewCallback cb){enterFs();b.fullscreenContainer.addView(v);b.fullscreenContainer.setVisibility(View.VISIBLE);}
            @Override public void onHideCustomView(){b.fullscreenContainer.removeAllViews();b.fullscreenContainer.setVisibility(View.GONE);exitFs();}
        });
        b.fullscreenBtn.setOnClickListener(v->{if(fs)exitFs();else enterFs();resetHide();});
        b.shareBtn.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,(title!=null?title:"")+" — Fountain Home");startActivity(Intent.createChooser(i,"Share"));resetHide();});
        b.pipBtn.setOnClickListener(v->{pip();resetHide();});
        if("tv".equals(type)||"anime".equals(type)){b.tvControls.setVisibility(View.VISIBLE);b.prevBtn.setOnClickListener(v->{save();if(episode>1)episode--;else if(season>1){season--;episode=1;}updateLabel();build();resetHide();});b.nextBtn.setOnClickListener(v->{save();episode++;updateLabel();build();resetHide();});}else b.tvControls.setVisibility(View.GONE);
        b.backBtn.setOnClickListener(v->{save();finish();});
        build();startSaving();enterFs();
    }
    private void toggleControls(){if(ctrlVisible)hideControls();else showControls();}
    private void showControls(){ctrlVisible=true;b.controlsBar.setVisibility(View.VISIBLE);b.topControls.setVisibility(View.VISIBLE);b.controlsBar.setAlpha(1f);b.topControls.setAlpha(1f);scheduleHide();}
    private void hideControls(){ctrlVisible=false;b.controlsBar.animate().alpha(0f).setDuration(400).withEndAction(()->b.controlsBar.setVisibility(View.INVISIBLE)).start();b.topControls.animate().alpha(0f).setDuration(400).withEndAction(()->b.topControls.setVisibility(View.INVISIBLE)).start();}
    private void scheduleHide(){ph.removeCallbacks(hideCtrl);ph.postDelayed(hideCtrl,3000);}
    private void resetHide(){showControls();}
    private void startSaving(){pr=new Runnable(){@Override public void run(){save();ph.postDelayed(this,10000);}};ph.postDelayed(pr,10000);}
    private void save(){try{long pos=usingExo&&exo!=null?exo.getCurrentPosition():0;LibraryManager.updateProgress(this,cur,season,episode,pos);}catch(Exception ignored){}}
    private void build(){
        sources="movie".equals(type)?SourceGenerator.getMovieSources(imdbId,tmdbId):SourceGenerator.getTVSources(imdbId,tmdbId,season,episode);
        String pref=AppPreferences.getSource(this);
        List<SourceGenerator.Source>ro=new ArrayList<>();
        for(SourceGenerator.Source src:sources)if(src.label.startsWith(pref))ro.add(0,src);
        for(SourceGenerator.Source src:sources)if(!src.label.startsWith(pref))ro.add(src);
        sources=ro;
        List<String>labels=new ArrayList<>();for(SourceGenerator.Source src:sources)labels.add(src.label);
        ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);b.sourceSpinner.setAdapter(a);
        b.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean init=true;public void onItemSelected(AdapterView<?>p,View v,int pos,long id){if(init){init=false;}play(pos);}public void onNothingSelected(AdapterView<?>p){}});
        if(!sources.isEmpty())play(0);
        long saved=WatchProgress.get(this,tmdbId,type,season,episode);
        if(saved>5000)ph.postDelayed(()->{if(usingExo&&exo!=null)exo.seekTo(saved);},2000);
    }
    private void play(int idx){
        if(idx<0||idx>=sources.size())return;String url=sources.get(idx).url;
        showLoad(true);freeExo();b.playerWebView.setVisibility(View.GONE);b.playerView.setVisibility(View.GONE);
        new StreamExtractor().extract(this,url,10000,new StreamExtractor.Callback(){
            @Override public void onFound(String su,Map<String,String>h){runOnUiThread(()->playExo(su,h));}
            @Override public void onFailed(){runOnUiThread(()->playWv(url));}
        });
    }
    private void playExo(String url,Map<String,String>headers){
        showLoad(false);usingExo=true;b.playerView.setVisibility(View.VISIBLE);b.playerWebView.setVisibility(View.GONE);
        DefaultHttpDataSource.Factory dsf=new DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 Chrome/120.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(headers).setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000).setReadTimeoutMs(8000);
        exo=new ExoPlayer.Builder(this).build();b.playerView.setPlayer(exo);
        MediaItem mi=MediaItem.fromUri(url);
        if(url.contains(".m3u8")||url.contains("/hls/"))exo.setMediaSource(new HlsMediaSource.Factory(dsf).createMediaSource(mi));
        else exo.setMediaSource(new ProgressiveMediaSource.Factory(dsf).createMediaSource(mi));
        if(("tv".equals(type)||"anime".equals(type))&&AppPreferences.getAutoplay(this))
            exo.addListener(new androidx.media3.common.Player.Listener(){@Override public void onPlaybackStateChanged(int st){if(st==androidx.media3.common.Player.STATE_ENDED){save();episode++;updateLabel();build();}}});
        exo.prepare();exo.setPlaybackSpeed(AppPreferences.getPlaybackSpeed(this));exo.play();
    }
    @SuppressLint("SetJavaScriptEnabled")
    private void playWv(String url){
        showLoad(false);usingExo=false;freeExo();
        b.playerWebView.setVisibility(View.VISIBLE);b.playerView.setVisibility(View.GONE);
        String html="<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            +"<style>*{margin:0;padding:0;background:#000}iframe,video{width:100vw;height:100vh;border:none}</style></head>"
            +"<body><iframe src='"+url+"' allowfullscreen allow='autoplay;fullscreen;encrypted-media;picture-in-picture'></iframe></body></html>";
        b.playerWebView.loadDataWithBaseURL("https://www.google.com",html,"text/html","utf-8",null);
    }
    private void enterFs(){fs=true;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    private void exitFs(){fs=false;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);}
    private void pip(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O&&AppPreferences.getPiP(this)){try{enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).build());}catch(Exception ignored){}}}
    @Override public void onPictureInPictureModeChanged(boolean inPip){if(b==null)return;b.controlsBar.setVisibility(inPip?View.GONE:View.VISIBLE);b.touchInterceptor.setVisibility(inPip?View.GONE:View.VISIBLE);if(inPip&&exo!=null)exo.play();}
    @Override protected void onUserLeaveHint(){super.onUserLeaveHint();if(AppPreferences.getPiP(this))pip();}
    private void showLoad(boolean show){b.loadingBar.setVisibility(show?View.VISIBLE:View.GONE);b.extractingText.setVisibility(show?View.VISIBLE:View.GONE);}
    private void updateLabel(){if("tv".equals(type)||"anime".equals(type)){b.episodeLabel.setVisibility(View.VISIBLE);b.episodeLabel.setText("S"+season+"·E"+episode);}else b.episodeLabel.setVisibility(View.GONE);}
    private void freeExo(){if(exo!=null){exo.release();exo=null;}}
    @Override public void onBackPressed(){if(fs){exitFs();return;}if(!usingExo&&b.playerWebView.canGoBack())b.playerWebView.goBack();else{save();super.onBackPressed();}}
    @Override protected void onPause(){super.onPause();save();if(exo!=null)exo.pause();else b.playerWebView.onPause();}
    @Override protected void onResume(){super.onResume();if(exo!=null)exo.play();else b.playerWebView.onResume();}
    @Override protected void onDestroy(){save();ph.removeCallbacksAndMessages(null);freeExo();b.playerWebView.destroy();super.onDestroy();}
}
