package com.fountainhome.streaming.api;

import com.fountainhome.streaming.api.Models.MovieDetail;
import com.fountainhome.streaming.api.Models.MovieResult;
import com.fountainhome.streaming.api.Models.PagedResponse;
import com.fountainhome.streaming.api.Models.SeasonDetail;
import com.fountainhome.streaming.api.Models.SearchResult;
import com.fountainhome.streaming.api.Models.TVDetail;
import com.fountainhome.streaming.api.Models.TVResult;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TMDBService {

    @GET("movie/popular")
    Call<PagedResponse<MovieResult>> getPopularMovies(@Query("page") int page);

    @GET("tv/popular")
    Call<PagedResponse<TVResult>> getPopularTV(@Query("page") int page);

    @GET("movie/now_playing")
    Call<PagedResponse<MovieResult>> getNowPlaying(@Query("page") int page);

    @GET("movie/top_rated")
    Call<PagedResponse<MovieResult>> getTopRatedMovies(@Query("page") int page);

    @GET("tv/top_rated")
    Call<PagedResponse<TVResult>> getTopRatedTV(@Query("page") int page);

    @GET("tv/on_the_air")
    Call<PagedResponse<TVResult>> getOnAirTV(@Query("page") int page);

    @GET("search/multi")
    Call<PagedResponse<SearchResult>> search(@Query("query") String query, @Query("page") int page);

    @GET("movie/{id}")
    Call<MovieDetail> getMovieDetail(
        @Path("id") int id,
        @Query("append_to_response") String append
    );

    @GET("tv/{id}")
    Call<TVDetail> getTVDetail(
        @Path("id") int id,
        @Query("append_to_response") String append
    );

    @GET("movie/{id}/similar")
    Call<PagedResponse<MovieResult>> getSimilarMovies(@Path("id") int id);

    @GET("tv/{id}/similar")
    Call<PagedResponse<TVResult>> getSimilarTV(@Path("id") int id);

    @GET("tv/{id}/season/{season}")
    Call<SeasonDetail> getSeason(@Path("id") int id, @Path("season") int season);
}
