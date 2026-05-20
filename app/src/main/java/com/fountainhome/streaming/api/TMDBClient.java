package com.fountainhome.streaming.api;

import com.fountainhome.streaming.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TMDBClient {

    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private static TMDBService instance;

    public static synchronized TMDBService get() {
        if (instance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request req = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + BuildConfig.TMDB_API_KEY)
                        .addHeader("accept", "application/json")
                        .build();
                    return chain.proceed(req);
                })
                .addInterceptor(logging)
                .build();

            instance = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TMDBService.class);
        }
        return instance;
    }
}
