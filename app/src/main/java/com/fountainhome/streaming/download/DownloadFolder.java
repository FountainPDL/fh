package com.fountainhome.streaming.download;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.fountainhome.streaming.service.AppPreferences;

import java.io.File;

/** Where downloads are stored: the app's own folder (default), an SD card, or any folder the user picks. */
public final class DownloadFolder {
    private DownloadFolder() {}

    /** The folder downloads go into right now. Falls back to the app folder if the chosen one is unusable. */
    public static File resolve(Context c) {
        String custom = AppPreferences.getDownloadDir(c);
        if (custom != null && !custom.isEmpty()) {
            File d = new File(custom);
            if (usable(c, d)) return d;
        }
        return appFolder(c);
    }

    public static File appFolder(Context c) {
        File base = c.getExternalFilesDir(null);
        if (base == null) base = c.getFilesDir();
        File d = new File(base, "Offline");
        if (!d.exists()) //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
        return d;
    }

    /** True when the user chose a folder but it can't be written to right now (so the app folder is used). */
    public static boolean customUnavailable(Context c) {
        String custom = AppPreferences.getDownloadDir(c);
        return custom != null && !custom.isEmpty() && !usable(c, new File(custom));
    }

    private static boolean usable(Context c, File d) {
        if (isInsideAppStorage(c, d)) { d.mkdirs(); return d.isDirectory() && d.canWrite(); }
        if (!hasAllFilesAccess(c)) return false;
        if (!d.exists() && !d.mkdirs()) return false;
        if (!d.isDirectory() || !d.canWrite()) return false;
        try {
            File probe = new File(d, ".fh_probe");
            boolean ok = probe.createNewFile() || probe.exists();
            //noinspection ResultOfMethodCallIgnored
            probe.delete();
            return ok;
        } catch (Exception e) { return false; }
    }

    private static boolean isInsideAppStorage(Context c, File d) {
        String p = d.getAbsolutePath();
        File[] mine = c.getExternalFilesDirs(null);
        if (mine != null) for (File m : mine) if (m != null && p.startsWith(m.getAbsolutePath())) return true;
        return p.startsWith(c.getFilesDir().getAbsolutePath());
    }

    public static boolean hasAllFilesAccess(Context c) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        if (Build.VERSION.SDK_INT >= 23)
            return c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        return true;
    }

    /** Intent that opens the system screen where the user grants file access (Android 11+), or null if a normal permission prompt is enough. */
    @Nullable public static Intent allFilesAccessIntent(Context c) {
        if (Build.VERSION.SDK_INT < 30) return null;
        Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + c.getPackageName()));
        if (i.resolveActivity(c.getPackageManager()) == null) i = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        return i;
    }

    /** Extra storage volumes (SD card) the app can use without any permission. */
    public static File[] appVolumes(Context c) {
        File[] all = c.getExternalFilesDirs(null);
        java.util.List<File> out = new java.util.ArrayList<>();
        if (all != null) for (File f : all) if (f != null && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(f))) out.add(new File(f, "Offline"));
        return out.toArray(new File[0]);
    }

    /** Converts a folder picked in the system picker into a real path, or null if it can't be (cloud folders etc.). */
    @Nullable public static String pathFromTree(Uri tree) {
        try {
            String id = DocumentsContract.getTreeDocumentId(tree);   // "primary:Movies/FH" or "1A2B-3C4D:Videos"
            int colon = id.indexOf(':');
            String vol = colon >= 0 ? id.substring(0, colon) : id;
            String rel = colon >= 0 ? id.substring(colon + 1) : "";
            File root = "primary".equalsIgnoreCase(vol) ? Environment.getExternalStorageDirectory() : new File("/storage/" + vol);
            return rel.isEmpty() ? root.getAbsolutePath() : new File(root, rel).getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    /** A short label for the settings screen. */
    public static String describe(Context c) {
        String custom = AppPreferences.getDownloadDir(c);
        if (custom == null || custom.isEmpty()) return "App storage";
        return custom.replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "Internal storage");
    }
}
