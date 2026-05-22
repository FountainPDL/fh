package com.fountainhome.streaming.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class DownloadService extends Service {

    private static final String CHANNEL_ID = "fh_downloads";
    private static final int NOTIF_ID = 100;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fountain Home")
            .setContentText("Downloading...")
            .setSmallIcon(com.fountainhome.streaming.R.drawable.ic_nav_more)
            .build();
        startForeground(NOTIF_ID, notif);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Download logic handled per-request
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
