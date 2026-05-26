package com.fountainhome.streaming.service;
import android.app.*;
import android.content.Intent;
import android.os.*;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
public class DownloadService extends Service {
    private static final String CH="fh_dl";
    @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CH,"Downloads",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);}Notification n=new NotificationCompat.Builder(this,CH).setContentTitle("Fountain Home").setContentText("Downloading...").setSmallIcon(android.R.drawable.stat_sys_download_done).build();startForeground(1,n);}
    @Override public int onStartCommand(Intent i,int f,int s){return START_NOT_STICKY;}
    @Nullable @Override public IBinder onBind(Intent i){return null;}
}
