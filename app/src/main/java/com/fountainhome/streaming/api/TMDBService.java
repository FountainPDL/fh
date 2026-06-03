package com.fountainhome.streaming.api;
import retrofit2.Call;
import retrofit2.http.*;
public interface TMDBService {
    @GET("movie/popular")     Call<Models.PagedResponse<Models.MovieResult>> getPopularMovies(@Query("page") int p);
    @GET("tv/popular")        Call<Models.PagedResponse<Models.TVResult>>    getPopularTV(@Query("page") int p);
    @GET("movie/now_playing") Call<Models.PagedResponse<Models.MovieResult>> getNowPlaying(@Query("page") int p);
    @GET("movie/top_rated")   Call<Models.PagedResponse<Models.MovieResult>> getTopRatedMovies(@Query("page") int p);
    @GET("tv/top_rated")      Call<Models.PagedResponse<Models.TVResult>>    getTopRatedTV(@Query("page") int p);
    @GET("tv/on_the_air")     Call<Models.PagedResponse<Models.TVResult>>    getOnAirTV(@Query("page") int p);
    @GET("search/multi")      Call<Models.PagedResponse<Models.SearchResult>>search(@Query("query") String q,@Query("page") int p);
    @GET("movie/{id}")        Call<Models.MovieDetail> getMovieDetail(@Path("id") int id,@Query("append_to_response") String a);
    @GET("tv/{id}")           Call<Models.TVDetail>    getTVDetail(@Path("id") int id,@Query("append_to_response") String a);
    @GET("movie/{id}/similar") Call<Models.PagedResponse<Models.MovieResult>> getSimilarMovies(@Path("id") int id);
    @GET("tv/{id}/similar")    Call<Models.PagedResponse<Models.TVResult>>    getSimilarTV(@Path("id") int id);
    @GET("tv/{id}/season/{s}") Call<Models.SeasonDetail> getSeason(@Path("id") int id,@Path("s") int s);
}
