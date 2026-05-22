package com.fountainhome.streaming.anime;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AniListClient {

    private static final String URL = "https://graphql.anilist.co";
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public static class AnimeItem {
        public int id;
        public String titleEnglish;
        public String titleRomaji;
        public String coverImage;
        public String bannerImage;
        public String description;
        public int episodes;
        public double averageScore;
        public String status;
        public String season;
        public int seasonYear;
        public List<String> genres = new ArrayList<>();
        public String format; // TV, MOVIE, OVA, ONA
        public boolean isAdult;

        public String displayTitle() {
            return titleEnglish != null && !titleEnglish.isEmpty() ? titleEnglish : titleRomaji;
        }
    }

    private static String query(String gql, String variables) throws IOException {
        String body = "{\"query\":\"" + gql.replace("\"","\\\"").replace("\n"," ")
            + "\",\"variables\":" + variables + "}";
        Request req = new Request.Builder()
            .url(URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(RequestBody.create(body, MediaType.parse("application/json")))
            .build();
        try (Response res = client.newCall(req).execute()) {
            if (res.body() != null) return res.body().string();
        }
        return "{}";
    }

    private static List<AnimeItem> parseMediaList(JsonArray arr) {
        List<AnimeItem> items = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            items.add(parseMedia(obj));
        }
        return items;
    }

    private static AnimeItem parseMedia(JsonObject obj) {
        AnimeItem a = new AnimeItem();
        a.id = obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsInt() : 0;

        if (obj.has("title") && !obj.get("title").isJsonNull()) {
            JsonObject t = obj.getAsJsonObject("title");
            if (t.has("english") && !t.get("english").isJsonNull())
                a.titleEnglish = t.get("english").getAsString();
            if (t.has("romaji") && !t.get("romaji").isJsonNull())
                a.titleRomaji = t.get("romaji").getAsString();
        }

        if (obj.has("coverImage") && !obj.get("coverImage").isJsonNull()) {
            JsonObject ci = obj.getAsJsonObject("coverImage");
            if (ci.has("large") && !ci.get("large").isJsonNull())
                a.coverImage = ci.get("large").getAsString();
        }

        if (obj.has("bannerImage") && !obj.get("bannerImage").isJsonNull())
            a.bannerImage = obj.get("bannerImage").getAsString();
        if (obj.has("description") && !obj.get("description").isJsonNull())
            a.description = obj.get("description").getAsString();
        if (obj.has("episodes") && !obj.get("episodes").isJsonNull())
            a.episodes = obj.get("episodes").getAsInt();
        if (obj.has("averageScore") && !obj.get("averageScore").isJsonNull())
            a.averageScore = obj.get("averageScore").getAsDouble() / 10.0;
        if (obj.has("status") && !obj.get("status").isJsonNull())
            a.status = obj.get("status").getAsString();
        if (obj.has("season") && !obj.get("season").isJsonNull())
            a.season = obj.get("season").getAsString();
        if (obj.has("seasonYear") && !obj.get("seasonYear").isJsonNull())
            a.seasonYear = obj.get("seasonYear").getAsInt();
        if (obj.has("format") && !obj.get("format").isJsonNull())
            a.format = obj.get("format").getAsString();
        if (obj.has("isAdult") && !obj.get("isAdult").isJsonNull())
            a.isAdult = obj.get("isAdult").getAsBoolean();

        if (obj.has("genres") && obj.get("genres").isJsonArray()) {
            for (JsonElement g : obj.getAsJsonArray("genres"))
                a.genres.add(g.getAsString());
        }

        return a;
    }

    public static void getTrending(Callback<List<AnimeItem>> cb) {
        exec.execute(() -> {
            try {
                String gql = "query { Page(page:1,perPage:20) { media(sort:TRENDING_DESC,type:ANIME,isAdult:false) { id title { english romaji } coverImage { large } bannerImage description episodes averageScore status season seasonYear format genres } } }";
                String res = query(gql, "{}");
                JsonObject root = JsonParser.parseString(res).getAsJsonObject();
                JsonArray arr = root.getAsJsonObject("data")
                    .getAsJsonObject("Page").getAsJsonArray("media");
                List<AnimeItem> items = parseMediaList(arr);
                mainHandler.post(() -> cb.onSuccess(items));
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public static void getPopular(Callback<List<AnimeItem>> cb) {
        exec.execute(() -> {
            try {
                String gql = "query { Page(page:1,perPage:20) { media(sort:POPULARITY_DESC,type:ANIME,isAdult:false) { id title { english romaji } coverImage { large } bannerImage episodes averageScore status format genres } } }";
                String res = query(gql, "{}");
                JsonObject root = JsonParser.parseString(res).getAsJsonObject();
                JsonArray arr = root.getAsJsonObject("data")
                    .getAsJsonObject("Page").getAsJsonArray("media");
                mainHandler.post(() -> cb.onSuccess(parseMediaList(arr)));
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public static void search(String keyword, Callback<List<AnimeItem>> cb) {
        exec.execute(() -> {
            try {
                String gql = "query ($search:String) { Page(page:1,perPage:20) { media(search:$search,type:ANIME,isAdult:false) { id title { english romaji } coverImage { large } episodes averageScore status format genres } } }";
                String vars = "{\"search\":\"" + keyword.replace("\"","") + "\"}";
                String res = query(gql, vars);
                JsonObject root = JsonParser.parseString(res).getAsJsonObject();
                JsonArray arr = root.getAsJsonObject("data")
                    .getAsJsonObject("Page").getAsJsonArray("media");
                mainHandler.post(() -> cb.onSuccess(parseMediaList(arr)));
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public static void getDetails(int anilistId, Callback<AnimeItem> cb) {
        exec.execute(() -> {
            try {
                String gql = "query ($id:Int) { Media(id:$id,type:ANIME) { id title { english romaji } coverImage { large } bannerImage description episodes averageScore status season seasonYear format genres streamingEpisodes { title thumbnail url site } } }";
                String vars = "{\"id\":" + anilistId + "}";
                String res = query(gql, vars);
                JsonObject root = JsonParser.parseString(res).getAsJsonObject();
                JsonObject media = root.getAsJsonObject("data").getAsJsonObject("Media");
                mainHandler.post(() -> cb.onSuccess(parseMedia(media)));
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public static void getThisSeason(Callback<List<AnimeItem>> cb) {
        exec.execute(() -> {
            try {
                String gql = "query { Page(page:1,perPage:20) { media(season:SPRING,seasonYear:2025,type:ANIME,sort:POPULARITY_DESC,isAdult:false) { id title { english romaji } coverImage { large } episodes averageScore status format } } }";
                String res = query(gql, "{}");
                JsonObject root = JsonParser.parseString(res).getAsJsonObject();
                JsonArray arr = root.getAsJsonObject("data")
                    .getAsJsonObject("Page").getAsJsonArray("media");
                mainHandler.post(() -> cb.onSuccess(parseMediaList(arr)));
            } catch (Exception e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }
}
