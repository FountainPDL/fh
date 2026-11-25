package com.fountainhome.streaming.anime.source;

import android.content.Context;
import android.content.SharedPreferences;

import com.fountainhome.streaming.service.AppPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Connects an AniList title to the anime websites: finds the same show on a site (fuzzy title match, remembered),
 * lists its real episodes, and turns an episode into a direct stream. All methods block (worker thread only).
 */
public final class AnimeLookup {
    private AnimeLookup() {}

    /** The title as AniList knows it. */
    public static class Ref {
        public int id, episodes;
        public String english, romaji;
        public Ref(int id, String english, String romaji, int episodes) {
            this.id = id; this.english = english; this.romaji = romaji; this.episodes = episodes;
        }
        List<String> queries() {
            Set<String> q = new LinkedHashSet<>();
            for (String t : new String[]{english, romaji}) {
                if (t == null || t.trim().length() < 2) continue;
                q.add(t.trim());
                int colon = t.indexOf(':');
                if (colon > 3) q.add(t.substring(0, colon).trim());
            }
            return new ArrayList<>(q);
        }
    }

    public static final String[] SITE_NAMES = {"Auto", "Anikoto", "AnimeFlixer", "AnimePahe"};

    private static final List<AnimeSource> ALL = Arrays.asList(new AnikotoSource(), new AnimeFlixerSource(), new AnimePaheSource());
    private static final Gson GSON = new Gson();

    private static List<AnimeSource> ordered(Context c) {
        String pref = AppPreferences.getAnimeSite(c);
        List<AnimeSource> out = new ArrayList<>();
        for (AnimeSource s : ALL) if (s.name().equalsIgnoreCase(pref)) out.add(s);
        for (AnimeSource s : ALL) if (!out.contains(s)) out.add(s);
        return out;
    }

    private static AnimeSource byId(String id) {
        for (AnimeSource s : ALL) if (s.id().equals(id)) return s;
        return null;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("anime_lookup", Context.MODE_PRIVATE);
    }

    /** Remember an AniList title's names so downloads (which only know the id) can still match it. */
    public static void remember(Context c, Ref r) {
        prefs(c).edit().putString("ref_" + r.id, GSON.toJson(r)).apply();
    }

    public static Ref recall(Context c, int id, String fallbackTitle) {
        String j = prefs(c).getString("ref_" + id, null);
        if (j != null) { try { Ref r = GSON.fromJson(j, Ref.class); if (r != null) return r; } catch (Exception ignored) { } }
        return new Ref(id, fallbackTitle, null, 0);
    }

    // ------------------------------------------------------------------ matching

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("\\(.*?\\)", " ")
            .replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static double score(String a, String b) {
        String x = norm(a), y = norm(b);
        if (x.isEmpty() || y.isEmpty()) return 0;
        if (x.equals(y)) return 1;
        Set<String> tx = new HashSet<>(Arrays.asList(x.split(" "))), ty = new HashSet<>(Arrays.asList(y.split(" ")));
        Set<String> inter = new HashSet<>(tx);
        inter.retainAll(ty);
        Set<String> union = new HashSet<>(tx);
        union.addAll(ty);
        double j = (double) inter.size() / union.size();
        if (x.contains(y) || y.contains(x)) j = Math.max(j, 0.8);
        return j;
    }

    private static class Hit { AnimeSource src; AnimeSource.Entry entry; double score; }

    /** Best match per site, best first. */
    private static List<Hit> matches(Context c, Ref ref) {
        List<Hit> hits = new ArrayList<>();
        for (AnimeSource s : ordered(c)) {
            Hit best = null;
            for (String q : ref.queries()) {
                try {
                    List<AnimeSource.Entry> r = s.search(q);
                    for (int i = 0; i < Math.min(r.size(), 10); i++) {
                        AnimeSource.Entry e = r.get(i);
                        double sc = 0;
                        for (String t : ref.queries()) sc = Math.max(sc, score(t, e.title));
                        if (ref.romaji != null) sc = Math.max(sc, score(ref.romaji, e.title));
                        if (best == null || sc > best.score) { best = new Hit(); best.src = s; best.entry = e; best.score = sc; }
                    }
                    if (best != null && best.score >= 0.95) break;
                } catch (IOException ignored) { }
            }
            if (best != null && best.score >= 0.6) hits.add(best);
        }
        java.util.Collections.sort(hits, (a, b) -> Double.compare(b.score, a.score));
        return hits;
    }

    // ------------------------------------------------------------------ episodes

