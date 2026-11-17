package com.fountainhome.streaming.service;

import android.content.Context;

import com.fountainhome.streaming.api.Models;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Complete offline copy of a details page: the title's data, every season/episode list,
 * the "more like this" list, and every image the page shows. Everything lives in the app's
 * private storage, so a saved page opens with no network at all.
 */
public final class OfflineStore {
    private OfflineStore() {}

    private static final Gson GSON = new Gson();
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();

    /** What is written to disk for one saved page. */
    public static class Saved {
        public ContentItem item;
        public Map<Integer, Models.SeasonDetail> seasons = new TreeMap<>();
        public List<ContentItem> similar = new ArrayList<>();
        public long savedAt;
    }

    // ------------------------------------------------------------------ paths

    private static File root(Context c) {
        File d = new File(c.getFilesDir(), "offline_details");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static File imgDir(Context c) {
        File d = new File(root(c), "img");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static File jsonFile(Context c, String type, int id) {
        return new File(root(c), type + "_" + id + ".json");
    }

    private static File imgFile(Context c, String path, String size) {
        String key;
        if (path.startsWith("http")) key = "x" + Integer.toHexString(path.hashCode()) + ".img";
        else key = size + path.replace('/', '_');
        return new File(imgDir(c), key);
    }

    // ------------------------------------------------------------------ queries

    public static boolean isSaved(Context c, String type, int id) {
        return jsonFile(c, type, id).isFile();
    }

    /** Blocking disk read; returns null when this title was never saved (or the file is damaged). */
    public static Saved load(Context c, String type, int id) {
        File f = jsonFile(c, type, id);
        if (!f.isFile()) return null;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            Saved s = GSON.fromJson(r, Saved.class);
            if (s == null || s.item == null) return null;
            if (s.seasons == null) s.seasons = new TreeMap<>();
            if (s.similar == null) s.similar = new ArrayList<>();
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    /** All saved pages, newest first. */
    public static List<Saved> list(Context c) {
        List<Saved> out = new ArrayList<>();
        File[] files = root(c).listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return out;
        for (File f : files) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                Saved s = GSON.fromJson(r, Saved.class);
                if (s != null && s.item != null) out.add(s);
            } catch (Exception ignored) {}
        }
        Collections.sort(out, new Comparator<Saved>() {
            @Override public int compare(Saved a, Saved b) { return Long.compare(b.savedAt, a.savedAt); }
        });
        return out;
    }

    // ------------------------------------------------------------------ writing

    /**
     * Cache every image of the page, then write the JSON. Blocking: call from a worker thread.
     * The JSON is written last (via a temp file) so a half-finished save never looks "saved".
     */
    public static void save(Context c, Saved s) throws IOException {
        Context app = c.getApplicationContext();
        ContentItem it = s.item;
        tryCache(app, it.posterPath, "w500");
        tryCache(app, it.posterPath, "w342");
        tryCache(app, it.backdropPath, "w780");
        for (ContentItem sim : s.similar) tryCache(app, sim.posterPath, "w342");
        for (Models.SeasonDetail sd : s.seasons.values()) {
            if (sd == null || sd.episodes == null) continue;
            for (Models.Episode e : sd.episodes) tryCache(app, e.still_path, "w300");
        }
        File out = jsonFile(app, it.mediaType, it.id);
        File tmp = new File(out.getPath() + ".tmp");
        s.savedAt = System.currentTimeMillis();
        try (OutputStream os = new FileOutputStream(tmp)) {
            os.write(GSON.toJson(s).getBytes(StandardCharsets.UTF_8));
        }
        if (out.exists()) out.delete();
        if (!tmp.renameTo(out)) throw new IOException("Could not write offline data");
    }

    public static void remove(Context c, String type, int id) {
        Saved s = load(c, type, id);
        jsonFile(c, type, id).delete();
        if (s == null) return;
        deleteImage(c, s.item.posterPath, "w500");
        deleteImage(c, s.item.posterPath, "w342");
        deleteImage(c, s.item.backdropPath, "w780");
        for (Models.SeasonDetail sd : s.seasons.values()) {
            if (sd == null || sd.episodes == null) continue;
            for (Models.Episode e : sd.episodes) deleteImage(c, e.still_path, "w300");
        }
    }

    // ------------------------------------------------------------------ images

    /**
     * What to give Glide for an image: the cached local file if we have one, otherwise the
     * normal TMDB / AniList URL. Cheap (one File.exists check).
     */
    public static Object image(Context c, String path, String size) {
        if (path == null || path.isEmpty()) return "";
        File f = imgFile(c, path, size);
        if (f.isFile() && f.length() > 0) return f;
        return SourceGenerator.imageUrl(path, size);
    }

    /** Download one image into the offline cache (no-op if already cached). Blocking. */
    public static File cacheImage(Context c, String path, String size) throws IOException {
        if (path == null || path.isEmpty()) return null;
        File f = imgFile(c, path, size);
        if (f.isFile() && f.length() > 0) return f;
        File tmp = new File(f.getPath() + ".tmp");
        Request req = new Request.Builder().url(SourceGenerator.imageUrl(path, size)).build();
        try (Response r = HTTP.newCall(req).execute()) {
            if (!r.isSuccessful() || r.body() == null) throw new IOException("Image HTTP " + r.code());
            try (InputStream in = r.body().byteStream(); OutputStream os = new FileOutputStream(tmp)) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            }
        }
        if (f.exists()) f.delete();
        if (!tmp.renameTo(f)) throw new IOException("Could not cache image");
        return f;
    }

    /** Images are best effort: a missing thumbnail must not stop the page from being saved. */
    private static void tryCache(Context c, String path, String size) {
        try { cacheImage(c, path, size); } catch (IOException ignored) {}
    }

    private static void deleteImage(Context c, String path, String size) {
        if (path == null || path.isEmpty()) return;
        imgFile(c, path, size).delete();
    }
}
