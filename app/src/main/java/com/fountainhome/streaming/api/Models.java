package com.fountainhome.streaming.api;
import java.util.List;
public class Models {
    public static class PagedResponse<T>{public int page;public List<T>results;}
    public static class MovieResult{public int id;public String title,overview,poster_path,backdrop_path,release_date;public double vote_average;}
    public static class TVResult{public int id;public String name,overview,poster_path,backdrop_path,first_air_date;public double vote_average;}
    public static class SearchResult{public String media_type;public int id;public String title,name,poster_path;public double vote_average;}
    public static class MovieDetail{public int id,runtime;public String imdb_id,title,overview,poster_path,backdrop_path,release_date;public double vote_average;}
    public static class TVDetail{public int id,number_of_seasons;public String name,overview,poster_path,backdrop_path,first_air_date;public double vote_average;public List<Season>seasons;}
    public static class SeasonDetail{public int season_number;public String name;public List<Episode>episodes;}
    public static class Episode{public int episode_number,runtime;public String name,overview,still_path,air_date;public double vote_average;}
    public static class Season{public int season_number,episode_count;public String name,poster_path;}
}
