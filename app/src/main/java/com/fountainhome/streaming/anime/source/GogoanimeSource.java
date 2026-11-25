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
import java.util.regex.Pattern;

/** gogoanime.by: WordPress theme, episode pages carry the player links. */
public class GogoanimeSource extends AnimeSource {
    private static final String HOST = "gogoanime.by";
    private static final String BASE = "https://" + HOST;
    private static final Pattern NUM = Pattern.compile("(?i)(?:episode|ep)\\s*(\\d+(?:\\.\\d+)?)");

    @Override public String id() { return "gogoanime"; }
    @Override public String name() { return "Gogoanime"; }

    @Override public List<Entry> search(String q) throws IOException {
        String url = BASE + "/?s=" + AnimeNet.enc(q.trim()) + "&paged=1";
        Document doc = Jsoup.parse(AnimeNet.get(url, null), url);
        Map<String, Entry> out = new LinkedHashMap<>();
        for (Element link : doc.select("article.bs .bsx a[href]")) {
            String href = link.absUrl("href");
            String title = link.attr("title").trim();
            String path;
            try { path = new URI(href).getPath(); } catch (Exception e) { continue; }
            if (!AnimeNet.sameSite(href, HOST) || path == null || !path.startsWith("/series/") || title.isEmpty()) continue;
            Element img = link.selectFirst("img[src]");
            if (!out.containsKey(href)) out.put(href, new Entry(title, href, img != null ? img.absUrl("src") : null));
        }
        return new ArrayList<>(out.values());
    }

    @Override public List<Ep> episodes(Entry s) throws IOException {
        if (!AnimeNet.sameSite(s.url, HOST)) throw new IOException("Invalid Gogoanime URL");
        Document doc = Jsoup.parse(AnimeNet.get(s.url, null), s.url);
        Map<String, Ep> out = new LinkedHashMap<>();
        for (Element a : doc.select(".episodes-container .episode-item a[href]")) {
            String url = a.absUrl("href"), title = a.text().trim();
            if (!AnimeNet.sameSite(url, HOST) || title.isEmpty() || out.containsKey(url)) continue;
            out.put(url, new Ep(title, url, AnimeNet.number(NUM, title)));
        }
        if (out.isEmpty()) throw new IOException("Gogoanime returned no episodes");
        List<Ep> l = new ArrayList<>(out.values());
        Collections.reverse(l);                       // the site lists newest first
        if (l.get(0).number >= 0) Collections.sort(l, (x, y) -> Float.compare(x.number, y.number));
        return l;
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        if (!AnimeNet.sameSite(ep.url, HOST)) throw new IOException("Invalid Gogoanime URL");
        Document doc = Jsoup.parse(AnimeNet.get(ep.url, null), ep.url);
        List<Option> out = new ArrayList<>();
        for (Element p : doc.select(".player-type-link[data-src]")) {
            String src = p.attr("data-src").trim();
            if (src.startsWith("//")) src = "https:" + src;
            if (!src.startsWith("https://")) continue;
            String label = p.text().trim();
            out.add(new Option(label.isEmpty() ? "Server " + (out.size() + 1) : label, label.toLowerCase().contains("dub"), src, ep.url));
        }
        if (out.isEmpty() && doc.selectFirst(".player-type-link[data-src]") != null)
            out.add(new Option("Site player", false, ep.url, BASE + "/"));
        if (out.isEmpty()) throw new IOException("Gogoanime did not provide a player for this episode");
        return out;
    }
}
