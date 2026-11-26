package com.fountainhome.streaming.download;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One video download (movie, or a single TV/anime episode). */
@Entity(tableName = "downloads")
public class DownloadItem {
    public static final String QUEUED = "queued", RESOLVING = "resolving", DOWNLOADING = "downloading",
        COMPLETED = "completed", FAILED = "failed", PAUSED = "paused";

    @PrimaryKey(autoGenerate = true) public int id;
    public int tmdbId, season, episode;
    /** "movie", "tv" or "anime" */
    public String type, title, posterPath, backdropPath, imdbId, overview;
    /** Absolute path of the finished video file inside the app's Offline folder. */
    public String localPath;
    public String status;
    /** Human readable reason when status == FAILED. */
    public String error;
    public int progress;          // 0..100
    public long totalBytes;       // size on disk once finished
    public long addedAt;
    public double rating;

    public boolean isActive() {
        return QUEUED.equals(status) || RESOLVING.equals(status) || DOWNLOADING.equals(status) || PAUSED.equals(status);
    }
}
