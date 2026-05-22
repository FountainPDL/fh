package com.fountainhome.streaming.download;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloads")
public class DownloadItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int tmdbId;
    public String type;         // "movie" or "tv"
    public String title;
    public String posterPath;
    public int season;
    public int episode;
    public String episodeTitle;
    public String localPath;    // absolute path on device
    public long fileSizeBytes;
    public int downloadId;      // DownloadManager ID
    public String status;       // "downloading","completed","failed","paused"
    public long addedAt;
    public String imdbId;
    public String overview;
    public double rating;
    public String releaseDate;
    public boolean isOfflineReady; // can be played offline
}
