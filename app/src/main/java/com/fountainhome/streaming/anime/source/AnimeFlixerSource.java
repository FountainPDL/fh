package com.fountainhome.streaming.anime.source;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** animeflixer.com: a plain JSON API. */
public class AnimeFlixerSource extends AnimeSource {
    private static final String BASE = "https://animeflixer.com";
    private static final String API = "https://api.animeflixer.com/api";
    private static final Pattern SERIES = Pattern.compile("/series/(\\d+)");

    @Override public String id() { return "animeflixer"; }
    @Override public String name() { return "AnimeFlixer"; }

    @Override public List<Entry> search(String q) throws IOException {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AnimeNet.get(API + "/filter?keyword=" + AnimeNet.enc(q.trim()), null));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null || !o.optBoolean("is_series")) continue;
                long id = o.optLong("id");
                String title = o.optString("title").trim();
                if (id <= 0 || title.isEmpty()) continue;
                String th = o.optString("thumbnail_url");
                out.add(new Entry(title, BASE + "/series/" + id, th.isEmpty() ? null : th));
            }
        } catch (JSONException e) { throw new IOException("AnimeFlixer sent an unexpected answer"); }
        return out;
    }

    private JSONObject series(String url) throws IOException {
        Matcher m = SERIES.matcher(url);
        if (!m.find()) throw new IOException("Bad AnimeFlixer series URL");
        try { return new JSONObject(AnimeNet.get(API + "/series/" + m.group(1), null)); }
        catch (JSONException e) { throw new IOException("AnimeFlixer sent an unexpected answer"); }
    }

    @Override public List<Ep> episodes(Entry s) throws IOException {
        JSONObject data = series(s.url);
        long id = data.optLong("id");
        JSONArray eps = data.optJSONArray("episodes");
        if (id <= 0 || eps == null) throw new IOException("AnimeFlixer did not return episodes");
        List<Ep> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < eps.length(); i++) {
            JSONObject o = eps.optJSONObject(i);
            if (o == null || !o.optBoolean("is_released", true)) continue;
            long eid = o.optLong("id");
            if (eid <= 0) continue;
            String url = BASE + "/watch/" + eid + "?series=" + id;
            if (!seen.add(url)) continue;
            int season = o.optInt("season_number", 0);
            String num = o.optString("episode_number").trim(), title = o.optString("title").trim();
            StringBuilder name = new StringBuilder();
            if (season > 0) name.append("Season ").append(season);
            if (!num.isEmpty()) name.append(name.length() > 0 ? " - " : "").append("Episode ").append(num);
            if (!title.isEmpty()) name.append(name.length() > 0 ? " - " : "").append(title);
            float n;
            try { n = Float.parseFloat(num); } catch (Exception e) { n = -1; }
            out.add(new Ep(name.length() > 0 ? name.toString() : "Episode", url, n));
        }
        if (out.isEmpty()) throw new IOException("AnimeFlixer returned no episodes");
        return out;
    }

    @Override public List<Option> options(Ep ep) throws IOException {
        Matcher w = Pattern.compile("/watch/(\\d+)\\?series=(\\d+)").matcher(ep.url);
        if (!w.find()) throw new IOException("Bad AnimeFlixer episode URL");
        long episodeId = Long.parseLong(w.group(1));
        JSONArray eps = series(BASE + "/series/" + w.group(2)).optJSONArray("episodes");
        if (eps == null) throw new IOException("AnimeFlixer did not return episodes");
        JSONObject episode = null;
        for (int i = 0; i < eps.length(); i++) {
            JSONObject o = eps.optJSONObject(i);
            if (o != null && o.optLong("id") == episodeId) { episode = o; break; }
        }
        if (episode == null) throw new IOException("This episode is no longer available");
        List<Option> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String def = episode.optString("video_url");
        JSONArray versions = episode.optJSONArray("video_versions");
        if (def.startsWith("https://") && seen.add(def)) out.add(new Option("Site player", false, def, BASE + "/"));
        if (versions != null) for (int i = 0; i < versions.length(); i++) {
            JSONObject v = versions.optJSONObject(i);
            if (v == null || !v.optBoolean("is_working", true)) continue;
            String url = v.optString("video_url");
            if (!url.startsWith("https://") || !seen.add(url)) continue;
            String label = v.optString("label").trim();
            if (label.isEmpty()) label = "Site player";
            out.add(new Option(label, label.toLowerCase().contains("dub"), url, BASE + "/"));
        }
        if (out.isEmpty()) throw new IOException("AnimeFlixer did not provide a player");
        return out;
    }
}
