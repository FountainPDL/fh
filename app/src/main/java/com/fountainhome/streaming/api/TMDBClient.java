package com.fountainhome.streaming.api;

import android.content.Context;

import com.fountainhome.streaming.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * TMDB client with a real disk cache:
 *  - details / seasons stay fresh for 6 hours, lists (trending, popular...) for 30 minutes;
 *  - if the network fails, whatever was cached (up to 14 days old) is served instead of an error.
 */
public class TMDBClient {
    private static final String BASE = "https://api.themoviedb.org/3/";
    private static final long CACHE_BYTES = 50L * 1024 * 1024;
    private static final int STALE_DAYS = 14;
    private static volatile TMDBService inst;
    private static volatile Cache cache;
    private static File cacheDir;

    /** Call once from Application.onCreate so the disk cache has somewhere to live. */
    public static synchronized void init(Context ctx) {
        if (cacheDir == null) cacheDir = new File(ctx.getApplicationContext().getCacheDir(), "tmdb_http");
    }

    public static synchronized void clearCache() {
        try { if (cache != null) cache.evictAll(); } catch (IOException ignored) { }
    }

    public static long cacheSize() {
        try { return cache != null ? cache.size() : 0; } catch (IOException e) { return 0; }
    }

    private static int freshSeconds(HttpUrl url) {
        String p = url.encodedPath();
        boolean list = p.contains("/trending/") || p.contains("/popular") || p.contains("/now_playing")
            || p.contains("/top_rated") || p.contains("/discover/") || p.contains("/search/")
            || p.contains("/upcoming") || p.contains("/on_the_air") || p.contains("/airing_today");
        return list ? 30 * 60 : 6 * 60 * 60;
    }

    public static synchronized TMDBService get() {
        if (inst == null) {
            OkHttpClient.Builder b = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    HttpUrl url = chain.request().url().newBuilder()
                        .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY).build();
                    return chain.proceed(chain.request().newBuilder().url(url).build());
                })
                // No connection (or a server error): fall back to the cached copy, however old.
                .addInterceptor(chain -> {
                    Request req = chain.request();
                    try {
                        Response r = chain.proceed(req);
                        if (r.code() < 500) return r;
                        r.close();
                    } catch (IOException e) {
                        // fall through to the cached copy
                    }
                    return chain.proceed(req.newBuilder().cacheControl(new CacheControl.Builder()
                        .onlyIfCached().maxStale(STALE_DAYS, TimeUnit.DAYS).build()).build());
                });
            if (cacheDir != null) {
                cache = new Cache(cacheDir, CACHE_BYTES);
                b.cache(cache);
                b.addNetworkInterceptor(chain -> {
                    Response res = chain.proceed(chain.request());
                    if (!res.isSuccessful()) return res;
                    return res.newBuilder().removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=" + freshSeconds(chain.request().url())).build();
                });
            }
            inst = new Retrofit.Builder().baseUrl(BASE).client(b.build())
                .addConverterFactory(GsonConverterFactory.create()).build().create(TMDBService.class);
        }
        return inst;
    }
}
