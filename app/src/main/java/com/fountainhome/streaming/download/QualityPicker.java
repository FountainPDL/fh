package com.fountainhome.streaming.download;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.fountainhome.streaming.service.AppPreferences;

/** Asks which quality to download in (unless the setting already says). */
public final class QualityPicker {
    private QualityPicker() {}

    public static final String[] LABELS = {"Best available", "1080p", "720p", "480p", "360p"};
    public static final String[] VALUES = {"best", "1080p", "720p", "480p", "360p"};

    public interface Done { void run(String quality); }

    public static void choose(Context ctx, final Done done) {
        String pref = AppPreferences.getDlQuality(ctx);
        if (!"ask".equals(pref)) { done.run(pref); return; }
        new AlertDialog.Builder(ctx).setTitle("Download quality")
            .setItems(LABELS, (d, i) -> done.run(VALUES[i]))
            .setNegativeButton("Cancel", null)
            .show();
    }
}
