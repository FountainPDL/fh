package com.fountainhome.streaming;

import android.app.Application;
import android.util.Log;
import com.fountainhome.streaming.api.TMDBClient;

public class FountainApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            TMDBClient.init(this);
        } catch (Exception e) {
            Log.e("FountainApp", "Init error: " + e.getMessage());
        }
    }
}
