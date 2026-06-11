package com.fountainhome.streaming.download;
import android.app.*;
import android.content.Context;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import java.io.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.*;
public class DownloadManager2 {
    private static final String CH = "fh_dl";
    private static final ExecutorService exec = Executors.newFixedThreadPool(2);

    // Save metadata only (no video file)
    public static void saveForOffline(Context ctx, ContentItem item) {
        exec.execute(() -> {
            DownloadItem dl = new DownloadItem();
            dl.tmdbId = item.id; dl.type = item.mediaType;
            dl.title = item.displayTitle();
            dl.posterPath = item.posterPath != null ? item.posterPath : "";
            dl.overview = item.overview != null ? item.overview : "";
            dl.rating = item.rating; dl.imdbId = item.imdbId != null ? item.imdbId : "";
            dl.season = item.lastSeason; dl.episode = item.lastEpisode;
            dl.status = "offline"; dl.isOfflineReady = false;
            dl.addedAt = System.currentTimeMillis();
            AppDatabase.get(ctx).downloadDao().insert(dl);
        });
    }

    // Quick save episode metadata
    public static void quickSave(Context ctx, int tmdbId, String type, String title, int season, int episode) {
        exec.execute(() -> {
            DownloadItem dl = new DownloadItem();
            dl.tmdbId = tmdbId; dl.type = type; dl.title = title;
            dl.season = season; dl.episode = episode;
            dl.status = "offline"; dl.isOfflineReady = false;
            dl.addedAt = System.currentTimeMillis();
            AppDatabase.get(ctx).downloadDao().insert(dl);
        });
    }

    // Real video download with progress notification
    public static void downloadVideo(Context ctx, ContentItem item, int season, int episode) {
        setupChannel(ctx);
        exec.execute(() -> {
            String label = item.displayTitle() + (season > 0 ? " S" + season + "E" + episode : "");
            int notifId = (item.id * 1000 + episode);
            showNotif(ctx, notifId, label, "Finding stream...", 0);

            // Extract stream URL
            new StreamExtractor().extract(ctx,
                "anime".equals(item.mediaType)
                    ? SourceGenerator.getAnimeSources(item.id, season, episode).get(0).url
                    : "movie".equals(item.mediaType)
                        ? SourceGenerator.getMovieSources(item.imdbId, item.id).get(0).url
                        : SourceGenerator.getTVSources(item.imdbId, item.id, season, episode).get(0).url,
                15000,
                new StreamExtractor.Callback() {
                    @Override public void onFound(String url, Map<String, String> headers) {
                        doDownload(ctx, item, season, episode, url, headers, label, notifId);
                    }
                    @Override public void onFailed() {
                        cancelNotif(ctx, notifId);
                        android.os.Handler h = new android.os.Handler(Looper.getMainLooper());
                        h.post(() -> android.widget.Toast.makeText(ctx, "Could not extract stream for download", android.widget.Toast.LENGTH_LONG).show());
                    }
                });
        });
    }

    private static void doDownload(Context ctx, ContentItem item, int season, int episode,
                                    String url, Map<String, String> headers, String label, int notifId) {
        // Create output directory
        File dir = new File(Environment.getExternalStorageDirectory(), "F-Home/" + sanitize(item.displayTitle()));
        dir.mkdirs();
        String filename = season > 0 ? "S" + season + "E" + episode + ".mp4" : item.displayTitle() + ".mp4";
        File outFile = new File(dir, filename);

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
            okhttp3.Request.Builder rb = new okhttp3.Request.Builder().url(url);
            for (Map.Entry<String, String> e : headers.entrySet()) rb.addHeader(e.getKey(), e.getValue());
            Response response = client.newCall(rb.build()).execute();
            if (response.body() == null) { cancelNotif(ctx, notifId); return; }
            long total = response.body().contentLength();
            InputStream in = response.body().byteStream();
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            long downloaded = 0; int read;
            while ((read = in.read(buf)) != -1) {
                fos.write(buf, 0, read);
                downloaded += read;
                if (total > 0) {
                    int pct = (int) (downloaded * 100 / total);
                    showNotif(ctx, notifId, label, "Downloading " + pct + "%", pct);
                }
            }
            fos.close(); in.close();

            // Save to DB
            DownloadItem dl = new DownloadItem();
            dl.tmdbId = item.id; dl.type = item.mediaType; dl.title = item.displayTitle();
            dl.posterPath = item.posterPath != null ? item.posterPath : "";
            dl.season = season; dl.episode = episode;
            dl.localPath = outFile.getAbsolutePath();
            dl.status = "completed"; dl.isOfflineReady = true;
            dl.addedAt = System.currentTimeMillis();
            AppDatabase.get(ctx).downloadDao().insert(dl);
            showCompleteNotif(ctx, notifId, label, outFile.getAbsolutePath());

        } catch (Exception e) {
            outFile.delete();
            cancelNotif(ctx, notifId);
        }
    }

    public static boolean isDownloaded(Context ctx, int tmdbId, String type, int s, int e) {
        DownloadItem dl = AppDatabase.get(ctx).downloadDao().find(tmdbId, type, s, e);
        return dl != null && "completed".equals(dl.status) && dl.localPath != null
            && new File(dl.localPath).exists();
    }
    public static void deleteDownload(Context ctx, DownloadItem item) {
        exec.execute(() -> {
            if (item.localPath != null) new File(item.localPath).delete();
            AppDatabase.get(ctx).downloadDao().delete(item);
        });
    }
    private static String sanitize(String s) { return s.replaceAll("[^a-zA-Z0-9 ]", "").trim(); }
    private static void setupChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "Downloads", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }
    private static void showNotif(Context ctx, int id, String title, String text, int pct) {
        setupChannel(ctx);
        Notification n = new NotificationCompat.Builder(ctx, CH)
            .setContentTitle(title).setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, pct, pct == 0).setOngoing(true).build();
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, n);
    }
    private static void showCompleteNotif(Context ctx, int id, String title, String path) {
        setupChannel(ctx);
        Notification n = new NotificationCompat.Builder(ctx, CH)
            .setContentTitle(title).setContentText("Download complete")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true).build();
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, n);
    }
    private static void cancelNotif(Context ctx, int id) {
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id);
    }
}
