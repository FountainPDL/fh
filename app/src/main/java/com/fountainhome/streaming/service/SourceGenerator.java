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

        // VidSrc (vsembed.ru) — updated URL as requested
        list.add(new Source(
            "https://vsembed.ru/embed/movie?tmdb=" + tmdbId,
            "VidSrc"));

        // 2Embed — updated URLs
        if (imdbId != null && !imdbId.isEmpty()) {
            list.add(new Source(
                "https://www.2embed.online/2embed.php?id=" + imdbId,
                "2Embed"));
            list.add(new Source(
                "https://www.2embed.online/iframe.php?id=" + imdbId,
                "2Embed Alt"));
        } else {
            list.add(new Source(
                "https://www.2embed.online/2embed.php?id=" + tmdbId,
                "2Embed"));
        }

        // AutoEmbed
        if (imdbId != null && !imdbId.isEmpty()) {
            list.add(new Source(
                "https://autoembed.cc/movie/imdb/" + imdbId,
                "AutoEmbed"));
        }
        list.add(new Source(
            "https://autoembed.cc/movie/tmdb/" + tmdbId,
            "AutoEmbed (T)"));

        // SuperEmbed (multiembed VIP — best quality)
        if (imdbId != null && !imdbId.isEmpty()) {
            list.add(new Source(
                "https://multiembed.mov/directstream.php?video_id=" + imdbId,
                "SuperEmbed VIP"));
            list.add(new Source(
                "https://multiembed.mov/?video_id=" + imdbId,
                "SuperEmbed"));
        }
        list.add(new Source(
            "https://multiembed.mov/directstream.php?video_id=" + tmdbId + "&tmdb=1",
            "SuperEmbed VIP (T)"));
        list.add(new Source(
            "https://multiembed.mov/?video_id=" + tmdbId + "&tmdb=1",
            "SuperEmbed (T)"));

        return list;
    }

    public static List<Source> getTVSources(String imdbId, int tmdbId, int season, int episode) {
        List<Source> list = new ArrayList<>();

        // VidSrc (vsembed.ru)
        list.add(new Source(
            "https://vsembed.ru/embed/tv?tmdb=" + tmdbId
                + "&season=" + season + "&episode=" + episode,
            "VidSrc"));

        // 2Embed TV — updated
        if (imdbId != null && !imdbId.isEmpty()) {
            list.add(new Source(
                "https://www.2embed.online/tv-2embed.php?id=" + imdbId
                    + "&season=" + season + "&episode=" + episode,
                "2Embed"));
        } else {
            list.add(new Source(
                "https://www.2embed.online/tv-2embed.php?id=" + tmdbId
                    + "&season=" + season + "&episode=" + episode,
                "2Embed"));
        }

        // AutoEmbed TV
        if (imdbId != null && !imdbId.isEmpty()) {
            list.add(new Source(
                "https://autoembed.cc/tv/imdb/" + imdbId + "/" + season + "/" + episode,
                "AutoEmbed"));
        }
        list.add(new Source(
            "https://autoembed.cc/tv/tmdb/" + tmdbId + "/" + season + "/" + episode,
            "AutoEmbed (T)"));

        // SuperEmbed TV VIP
        if (imdbId != null && !imdbId.isEmpty()) {
            list.add(new Source(
                "https://multiembed.mov/directstream.php?video_id=" + imdbId
                    + "&s=" + season + "&e=" + episode,
                "SuperEmbed VIP"));
            list.add(new Source(
                "https://multiembed.mov/?video_id=" + imdbId
                    + "&s=" + season + "&e=" + episode,
                "SuperEmbed"));
        }
        list.add(new Source(
            "https://multiembed.mov/directstream.php?video_id=" + tmdbId
                + "&tmdb=1&s=" + season + "&e=" + episode,
            "SuperEmbed VIP (T)"));
        list.add(new Source(
            "https://multiembed.mov/?video_id=" + tmdbId
                + "&tmdb=1&s=" + season + "&e=" + episode,
            "SuperEmbed (T)"));

        return list;
    }

    public static String imageUrl(String path, String size) {
        if (path == null || path.isEmpty()) return "";
        return "https://image.tmdb.org/t/p/" + size + path;
    }
}
