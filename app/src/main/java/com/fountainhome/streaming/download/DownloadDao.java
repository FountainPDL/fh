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
    @Insert long insert(DownloadItem i);
    @Update void update(DownloadItem i);
    @Delete void delete(DownloadItem i);

    @Query("SELECT * FROM downloads ORDER BY addedAt DESC")
    LiveData<List<DownloadItem>> getAll();

    @Query("SELECT * FROM downloads WHERE status='completed' ORDER BY addedAt DESC")
    LiveData<List<DownloadItem>> getCompleted();

    @Query("SELECT * FROM downloads WHERE status!='completed' ORDER BY addedAt ASC")
    LiveData<List<DownloadItem>> getUnfinished();

    @Query("SELECT * FROM downloads WHERE status='completed' ORDER BY addedAt DESC")
    List<DownloadItem> getCompletedNow();

    @Query("SELECT * FROM downloads WHERE id=:id LIMIT 1")
    DownloadItem byId(int id);

    @Query("SELECT * FROM downloads WHERE tmdbId=:t AND type=:tp AND season=:s AND episode=:e LIMIT 1")
    DownloadItem find(int t, String tp, int s, int e);

    /** Anything left "in progress" by a killed process is not really running any more. */
    @Query("UPDATE downloads SET status='paused', error='Interrupted - tap resume to continue' WHERE status IN ('queued','resolving','downloading')")
    void failInterrupted();

    @Query("UPDATE downloads SET status=:status, progress=:progress, error=:error WHERE id=:id")
    void setState(int id, String status, int progress, String error);

    @Query("UPDATE downloads SET status=:status, error=:error WHERE id=:id")
    void setStatus(int id, String status, String error);

    @Query("UPDATE downloads SET progress=:progress WHERE id=:id")
    void setProgress(int id, int progress);
}
