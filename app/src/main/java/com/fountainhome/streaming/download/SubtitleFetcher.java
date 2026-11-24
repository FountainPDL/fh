package com.fountainhome.streaming.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * Saves English subtitles next to a downloaded video as "&lt;video&gt;.en.vtt" or ".en.srt".
 * Source 1: the subtitle track listed in the stream's own HLS playlist. Source 2: a public subtitle
 * lookup by TMDB id. Everything here is best effort: a failure never fails the video download.
 */
public final class SubtitleFetcher {
    private SubtitleFetcher() {}

    private static final String[] EXTS = {".user.srt", ".user.vtt", ".user.ass", ".en.srt", ".en.vtt"};

    /** The subtitle file that belongs to a video, or null. */
    public static File find(File video) {
        if (video == null) return null;
        String n = video.getName();
        String stem = n.contains(".") ? n.substring(0, n.lastIndexOf('.')) : n;
        for (String e : EXTS) {
            File f = new File(video.getParentFile(), stem + e);
            if (f.isFile() && f.length() > 0) return f;
        }
        return null;
    }

    /** Deletes every subtitle sidecar of a video (used when the download is deleted). */
    public static void deleteFor(File video) {
        if (video == null) return;
        String n = video.getName();
        String stem = n.contains(".") ? n.substring(0, n.lastIndexOf('.')) : n;
        for (String e : EXTS) //noinspection ResultOfMethodCallIgnored
            new File(video.getParentFile(), stem + e).delete();
    }

    /** Returns the saved file or null if no English subtitles could be found. */
    public static File fetch(OkHttpClient http, String streamUrl, Map<String, String> headers,
                             String type, int tmdbId, int season, int episode, File video) {
        String n = video.getName();
        String stem = n.contains(".") ? n.substring(0, n.lastIndexOf('.')) : n;
        File dir = video.getParentFile();
        File got = null;
        try { got = fromPlaylist(http, streamUrl, headers, new File(dir, stem + ".en.vtt")); } catch (Throwable ignored) { }
        if (got == null && tmdbId > 0 && ("movie".equals(type) || "tv".equals(type))) {
            try { got = fromLookup(http, type, tmdbId, season, episode, new File(dir, stem + ".en.srt")); } catch (Throwable ignored) { }
        }
        return got;
    }

    // ---- 1. subtitle rendition inside the HLS master playlist
    private static File fromPlaylist(OkHttpClient http, String url, Map<String, String> headers, File out) throws Exception {
        if (url == null || !url.toLowerCase().contains(".m3u8")) return null;
        String master = new String(HlsDownloader.getBytes(http, url, headers), StandardCharsets.UTF_8);
        String best = null;
        int bestScore = 0;
        for (String line : master.split("\n")) {
            line = line.trim();
            if (!line.startsWith("#EXT-X-MEDIA") || !"SUBTITLES".equalsIgnoreCase(HlsDownloader.attr(line, "TYPE"))) continue;
            String uri = HlsDownloader.attr(line, "URI");
            if (uri == null) continue;
            String lang = String.valueOf(HlsDownloader.attr(line, "LANGUAGE")).toLowerCase();
            String name = String.valueOf(HlsDownloader.attr(line, "NAME")).toLowerCase();
            int score = 0;
            if (lang.equals("en") || lang.startsWith("en-") || lang.equals("eng")) score = 3;
            else if (name.contains("english")) score = 2;
            if (score > 0 && name.contains("forced")) score = 1;
            if (score > bestScore) { bestScore = score; best = HlsDownloader.resolve(url, uri); }
        }
        if (best == null) return null;
        String text;
        if (best.toLowerCase().contains(".vtt") || best.toLowerCase().contains(".webvtt")) {
            text = new String(HlsDownloader.getBytes(http, best, headers), StandardCharsets.UTF_8);
        } else {
            // a playlist of WebVTT segments: join them, keeping a single WEBVTT header
            String pl = new String(HlsDownloader.getBytes(http, best, headers), StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder("WEBVTT\n\n");
            for (String l : pl.split("\n")) {
                l = l.trim();
                if (l.isEmpty() || l.startsWith("#")) continue;
                String seg = new String(HlsDownloader.getBytes(http, HlsDownloader.resolve(best, l), headers), StandardCharsets.UTF_8);
                int blank = seg.indexOf("\n\n");
                sb.append(seg.startsWith("WEBVTT") && blank >= 0 ? seg.substring(blank + 2) : seg).append("\n\n");
            }
            text = sb.toString();
        }
        if (!text.contains("-->")) return null;
        write(out, text.getBytes(StandardCharsets.UTF_8));
        return out;
    }

    // ---- 2. public lookup by TMDB id (JSON list of subtitle files)
    private static File fromLookup(OkHttpClient http, String type, int tmdbId, int season, int episode, File out) throws Exception {
        StringBuilder u = new StringBuilder("https://sub.wyzie.ru/search?id=").append(tmdbId)
            .append("&language=en&format=srt");
        if ("tv".equals(type)) u.append("&season=").append(Math.max(1, season)).append("&episode=").append(Math.max(1, episode));
        String json = new String(HlsDownloader.getBytes(http, u.toString(), null), StandardCharsets.UTF_8);
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement el : arr) {
            if (!el.isJsonObject() || !el.getAsJsonObject().has("url")) continue;
            if (el.getAsJsonObject().has("isHearingImpaired") && el.getAsJsonObject().get("isHearingImpaired").getAsBoolean() && arr.size() > 1) continue;
            String sub = el.getAsJsonObject().get("url").getAsString();
            byte[] data = HlsDownloader.getBytes(http, sub, null);
            String text = new String(data, StandardCharsets.UTF_8);
            if (!text.contains("-->")) continue;
            write(out, data);
            return out;
        }
        return null;
    }

    private static void write(File f, byte[] data) throws Exception {
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(data); }
    }
}
