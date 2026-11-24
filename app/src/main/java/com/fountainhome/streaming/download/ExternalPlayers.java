package com.fountainhome.streaming.download;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.fountainhome.streaming.player.OfflinePlayerActivity;
import com.fountainhome.streaming.service.AppPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Opens downloaded videos the way the user chose in Settings:
 *  builtin  - the app's own player
 *  external - always straight into the one external app they picked
 *  ask      - the system "open with" list every time
 */
public final class ExternalPlayers {
    private ExternalPlayers() {}

    public static final String BUILTIN = "builtin", EXTERNAL = "external", ASK = "ask";

    /** Entry point for every "play this download" tap. */
    public static void play(Context ctx, DownloadItem d) {
        String mode = AppPreferences.getOfflinePlayer(ctx);
        if (EXTERNAL.equals(mode)) {
            if (openPreferred(ctx, d)) return;
        } else if (ASK.equals(mode)) {
            if (openExternal(ctx, d, null)) return;
            Toast.makeText(ctx, "No other video player found. Using the built-in player.", Toast.LENGTH_SHORT).show();
        }
        openBuiltIn(ctx, d);
    }

    public static void openBuiltIn(Context ctx, DownloadItem d) {
        Intent i = new Intent(ctx, OfflinePlayerActivity.class);
        i.putExtra(OfflinePlayerActivity.EXTRA_DOWNLOAD_ID, d.id);
        if (!(ctx instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    /** The app chosen in Settings; if none is chosen (or it was uninstalled) the system list is shown instead. */
    public static boolean openPreferred(Context ctx, DownloadItem d) {
        String pkg = AppPreferences.getExternalPlayerPkg(ctx);
        if (!pkg.isEmpty() && isInstalled(ctx, pkg)) return openExternal(ctx, d, pkg);
        Toast.makeText(ctx, "Pick your external player in Settings > Offline Player", Toast.LENGTH_SHORT).show();
        return openExternal(ctx, d, null);
    }

    public static boolean isInstalled(Context c, String pkg) {
        try { c.getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    /** Video apps installed on the phone (this app excluded), one entry per app. */
    public static List<ResolveInfo> installedPlayers(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_VIEW).setType("video/*");
        List<ResolveInfo> all = pm.queryIntentActivities(probe, 0);
        List<ResolveInfo> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo r : all) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
            out.add(r);
        }
        java.util.Collections.sort(out, (a, b) ->
            String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
        return out;
    }

    public static String labelOf(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (Exception e) { return pkg; }
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
