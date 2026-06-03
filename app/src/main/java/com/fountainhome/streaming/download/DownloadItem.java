package com.fountainhome.streaming.download;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName="downloads")
public class DownloadItem {
    @PrimaryKey(autoGenerate=true) public int id;
    public int tmdbId,season,episode;
    public String type,title,posterPath,localPath,status,imdbId,overview;
    public long addedAt; public double rating; public boolean isOfflineReady;
}
