package com.fountainhome.streaming.anime.source;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** animepahetv.to: HTML search, JSON release and server lists, embed players (kwik style). */
public class AnimePaheSource extends AnimeSource {
    private static final String BASE = "https://animepahetv.to";
    private static final Pattern ANIME_ID = Pattern.compile("/anime/([0-9a-fA-F]{32})");

    @Override public String id() { return "animepahe"; }
    @Override public String name() { return "AnimePahe"; }

    @Override public List<Entry> search(String q) throws IOException {
        for (String key : new String[]{"keyword", "q", "s"}) {
            String html;
            try { html = AnimeNet.get(BASE + "/search?" + key + "=" + AnimeNet.enc(q.trim()), null); }
            catch (IOException e) { if (String.valueOf(e.getMessage()).contains("404")) continue; throw e; }
            List<Entry> r = parseListing(html);
            if (!r.isEmpty()) return r;
        }
        return new ArrayList<>();
    }

    private List<Entry> parseListing(String html) {
        Document doc = Jsoup.parse(html, BASE);
        Map<String, Entry> found = new LinkedHashMap<>();
        for (Element a : doc.select("a[href*=/anime/]")) {
            if (inChrome(a)) continue;
            Matcher m = ANIME_ID.matcher(a.absUrl("href"));
            if (!m.find()) continue;
            String url = BASE + "/anime/" + m.group(1);
            Element box = a.selectFirst("img") != null ? a : parentWithImage(a);
            if (box == null) box = a;
            String title = firstNonBlank(a.attr("title"),
                box.selectFirst("img[alt]") != null ? box.selectFirst("img[alt]").attr("alt") : null,
                box.selectFirst(".title, .name, .film-name, h2, h3, h4, h5") != null
                    ? box.selectFirst(".title, .name, .film-name, h2, h3, h4, h5").text() : null, a.text());
            title = title.replaceFirst("^Poster of ", "").trim();
            Element img = box.selectFirst("img");
            String thumb = img == null ? null : firstNonBlank(img.absUrl("src"), img.absUrl("data-src"));
            Entry e = found.get(url);
            if (e == null) found.put(url, new Entry(title, url, thumb == null || thumb.isEmpty() ? null : thumb));
            else {
                if (e.title.isEmpty() && !title.isEmpty()) e.title = title;
                if (e.thumb == null && thumb != null && !thumb.isEmpty()) e.thumb = thumb;
            }
        }
        List<Entry> out = new ArrayList<>();
        for (Entry e : found.values()) if (!e.title.isEmpty()) out.add(e);
        return out;
    }

    private static Element parentWithImage(Element a) {
        for (Element p : a.parents()) if (p.selectFirst("img") != null && p.select("a[href]").size() <= 4) return p;
        return null;
    }

    private static boolean inChrome(Element el) {
        for (Element p : el.parents()) {
            String t = p.tagName();
            if (t.equals("nav") || t.equals("header") || t.equals("footer") || p.hasClass("dropdown-menu") || p.hasClass("navbar")) return true;
        }
        return false;
    }

    private static String firstNonBlank(String... v) {
        for (String s : v) if (s != null && !s.trim().isEmpty()) return s.trim();
        return "";
    }

    @Override public List<Ep> episodes(Entry series) throws IOException {
        Matcher m = ANIME_ID.matcher(series.url);
        if (!m.find()) throw new IOException("Bad AnimePahe URL");
        String animeId = m.group(1);
        Map<String, String> h = AnimeNet.map("Referer", series.url, "Accept", "application/json");
        List<Ep> out = new ArrayList<>();
        int page = 1, last = 1;
        try {
            do {
                JSONObject r = new JSONObject(AnimeNet.get(BASE + "/viewApi?m=release&id=" + animeId
                    + "&sort=episode_desc&page=" + page, h));
                JSONArray data = r.optJSONArray("data");
                if (data != null) for (int i = 0; i < data.length(); i++) {
                    JSONObject o = data.optJSONObject(i);
                    if (o == null) continue;
                    String session = o.optString("session");
                    if (session.isEmpty()) continue;
                    float n = (float) o.optDouble("episode", -1);
                    String title = o.optString("title");
                    String num = n % 1f == 0f ? String.valueOf((int) n) : String.valueOf(n);
                    out.add(new Ep(title.isEmpty() ? "Episode " + num : title, BASE + "/play/" + animeId + "/" + session, n));
                }
                last = Math.max(1, r.optInt("last_page", 1));
                page++;
            } while (page <= last && page < 60);
        } catch (JSONException e) { throw new IOException("AnimePahe sent an unexpected answer"); }
        if (out.isEmpty()) throw new IOException("AnimePahe returned no episodes");
        Collections.sort(out, new Comparator<Ep>() { @Override public int compare(Ep a, Ep b) { return Float.compare(a.number, b.number); } });
        return out;
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        String session = ep.url.substring(ep.url.lastIndexOf('/') + 1).split("[?#]")[0];
        if (session.isEmpty()) throw new IOException("No release session");
        List<Option> out = new ArrayList<>();
        try {
            JSONObject r = new JSONObject(AnimeNet.get(BASE + "/anime/get-servers/" + session,
                AnimeNet.map("Referer", ep.url, "Accept", "application/json")));
            if (!"success".equalsIgnoreCase(r.optString("status"))) throw new IOException("AnimePahe has no servers");
            JSONArray s = r.optJSONArray("servers");
            if (s != null) for (int i = 0; i < s.length(); i++) {
                JSONObject o = s.optJSONObject(i);
                if (o == null) continue;
                String raw = o.optString("url").trim();
                if (raw.isEmpty()) continue;
                String url = raw.startsWith("http") ? raw : raw.startsWith("//") ? "https:" + raw : BASE + (raw.startsWith("/") ? "" : "/") + raw;
                String name = o.optString("name");
                String res = o.optString("resolution");
                String label = name.isEmpty() ? "Server " + (i + 1) : name;
                if (!res.isEmpty() && !res.equalsIgnoreCase("multi")) label += " " + res;
                out.add(new Option(label, name.toLowerCase().contains("dub"), url, ep.url));
            }
        } catch (JSONException e) { throw new IOException("AnimePahe sent an unexpected answer"); }
        if (out.isEmpty()) throw new IOException("AnimePahe returned no servers");
        return out;
    }
}
