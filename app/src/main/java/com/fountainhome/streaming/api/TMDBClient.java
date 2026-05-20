package com.fountainhome.streaming.api;

import com.fountainhome.streaming.BuildConfig;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TMDBClient {
    private static final String BASE = "https://api.themoviedb.org/3/";
    private static TMDBService instance;

    public static synchronized TMDBService get() {
        if (instance == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                // Disable auto gzip — fixes "gzip finished without exhausting source"
                .addNetworkInterceptor(chain -> {
                    Request req = chain.request().newBuilder()
                        .header("Accept-Encoding", "identity")
                        .header("Accept", "application/json")
                        .build();
                    return chain.proceed(req);
                })
                .addInterceptor(chain -> {
                    HttpUrl url = chain.request().url().newBuilder()
                        .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                        .build();
                    return chain.proceed(chain.request().newBuilder().url(url).build());
                })
                .build();

            instance = new Retrofit.Builder()
                .baseUrl(BASE).client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(TMDBService.class);
        }
        return instance;
    }
}
