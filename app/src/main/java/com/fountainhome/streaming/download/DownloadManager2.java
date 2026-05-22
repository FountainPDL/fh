package com.fountainhome.streaming.download;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadManager2 {

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * Download a movie or episode.
     * Uses Android DownloadManager — works in background, survives app close.
     */
    public static void download(Context ctx, ContentItem item,
                                 int season, int episode, String episodeTitle,
                                 String downloadUrl) {
        executor.execute(() -> {
            // Check if already downloaded
            DownloadItem existing = AppDatabase.get(ctx).downloadDao()
                .find(item.id, item.mediaType, season, episode);
            if (existing != null && "completed".equals(existing.status)) {
                return;
            }

            // Build filename
            String filename;
            if ("movie".equals(item.mediaType)) {
                filename = "FH_" + item.id + "_movie.mp4";
            } else {
                filename = "FH_" + item.id + "_S" + season + "E" + episode + ".mp4";
            }

            File dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "FountainHome");
            if (!dir.exists()) dir.mkdirs();

            // Save metadata first (for offline browsing even before download completes)
            DownloadItem dl = new DownloadItem();
            dl.tmdbId = item.id;
            dl.type = item.mediaType;
            dl.title = item.displayTitle();
            dl.posterPath = item.posterPath;
            dl.season = season;
            dl.episode = episode;
            dl.episodeTitle = episodeTitle;
            dl.localPath = new File(dir, filename).getAbsolutePath();
            dl.status = "downloading";
            dl.addedAt = System.currentTimeMillis();
            dl.imdbId = item.imdbId;
            dl.overview = item.overview;
            dl.rating = item.rating;
            dl.isOfflineReady = false;

            AppDatabase.get(ctx).downloadDao().insert(dl);

            // Kick off DownloadManager
            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                try {
                    DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(downloadUrl))
                        .setTitle("Downloading: " + dl.title)
                        .setDescription(episodeTitle != null ? episodeTitle : "")
                        .setDestinationUri(Uri.fromFile(new File(dl.localPath)))
                        .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(false);

                    long dlId = dm.enqueue(req);

                    // Update with download ID
                    DownloadItem saved = AppDatabase.get(ctx).downloadDao()
                        .find(item.id, item.mediaType, season, episode);
                    if (saved != null) {
                        saved.downloadId = (int) dlId;
                        AppDatabase.get(ctx).downloadDao().update(saved);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // No direct URL — mark as metadata-only (offline browsing of watchlist)
                dl.status = "completed";
                dl.isOfflineReady = false; // can browse but not play
                AppDatabase.get(ctx).downloadDao().update(dl);
            }
        });
    }

    /**
     * Add to offline watchlist (saves metadata only, no video download)
     */
    public static void saveForOffline(Context ctx, ContentItem item) {
        executor.execute(() -> {
            DownloadItem dl = new DownloadItem();
            dl.tmdbId = item.id;
            dl.type = item.mediaType;
            dl.title = item.displayTitle();
            dl.posterPath = item.posterPath;
            dl.season = item.lastSeason;
            dl.episode = item.lastEpisode;
            dl.overview = item.overview;
            dl.rating = item.rating;
            dl.releaseDate = item.releaseDate;
            dl.imdbId = item.imdbId;
            dl.status = "completed";
            dl.isOfflineReady = false;
            dl.addedAt = System.currentTimeMillis();
            AppDatabase.get(ctx).downloadDao().insert(dl);
        });
    }

    /**
     * Check if item has been downloaded
     */
    public static boolean isDownloaded(Context ctx, int tmdbId, String type, int season, int episode) {
        DownloadItem dl = AppDatabase.get(ctx).downloadDao().find(tmdbId, type, season, episode);
        return dl != null && "completed".equals(dl.status)
            && dl.localPath != null && new File(dl.localPath).exists();
    }

    public static boolean isMovieDownloaded(Context ctx, int tmdbId) {
        DownloadItem dl = AppDatabase.get(ctx).downloadDao().findMovie(tmdbId);
        return dl != null && "completed".equals(dl.status)
            && dl.localPath != null && new File(dl.localPath).exists();
    }

    public static String getLocalPath(Context ctx, int tmdbId, String type, int season, int episode) {
        DownloadItem dl = AppDatabase.get(ctx).downloadDao().find(tmdbId, type, season, episode);
        return (dl != null && dl.localPath != null && new File(dl.localPath).exists())
            ? dl.localPath : null;
    }

    public static void deleteDownload(Context ctx, DownloadItem item) {
        executor.execute(() -> {
            if (item.localPath != null) {
                File f = new File(item.localPath);
                if (f.exists()) f.delete();
            }
            AppDatabase.get(ctx).downloadDao().delete(item);
        });
    }
}
