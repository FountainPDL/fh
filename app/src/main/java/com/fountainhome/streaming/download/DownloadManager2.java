package com.fountainhome.streaming.download;
import android.content.Context;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class DownloadManager2 {
    private static final ExecutorService exec=Executors.newFixedThreadPool(2);
    public static void saveForOffline(Context ctx,ContentItem item){exec.execute(()->{DownloadItem dl=new DownloadItem();dl.tmdbId=item.id;dl.type=item.mediaType;dl.title=item.displayTitle();dl.posterPath=item.posterPath;dl.overview=item.overview;dl.rating=item.rating;dl.imdbId=item.imdbId;dl.season=item.lastSeason;dl.episode=item.lastEpisode;dl.status="completed";dl.isOfflineReady=false;dl.addedAt=System.currentTimeMillis();AppDatabase.get(ctx).downloadDao().insert(dl);});}
    public static boolean isDownloaded(Context ctx,int tmdbId,String type,int s,int e){DownloadItem dl=AppDatabase.get(ctx).downloadDao().find(tmdbId,type,s,e);return dl!=null&&"completed".equals(dl.status)&&dl.localPath!=null&&new File(dl.localPath).exists();}
    public static void deleteDownload(Context ctx,DownloadItem item){exec.execute(()->{if(item.localPath!=null)new File(item.localPath).delete();AppDatabase.get(ctx).downloadDao().delete(item);});}
}
