package com.fountainhome.streaming.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.fountainhome.streaming.anime.AniListClient;
import com.fountainhome.streaming.download.AppDatabase;
import com.fountainhome.streaming.download.DownloadDao;
import com.fountainhome.streaming.download.DownloadItem;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.download.HlsDownloader;
import com.fountainhome.streaming.download.Remuxer;
import com.fountainhome.streaming.download.StreamResolver;
import com.fountainhome.streaming.ui.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Foreground service that works through the download queue, one video at a time:
 *   1. find the real stream URL on each server in turn (StreamResolver),
 *   2. download it into the Offline folder (HlsDownloader),
 *   3. record progress / result in the database so the Downloads and Offline pages update live.
 */
public class DownloadService extends Service {
    public static final String ACTION_START = "com.fountainhome.streaming.DL_START";
    public static final String ACTION_CANCEL = "com.fountainhome.streaming.DL_CANCEL";
    public static final String EXTRA_ID = "id";

    private static final String CHANNEL = "fh_downloads";
    private static final int FG_ID = 4242;
    private static final int RESOLVE_TIMEOUT_MS = 20000;
    private static final int MAX_SERVERS = 8;

    private final Handler main = new Handler(Looper.getMainLooper());
    /** Up to MAX_THREADS downloads can exist; how many actually run at once is the "Simultaneous downloads" setting. */
    private static final int MAX_THREADS = 6;
    private final ExecutorService worker = Executors.newFixedThreadPool(MAX_THREADS);
    private final Object gate = new Object();
    private int running = 0;
    private final java.util.Map<Integer, StreamResolver> resolvers = new ConcurrentHashMap<>();
    private final java.util.Map<Integer, Long> lastNotif = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean repaired = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final Set<Integer> cancelled = ConcurrentHashMap.newKeySet();
    private OkHttpClient http;
    private volatile int pending = 0;                       // main thread only