    public static class Series {
        public String sourceId, url, title;
        public List<AnimeSource.Ep> episodes = new ArrayList<>();
        public long fetchedAt;
    }

    private static File cacheFile(Context c, int id) {
        File d = new File(c.getCacheDir(), "anime_eps");
        if (!d.exists()) d.mkdirs();
        return new File(d, id + ".json");
    }

    private static Series readCache(Context c, int id) {
        File f = cacheFile(c, id);
        if (!f.isFile()) return null;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            return GSON.fromJson(r, new TypeToken<Series>() { }.getType());
        } catch (Exception e) { return null; }
    }

    private static void writeCache(Context c, int id, Series s) {
        try (FileOutputStream o = new FileOutputStream(cacheFile(c, id))) {
            o.write(GSON.toJson(s).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    /** The show's episodes from the best site (cached for 3 hours, stale copy used if the site is down). */
    public static Series series(Context c, Ref ref) throws IOException {
        return series(c, ref, null);
    }

    private static Series series(Context c, Ref ref, Set<String> skipSources) throws IOException {
        Series cached = readCache(c, ref.id);
        boolean usable = cached != null && cached.url != null && (skipSources == null || !skipSources.contains(cached.sourceId));
        if (usable && System.currentTimeMillis() - cached.fetchedAt < 3L * 3600 * 1000 && !cached.episodes.isEmpty()) return cached;
        IOException last = null;
        // the remembered site first, then fresh matches
        if (usable) {
            AnimeSource s = byId(cached.sourceId);
            if (s != null) {
                try {
                    Series fresh = build(s, new AnimeSource.Entry(cached.title, cached.url, null));
                    writeCache(c, ref.id, fresh);
                    return fresh;
                } catch (IOException e) { last = e; }
            }
        }
        for (Hit h : matches(c, ref)) {
            if (skipSources != null && skipSources.contains(h.src.id())) continue;
            try {
                Series s = build(h.src, h.entry);
                writeCache(c, ref.id, s);
                return s;
            } catch (IOException e) { last = e; }
        }
        if (usable && !cached.episodes.isEmpty()) return cached;
        throw last != null ? last : new IOException("This anime was not found on any site");
    }

    private static Series build(AnimeSource src, AnimeSource.Entry e) throws IOException {
        Series s = new Series();
        s.sourceId = src.id(); s.url = e.url; s.title = e.title;
        s.episodes = src.episodes(e);
        java.util.Collections.sort(s.episodes, (a, b) -> Float.compare(a.number, b.number));
        s.fetchedAt = System.currentTimeMillis();
        return s;
    }

    // ------------------------------------------------------------------ streams

    private static AnimeSource.Ep pick(Series s, int number) {
        for (AnimeSource.Ep e : s.episodes) if (Math.abs(e.number - number) < 0.01f) return e;
        if (number >= 1 && number <= s.episodes.size()) return s.episodes.get(number - 1);
        return null;
    }

    public static class Result {
        public AnimeSource.Stream stream;
        public String site, serverLabel;
        public boolean dub;
    }

    /** A direct stream for one episode in the wanted language. Tries every server, then the other sites. */
    public static Result stream(Context c, Ref ref, int episode, boolean dub) throws IOException {
        Set<String> failed = new HashSet<>();
        IOException last = null;
        for (int round = 0; round < ALL.size(); round++) {
            Series s;
            try { s = series(c, ref, failed); }
            catch (IOException e) { throw last != null ? last : e; }
            AnimeSource src = byId(s.sourceId);
            AnimeSource.Ep ep = pick(s, episode);
            if (src == null || ep == null) { failed.add(s.sourceId); last = new IOException("Episode " + episode + " is not on " + s.sourceId); continue; }
            try {
                List<AnimeSource.Option> options = src.options(ep);
                List<AnimeSource.Option> wanted = new ArrayList<>();
                for (AnimeSource.Option o : options) if (o.dub == dub) wanted.add(o);
                boolean fellBack = wanted.isEmpty();
                if (fellBack) wanted = options;
                for (AnimeSource.Option o : wanted) {
                    try {
                        Result r = new Result();
                        r.stream = src.resolve(o);
                        r.site = src.name(); r.serverLabel = o.label; r.dub = o.dub;
                        return r;
                    } catch (IOException e) { last = e; }
                }
            } catch (IOException e) { last = e; }
            failed.add(s.sourceId);
            cacheFile(c, ref.id).delete();
        }
        throw last != null ? last : new IOException("No playable stream found");
    }
}
