package com.fountainhome.streaming.ui.viewmodel;

import com.fountainhome.streaming.api.Models;
import java.util.List;

public class ContentItem {
    public int id;
    public String title;       // movies
    public String name;        // tv shows
    public String posterPath;
    public String backdropPath;
    public String mediaType;   // "movie" or "tv"
    public double rating;
    public String imdbId;
    public int numberOfSeasons;
    public List<Models.Season> seasons;

    // convenience: display title regardless of type
    public String displayTitle() {
        return title != null && !title.isEmpty() ? title : (name != null ? name : "");
    }
}
