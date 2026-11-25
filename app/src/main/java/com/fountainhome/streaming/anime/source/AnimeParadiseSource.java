package com.fountainhome.streaming.anime.source;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** animeparadise.moe: JSON API, episodes play in the site's own player (found by the stream sniffer). */
public class AnimeParadiseSource extends AnimeSource {
    private static final String HOST = "www.animeparadise.moe";
    private static final String BASE = "https://" + HOST;
    private static final String API = "https://api.animeparadise.moe";
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Pattern SERIES = Pattern.compile("^/anime/([A-Za-z0-9_-]+)/?$");

    @Override public String id() { return "animeparadise"; }
    @Override public String name() { return "AnimeParadise"; }

    @Override public List<Entry> search(String q) throws IOException {
        return catalog(API + "/search?q=" + AnimeNet.enc(q.trim()) + "&page=1&limit=25&v=1", false);
    }

    private List<Entry> catalog(String url, boolean recent) throws IOException {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray data = new JSONObject(AnimeNet.get(url, null)).optJSONArray("data");
            if (data == null) throw new IOException("AnimeParadise did not return a catalog");
            Map<String, Entry> seen = new LinkedHashMap<>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) continue;
                JSONObject anime = recent ? item.optJSONObject("origin") : item;
                if (anime == null) continue;
                String slug = anime.optString("link");
                String title = anime.optString("title").trim();
                if (!ID.matcher(slug).matches() || title.isEmpty()) continue;
                JSONObject poster = anime.optJSONObject("posterImage");
                String img = poster != null ? poster.optString("large") : "";
                String u = BASE + "/anime/" + slug;
                if (!seen.containsKey(u)) seen.put(u, new Entry(title, u, img.isEmpty() ? null : img));
            }
            out.addAll(seen.values());
        } catch (JSONException e) { throw new IOException("AnimeParadise sent an unexpected answer"); }
        return out;
    }

    private JSONObject details(String url) throws IOException {
        java.util.regex.Matcher m = SERIES.matcher(url.startsWith(BASE) ? url.substring(BASE.length()) : "");
        if (!AnimeNet.sameSite(url, HOST) || !m.matches()) throw new IOException("Invalid AnimeParadise series URL");
        try {
            JSONObject d = new JSONObject(AnimeNet.get(API + "/anime/" + m.group(1), null)).optJSONObject("data");
            if (d == null) throw new IOException("AnimeParadise did not return series details");
            return d;
        } catch (JSONException e) { throw new IOException("AnimeParadise sent an unexpected answer"); }
    }

    @Override public List<Ep> episodes(Entry s) throws IOException {
        String id = details(s.url).optString("_id");
        if (!ID.matcher(id).matches()) throw new IOException("AnimeParadise returned an invalid series ID");
        List<Ep> out = new ArrayList<>();
        try {
            JSONArray data = new JSONObject(AnimeNet.get(API + "/anime/" + id + "/episode", null)).optJSONArray("data");
            if (data == null) throw new IOException("AnimeParadise did not return episodes");
            Map<String, Ep> seen = new LinkedHashMap<>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject it = data.optJSONObject(i);
                if (it == null) continue;
                String uid = it.optString("uid");
                if (!uid.matches("[A-Za-z0-9-]+")) continue;
                String num = it.optString("number").trim(), title = it.optString("title").trim();
                StringBuilder name = new StringBuilder();
                if (!num.isEmpty()) name.append("Episode ").append(num);
                if (!title.isEmpty()) name.append(name.length() > 0 ? " - " : "").append(title);
                float n; try { n = Float.parseFloat(num); } catch (Exception e) { n = -1; }
                String url = BASE + "/watch/" + uid + "?origin=" + id;
                if (!seen.containsKey(url)) seen.put(url, new Ep(name.length() > 0 ? name.toString() : "Episode", url, n));
            }
            out.addAll(seen.values());
        } catch (JSONException e) { throw new IOException("AnimeParadise sent an unexpected answer"); }
        if (out.isEmpty()) throw new IOException("AnimeParadise returned no episodes");
        Collections.sort(out, (a, b) -> Float.compare(a.number, b.number));
        return out;
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        if (!AnimeNet.sameSite(ep.url, HOST)) throw new IOException("Invalid AnimeParadise episode URL");
        List<Option> o = new ArrayList<>();
        o.add(new Option("Site player", false, ep.url, BASE + "/"));
        return o;
    }
}
