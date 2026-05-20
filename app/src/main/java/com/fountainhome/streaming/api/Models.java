package com.fountainhome.streaming.api;

import java.util.List;

public class Models {

    public static class PagedResponse<T> {
        public int page;
        public List<T> results;
        public int total_pages;
    }

    public static class MovieResult {
        public int id;
        public String title;
        public String overview;
        public String poster_path;
        public String backdrop_path;
        public double vote_average;
        public String release_date;
    }

    public static class TVResult {
        public int id;
        public String name;
        public String overview;
        public String poster_path;
        public String backdrop_path;
        public double vote_average;
        public String first_air_date;
    }

    public static class SearchResult {
        public String media_type;
        public int id;
        public String title;
        public String name;
        public String overview;
        public String poster_path;
        public double vote_average;
    }

    public static class MovieDetail {
        public int id;
        public String imdb_id;
        public String title;
        public String overview;
        public String poster_path;
        public String backdrop_path;
        public double vote_average;
        public int runtime;
        public String release_date;
        public List<Genre> genres;
        public Credits credits;
    }

    public static class TVDetail {
        public int id;
        public String name;
        public String overview;
        public String poster_path;
        public String backdrop_path;
        public double vote_average;
        public String first_air_date;
        public int number_of_seasons;
        public List<Genre> genres;
        public Credits credits;
        public List<Season> seasons;
    }

    public static class SeasonDetail {
        public int season_number;
        public String name;
        public List<Episode> episodes;
    }

    public static class Episode {
        public int episode_number;
        public String name;
        public String overview;
        public String still_path;
        public int runtime;
        public double vote_average;
    }

    public static class Season {
        public int season_number;
        public String name;
        public int episode_count;
        public String poster_path;
    }

    public static class Genre {
        public int id;
        public String name;
    }

    public static class Credits {
        public List<CastMember> cast;
    }

    public static class CastMember {
        public int id;
        public String name;
        public String character;
        public String profile_path;
    }
}
