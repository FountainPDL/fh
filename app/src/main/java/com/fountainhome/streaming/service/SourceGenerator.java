package com.fountainhome.streaming.service;

import java.util.ArrayList;
import java.util.List;

public class SourceGenerator {

    public static class Source {
        public final String url;
        public final String label;

        public Source(String url, String label) {
            this.url = url;
            this.label = label;
        }
    }

    public static List<Source> getMovieSources(String imdbId, int tmdbId) {
        List<Source> list = new ArrayList<>();
        boolean hasImdb = imdbId != null && !imdbId.isEmpty();

        if (hasImdb) {
            list.add(new Source("https://vidsrc.xyz/embed/movie/" + imdbId,             "VidSrc"));
            list.add(new Source("https://www.2embed.cc/embed/" + imdbId,                "2Embed"));
            list.add(new Source("https://autoembed.cc/movie/imdb/" + imdbId,            "AutoEmbed"));
            list.add(new Source("https://multiembed.mov/?video_id=" + imdbId,           "MultiEmbed"));
        }

        // TMDB fallbacks always included
        list.add(new Source("https://vidsrc.xyz/embed/movie/" + tmdbId,                 "VidSrc (T)"));
        list.add(new Source("https://www.2embed.cc/embed/tmdb/movie?id=" + tmdbId,      "2Embed (T)"));
        list.add(new Source("https://autoembed.cc/movie/tmdb/" + tmdbId,                "AutoEmbed (T)"));
        list.add(new Source("https://multiembed.mov/?video_id=" + tmdbId + "&tmdb=1",   "MultiEmbed (T)"));

        return list;
    }

    public static List<Source> getTVSources(String imdbId, int tmdbId, int season, int episode) {
        List<Source> list = new ArrayList<>();
        boolean hasImdb = imdbId != null && !imdbId.isEmpty();

        if (hasImdb) {
            list.add(new Source("https://vidsrc.xyz/embed/tv/" + imdbId + "/" + season + "/" + episode,
                "VidSrc"));
            list.add(new Source("https://www.2embed.cc/embedtv/" + imdbId + "&s=" + season + "&e=" + episode,
                "2Embed"));
            list.add(new Source("https://autoembed.cc/tv/imdb/" + imdbId + "/" + season + "/" + episode,
                "AutoEmbed"));
            list.add(new Source("https://multiembed.mov/?video_id=" + imdbId + "&s=" + season + "&e=" + episode,
                "MultiEmbed"));
        }

        list.add(new Source("https://vidsrc.xyz/embed/tv/" + tmdbId + "/" + season + "/" + episode,
            "VidSrc (T)"));
        list.add(new Source("https://www.2embed.cc/embedtv/tmdb/tv?id=" + tmdbId + "&s=" + season + "&e=" + episode,
            "2Embed (T)"));
        list.add(new Source("https://autoembed.cc/tv/tmdb/" + tmdbId + "/" + season + "/" + episode,
            "AutoEmbed (T)"));
        list.add(new Source("https://multiembed.mov/?video_id=" + tmdbId + "&tmdb=1&s=" + season + "&e=" + episode,
            "MultiEmbed (T)"));

        return list;
    }

    public static String imageUrl(String path, String size) {
        if (path == null || path.isEmpty()) return "";
        return "https://image.tmdb.org/t/p/" + size + path;
    }
}
