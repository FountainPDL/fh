package com.fountainhome.streaming.ui.viewmodel;
public class ContentItem {
    public int id,numberOfSeasons,runtime,lastSeason=1,lastEpisode=1;
    public long lastPositionMs=0;
    public String title,name,posterPath,backdropPath,mediaType,imdbId,overview,releaseDate;
    public double rating; public boolean isAnime=false;
    public String displayTitle(){return title!=null&&!title.isEmpty()?title:(name!=null?name:"");}
    public String year(){String d=releaseDate!=null?releaseDate:"";return d.length()>=4?d.substring(0,4):"";}
}
