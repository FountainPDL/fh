package com.fountainhome.streaming.download;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.fountainhome.streaming.player.OfflinePlayerActivity;
import com.fountainhome.streaming.service.AppPreferences;

import java.io.File;

/** Opens downloaded videos in the built-in player, Fountain Play, or any other installed video player. */
public final class ExternalPlayers {
    private ExternalPlayers() {}

    public static final String FOUNTAIN_PLAY = "com.fountainpdl.fountainplay";
    public static final String BUILTIN = "builtin", FOUNTAIN = "fountainplay", CHOOSER = "chooser";

    public static boolean fountainPlayInstalled(Context c) {
        try { c.getPackageManager().getPackageInfo(FOUNTAIN_PLAY, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    /** Entry point for every "play this download" tap: honours the player chosen in Settings. */
    public static void play(Context ctx, DownloadItem d) {
        // Fountain Play is on the phone but the user was never asked: recommend it once.
        if (ctx instanceof Activity && fountainPlayInstalled(ctx) && !AppPreferences.getOfflinePlayerAsked(ctx)
            && BUILTIN.equals(AppPreferences.getOfflinePlayer(ctx))) {
            AppPreferences.setOfflinePlayerAsked(ctx);
            new AlertDialog.Builder(ctx)
                .setTitle("Fountain Play found")
                .setMessage("Fountain Play is installed on this phone. Want to play your downloads in it? You can change this any time in Settings.")
                .setPositiveButton("Use Fountain Play", (x, w) -> { AppPreferences.setOfflinePlayer(ctx, FOUNTAIN); play(ctx, d); })
                .setNegativeButton("Keep built-in player", (x, w) -> play(ctx, d))
                .setOnCancelListener(x -> play(ctx, d))
                .show();
            return;
        }
        String mode = AppPreferences.getOfflinePlayer(ctx);
        if (FOUNTAIN.equals(mode)) {
            if (openExternal(ctx, d, FOUNTAIN_PLAY)) return;
            Toast.makeText(ctx, "Fountain Play isn't installed. Using the built-in player.", Toast.LENGTH_SHORT).show();
        } else if (CHOOSER.equals(mode)) {
            if (openExternal(ctx, d, null)) return;
        }
        openBuiltIn(ctx, d);
    }

    public static void openBuiltIn(Context ctx, DownloadItem d) {
        Intent i = new Intent(ctx, OfflinePlayerActivity.class);
        i.putExtra(OfflinePlayerActivity.EXTRA_DOWNLOAD_ID, d.id);
        if (!(ctx instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    /** pkg == null shows the system "open with" list. Returns false if nothing could open it. */
    public static boolean openExternal(Context ctx, DownloadItem d, String pkg) {
        if (d == null || d.localPath == null || !new File(d.localPath).isFile()) {
            Toast.makeText(ctx, "This file is missing.", Toast.LENGTH_SHORT).show();
            return true;   // handled: nothing else to try
        }
        try {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", new File(d.localPath));
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, d.localPath.toLowerCase().endsWith(".mp4") ? "video/mp4" : "video/*");
            view.putExtra(Intent.EXTRA_TITLE, d.title);
            view.putExtra("media_title", d.title);
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!(ctx instanceof Activity)) view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (pkg != null) {
                view.setPackage(pkg);
                ctx.startActivity(view);
            } else {
                Intent chooser = Intent.createChooser(view, "Play with");
                if (!(ctx instanceof Activity)) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(chooser);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
