package com.fountainhome.streaming.download;
import android.content.Context;
import androidx.room.*;
@Database(entities={DownloadItem.class},version=1,exportSchema=false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase inst;
    public abstract DownloadDao downloadDao();
    public static synchronized AppDatabase get(Context c){if(inst==null)inst=Room.databaseBuilder(c.getApplicationContext(),AppDatabase.class,"fh_db").allowMainThreadQueries().fallbackToDestructiveMigration().build();return inst;}
}
