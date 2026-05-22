package com.fountainhome.streaming.download;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DownloadItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract DownloadDao downloadDao();

    public static synchronized AppDatabase get(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "fh_database")
                .allowMainThreadQueries()   // safe for now; moves to async later
                .fallbackToDestructiveMigration()
                .build();
        }
        return instance;
    }
}
