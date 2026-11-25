package com.fountainhome.streaming.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;

/**
 * Saves subtitles in the same folder as the video, named exactly like it ("Show S01E02.mp4" -> "Show S01E02.srt"),
 * so the built-in player and every external player (VLC, MX Player, Fountain Play...) pick them up by themselves.
 * Sources, in order: subtitle tracks the stream page itself loaded, the subtitle rendition of the HLS playlist,
 * then a public subtitle lookup by TMDB id. Everything is best effort: a failure never fails the video download.
 */
public final class SubtitleFetcher {
    private SubtitleFetcher() {}

    /** code, English name. Used by the settings screen and the matching below. */
    public static final String[][] LANGS = {
        {"en", "English"}, {"es", "Spanish"}, {"fr", "French"}, {"pt", "Portuguese"}, {"ar", "Arabic"},
        {"de", "German"}, {"it", "Italian"}, {"ja", "Japanese"}, {"ko", "Korean"}, {"ru", "Russian"},
        {"tr", "Turkish"}, {"hi", "Hindi"}, {"id", "Indonesian"}, {"zh", "Chinese"}, {"nl", "Dutch"},
        {"pl", "Polish"}, {"vi", "Vietnamese"}, {"th", "Thai"}
    };

    private static final String[] ALT_NAMES = {
        "en:eng:english", "es:spa:spanish:espanol:español:latino", "fr:fre:fra:french:français:francais",
        "pt:por:portuguese:português:portugues:brazil", "ar:ara:arabic", "de:ger:deu:german:deutsch",
        "it:ita:italian:italiano", "ja:jpn:japanese", "ko:kor:korean", "ru:rus:russian",
        "tr:tur:turkish", "hi:hin:hindi", "id:ind:indonesian", "zh:chi:zho:chinese", "nl:dut:nld:dutch",
        "pl:pol:polish", "vi:vie:vietnamese", "th:tha:thai"
    };

    private static final String[] EXTS = {".user.srt", ".user.vtt", ".user.ass", ".srt", ".vtt", ".ass", ".ssa", ".en.srt", ".en.vtt"};

    private static String stemOf(File video) {
        String n = video.getName();
        return n.contains(".") ? n.substring(0, n.lastIndexOf('.')) : n;
    }

    /** The subtitle file that belongs to a video, or null. */
    public static File find(File video) {
        if (video == null) return null;
        String stem = stemOf(video);
        for (String e : EXTS) {
            File f = new File(video.getParentFile(), stem + e);
            if (f.isFile() && f.length() > 0) return f;
        }
        for (String[] l : LANGS) {                       // e.g. "Show S01E02.fr.srt"
            for (String e : new String[]{".srt", ".vtt"}) {
                File f = new File(video.getParentFile(), stem + "." + l[0] + e);
                if (f.isFile() && f.length() > 0) return f;
            }
        }
        return null;
    }

    /** Deletes every subtitle sidecar of a video (used when the download is deleted). */
    public static void deleteFor(File video) {
        if (video == null) return;
        String stem = stemOf(video);
        for (String e : EXTS) //noinspection ResultOfMethodCallIgnored
            new File(video.getParentFile(), stem + e).delete();
        for (String[] l : LANGS) for (String e : new String[]{".srt", ".vtt"}) //noinspection ResultOfMethodCallIgnored
            new File(video.getParentFile(), stem + "." + l[0] + e).delete();
    }

    /** A subtitle file URL a stream page told us about. */
    public static final class Track {
        public final String url, label;
        public Track(String url, String label) { this.url = url; this.label = label == null ? "" : label; }
    }

