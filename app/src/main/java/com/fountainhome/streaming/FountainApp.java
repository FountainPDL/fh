package com.fountainhome.streaming;

import android.app.Application;
import android.util.Log;

public class FountainApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FountainApp", "App started");
    }
}
