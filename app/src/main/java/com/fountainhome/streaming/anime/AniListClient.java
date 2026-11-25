package com.fountainhome.streaming.anime;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.*;
import okhttp3.*;
import java.io.File;
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
        /** From AniList "streamingEpisodes": real per-episode pictures and titles, by episode number. */
        public java.util.Map<Integer, String> episodeThumbs = new java.util.HashMap<>();
        public java.util.Map<Integer, String> episodeTitles = new java.util.HashMap<>();
        public String displayTitle() {
            return titleEnglish != null && !titleEnglish.isEmpty() ? titleEnglish : (titleRomaji != null ? titleRomaji : "");
        }
    }
    // ---- disk cache: fresh for 30 min, served as a fallback for up to 7 days if the network fails
    private static final long FRESH_MS = 30L * 60 * 1000, STALE_MS = 7L * 24 * 60 * 60 * 1000;
    private static File cacheDir;

    public static synchronized void init(android.content.Context ctx) {
        if (cacheDir == null) {
            cacheDir = new File(ctx.getApplicationContext().getCacheDir(), "anilist_http");
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
        }
    }

    public static void clearCache() {
        File[] f = cacheDir == null ? null : cacheDir.listFiles();
        if (f != null) for (File x : f) //noinspection ResultOfMethodCallIgnored
            x.delete();
    }

    private static File cacheFile(String body) {
        if (cacheDir == null) return null;
        return new File(cacheDir, Integer.toHexString(body.hashCode()) + "_" + body.length() + ".json");
    }

    private static String readFile(File f) {
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            return o.toString("UTF-8");
        } catch (Exception e) { return null; }
    }

    private static String query(String gql, String vars) throws IOException {
        String q = gql.replace("\n", " ");
        String body = "{\"query\":\"" + q + "\",\"variables\":" + vars + "}";
        File cf = cacheFile(body);
        long age = cf != null && cf.exists() ? System.currentTimeMillis() - cf.lastModified() : Long.MAX_VALUE;
        if (age < FRESH_MS) { String c = readFile(cf); if (c != null && c.contains("\"data\"")) return c; }
        try {
            Request req = new Request.Builder().url(URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(RequestBody.create(body, MediaType.parse("application/json"))).build();
            try (Response res = client.newCall(req).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    String text = res.body().string();
                    if (cf != null && text.contains("\"data\"")) {
                        try (java.io.FileOutputStream o = new java.io.FileOutputStream(cf)) { o.write(text.getBytes("UTF-8")); }
                    }
                    return text;
                }
            }
        } catch (IOException e) {
            if (age < STALE_MS) { String c = readFile(cf); if (c != null) return c; }
            throw e;
        }
        if (age < STALE_MS) { String c = readFile(cf); if (c != null) return c; }
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
                // Airing shows: only the episodes that are already out.
                if (o.has("nextAiringEpisode") && !o.get("nextAiringEpisode").isJsonNull()) {
                    int next = gi(o.getAsJsonObject("nextAiringEpisode"), "episode", 0);
                    if (next > 0) a.episodes = next - 1;
                }
                double sc = gd(o, "averageScore");
                a.averageScore = sc > 0 ? sc / 10.0 : 0;
                a.status = gs(o, "status");
                a.season = gs(o, "season");
                a.seasonYear = gi(o, "seasonYear", 0);
                a.format = gs(o, "format");
                if (o.has("genres") && o.get("genres").isJsonArray())
                    for (JsonElement g : o.getAsJsonArray("genres")) a.genres.add(g.getAsString());
                if (o.has("streamingEpisodes") && o.get("streamingEpisodes").isJsonArray()) {
                    java.util.regex.Pattern np = java.util.regex.Pattern.compile("(?i)episode\\s+(\\d+)");
                    for (JsonElement sel : o.getAsJsonArray("streamingEpisodes")) {
                        if (!sel.isJsonObject()) continue;
                        JsonObject se = sel.getAsJsonObject();
                        java.util.regex.Matcher nm = np.matcher(gs(se, "title"));
                        if (!nm.find()) continue;
                        int n = Integer.parseInt(nm.group(1));
                        String th = gs(se, "thumbnail");
                        if (!th.isEmpty()) a.episodeThumbs.put(n, th);
                        a.episodeTitles.put(n, gs(se, "title"));
                    }
                }
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
    /** One title by AniList id (used when a page is opened from a list that only knows the cover). */
    public static void getById(int id, Callback<AnimeItem> cb) {
        run("query{Page(page:1,perPage:1){media(id:" + id + ",type:ANIME){id title{english romaji}coverImage{large}bannerImage description episodes nextAiringEpisode{episode} averageScore status season seasonYear format genres streamingEpisodes{title thumbnail}}}}", "{}",
            new Callback<List<AnimeItem>>() {
                @Override public void onSuccess(List<AnimeItem> r) { if (r.isEmpty()) cb.onError("Not found"); else cb.onSuccess(r.get(0)); }
                @Override public void onError(String e) { cb.onError(e); }
            });
    }
    public static void getTrending(Callback<List<AnimeItem>> cb) { getTrending(1, cb); }
    public static void getTrending(int page, Callback<List<AnimeItem>> cb) {
        run("query{Page(page:" + page + ",perPage:20){media(sort:TRENDING_DESC,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}bannerImage description episodes nextAiringEpisode{episode} averageScore status season seasonYear format genres}}}", "{}", cb);
    }
    public static void getPopular(Callback<List<AnimeItem>> cb) { getPopular(1, cb); }
    public static void getPopular(int page, Callback<List<AnimeItem>> cb) {
        run("query{Page(page:" + page + ",perPage:20){media(sort:POPULARITY_DESC,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}bannerImage description episodes nextAiringEpisode{episode} averageScore status season seasonYear format genres}}}", "{}", cb);
    }
    public static void getThisSeason(Callback<List<AnimeItem>> cb) { getThisSeason(1, cb); }
    public static void getThisSeason(int page, Callback<List<AnimeItem>> cb) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int m = cal.get(java.util.Calendar.MONTH);          // 0 = January
        String season = m <= 2 ? "WINTER" : m <= 5 ? "SPRING" : m <= 8 ? "SUMMER" : "FALL";
        int year = cal.get(java.util.Calendar.YEAR);
        run("query{Page(page:" + page + ",perPage:20){media(season:" + season + ",seasonYear:" + year + ",type:ANIME,sort:POPULARITY_DESC,isAdult:false){id title{english romaji}coverImage{large}bannerImage description episodes nextAiringEpisode{episode} averageScore status season seasonYear format genres}}}", "{}", cb);
    }
    public static void search(String kw, Callback<List<AnimeItem>> cb) { search(kw, 1, cb); }
    public static void search(String kw, int page, Callback<List<AnimeItem>> cb) {
        String safeKw = kw.replace("\"", "").replace("\\", "");
        run("query($s:String){Page(page:" + page + ",perPage:20){media(search:$s,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}bannerImage description episodes nextAiringEpisode{episode} averageScore status season seasonYear format genres}}}", "{\"s\":\"" + safeKw + "\"}", cb);
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