    /** Returns the saved file or null if no subtitles in the wanted language could be found. */
    public static File fetch(OkHttpClient http, String streamUrl, Map<String, String> headers,
                             String type, int tmdbId, int season, int episode, File video,
                             List<Track> pageTracks, String lang) {
        if (lang == null || lang.isEmpty()) lang = "en";
        File dir = video.getParentFile();
        File out = new File(dir, stemOf(video) + ".srt");
        String text = null;
        // 1. tracks the stream page itself loaded
        if (pageTracks != null) {
            for (Track t : pageTracks) {
                if (!matchesLang(t.label + " " + t.url, lang)) continue;
                try { text = fetchText(http, t.url, headers); if (text != null) break; } catch (Throwable ignored) { }
            }
        }
        // 2. subtitle rendition inside the HLS master playlist
        if (text == null) { try { text = fromPlaylist(http, streamUrl, headers, lang); } catch (Throwable ignored) { } }
        // 3. public lookup by TMDB id
        if (text == null && tmdbId > 0 && ("movie".equals(type) || "tv".equals(type))) {
            try { text = fromLookup(http, type, tmdbId, season, episode, lang); } catch (Throwable ignored) { }
        }
        if (text == null) return null;
        try {
            String srt = toSrt(text);
            if (srt == null) return null;
            // one subtitle per video: remove older automatic ones first (a file the user loaded stays)
            for (String e : new String[]{".srt", ".vtt", ".ass", ".ssa", ".en.srt", ".en.vtt"}) //noinspection ResultOfMethodCallIgnored
                new File(dir, stemOf(video) + e).delete();
            write(out, srt.getBytes(StandardCharsets.UTF_8));
            return out;
        } catch (Throwable t) { return null; }
    }

    // ------------------------------------------------------------------ language matching

