package com.fountainhome.streaming.anime.source;

import android.content.Context;

import com.fountainhome.streaming.anime.AniListClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 18+ browsing and search. These titles come straight from the adult sites, not from AniList, so each one gets a
 * stable negative id and a remembered "pin" (site + page) that the details page, player and downloads all use.
 * Blocking calls: worker thread only.
 */
public final class AdultCatalog {
    private AdultCatalog() {}

    public static int pseudoId(String sourceId, String url) {
        int h = (sourceId + "|" + url).hashCode();
        return h == Integer.MIN_VALUE ? -1 : -Math.abs(h) - 1;
    }

    private static List<AniListClient.AnimeItem> convert(Context c, AnimeSource s, List<AnimeSource.Entry> entries) {
        List<AniListClient.AnimeItem> out = new ArrayList<>();
        for (AnimeSource.Entry e : entries) {
            AniListClient.AnimeItem a = new AniListClient.AnimeItem();
            a.id = pseudoId(s.id(), e.url);
            a.titleEnglish = e.title;
            a.coverImage = e.thumb;
            a.format = s.name();
            AnimeLookup.Ref r = new AnimeLookup.Ref(a.id, e.title, null, 0);
            r.pinSource = s.id(); r.pinUrl = e.url;
            AnimeLookup.remember(c, r);
            out.add(a);
        }
        return out;
    }

    private interface Job { List<AnimeSource.Entry> run(AnimeSource s) throws IOException; }

    private static List<AniListClient.AnimeItem> fanOut(final Context c, final Job job) throws IOException {
        List<AnimeSource> sources = AnimeLookup.adultSources();
        ExecutorService ex = Executors.newFixedThreadPool(sources.size());
        try {
            List<Future<List<AnimeSource.Entry>>> fs = new ArrayList<>();
            for (final AnimeSource s : sources) fs.add(ex.submit(() -> job.run(s)));
            List<AniListClient.AnimeItem> out = new ArrayList<>();
            IOException last = null;
            boolean any = false;
            for (int i = 0; i < sources.size(); i++) {
                try { out.addAll(convert(c, sources.get(i), fs.get(i).get())); any = true; }
                catch (Exception e) { last = new IOException(sources.get(i).name() + ": " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())); }
            }
            if (!any && last != null) throw last;
            return out;
        } finally { ex.shutdownNow(); }
    }

    public static List<AniListClient.AnimeItem> latest(Context c, final int page) throws IOException {
        return fanOut(c, s -> s.latest(page));
    }

    public static List<AniListClient.AnimeItem> search(Context c, final String q) throws IOException {
        return fanOut(c, s -> s.search(q));
    }
}
