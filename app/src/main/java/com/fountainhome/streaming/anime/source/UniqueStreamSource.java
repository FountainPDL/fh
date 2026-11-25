package com.fountainhome.streaming.anime.source;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** anime.uniquestream.net: JSON API, plays in the site's own player (found by the stream sniffer). */
public class UniqueStreamSource extends AnimeSource {
    private static final String HOST = "anime.uniquestream.net";
    private static final String BASE = "https://" + HOST;
    private static final int PAGE = 20;

    @Override public String id() { return "animestream"; }
    @Override public String name() { return "AnimeStream"; }

    private static String validId(String v) { return v != null && v.matches("[A-Za-z0-9]+") ? v : null; }

    @Override public List<Entry> search(String q) throws IOException {
        List<Entry> out = new ArrayList<>();
        try {
            JSONObject r = new JSONObject(AnimeNet.get(BASE + "/api/v1/search?page=1&limit=" + PAGE + "&query=" + AnimeNet.enc(q.trim()) + "&t=series", null));
            JSONArray a = r.optJSONArray("series");
            if (a == null) throw new IOException("AnimeStream did not return search results");
            Map<String, Entry> seen = new LinkedHashMap<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject it = a.optJSONObject(i);
                if (it == null || !"show".equals(it.optString("type"))) continue;
                String id = validId(it.optString("content_id"));
                String title = it.optString("title").trim();
                if (id == null || title.isEmpty()) continue;
                String slug = title.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$", "");
                String url = BASE + "/series/" + id + "/" + (slug.isEmpty() ? "anime" : slug);
                String img = it.optString("image");
                if (!seen.containsKey(url)) seen.put(url, new Entry(title, url, img.isEmpty() ? null : img));
            }
            out.addAll(seen.values());
        } catch (JSONException e) { throw new IOException("AnimeStream sent an unexpected answer"); }
        return out;
    }

    private JSONObject series(String url) throws IOException {
        String id = null;
        try { String[] p = new URI(url).getPath().split("/"); if (p.length > 2) id = validId(p[2]); } catch (Exception ignored) { }
        if (!AnimeNet.sameSite(url, HOST) || id == null) throw new IOException("Invalid AnimeStream series URL");
        try { return new JSONObject(AnimeNet.get(BASE + "/api/v1/series/" + id, null)); }
        catch (JSONException e) { throw new IOException("AnimeStream sent an unexpected answer"); }
    }

    @Override public List<Ep> episodes(Entry s) throws IOException {
        JSONArray seasons = series(s.url).optJSONArray("seasons");
        if (seasons == null) throw new IOException("AnimeStream did not return a season list");
        Map<String, Ep> out = new LinkedHashMap<>();
        int counter = 0;
        try {
            for (int i = 0; i < seasons.length(); i++) {
                JSONObject se = seasons.optJSONObject(i);
                if (se == null) continue;
                String id = validId(se.optString("content_id"));
                if (id == null) throw new IOException("Invalid AnimeStream season ID");
                String seasonName = se.optString("title").trim();
                for (int page = 1; page < 60; page++) {
                    JSONArray eps = new JSONArray(AnimeNet.get(BASE + "/api/v1/season/" + id + "/episodes?page=" + page + "&limit=" + PAGE + "&order_by=asc", null));
                    int before = out.size();
                    for (int k = 0; k < eps.length(); k++) {
                        JSONObject e = eps.optJSONObject(k);
                        if (e == null) continue;
                        String eid = validId(e.optString("content_id"));
                        if (eid == null) continue;
                        String num = e.optString("episode").trim(), title = e.optString("title").trim();
                        StringBuilder name = new StringBuilder(seasons.length() > 1 ? seasonName : "");
                        if (!num.isEmpty()) name.append(name.length() > 0 ? " - " : "").append("Episode ").append(num);
                        if (!title.isEmpty()) name.append(name.length() > 0 ? " - " : "").append(title);
                        String url = BASE + "/watch/" + eid;
                        if (!out.containsKey(url)) out.put(url, new Ep(name.length() > 0 ? name.toString() : "Episode", url, ++counter));
                    }
                    if (eps.length() < PAGE) break;
                    if (out.size() == before) throw new IOException("AnimeStream repeated an episode page");
                }
            }
        } catch (JSONException e) { throw new IOException("AnimeStream sent an unexpected answer"); }
        if (out.isEmpty()) throw new IOException("AnimeStream returned no episodes");
        return new ArrayList<>(out.values());
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        if (!AnimeNet.sameSite(ep.url, HOST)) throw new IOException("Invalid AnimeStream episode URL");
        List<Option> o = new ArrayList<>();
        o.add(new Option("Site player", false, ep.url, BASE + "/"));
        return o;
    }
}
