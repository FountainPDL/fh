package com.fountainhome.streaming.service;
import java.util.ArrayList;
import java.util.List;
public class SourceGenerator {
    public static class Source {
        public final String url, label;
        public Source(String u, String l) { url = u; label = l; }
    }

    public static List<Source> getMovieSources(String imdbId, int tmdbId) {
        List<Source> l = new ArrayList<>();
        boolean has = imdbId != null && !imdbId.isEmpty();
        l.add(new Source("https://vsembed.ru/embed/movie?tmdb=" + tmdbId, "VidSrc"));
        l.add(new Source("https://vidlink.pro/movie/" + tmdbId, "VidLink"));
        l.add(new Source("https://vidrock.ru/movie/" + tmdbId, "VidRock"));
        l.add(new Source("https://player.videasy.net/movie/" + tmdbId, "VIDEASY"));
        l.add(new Source("https://vidfast.pro/movie/" + tmdbId, "VidFast"));
        l.add(new Source("https://www.vidking.net/embed/movie/" + tmdbId, "VidKing"));
        l.add(new Source("https://111movies.net/movie/" + tmdbId, "111Movies"));
        l.add(new Source("https://peachify.pro/embed/movie/" + tmdbId, "Peachify"));
        l.add(new Source("https://superflixapi.pro/filme/" + tmdbId, "SuperFlix"));
        l.add(new Source("https://vidnest.fun/movie/" + tmdbId, "VidNest"));
        l.add(new Source("https://www.2embed.online/embed/movie/" + tmdbId, "2Embed"));
        l.add(new Source("https://autoembed.cc/movie/tmdb/" + tmdbId, "AutoEmbed"));
        l.add(new Source("https://multiembed.mov/directstream.php?video_id=" + tmdbId + "&tmdb=1", "SuperEmbed VIP"));
        l.add(new Source("https://multiembed.mov/?video_id=" + tmdbId + "&tmdb=1", "SuperEmbed"));
        if (has) {
            l.add(new Source("https://autoembed.cc/movie/imdb/" + imdbId, "AutoEmbed(I)"));
            l.add(new Source("https://multiembed.mov/directstream.php?video_id=" + imdbId, "SuperEmbed VIP(I)"));
        }
        return l;
    }

    public static List<Source> getTVSources(String imdbId, int tmdbId, int s, int e) {
        List<Source> l = new ArrayList<>();
        boolean has = imdbId != null && !imdbId.isEmpty();
        l.add(new Source("https://vsembed.ru/embed/tv?tmdb=" + tmdbId + "&season=" + s + "&episode=" + e, "VidSrc"));
        l.add(new Source("https://vidlink.pro/tv/" + tmdbId + "/" + s + "/" + e, "VidLink"));
        l.add(new Source("https://vidrock.ru/tv/" + tmdbId + "/" + s + "/" + e, "VidRock"));
        l.add(new Source("https://player.videasy.net/tv/" + tmdbId + "/" + s + "/" + e, "VIDEASY"));
        l.add(new Source("https://vidfast.pro/tv/" + tmdbId + "/" + s + "/" + e, "VidFast"));
        l.add(new Source("https://www.vidking.net/embed/tv/" + tmdbId + "/" + s + "/" + e, "VidKing"));
        l.add(new Source("https://111movies.net/tv/" + tmdbId + "/" + s + "/" + e, "111Movies"));
        l.add(new Source("https://peachify.pro/embed/tv/" + tmdbId + "/" + s + "/" + e, "Peachify"));
        l.add(new Source("https://superflixapi.pro/serie/" + tmdbId + "/" + s + "/" + e, "SuperFlix"));
        l.add(new Source("https://vidnest.fun/tv/" + tmdbId + "/" + s + "/" + e, "VidNest"));
        l.add(new Source("https://www.2embed.online/embed/tv/" + tmdbId + "/" + s + "/" + e, "2Embed"));
        l.add(new Source("https://autoembed.cc/tv/tmdb/" + tmdbId + "/" + s + "/" + e, "AutoEmbed"));
        l.add(new Source("https://multiembed.mov/directstream.php?video_id=" + tmdbId + "&tmdb=1&s=" + s + "&e=" + e, "SuperEmbed VIP"));
        l.add(new Source("https://multiembed.mov/?video_id=" + tmdbId + "&tmdb=1&s=" + s + "&e=" + e, "SuperEmbed"));
        if (has) {
            l.add(new Source("https://autoembed.cc/tv/imdb/" + imdbId + "/" + s + "/" + e, "AutoEmbed(I)"));
            l.add(new Source("https://multiembed.mov/directstream.php?video_id=" + imdbId + "&s=" + s + "&e=" + e, "SuperEmbed VIP(I)"));
        }
        return l;
    }

    // Anime uses its own separate source list — never the movie/TV ones.
    // idMal (MyAnimeList ID, resolved on demand via AniList's own idMal field) is required
    // for VidLink, our preferred/default anime source. Pass 0 if it isn't known yet;
    // VidLink is simply skipped for that attempt.
    public static List<Source> getAnimeSources(int anilistId, int idMal, int season, int episode) {
        List<Source> l = new ArrayList<>();
        if (idMal > 0) {
            l.add(new Source("https://vidlink.pro/anime/" + idMal + "/" + episode + "/sub", "VidLink Sub"));
            l.add(new Source("https://vidlink.pro/anime/" + idMal + "/" + episode + "/dub?fallback=true", "VidLink Dub"));
        }
        l.add(new Source("https://player.videasy.net/anime/" + anilistId + "/" + episode, "VIDEASY"));
        l.add(new Source("https://superflixapi.pro/serie/" + anilistId + "/" + season + "/" + episode, "SuperFlix"));
        l.add(new Source("https://autoembed.cc/anime/anilist/" + anilistId + "/" + episode, "AutoEmbed Sub"));
        l.add(new Source("https://autoembed.cc/anime/anilist/" + anilistId + "/" + episode + "?dubbed=1", "AutoEmbed Dub"));
        l.add(new Source("https://2anime.xyz/embed/" + anilistId + "/" + episode, "2Anime"));
        l.add(new Source("https://yugen.to/embed/" + anilistId + "/" + episode, "Yugen"));
        return l;
    }

    public static String imageUrl(String path, String size) {
        if (path == null || path.isEmpty()) return "";
        if (path.startsWith("http")) return path;
        return "https://image.tmdb.org/t/p/" + size + path;
    }
}