    public static boolean matchesLang(String haystack, String code) {
        String h = haystack.toLowerCase(Locale.ROOT);
        for (String group : ALT_NAMES) {
            String[] names = group.split(":");
            if (!names[0].equals(code)) continue;
            for (int i = 0; i < names.length; i++) {
                String n = names[i];
                if (i == 0 || n.length() == 2) {
                    if (Pattern.compile("(^|[^a-z])" + n + "([^a-z]|$)").matcher(h).find()) return true;
                } else if (h.contains(n)) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ 2. HLS subtitle rendition

    private static String fromPlaylist(OkHttpClient http, String url, Map<String, String> headers, String lang) throws Exception {
        if (url == null || !url.toLowerCase().contains("m3u8")) return null;
        String master = new String(HlsDownloader.getBytes(http, url, headers), StandardCharsets.UTF_8);
        String best = null;
        int bestScore = 0;
        for (String line : master.split("\n")) {
            line = line.trim();
            if (!line.startsWith("#EXT-X-MEDIA") || !"SUBTITLES".equalsIgnoreCase(HlsDownloader.attr(line, "TYPE"))) continue;
            String uri = HlsDownloader.attr(line, "URI");
            if (uri == null) continue;
            String l = String.valueOf(HlsDownloader.attr(line, "LANGUAGE"));
            String name = String.valueOf(HlsDownloader.attr(line, "NAME"));
            int score = 0;
            if (matchesLang(l, lang)) score = 3;
            else if (matchesLang(name, lang)) score = 2;
            if (score > 0 && name.toLowerCase(Locale.ROOT).contains("forced")) score = 1;
            if (score > bestScore) { bestScore = score; best = HlsDownloader.resolve(url, uri); }
        }
        if (best == null) return null;
        String text;
        String low = best.toLowerCase(Locale.ROOT);
        if (low.contains(".vtt") || low.contains(".webvtt") || low.contains(".srt")) {
            text = decode(HlsDownloader.getBytes(http, best, headers));
        } else {
            // a playlist of WebVTT segments: join them, keeping a single WEBVTT header
            String pl = new String(HlsDownloader.getBytes(http, best, headers), StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder("WEBVTT\n\n");
            for (String ln : pl.split("\n")) {
                ln = ln.trim();
                if (ln.isEmpty() || ln.startsWith("#")) continue;
                String seg = decode(HlsDownloader.getBytes(http, HlsDownloader.resolve(best, ln), headers));
                int blank = seg.indexOf("\n\n");
                sb.append(seg.startsWith("WEBVTT") && blank >= 0 ? seg.substring(blank + 2) : seg).append("\n\n");
            }
            text = sb.toString();
        }
        return text.contains("-->") ? text : null;
    }

    // ------------------------------------------------------------------ 3. public lookup

    private static String fromLookup(OkHttpClient http, String type, int tmdbId, int season, int episode, String lang) throws Exception {
        for (String fmt : new String[]{"&format=srt", ""}) {
            StringBuilder u = new StringBuilder("https://sub.wyzie.ru/search?id=").append(tmdbId)
                .append("&language=").append(lang).append(fmt);
            if ("tv".equals(type)) u.append("&season=").append(Math.max(1, season)).append("&episode=").append(Math.max(1, episode));
            JsonArray arr;
            try { arr = JsonParser.parseString(new String(HlsDownloader.getBytes(http, u.toString(), null), StandardCharsets.UTF_8)).getAsJsonArray(); }
            catch (Exception e) { continue; }
            for (int pass = 0; pass < 2; pass++) {            // first without hearing-impaired versions
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    if (!o.has("url")) continue;
                    boolean hi = o.has("isHearingImpaired") && !o.get("isHearingImpaired").isJsonNull() && o.get("isHearingImpaired").getAsBoolean();
                    if (pass == 0 && hi) continue;
                    String t = fetchText(http, o.get("url").getAsString(), null);
                    if (t != null) return t;
                }
            }
        }
        return null;
    }

    private static String fetchText(OkHttpClient http, String url, Map<String, String> headers) throws Exception {
        String t = decode(HlsDownloader.getBytes(http, url, headers));
        return t.contains("-->") || t.contains("Dialogue:") ? t : null;
    }

    // ------------------------------------------------------------------ text handling

    /** UTF-8 if the bytes are valid UTF-8, otherwise Latin-1 (older subtitle files). Strips a BOM. */
    private static String decode(byte[] data) {
        String s;
        try {
            s = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            s = new String(data, StandardCharsets.ISO_8859_1);
        }
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    private static final Pattern VTT_TIME = Pattern.compile(
        "^(?:(\\d+):)?(\\d{2}):(\\d{2})[.,](\\d{1,3})\\s*-->\\s*(?:(\\d+):)?(\\d{2}):(\\d{2})[.,](\\d{1,3}).*$");

    private static String ms(String v) { return (v + "00").substring(0, 3); }

    /** SubRip text from SubRip, WebVTT or ASS/SSA input; null if it has no cues. */
    static String toSrt(String text) {
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        if (text.contains("[Events]") && text.contains("Dialogue:")) return assToSrt(text);
        if (!text.contains("-->")) return null;
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (String block : text.split("\n\n+")) {
            String[] lines = block.trim().split("\n");
            int ti = -1;
            for (int i = 0; i < lines.length; i++) if (lines[i].contains("-->")) { ti = i; break; }
            if (ti < 0) continue;
            Matcher m = VTT_TIME.matcher(lines[ti].trim());
            if (!m.matches()) continue;
            StringBuilder body = new StringBuilder();
            for (int i = ti + 1; i < lines.length; i++) {
                String l = lines[i].replaceAll("<(?!/?[biu]>)[^>]*>", "");
                if (body.length() > 0) body.append('\n');
                body.append(l);
            }
            if (body.toString().trim().isEmpty()) continue;
            out.append(++n).append('\n')
               .append(String.format(Locale.US, "%02d:%s:%s,%s", m.group(1) == null ? 0 : Integer.parseInt(m.group(1)), m.group(2), m.group(3), ms(m.group(4))))
               .append(" --> ")
               .append(String.format(Locale.US, "%02d:%s:%s,%s", m.group(5) == null ? 0 : Integer.parseInt(m.group(5)), m.group(6), m.group(7), ms(m.group(8))))
               .append('\n').append(body).append("\n\n");
        }
        return n == 0 ? null : out.toString();
    }

    private static String assTime(String t) {
        Matcher m = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})[.](\\d{1,2})").matcher(t.trim());
        if (!m.matches()) return null;
        return String.format(Locale.US, "%02d:%s:%s,%s0", Integer.parseInt(m.group(1)), m.group(2), m.group(3),
            (m.group(4) + "0").substring(0, 2).substring(0, 2));
    }

    private static String assToSrt(String text) {
        int start = 1, end = 2, txt = 9;
        List<String> fields = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.startsWith("Format:") && line.contains("Text")) {
                fields.clear();
                for (String f : line.substring(7).split(",")) fields.add(f.trim());
                start = fields.indexOf("Start"); end = fields.indexOf("End"); txt = fields.indexOf("Text");
            } else if (line.startsWith("Dialogue:") && txt >= 0 && start >= 0 && end >= 0) {
                String[] p = line.substring(9).trim().split(",", txt + 1);
                if (p.length <= txt) continue;
                String a = assTime(p[start]), b = assTime(p[end]);
                if (a == null || b == null) continue;
                String body = p[txt].replaceAll("\\{[^}]*\\}", "").replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ").trim();
                if (body.isEmpty()) continue;
                out.append(++n).append('\n').append(a).append(" --> ").append(b).append('\n').append(body).append("\n\n");
            }
        }
        return n == 0 ? null : out.toString();
    }

    private static void write(File f, byte[] data) throws Exception {
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(data); }
    }
}
