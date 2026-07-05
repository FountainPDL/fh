import os

print("=== v1.27b hotfix: DownloadManager2 anime download call ===")

S = "app/src/main/java/com/fountainhome/streaming"

# ── 1. AniListClient — add a synchronous MAL-id lookup for use from background
#    threads that aren't already UI-callback-based (DownloadManager2 runs its own
#    executor thread, so a blocking call here is safe and simpler than nesting
#    another callback). ──
w = lambda p, t: (os.makedirs(os.path.dirname(p), exist_ok=True), open(p, 'w').write(t))
w(f"{S}/anime/AniListClient.java", r'''package com.fountainhome.streaming.anime;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class AniListClient {
    private static final String URL = "https://graphql.anilist.co";
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final Handler mh = new Handler(Looper.getMainLooper());
    public interface Callback<T> { void onSuccess(T r); void onError(String e); }
    public interface IntCallback { void onResult(int value); }
    public static class AnimeItem {
        public int id, episodes, seasonYear;
        public String titleEnglish, titleRomaji, coverImage, bannerImage, description, status, season, format;
        public double averageScore;
        public List<String> genres = new ArrayList<>();
        public String displayTitle() {
            return titleEnglish != null && !titleEnglish.isEmpty() ? titleEnglish : (titleRomaji != null ? titleRomaji : "");
        }
    }
    private static String query(String gql, String vars) throws IOException {
        String q = gql.replace("\n", " ");
        String body = "{\"query\":\"" + q + "\",\"variables\":" + vars + "}";
        Request req = new Request.Builder().url(URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(RequestBody.create(body, MediaType.parse("application/json"))).build();
        try (Response res = client.newCall(req).execute()) {
            if (res.body() != null) return res.body().string();
        }
        return "{}";
    }
    private static List<AnimeItem> parse(JsonArray arr) {
        List<AnimeItem> items = new ArrayList<>();
        for (JsonElement el : arr) {
            try {
                JsonObject o = el.getAsJsonObject();
                AnimeItem a = new AnimeItem();
                a.id = gi(o, "id", 0);
                JsonObject t = o.has("title") && !o.get("title").isJsonNull() ? o.getAsJsonObject("title") : null;
                if (t != null) { a.titleEnglish = gs(t, "english"); a.titleRomaji = gs(t, "romaji"); }
                JsonObject ci = o.has("coverImage") && !o.get("coverImage").isJsonNull() ? o.getAsJsonObject("coverImage") : null;
                if (ci != null) a.coverImage = gs(ci, "large");
                a.bannerImage = gs(o, "bannerImage");
                a.description = gs(o, "description");
                a.episodes = gi(o, "episodes", 0);
                double sc = gd(o, "averageScore");
                a.averageScore = sc > 0 ? sc / 10.0 : 0;
                a.status = gs(o, "status");
                a.season = gs(o, "season");
                a.seasonYear = gi(o, "seasonYear", 0);
                a.format = gs(o, "format");
                if (o.has("genres") && o.get("genres").isJsonArray())
                    for (JsonElement g : o.getAsJsonArray("genres")) a.genres.add(g.getAsString());
                items.add(a);
            } catch (Exception ignored) {}
        }
        return items;
    }
    private static String gs(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    private static int gi(JsonObject o, String k, int d) { try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : d; } catch (Exception e) { return d; } }
    private static double gd(JsonObject o, String k) { try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0; } catch (Exception e) { return 0; } }
    private static void run(String gql, String vars, Callback<List<AnimeItem>> cb) {
        exec.execute(() -> {
            try {
                String res = query(gql, vars);
                JsonArray arr = JsonParser.parseString(res).getAsJsonObject()
                    .getAsJsonObject("data").getAsJsonObject("Page").getAsJsonArray("media");
                mh.post(() -> cb.onSuccess(parse(arr)));
            } catch (Exception e) { mh.post(() -> cb.onError(e.getMessage())); }
        });
    }
    public static void getTrending(Callback<List<AnimeItem>> cb) {
        run("query{Page(page:1,perPage:20){media(sort:TRENDING_DESC,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}bannerImage description episodes averageScore status season seasonYear format genres}}}", "{}", cb);
    }
    public static void getPopular(Callback<List<AnimeItem>> cb) {
        run("query{Page(page:1,perPage:20){media(sort:POPULARITY_DESC,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format genres}}}", "{}", cb);
    }
    public static void getThisSeason(Callback<List<AnimeItem>> cb) {
        run("query{Page(page:1,perPage:20){media(season:SPRING,seasonYear:2025,type:ANIME,sort:POPULARITY_DESC,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format}}}", "{}", cb);
    }
    public static void search(String kw, Callback<List<AnimeItem>> cb) {
        String safeKw = kw.replace("\"", "").replace("\\", "");
        run("query($s:String){Page(page:1,perPage:20){media(search:$s,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format genres}}}", "{\"s\":\"" + safeKw + "\"}", cb);
    }
    // Async version — hops back to the main thread for the callback (used from UI code).
    public static void getMalId(int anilistId, IntCallback cb) {
        exec.execute(() -> {
            int result = getMalIdSync(anilistId);
            mh.post(() -> cb.onResult(result));
        });
    }
    // Synchronous version — makes a blocking network call, so only call this from a
    // background thread you already own (e.g. inside DownloadManager2's executor).
    // Never call this from the main/UI thread.
    public static int getMalIdSync(int anilistId) {
        try {
            String res = query("query($id:Int){Media(id:$id,type:ANIME){idMal}}", "{\"id\":" + anilistId + "}");
            JsonObject media = JsonParser.parseString(res).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("Media");
            return media.has("idMal") && !media.get("idMal").isJsonNull() ? media.get("idMal").getAsInt() : 0;
        } catch (Exception e) { return 0; }
    }
}
''')
print("[OK] AniListClient.java rewritten with getMalIdSync (and getMalId now reuses it)")

# ── 2. Patch DownloadManager2's call site to pass the resolved MAL id ──
dm_path = "app/src/main/java/com/fountainhome/streaming/download/DownloadManager2.java"
if os.path.exists(dm_path):
    content = open(dm_path, encoding='utf-8').read()
    anchor = "SourceGenerator.getAnimeSources(item.id, season, episode).get(0).url"
    replacement = "SourceGenerator.getAnimeSources(item.id, com.fountainhome.streaming.anime.AniListClient.getMalIdSync(item.id), season, episode).get(0).url"
    if anchor in content:
        content = content.replace(anchor, replacement, 1)
        open(dm_path, 'w', encoding='utf-8').write(content)
        print("[OK] DownloadManager2.java patched to pass a resolved MAL id")
    elif "getMalIdSync(item.id)" in content:
        print("[SKIP] DownloadManager2.java already patched")
    else:
        print("[WARN] Could not find the expected getAnimeSources call in DownloadManager2.java.")
        print("       Replace this line manually:")
        print("         " + anchor)
        print("       with:")
        print("         " + replacement)
else:
    print("[WARN] DownloadManager2.java not found at expected path — skipping")

print()
print("=== Hotfix complete ===")