    @Override public void onCreate() {
        super.onCreate();
        http = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS)
            .followRedirects(true).followSslRedirects(true).build();
        createChannel();
        startFg(progressNotif("Fountain Home", "Preparing download…", -1));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) { maybeStop(); return START_NOT_STICKY; }
        final int id = intent.getIntExtra(EXTRA_ID, -1);
        if (ACTION_CANCEL.equals(intent.getAction())) {
            cancelled.add(id);
            main.post(() -> { StreamResolver r = resolvers.get(id); if (r != null) r.cancel(); });
            synchronized (gate) { gate.notifyAll(); }
            maybeStop();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(intent.getAction()) && id >= 0) {
            cancelled.remove(id);
            pending++;
            worker.execute(() -> {
                boolean got = false;
                try {
                    got = acquireSlot(id);
                    if (got) process(id);
                    else dropQueued(id);
                }
                catch (Throwable t) { safeFail(id, "Unexpected error: " + t.getMessage()); }
                finally {
                    if (got) releaseSlot();
                    main.post(() -> { pending--; maybeStop(); });
                }
            });
        }
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        worker.shutdownNow();
        main.post(() -> { for (StreamResolver r : resolvers.values()) r.cancel(); });
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    // ------------------------------------------------------------------ the job

    /** Waits (state stays "queued") until fewer than the allowed number of downloads are running. */
    private boolean acquireSlot(int id) throws InterruptedException {
        synchronized (gate) {
            while (true) {
                if (cancelled.contains(id)) return false;
                int limit = Math.max(1, Math.min(MAX_THREADS, AppPreferences.getParallelDownloads(this)));
                if (running < limit) { running++; updateSummary(); return true; }
                gate.wait(2000);
            }
        }
    }

    private void releaseSlot() {
        synchronized (gate) { running--; gate.notifyAll(); }
        updateSummary();
    }

    private void dropQueued(int id) {
        try {
            DownloadDao dao = AppDatabase.get(this).downloadDao();
            DownloadItem it = dao.byId(id);
            if (it != null) dropCancelled(dao, it);
        } catch (Throwable ignored) { }
    }

    /** The one foreground notification just says how many are going. */
    private void updateSummary() {
        final int n, waiting;
        synchronized (gate) { n = running; }
        waiting = Math.max(0, pending - n);
        try {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(FG_ID,
                progressNotif("Fountain Home downloads",
                    n + (n == 1 ? " download" : " downloads") + " running" + (waiting > 0 ? ", " + waiting + " waiting" : ""), -1));
        } catch (Throwable ignored) { }
    }

    private void process(int id) {
        final DownloadDao dao = AppDatabase.get(this).downloadDao();
        final DownloadItem it = dao.byId(id);
        if (it == null) return;
        if (repaired.compareAndSet(false, true)) repairOldTs(dao);
        if (cancelled.contains(id)) { dropCancelled(dao, it); return; }

        if (AppPreferences.getWifiOnly(this) && !onUnmeteredNetwork()) {
            fail(dao, it, "Waiting for Wi-Fi (\"Wi-Fi only\" is on in Settings)");
            return;
        }
        final String label = label(it);
        dao.setState(id, DownloadItem.RESOLVING, 0, null);
        notifyProgress(id, label, "Finding a download link…", -1, true);

        String lastError = "No downloadable link was found on any server";
        // Anime: ask the anime sites for a direct stream first (no embed page, no ads, real quality choice).
        if ("anime".equals(it.type)) {
            try {
                notifyProgress(id, label, "Finding this episode\u2026", -1, true);
                com.fountainhome.streaming.anime.source.AnimeLookup.Ref ref =
                    com.fountainhome.streaming.anime.source.AnimeLookup.recall(this, it.tmdbId, it.title);
                com.fountainhome.streaming.anime.source.AnimeLookup.Result r =
                    com.fountainhome.streaming.anime.source.AnimeLookup.stream(this, ref, Math.max(1, it.episode),
                        "dub".equals(AppPreferences.getAnimeDubSub(this)));
                StreamResolver.Found direct = new StreamResolver.Found();
                direct.url = r.stream.url;
                direct.headers.putAll(r.stream.headers);
                direct.subs.addAll(r.stream.subs);
                if (cancelled.contains(id)) { dropCancelled(dao, it); return; }
                if (attempt(dao, it, id, label, direct)) return;
            } catch (IOException e) {
                if (cancelled.contains(id)) { dropCancelled(dao, it); return; }
                lastError = e.getMessage() != null ? e.getMessage() : lastError;
            }
        }

        if ("anime".equals(it.type)) { fail(dao, it, lastError); return; }

        List<String> embeds = embedUrls(it);
        if (embeds.isEmpty()) { fail(dao, it, lastError.startsWith("No downloadable") ? "No servers available for this title" : lastError); return; }

        int tried = 0;
        for (String embed : embeds) {
            if (cancelled.contains(id)) { dropCancelled(dao, it); return; }
            if (++tried > MAX_SERVERS) break;
            notifyProgress(id, label, "Checking server " + tried + " of " + Math.min(embeds.size(), MAX_SERVERS) + "…", -1, true);

            StreamResolver.Found found = resolveBlocking(id, embed);
            if (cancelled.contains(id)) { dropCancelled(dao, it); return; }
            if (found == null) continue;

            try {
                if (attempt(dao, it, id, label, found)) return;
            } catch (IOException e) {
                if (cancelled.contains(id)) { dropCancelled(dao, it); return; }
                lastError = e.getMessage() != null ? e.getMessage() : "Download failed";
                // try the next server
            }
        }
        fail(dao, it, lastError);
    }

    /** Downloads one resolved stream, converts it, fetches subtitles and records the result. Returns true when done. */
    private boolean attempt(final DownloadDao dao, final DownloadItem it, final int id, final String label,
                            final StreamResolver.Found found) throws IOException {
        dao.setState(id, DownloadItem.DOWNLOADING, 0, null);
            final String[] qualityNote = new String[1];
            File dir = new File(DownloadManager2.offlineRoot(this), DownloadManager2.safeName(it.title));
            String base = "movie".equals(it.type)
                ? DownloadManager2.safeName(it.title)
                : String.format("S%02dE%02d", it.season, it.episode);
                File out = HlsDownloader.download(http, found.url, found.headers,
                    AppPreferences.effectiveQuality(this, id), dir, base, new HlsDownloader.Progress() {
                        int last = -1;
                        @Override public void onInfo(String info) { qualityNote[0] = info; notifyProgress(id, label, "Quality: " + info, -1, true); }
                        @Override public void onProgress(int pct) {
                            if (pct == last) return;
                            last = pct;
                            dao.setProgress(id, pct);
                            notifyProgress(id, label, pct + "%", pct, false);
                        }
                        @Override public boolean isCancelled() { return cancelled.contains(id); }
                    });
                // Raw .ts has no seek index: convert to .mp4 (no re-encode) so seeking works.
                dao.setProgress(id, 100);
                notifyProgress(id, label, "Converting to MP4\u2026", 0, false);
                out = Remuxer.makeSeekable(out, pct -> notifyProgress(id, label, "Converting to MP4 " + pct + "%", pct, false));
                if (AppPreferences.getSubDownload(this)) {
                    notifyProgress(id, label, "Getting subtitles\u2026", -1, true);
                    try {
                        com.fountainhome.streaming.download.SubtitleFetcher.fetch(http, found.url, found.headers,
                            it.type, it.tmdbId, it.season, it.episode, out, found.subs, AppPreferences.getSubLang(this));
                    } catch (Throwable ignored) { }
                }
                it.localPath = out.getAbsolutePath();
                it.totalBytes = out.length();
                it.status = DownloadItem.COMPLETED;
                it.progress = 100;
                it.error = null;
                dao.update(it);
                cacheArtwork(it);
                notifyDone(it, label, true, qualityNote[0]);
                return true;
    }

    /** Old downloads that are still raw .ts get converted to .mp4 and their records updated. */
    private void repairOldTs(DownloadDao dao) {
        try {
            for (DownloadItem d : dao.getCompletedNow()) {
                if (d.localPath == null) continue;
                File f = new File(d.localPath);
                if (!Remuxer.needsRemux(f)) continue;
                File r = Remuxer.makeSeekable(f, null);
                if (r != f) { d.localPath = r.getAbsolutePath(); d.totalBytes = r.length(); dao.update(d); }
            }
        } catch (Throwable ignored) {}
    }

    /** Runs the resolver on the main thread and waits for its answer on this worker thread. */
    private StreamResolver.Found resolveBlocking(final int id, String embed) {
        final StreamResolver.Found[] holder = new StreamResolver.Found[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final StreamResolver r = new StreamResolver();
        main.post(() -> {
            resolvers.put(id, r);
            try {
                r.resolve(DownloadService.this, embed, RESOLVE_TIMEOUT_MS, found -> {
                    holder[0] = found;
                    latch.countDown();
                });
            } catch (Throwable t) {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(RESOLVE_TIMEOUT_MS + 5000L, TimeUnit.MILLISECONDS)) {
                main.post(r::cancel);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            main.post(r::cancel);
        }
        resolvers.remove(id);
        return holder[0];
    }

    /** All embed URLs for this item, the user's preferred server first. */
    private List<String> embedUrls(DownloadItem it) {
        List<SourceGenerator.Source> list;
        if ("anime".equals(it.type)) {
            int mal = AniListClient.getMalIdSync(it.tmdbId);
            list = SourceGenerator.getAnimeSources(it.tmdbId, mal, Math.max(1, it.season), Math.max(1, it.episode));
        } else if ("movie".equals(it.type)) {
            list = SourceGenerator.getMovieSources(it.imdbId, it.tmdbId);
        } else {
            list = SourceGenerator.getTVSources(it.imdbId, it.tmdbId, Math.max(1, it.season), Math.max(1, it.episode));
        }
        String pref = "anime".equals(it.type) ? AppPreferences.getAnimeSource(this) : AppPreferences.getSource(this);
        List<String> out = new ArrayList<>();
        for (SourceGenerator.Source s : list) if (s.label.startsWith(pref)) out.add(s.url);
        for (SourceGenerator.Source s : list) if (!s.label.startsWith(pref)) out.add(s.url);
        return out;
    }

    private void cacheArtwork(DownloadItem it) {
        try { OfflineStore.cacheImage(this, it.posterPath, "w342"); } catch (Exception ignored) {}
        try { OfflineStore.cacheImage(this, it.backdropPath, "w780"); } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ outcomes

    private void fail(DownloadDao dao, DownloadItem it, String why) {
        dao.setState(it.id, DownloadItem.FAILED, 0, why);
        notifyDone(it, label(it), false, why);
    }

    private void safeFail(int id, String why) {
        try {
            DownloadDao dao = AppDatabase.get(this).downloadDao();
            DownloadItem it = dao.byId(id);
            if (it != null) fail(dao, it, why);
        } catch (Throwable ignored) {}
    }

    /** User cancelled: remove the row and any partial file. */
    private void dropCancelled(DownloadDao dao, DownloadItem it) {
        DownloadItem fresh = dao.byId(it.id);
        if (fresh != null) dao.delete(fresh);
        File dir = new File(DownloadManager2.offlineRoot(this), DownloadManager2.safeName(it.title));
        try { ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(30000 + it.id); } catch (Throwable ignored) { }
        final String base = "movie".equals(it.type) ? DownloadManager2.safeName(it.title)
            : String.format("S%02dE%02d", it.season, it.episode);
        File[] parts = dir.listFiles((d, n) -> n.endsWith(".part") && n.startsWith(base + "."));
        if (parts != null) for (File f : parts) f.delete();
        String[] left = dir.list();
        if (left != null && left.length == 0) dir.delete();
    }

    private void maybeStop() {
        if (pending > 0) return;
        stopForeground(true);
        stopSelf();
    }

    private boolean onUnmeteredNetwork() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            if (n == null) return false;
            NetworkCapabilities c = cm.getNetworkCapabilities(n);
            return c != null && (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Throwable t) {
            return true;
        }
    }

    // ------------------------------------------------------------------ notifications

    private static String label(DownloadItem it) {
        return "movie".equals(it.type) ? it.title : it.title + "  S" + it.season + " E" + it.episode;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Download progress and results");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private PendingIntent openOfflineIntent() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("open_offline", true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getActivity(this, 1, i, fl);
    }

    private Notification progressNotif(String title, String text, int pct) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title).setContentText(text)
            .setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(openOfflineIntent());
        if (pct >= 0) b.setProgress(100, pct, false); else b.setProgress(0, 0, true);
        return b.build();
    }

    private void startFg(Notification n) {
        if (Build.VERSION.SDK_INT >= 29) startForeground(FG_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(FG_ID, n);
    }

    private void notifyProgress(int id, String title, String text, int pct, boolean force) {
        long now = System.currentTimeMillis();
        Long last = lastNotif.get(id);
        if (!force && last != null && now - last < 800) return;
        lastNotif.put(id, now);
        try {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(30000 + id, progressNotif(title, text, pct));
        } catch (Throwable ignored) {}
    }

    private void notifyDone(DownloadItem it, String title, boolean ok, String why) {
        try {
            NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(ok ? android.R.drawable.stat_sys_download_done : android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(ok ? "Download complete" + (why != null ? " (" + why + ")" : "") + " — watch it under More › Offline" : "Download failed: " + why)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(ok ? "Download complete" : "Download failed: " + why))
                .setAutoCancel(true).setContentIntent(openOfflineIntent());
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(30000 + it.id);
            lastNotif.remove(it.id);
            nm.notify(10000 + it.id, b.build());
        } catch (Throwable ignored) {}
    }
}
