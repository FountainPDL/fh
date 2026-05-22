package com.fountainhome.streaming.download;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DownloadDao {
    @Insert
    void insert(DownloadItem item);

    @Update
    void update(DownloadItem item);

    @Delete
    void delete(DownloadItem item);

    @Query("SELECT * FROM downloads ORDER BY addedAt DESC")
    LiveData<List<DownloadItem>> getAll();

    @Query("SELECT * FROM downloads WHERE status = 'completed'")
    LiveData<List<DownloadItem>> getCompleted();

    @Query("SELECT * FROM downloads WHERE tmdbId = :tmdbId AND type = :type AND season = :season AND episode = :episode LIMIT 1")
    DownloadItem find(int tmdbId, String type, int season, int episode);

    @Query("SELECT * FROM downloads WHERE tmdbId = :tmdbId AND type = 'movie' LIMIT 1")
    DownloadItem findMovie(int tmdbId);

    @Query("SELECT * FROM downloads WHERE downloadId = :dlId LIMIT 1")
    DownloadItem findByDownloadId(int dlId);

    @Query("DELETE FROM downloads WHERE tmdbId = :tmdbId")
    void deleteByTmdb(int tmdbId);

    @Query("SELECT SUM(fileSizeBytes) FROM downloads WHERE status = 'completed'")
    long getTotalSize();
}
