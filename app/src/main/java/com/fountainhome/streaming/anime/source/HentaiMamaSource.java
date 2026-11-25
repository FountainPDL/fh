package com.fountainhome.streaming.anime.source;

import org.json.JSONArray;
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

/** hentaimama.io (18+). Mirrors come from an admin-ajax endpoint and end in an HLS playlist. */
public class HentaiMamaSource extends AnimeSource {
    private static final String HOST = "hentaimama.io";
    private static final String BASE = "https://" + HOST;
    private static final Pattern EPNUM = Pattern.compile("(?i)\\b(?:episode|ep)\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern SERIES = Pattern.compile("/tvshows/[^/]+/");
    private static final Pattern EPISODE = Pattern.compile("/episodes/[^/]+/");

    @Override public String id() { return "hentaimama"; }
    @Override public String name() { return "HentaiMama"; }
    @Override public boolean adult() { return true; }

    private static boolean isPath(String url, Pattern p) {
        try {
            URI u = new URI(url);
            return AnimeNet.sameSite(url, HOST) && u.getRawQuery() == null && p.matcher(u.getPath()).matches();
        } catch (Exception e) { return false; }
    }

    private static List<String> tags(Element root, String sel) {
        List<String> t = new ArrayList<>();
        for (Element e : root.select(sel)) { String s = e.text().trim(); if (!s.isEmpty()) t.add(s); }
        return t;
    }

    private static Document series(String url) throws IOException {
        if (!isPath(url, SERIES)) throw new IOException("Invalid HentaiMama series URL");
        return Jsoup.parse(AnimeNet.get(url, null), url);
    }

    private static List<String> validate(Document doc) throws IOException {
        List<String> t = tags(doc, ".dsc-genres a[rel=tag]");
        if (t.isEmpty()) throw new IOException("HentaiMama did not provide content tags");
        return t;
    }

    private static String cover(Element img) {
        if (img == null) return null;
        String s = img.absUrl("src");
        return s.startsWith("https://" + HOST + "/") ? s : null;
    }

    @Override public List<Entry> latest(int page) throws IOException {
        String url = page <= 1 ? BASE + "/tvshows/" : BASE + "/tvshows/page/" + page + "/";
        Document doc = Jsoup.parse(AnimeNet.get(url, null), url);
        Map<String, Entry> items = new LinkedHashMap<>();
        for (Element card : doc.select("article.item.tvshows")) {
            Element link = card.selectFirst(".data h3 a[href]");
            if (link == null) continue;
            String u = link.absUrl("href"), title = link.text().trim();
            if (!isPath(u, SERIES) || title.isEmpty() || items.containsKey(u)) continue;
            items.put(u, new Entry(title, u, cover(card.selectFirst(".poster img"))));
        }
        return AnimeNet.filterParallel(new ArrayList<>(items.values()), e -> {
            List<String> t = validate(series(e.url));
            t.add(e.title);
            return !AdultFilter.blocked(t);
        });
    }

    @Override public List<Entry> search(String q) throws IOException {
        if (q.trim().isEmpty() || AdultFilter.blocked(q)) return new ArrayList<>();
        String url = BASE + "/?s=" + AnimeNet.enc(q.trim());
        Document doc = Jsoup.parse(AnimeNet.get(url, null), url);
        Map<String, Entry> items = new LinkedHashMap<>();
        for (Element card : doc.select("article.series-card")) {
            Element link = card.selectFirst(".sc-title a[href]");
            if (link == null) continue;
            String u = link.absUrl("href"), title = link.text().trim();
            List<String> t = tags(card, ".sc-genres a[rel=tag]");
            if (!isPath(u, SERIES) || title.isEmpty() || t.isEmpty()) continue;
            t.add(title);
            if (AdultFilter.blocked(t)) continue;
            if (!items.containsKey(u)) items.put(u, new Entry(title, u, cover(card.selectFirst(".sc-poster img"))));
        }
        return new ArrayList<>(items.values());
    }

    @Override public List<Ep> episodes(Entry s) throws IOException {
        Document doc = series(s.url);
        List<String> t = validate(doc);
        Element h = doc.selectFirst("h1.dsc-title");
        if (h != null) t.add(h.text());
        if (AdultFilter.blocked(t)) throw new IOException("Blocked: this title is tagged as depicting minors");
        Map<String, Ep> out = new LinkedHashMap<>();
        for (Element a : doc.select(".dt-se-list a.dt-se-item[href]")) {
            String url = a.absUrl("href");
            if (!isPath(url, EPISODE)) continue;
            Element te = a.selectFirst(".dt-se-title"), ne = a.selectFirst(".dt-se-num");
            String title = te != null ? te.text().trim() : "";
            String num = ne != null && !ne.text().trim().isEmpty() ? ne.text().trim() : title;
            if (!out.containsKey(url)) out.put(url, new Ep(title.isEmpty() ? num : title, url, AnimeNet.number(EPNUM, num)));
        }
        if (out.isEmpty()) throw new IOException("HentaiMama returned no available episodes");
        List<Ep> l = new ArrayList<>(out.values());
        Collections.sort(l, (x, y) -> Float.compare(x.number, y.number));
        return l;
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        if (!isPath(ep.url, EPISODE)) throw new IOException("Invalid HentaiMama episode URL");
        Document page = Jsoup.parse(AnimeNet.get(ep.url, null), ep.url);
        List<String> tags = tags(page, ".ep-genres a[rel=tag]");
        if (tags.isEmpty()) throw new IOException("HentaiMama episode tags not found");
        if (AdultFilter.blocked(tags)) throw new IOException("Blocked: this episode is tagged as depicting minors");
        String post = null;
        Element dl = page.selectFirst("#dt-dl-modal[data-post]");
        if (dl != null && dl.attr("data-post").matches("\\d+")) post = dl.attr("data-post");
        if (post == null) { Matcher m = Pattern.compile("\"episode\":\"(\\d+)\"").matcher(page.html()); if (m.find()) post = m.group(1); }
        if (post == null) throw new IOException("HentaiMama episode id not found");
        List<Option> out = new ArrayList<>();
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (Element a : page.select(".dt-mi-tabs a[href^=#option-]")) {
            int idx;
            try { idx = Integer.parseInt(a.attr("href").substring("#option-".length())); } catch (Exception e) { continue; }
            if (!seen.add(idx)) continue;
            String label = a.text().trim();
            out.add(new Option(label.isEmpty() ? "Mirror " + idx : label, false, "hentaimama-mirror:" + post + ":" + idx, ep.url));
        }
        if (out.isEmpty()) throw new IOException("HentaiMama did not list any mirrors");
        return out;
    }

    @Override public Stream resolve(Option o) throws IOException {
        if (!o.url.startsWith("hentaimama-mirror:")) return super.resolve(o);
        String[] p = o.url.split(":");
        String post = p[1];
        int index = Integer.parseInt(p[2]);
        String response = AnimeNet.post(BASE + "/wp-admin/admin-ajax.php",
            AnimeNet.map("action", "get_player_contents", "a", post, "i", String.valueOf(index)),
            AnimeNet.map("Referer", o.referer));
        String html;
        try { html = new JSONArray(response).optString(index - 1); } catch (Exception e) { html = ""; }
        Element ifr = html.isEmpty() ? null : Jsoup.parseBodyFragment(html, BASE + "/").selectFirst("iframe[src]");
        String embed = ifr != null ? ifr.absUrl("src") : "";
        if (!AnimeNet.sameSite(embed, HOST) || !embed.contains("dt_embed=hls")) throw new IOException("HentaiMama mirror is empty");
        String page = AnimeNet.get(embed, AnimeNet.map("Referer", o.referer)).replace("\\/", "/");
        Matcher m = Pattern.compile("\"file\"\\s*:\\s*\"([^\"]+?\\.m3u8)\"").matcher(page);
        if (!m.find()) throw new IOException("HentaiMama mirror has no supported HLS stream");
        String url = m.group(1);
        if (!AnimeNet.sameSite(url, "hentaidoge.org")) throw new IOException("HentaiMama mirror uses an unknown host");
        return new Stream(url, AnimeNet.map("Referer", BASE + "/"));
    }
}
