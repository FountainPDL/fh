package com.fountainhome.streaming.download;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.fountainhome.streaming.service.DownloadService;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.io.File;

/**
 * Entry point the UI uses for downloads. It only creates / updates the database row and
 * starts {@link DownloadService}; the service does the actual work.
 */
public class DownloadManager2 {
    private DownloadManager2() {}

    /** The folder downloads are saved in (see {@link DownloadFolder}). */
    public static File offlineRoot(Context c) {
        return DownloadFolder.resolve(c);
    }

    public static String safeName(String s) {
        String n = s == null ? "" : s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").trim();
        if (n.length() > 60) n = n.substring(0, 60).trim();
        return n.isEmpty() ? "video" : n;
    }

    public static boolean isDownloaded(Context c, int tmdbId, String type, int season, int episode) {
        DownloadItem d = AppDatabase.get(c).downloadDao().find(tmdbId, type, norm(type, season), norm(type, episode));
        return d != null && DownloadItem.COMPLETED.equals(d.status) && d.localPath != null && new File(d.localPath).isFile();
    }

    /** Queue a movie (season/episode ignored) or one episode for download. */
    public static void enqueue(Context ctx, ContentItem item, int season, int episode) {
        enqueue(ctx, item, season, episode, null);
    }

    /** quality: "best", "1080p"... or null to use the setting. */
    public static void enqueue(Context ctx, ContentItem item, int season, int episode, String quality) {
        Context app = ctx.getApplicationContext();
        DownloadDao dao = AppDatabase.get(app).downloadDao();
        int s = norm(item.mediaType, season), e = norm(item.mediaType, episode);

        DownloadItem d = dao.find(item.id, item.mediaType, s, e);
        if (d != null) {
            if (d.isActive()) {
                Toast.makeText(app, "Already downloading", Toast.LENGTH_SHORT).show();
                return;
            }
            if (DownloadItem.COMPLETED.equals(d.status) && d.localPath != null && new File(d.localPath).isFile()) {
                Toast.makeText(app, "Already downloaded — find it under More › Offline", Toast.LENGTH_LONG).show();
                return;
            }
        }
        boolean fresh = d == null;
        if (fresh) d = new DownloadItem();
        d.tmdbId = item.id;
        d.type = item.mediaType;
        d.season = s;
        d.episode = e;
        d.title = item.displayTitle();
        d.posterPath = item.posterPath != null ? item.posterPath : "";
        d.backdropPath = item.backdropPath != null ? item.backdropPath : "";
        d.imdbId = item.imdbId != null ? item.imdbId : "";
        d.overview = item.overview != null ? item.overview : "";
        d.rating = item.rating;
        d.status = DownloadItem.QUEUED;
        d.progress = 0;
        d.error = null;
        d.localPath = null;
        d.totalBytes = 0;
        d.addedAt = System.currentTimeMillis();
        if (fresh) d.id = (int) dao.insert(d); else dao.update(d);
        com.fountainhome.streaming.service.AppPreferences.setDlOverride(app, d.id, quality != null ? quality : "");

        start(app, d.id);
        Toast.makeText(app, "Download started — progress is in the notification and under More › Downloads",
            Toast.LENGTH_LONG).show();
    }

    public static void retry(Context ctx, DownloadItem d) {
        Context app = ctx.getApplicationContext();
        DownloadDao dao = AppDatabase.get(app).downloadDao();
        dao.setState(d.id, DownloadItem.QUEUED, 0, null);
        start(app, d.id);
    }

    /** Stop a running download but keep what is already downloaded. */
    public static void pause(Context ctx, DownloadItem d) {
        Context app = ctx.getApplicationContext();
        AppDatabase.get(app).downloadDao().setStatus(d.id, DownloadItem.PAUSED, null);
        Intent i = new Intent(app, DownloadService.class)
            .setAction(DownloadService.ACTION_PAUSE).putExtra(DownloadService.EXTRA_ID, d.id);
        try { app.startService(i); } catch (Exception ignored) {}
    }

    /** Continue a paused download from where it stopped. */
    public static void resume(Context ctx, DownloadItem d) {
        Context app = ctx.getApplicationContext();
        AppDatabase.get(app).downloadDao().setStatus(d.id, DownloadItem.QUEUED, null);
        start(app, d.id);
    }

    /** Removes the half-finished files of a download that never completed. */
    public static void cleanPartials(Context ctx, DownloadItem d) {
        try {
            File dir = new File(offlineRoot(ctx), safeName(d.title));
            final String base = "movie".equals(d.type) ? safeName(d.title) : String.format("S%02dE%02d", d.season, d.episode);
            File[] parts = dir.listFiles((x, n) -> n.startsWith(base + ".") && (n.endsWith(".part") || n.endsWith(".part.state")));
            if (parts != null) for (File f : parts) f.delete();
            String[] left = dir.list();
            if (left != null && left.length == 0) dir.delete();
        } catch (Throwable ignored) { }
    }

    /** Stop (if running) and remove the download together with its file. */
    public static void delete(Context ctx, DownloadItem d) {
        Context app = ctx.getApplicationContext();
        if (d.isActive()) {
            Intent i = new Intent(app, DownloadService.class)
                .setAction(DownloadService.ACTION_CANCEL).putExtra(DownloadService.EXTRA_ID, d.id);
            try { app.startService(i); } catch (Exception ignored) {}
        }
        if (d.localPath != null) {
            File f = new File(d.localPath);
            SubtitleFetcher.deleteFor(f);
            f.delete();
            File dir = f.getParentFile();
            if (dir != null && !dir.equals(offlineRoot(app))) {
                String[] left = dir.list();
                if (left != null && left.length == 0) dir.delete();   // tidy empty show folders
            }
        }
        if (!DownloadItem.COMPLETED.equals(d.status)) cleanPartials(app, d);
        AppDatabase.get(app).downloadDao().delete(d);
    }

    private static void start(Context app, int id) {
        Intent i = new Intent(app, DownloadService.class)
            .setAction(DownloadService.ACTION_START).putExtra(DownloadService.EXTRA_ID, id);
        ContextCompat.startForegroundService(app, i);
    }

    /** Movies have no season/episode; TV and anime do. */
    private static int norm(String type, int v) {
        return "movie".equals(type) ? 0 : v;
    }
}
