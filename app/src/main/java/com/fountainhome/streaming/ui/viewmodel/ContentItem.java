package com.fountainhome.streaming.ui.viewmodel;

import com.fountainhome.streaming.api.Models;
import java.util.List;

public class ContentItem {
    public int    id;
    public String title;
    public String name;
    public String posterPath;
    public String backdropPath;
    public String mediaType;
    public double rating;
    public String imdbId;
    public int    numberOfSeasons;
    public String overview;
    public String releaseDate;
    public int    runtime;
    public int    lastSeason  = 1;
    public int    lastEpisode = 1;
    public List<Models.Season> seasons;

    public String displayTitle() {
        return title != null && !title.isEmpty() ? title : (name != null ? name : "");
    }
    public String year() {
        String d = releaseDate != null ? releaseDate : "";
        return d.length() >= 4 ? d.substring(0, 4) : "";
    }
}
