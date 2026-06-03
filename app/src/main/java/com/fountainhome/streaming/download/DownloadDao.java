package com.fountainhome.streaming.download;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
@Dao public interface DownloadDao {
    @Insert void insert(DownloadItem i);
    @Update void update(DownloadItem i);
    @Delete void delete(DownloadItem i);
    @Query("SELECT * FROM downloads ORDER BY addedAt DESC") LiveData<List<DownloadItem>> getAll();
    @Query("SELECT * FROM downloads WHERE status='completed'") LiveData<List<DownloadItem>> getCompleted();
    @Query("SELECT * FROM downloads WHERE tmdbId=:t AND type=:tp AND season=:s AND episode=:e LIMIT 1") DownloadItem find(int t,String tp,int s,int e);
}
