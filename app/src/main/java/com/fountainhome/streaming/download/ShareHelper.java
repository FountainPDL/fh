package com.fountainhome.streaming.download;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

/** Shares a downloaded video file with other apps (WhatsApp, Telegram, Bluetooth, ...). */
public final class ShareHelper {
    private ShareHelper() {}

    public static void shareVideo(Context ctx, DownloadItem d) {
        if (d == null || d.localPath == null || !new File(d.localPath).isFile()) {
            Toast.makeText(ctx, "This file is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", new File(d.localPath));
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(d.localPath.endsWith(".mp4") ? "video/mp4" : "video/*");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, d.title);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(send, "Share video");
            if (!(ctx instanceof android.app.Activity)) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(ctx, "Could not share this file.", Toast.LENGTH_SHORT).show();
        }
    }
}
