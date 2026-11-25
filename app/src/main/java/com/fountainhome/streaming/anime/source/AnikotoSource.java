package com.fountainhome.streaming.anime.source;

import org.json.JSONObject;
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

/** anikototv.to: HTML catalog plus small JSON "ajax" endpoints for episodes and servers. */
public class AnikotoSource extends AnimeSource {
    private static final String BASE = "https://anikototv.to";
    private static final Pattern EP_TAIL = Pattern.compile("/ep-[A-Za-z0-9._-]+/?$");

    @Override public String id() { return "anikoto"; }
    @Override public String name() { return "Anikoto"; }

    @Override public List<Entry> search(String q) throws IOException {
        Document doc = Jsoup.parse(AnimeNet.get(BASE + "/search?keyword=" + AnimeNet.enc(q.trim()), null), BASE);
        Map<String, Entry> out = new LinkedHashMap<>();
        for (Element link : doc.select(".item .info a.name.d-title[href]")) {
            String href = link.absUrl("href");
            String path;
            try { path = new URI(href).getPath(); } catch (Exception e) { continue; }
            path = EP_TAIL.matcher(path == null ? "" : path).replaceAll("");
            if (!path.startsWith("/watch/")) continue;
            String title = link.text().trim();
            if (title.isEmpty()) continue;
            Element inner = link.closest(".inner");
            Element img = inner != null ? inner.selectFirst(".poster img[src]") : null;
            String url = BASE + path;
            if (!out.containsKey(url)) out.put(url, new Entry(title, url, img != null ? img.absUrl("src") : null));
        }
        return new ArrayList<>(out.values());
    }

    private String seriesId(String seriesUrl) throws IOException {
        Document doc = Jsoup.parse(AnimeNet.get(seriesUrl, null), seriesUrl);
        Element w = doc.selectFirst("#watch-main[data-id]");
        String id = w != null ? w.attr("data-id") : "";
        if (!id.matches("\\d+")) throw new IOException("Anikoto did not provide a series ID");
        return id;
    }

    private static String ajax(String body, String what) throws IOException {
        try {
            JSONObject o = new JSONObject(body);
            if (o.optInt("status") != 200) throw new IOException("Anikoto could not load " + what);
            return o.optString("result");
        } catch (org.json.JSONException e) {
            throw new IOException("Anikoto sent an unexpected answer for " + what);
        }
    }

    @Override public List<Ep> episodes(Entry series) throws IOException {
        String id = seriesId(series.url);
        String html = ajax(AnimeNet.get(BASE + "/ajax/episode/list/" + id,
            AnimeNet.map("X-Requested-With", "XMLHttpRequest", "Referer", series.url)), "episodes");
        List<Ep> eps = new ArrayList<>();
        for (Element a : Jsoup.parse(html).select("a[data-id][data-slug][data-num]")) {
            String num = a.attr("data-num").trim();
            Element t = a.selectFirst(".d-title");
            String title = t != null ? t.text().trim() : "";
            String name = "Episode " + num + (!title.isEmpty() && !title.equals("Episode " + num) ? " - " + title : "");
            float n;
            try { n = Float.parseFloat(num); } catch (Exception e) { n = -1; }
            eps.add(new Ep(name, series.url.replaceAll("/+$", "") + "/ep-" + a.attr("data-slug"), n));
        }
        if (eps.isEmpty()) throw new IOException("Anikoto returned no episodes");
        return eps;
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        int cut = ep.url.lastIndexOf("/ep-");
        if (cut < 0) throw new IOException("Bad Anikoto episode URL");
        String slug = ep.url.substring(cut + 4).replaceAll("/+$", "");
        String seriesUrl = ep.url.substring(0, cut);
        String id = seriesId(seriesUrl);
        Map<String, String> h = AnimeNet.map("X-Requested-With", "XMLHttpRequest", "Referer", ep.url);
        String eps = ajax(AnimeNet.get(BASE + "/ajax/episode/list/" + id, h), "episodes");
        String ids = null;
        for (Element a : Jsoup.parse(eps).select("a[data-slug][data-ids]"))
            if (a.attr("data-slug").equals(slug)) { ids = a.attr("data-ids"); break; }
        if (ids == null || ids.isEmpty()) throw new IOException("Anikoto has no servers for this episode");
        String servers = ajax(AnimeNet.get(BASE + "/ajax/server/list?servers=" + AnimeNet.enc(ids), h), "servers");
        List<Option> out = new ArrayList<>();
        for (Element li : Jsoup.parse(servers).select(".servers .type[data-type] li[data-link-id]")) {
            Element ul = li.parent();
            Element type = ul != null ? ul.parent() : null;
            String t = type != null ? type.attr("data-type") : "";
            if (!t.equals("sub") && !t.equals("dub")) continue;
            String linkId = li.attr("data-link-id").trim(), label = li.text().trim();
            if (linkId.isEmpty() || label.isEmpty()) continue;
            out.add(new Option(t.toUpperCase() + " " + label, t.equals("dub"), "anikoto-link:" + linkId, ep.url));
        }
        if (out.isEmpty()) throw new IOException("Anikoto returned no servers");
        return out;
    }

    @Override public Stream resolve(Option o) throws IOException {
        if (!o.url.startsWith("anikoto-link:")) return super.resolve(o);
        String linkId = o.url.substring("anikoto-link:".length());
        JSONObject data;
        try {
            data = new JSONObject(AnimeNet.get(BASE + "/ajax/server?get=" + AnimeNet.enc(linkId),
                AnimeNet.map("X-Requested-With", "XMLHttpRequest", "Referer", o.referer)));
        } catch (org.json.JSONException e) { throw new IOException("Anikoto sent an unexpected answer"); }
        JSONObject res = data.optJSONObject("result");
        String embed = res != null ? res.optString("url") : "";
        if (data.optInt("status") != 200 || !embed.startsWith("https://")) throw new IOException("Anikoto server unavailable");
        return AnimeNet.toStream(embed, o.referer);
    }
}
