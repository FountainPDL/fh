package com.fountainhome.streaming.anime.source;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** hentaiplay.net (18+). Every post is one episode with a direct MP4. */
public class HentaiPlaySource extends AnimeSource {
    private static final String HOST = "hentaiplay.net";
    private static final String BASE = "https://" + HOST;
    private static final Pattern EPNUM = Pattern.compile("(?i)\\bepisode\\s+(\\d+(?:\\.\\d+)?)");

    @Override public String id() { return "hentaiplay"; }
    @Override public String name() { return "HentaiPlay"; }
    @Override public boolean adult() { return true; }

    private static boolean path(String url, String re) {
        try { URI u = new URI(url); return AnimeNet.sameSite(url, HOST) && u.getRawQuery() == null && u.getPath().matches(re); }
        catch (Exception e) { return false; }
    }
    private static boolean isPost(String u) { return path(u, "/[^/]+/"); }
    private static boolean isSeries(String u) { return path(u, "/episode-list/[^/]+/"); }

    private static List<String> tags(Element e) {
        List<String> t = new ArrayList<>();
        for (Element a : e.select("a[rel=tag][href*=/genre/]")) { String s = a.text().trim(); if (!s.isEmpty()) t.add(s); }
        return t;
    }

    private static List<Entry> catalog(Document doc) throws IOException {
        org.jsoup.select.Elements cards = doc.select("div.item.item-post");
        if (cards.isEmpty() && doc.selectFirst(".post.no-results") == null && doc.selectFirst(".loop-content") == null)
            throw new IOException("HentaiPlay catalog markup not found");
        Map<String, Entry> out = new LinkedHashMap<>();
        for (Element card : cards) {
            Element link = card.selectFirst("a.clip-link[href]");
            if (link == null) continue;
            String url = link.absUrl("href"), title = link.attr("title").trim();
            List<String> t = tags(card);
            if (!isPost(url) || title.isEmpty() || t.isEmpty()) continue;
            t.add(title);
            if (AdultFilter.blocked(t)) continue;
            Element img = card.selectFirst("img[src]");
            String cover = img != null ? img.absUrl("src") : "";
            if (!out.containsKey(url)) out.put(url, new Entry(title, url, cover.startsWith("https://") ? cover : null));
        }
        return new ArrayList<>(out.values());
    }

    @Override public List<Entry> latest(int page) throws IOException {
        String url = BASE + "/hentai/episodes/" + (page <= 1 ? "" : "page/" + page + "/");
        return catalog(Jsoup.parse(AnimeNet.get(url, null), url));
    }

    @Override public List<Entry> search(String q) throws IOException {
        if (q.trim().isEmpty() || AdultFilter.blocked(q)) return new ArrayList<>();
        String url = BASE + "/?s=" + AnimeNet.enc(q.trim());
        return catalog(Jsoup.parse(AnimeNet.get(url, null), url));
    }

    private static Document checked(String url) throws IOException {
        if (!isPost(url)) throw new IOException("Invalid HentaiPlay episode URL");
        Document doc = Jsoup.parse(AnimeNet.get(url, null), url);
        Element post = doc.selectFirst("div.post[id^=post-]:not(.item)");
        if (post == null) throw new IOException("HentaiPlay episode metadata not found");
        List<String> t = tags(post);
        if (t.isEmpty()) throw new IOException("HentaiPlay episode tags not found");
        for (Element h : doc.select("h1.entry-title")) t.add(h.text());
        if (AdultFilter.blocked(t)) throw new IOException("Blocked: this episode is tagged as depicting minors");
        return doc;
    }

    private static String nextSeries(Document doc, String seriesUrl) {
        Element n = doc.selectFirst(".wp-pagenavi a.nextpostslink[href], link[rel=next][href]");
        if (n == null) return null;
        String next = n.absUrl("href");
        try {
            URI u = new URI(next);
            String root = new URI(seriesUrl).getPath();
            if (AnimeNet.sameSite(next, HOST) && u.getRawQuery() == null && u.getPath().matches(Pattern.quote(root) + "page/\\d+/")) return next;
        } catch (Exception ignored) { }
        return null;
    }

    @Override public List<Ep> episodes(Entry s) throws IOException {
        Document doc = checked(s.url);
        Element sm = doc.selectFirst(".seriesmetatop a[href]");
        String series = sm != null ? sm.absUrl("href") : null;
        List<Ep> out = new ArrayList<>();
        if (series == null || !isSeries(series)) {
            out.add(new Ep(s.title, s.url, Math.max(1f, AnimeNet.number(EPNUM, s.title))));
            return out;
        }
        Map<String, Ep> eps = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        String next = series;
        while (next != null && seen.add(next) && seen.size() < 30) {
            Document page = Jsoup.parse(AnimeNet.get(next, null), next);
            for (Entry e : catalog(page))
                if (!eps.containsKey(e.url)) eps.put(e.url, new Ep(e.title, e.url, AnimeNet.number(EPNUM, e.title)));
            next = nextSeries(page, series);
        }
        if (eps.isEmpty()) throw new IOException("HentaiPlay returned no available episodes");
        out.addAll(eps.values());
        Collections.sort(out, (x, y) -> Float.compare(x.number, y.number));
        return out;
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        Document doc = checked(ep.url);
        Element src = doc.selectFirst("video source[src]");
        String url = src != null ? src.absUrl("src") : "";
        if (url.isEmpty()) throw new IOException("HentaiPlay did not provide a video");
        if (!AnimeNet.sameSite(url, "hentaiplanet.info") || !url.split("[?#]")[0].toLowerCase().endsWith(".mp4"))
            throw new IOException("HentaiPlay did not provide a supported MP4");
        List<Option> o = new ArrayList<>();
        o.add(new Option("MP4", false, url, BASE + "/"));
        return o;
    }
}
