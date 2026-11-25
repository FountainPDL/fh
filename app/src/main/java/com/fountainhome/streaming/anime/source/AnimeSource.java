package com.fountainhome.streaming.anime.source;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** One anime website. Every method blocks, so call them from a worker thread. */
public abstract class AnimeSource {
    public static class Entry {
        public String title, url, thumb;
        public Entry(String title, String url, String thumb) { this.title = title; this.url = url; this.thumb = thumb; }
    }

    public static class Ep {
        public String name, url;
        public float number;
        public Ep(String name, String url, float number) { this.name = name; this.url = url; this.number = number; }
    }

    /** One server of an episode (SUB or DUB); url is an embed page or a direct stream. */
    public static class Option {
        public String label, url, referer;
        public boolean dub;
        public Option(String label, boolean dub, String url, String referer) {
            this.label = label; this.dub = dub; this.url = url; this.referer = referer;
        }
    }

    public static class Stream {
        public final String url;
        public final Map<String, String> headers;
        public Stream(String url, Map<String, String> headers) { this.url = url; this.headers = headers; }
    }

    public abstract String id();
    public abstract String name();
    public abstract List<Entry> search(String query) throws IOException;
    /** Episodes in ascending order. */
    public abstract List<Ep> episodes(Entry series) throws IOException;
    public abstract List<Option> options(Ep ep) throws IOException;

    public Stream resolve(Option o) throws IOException {
        return AnimeNet.toStream(o.url, o.referer);
    }

    static <T> List<T> list() { return new ArrayList<>(); }
}
