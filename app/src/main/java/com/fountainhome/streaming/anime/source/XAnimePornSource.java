package com.fountainhome.streaming.anime.source;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

/** xanimeporn.com (18+). Episodes are posts; the download buttons redirect to direct MP4 files. */
public class XAnimePornSource extends AnimeSource {
    private static final String HOST = "xanimeporn.com";
    private static final String BASE = "https://" + HOST;
    private static final Pattern EPNUM = Pattern.compile("(?i)\\bepisode\\s+(\\d+(?:\\.\\d+)?)");
    private static final Pattern QUALITY = Pattern.compile("(?i)(\\d+)p");

    @Override public String id() { return "xanimeporn"; }
    @Override public String name() { return "XAnimePorn"; }
    @Override public boolean adult() { return true; }

    private static boolean isPost(String url) {
        try { URI u = new URI(url); return AnimeNet.sameSite(url, HOST) && u.getPath().matches("/[^/]+/") && u.getQuery() == null; }
        catch (Exception e) { return false; }
    }

    private static List<String> tags(Document doc) {
        List<String> t = new ArrayList<>();
        for (Element e : doc.select("#cat-tag a[rel=tag]")) t.add(e.text().trim());
        return t;
    }

    private static boolean blockedPost(Document doc) throws IOException {
        List<String> t = tags(doc);
        if (t.isEmpty()) throw new IOException("XAnimePorn did not provide content tags");
        for (Element h : doc.select("h1")) t.add(h.text());
        return AdultFilter.blocked(t);
    }

    private static Document post(String url) throws IOException {
        if (!isPost(url)) throw new IOException("Invalid XAnimePorn episode URL");
        return Jsoup.parse(AnimeNet.get(url, null), url);
    }

    private static List<Entry> catalog(String url) throws IOException {
        Document doc = Jsoup.parse(AnimeNet.get(url, null), url);
        if (doc.selectFirst("ul.listing-videos") == null) throw new IOException("XAnimePorn catalog markup was not found");
        Map<String, Entry> items = new LinkedHashMap<>();
        for (Element card : doc.select("ul.listing-videos > li")) {
            Element link = card.selectFirst("a[href][title]");
            if (link == null) continue;
            String u = link.absUrl("href"), title = link.attr("title").trim();
            if (!isPost(u) || title.isEmpty() || AdultFilter.blocked(title) || items.containsKey(u)) continue;
            Element img = card.selectFirst("img");
            String cover = img == null ? "" : (!img.absUrl("data-lazy-src").isEmpty() ? img.absUrl("data-lazy-src") : img.absUrl("src"));
            items.put(u, new Entry(title, u, cover.startsWith("https://") ? cover : null));
        }
        return AnimeNet.filterParallel(new ArrayList<>(items.values()), e -> !blockedPost(post(e.url)));
    }

    @Override public List<Entry> latest(int page) throws IOException {
        return catalog(BASE + "/page/" + Math.max(1, page) + "/?filtre=date");
    }

    @Override public List<Entry> search(String q) throws IOException {
        if (q.trim().isEmpty() || AdultFilter.blocked(q)) return new ArrayList<>();
        return catalog(BASE + "/page/1/?s=" + AnimeNet.enc(q.trim()));
    }

    @Override public List<Ep> episodes(Entry s) throws IOException {
        Document doc = post(s.url);
        if (blockedPost(doc)) throw new IOException("Blocked: this episode is tagged as depicting minors");
        Map<String, Ep> eps = new LinkedHashMap<>();
        for (Element a : doc.select(".su-tabs-pane[data-title=Episodes] a[href]")) {
            String url = a.absUrl("href"), label = a.text().trim();
            if (!isPost(url) || label.isEmpty() || AdultFilter.blocked(label) || eps.containsKey(url)) continue;
            eps.put(url, new Ep(label, url, AnimeNet.number(EPNUM, label)));
        }
        List<Ep> list = new ArrayList<>(eps.values());
        if (list.isEmpty()) list.add(new Ep(s.title, s.url, Math.max(1f, AnimeNet.number(EPNUM, s.title))));
        list = AnimeNet.filterParallel(list, e -> e.url.equals(s.url) || !blockedPost(post(e.url)));
        if (list.isEmpty()) throw new IOException("XAnimePorn returned no available episodes");
        Collections.sort(list, (x, y) -> Float.compare(x.number, y.number));
        return list;
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        Document doc = post(ep.url);
        if (blockedPost(doc)) throw new IOException("Blocked: this episode is tagged as depicting minors");
        List<Option> out = new ArrayList<>();
        Map<String, String> seen = new LinkedHashMap<>();
        for (Element a : doc.select(".su-tabs-pane[data-title=Downloads] a[href]")) {
            String url = a.absUrl("href"), label = a.text().trim();
            try {
                URI u = new URI(url);
                if (!AnimeNet.sameSite(url, "videos.xanimeporn.com") || !"/download.php".equals(u.getPath()) || label.isEmpty()) continue;
            } catch (Exception e) { continue; }
            if (!seen.containsKey(url)) { seen.put(url, label); out.add(new Option(label, false, url, ep.url)); }
        }
        if (out.isEmpty()) throw new IOException("XAnimePorn did not provide downloadable video links");
        Collections.sort(out, (x, y) -> Integer.compare(q(y.label), q(x.label)));
        return out;
    }

    private static int q(String l) { Matcher m = QUALITY.matcher(l); return m.find() ? Integer.parseInt(m.group(1)) : 0; }

    @Override public Stream resolve(Option o) throws IOException {
        Request req = new Request.Builder().url(o.url).header("User-Agent", AnimeNet.UA).header("Referer", o.referer).build();
        try (Response r = AnimeNet.HTTP.newCall(req).execute()) {
            if (!r.isSuccessful()) throw new IOException("XAnimePorn video returned HTTP " + r.code());
            String fin = r.request().url().toString();
            String type = r.header("Content-Type", "");
            if (!AnimeNet.sameSite(fin, "download.xanimeporn.com") || !fin.split("[?#]")[0].toLowerCase().endsWith(".mp4")
                || !type.split(";")[0].trim().equals("video/mp4")) throw new IOException("XAnimePorn did not return a direct MP4");
            return new Stream(fin, AnimeNet.map("Referer", o.referer));
        }
    }
}
