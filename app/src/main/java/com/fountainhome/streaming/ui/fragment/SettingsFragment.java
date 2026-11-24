package com.fountainhome.streaming.ui.fragment;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.FragmentSettingsBinding;
import com.fountainhome.streaming.download.AppDatabase;
import com.fountainhome.streaming.download.DownloadItem;
import com.fountainhome.streaming.download.DownloadFolder;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.OfflineStore;
import com.fountainhome.streaming.service.SourceGenerator;
import com.fountainhome.streaming.ui.theme.ThemeManager;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Settings are built from small rows so every option is one line of code and always themed. */
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;
    private LinearLayout box;
    private TextView downloadsInfo, cacheInfo, folderInfo, extInfo;

    private final ActivityResultLauncher<android.net.Uri> treePicker =
        registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri == null || !isAdded()) return;
            String path = DownloadFolder.pathFromTree(uri);
            if (path == null) { toast("That location can't be used. Pick a folder on this phone or SD card."); return; }
            AppPreferences.setDownloadDir(ctx(), path);
            askFileAccessIfNeeded();
            refreshFolderInfo();
        });
    private final ActivityResultLauncher<String> legacyPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> { if (isAdded()) refreshFolderInfo(); });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentSettingsBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        b.backBtn.setOnClickListener(x -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        box = b.settingsContainer;
        build();
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }

    private Context ctx() { return requireContext(); }

    // ------------------------------------------------------------------ content

    private void build() {
        Context c = ctx();

        section("APPEARANCE");
        choice("Theme", "Light, dark, or match your phone", ThemeManager.MODE_LABELS,
            indexOf(ThemeManager.MODES, AppPreferences.getThemeMode(c)), i -> {
                AppPreferences.setThemeMode(ctx(), ThemeManager.MODES[i]);
                ThemeManager.applyNightMode(ctx());
                requireActivity().recreate();
            });
        swatches();
        choice("Start screen", "What opens when you launch the app", new String[]{"Home", "Movies", "TV Shows", "Anime"},
            AppPreferences.getStartTab(c), i -> AppPreferences.setStartTab(ctx(), i));
        choice("Poster grid columns", "Used in lists and search", new String[]{"2", "3", "4"},
            AppPreferences.getGridColumns(c) - 2, i -> AppPreferences.setGridColumns(ctx(), i + 2));
        toggle("Show ratings on posters", null, AppPreferences.getShowRating(c), v -> AppPreferences.setShowRating(ctx(), v));

        section("HOME");
        toggle("Continue Watching row", "Pick up where you left off", AppPreferences.getShowContinue(c), v -> AppPreferences.setShowContinue(ctx(), v));
        toggle("Top 10 row", "The numbered trending row", AppPreferences.getShowTop10(c), v -> AppPreferences.setShowTop10(ctx(), v));
        toggle("My List row", null, AppPreferences.getShowMyList(c), v -> AppPreferences.setShowMyList(ctx(), v));

        section("STREAMING");
        choice("Default server (movies and TV)", "The server that is tried first", SourceGenerator.SERVER_NAMES,
            indexOf(SourceGenerator.SERVER_NAMES, AppPreferences.getSource(c)), i -> AppPreferences.setSource(ctx(), SourceGenerator.SERVER_NAMES[i]));
        String[] animeServers = {"VidLink", "VIDEASY", "SuperFlix", "AutoEmbed", "2Anime", "Yugen"};
        choice("Default server (anime)", "The server that is tried first for anime", animeServers,
            indexOf(animeServers, AppPreferences.getAnimeSource(c)), i -> AppPreferences.setAnimeSource(ctx(), animeServers[i]));
        choice("Anime language", "Subtitled or dubbed, where the server offers both", new String[]{"Subbed", "Dubbed"},
            "dub".equals(AppPreferences.getAnimeDubSub(c)) ? 1 : 0, i -> AppPreferences.setAnimeDubSub(ctx(), i == 1 ? "dub" : "sub"));
        toggle("Autoplay", "Start playing as soon as a server loads", AppPreferences.getAutoplay(c), v -> AppPreferences.setAutoplay(ctx(), v));
        toggle("Picture-in-picture", "Keep watching in a small window when you leave the app", AppPreferences.getPiP(c), v -> AppPreferences.setPiP(ctx(), v));
        toggle("Keep screen on while watching", null, AppPreferences.getKeepScreenOn(c), v -> AppPreferences.setKeepScreenOn(ctx(), v));
        toggle("Play in background", "Keep the sound going when the screen is off", AppPreferences.getBackgroundPlayback(c), v -> AppPreferences.setBackgroundPlayback(ctx(), v));
        toggle("Compatibility mode", "Load the server page directly. Turn on if a server shows a blank player.", AppPreferences.getEmbedDirect(c), v -> AppPreferences.setEmbedDirect(ctx(), v));

        section("OFFLINE PLAYER");
        final String[] playerVals = {"builtin", "external", "ask"};
        choice("Play downloads in", "Built-in player, one external app you choose once, or pick an app every time",
            new String[]{"Built-in player", "External player", "Ask every time"},
            indexOf(playerVals, AppPreferences.getOfflinePlayer(c)), i -> {
                AppPreferences.setOfflinePlayer(ctx(), playerVals[i]);
                if (i == 1 && AppPreferences.getExternalPlayerPkg(ctx()).isEmpty()) pickExternalApp();
            });
        extInfo = action("External player app", externalSummary(), false, this::pickExternalApp);
        toggle("Auto picture-in-picture", "Keep the built-in player floating when you leave the app", AppPreferences.getAutoPip(c), v -> AppPreferences.setAutoPip(ctx(), v));
        int[] skips = {5, 10, 15, 30};
        choice("Double-tap skip", "Seconds to jump when you double-tap or press skip", new String[]{"5 seconds", "10 seconds", "15 seconds", "30 seconds"},
            indexOf(skips, AppPreferences.getDoubleTapSeek(c)), i -> AppPreferences.setDoubleTapSeek(ctx(), skips[i]));
        toggle("Swipe gestures", "Left side brightness, right side volume, hold for fast speed", AppPreferences.getGestureControls(c), v -> AppPreferences.setGestureControls(ctx(), v));
        toggle("Swipe to seek", "Drag left or right to scrub through the video", AppPreferences.getSwipeSeek(c), v -> AppPreferences.setSwipeSeek(ctx(), v));
        float[] speeds = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};
        String[] speedNames = {"0.5x", "0.75x", "Normal", "1.25x", "1.5x", "1.75x", "2x"};
        choice("Default playback speed", null, speedNames, indexOf(speeds, AppPreferences.getPlaybackSpeed(c)), i -> AppPreferences.setPlaybackSpeed(ctx(), speeds[i]));
        float[] holds = {1.5f, 2f, 3f};
        choice("Hold-to-speed-up", "Speed while you press and hold the screen", new String[]{"1.5x", "2x", "3x"},
            indexOf(holds, AppPreferences.getHoldSpeed(c)), i -> AppPreferences.setHoldSpeed(ctx(), holds[i]));
        String[] resizes = {"fit", "fill", "zoom"};
        choice("Default picture size", "Fit shows everything, Crop fills the screen", new String[]{"Fit", "Stretch", "Crop"},
            indexOf(resizes, AppPreferences.getResizeMode(c)), i -> AppPreferences.setResizeMode(ctx(), resizes[i]));
        int[] hides = {2, 4, 6, 10};
        choice("Hide controls after", null, new String[]{"2 seconds", "4 seconds", "6 seconds", "10 seconds"},
            indexOf(hides, AppPreferences.getControlsTimeout(c)), i -> AppPreferences.setControlsTimeout(ctx(), hides[i]));
        toggle("Resume where I stopped", null, AppPreferences.getResumePlayback(c), v -> AppPreferences.setResumePlayback(ctx(), v));
        toggle("Play next downloaded episode", "Continue automatically when an episode ends", AppPreferences.getAutoPlayNext(c), v -> AppPreferences.setAutoPlayNext(ctx(), v));

        section("DOWNLOADS");
        toggle("Wi-Fi only", "Do not download over mobile data", AppPreferences.getWifiOnly(c), v -> AppPreferences.setWifiOnly(ctx(), v));
        String[] quals = {"360p", "480p", "720p", "1080p"};
        choice("Download quality", "Higher quality uses more storage", quals,
            indexOf(quals, AppPreferences.getDlQuality(c)), i -> AppPreferences.setDlQuality(ctx(), quals[i]));
        folderInfo = action("Download folder", DownloadFolder.describe(c), false, this::pickFolder);
        refreshFolderInfo();
        downloadsInfo = action("Downloaded videos", "Computing…", false, null);
        action("Delete all downloads", "Removes every downloaded video from this phone", true, this::confirmDeleteDownloads);
        action("Delete all saved pages", "Removes pages saved for offline viewing", true, this::confirmDeleteSavedPages);
        refreshStorage();

        section("DATA");
        int[] imgMb = {100, 250, 500, 1000};
        choice("Image cache size", "Posters and backdrops kept on this phone (applies after restart)",
            new String[]{"100 MB", "250 MB", "500 MB", "1 GB"}, indexOf(imgMb, AppPreferences.getImageCacheMb(c)),
            i -> AppPreferences.setImageCacheMb(ctx(), imgMb[i]));
        cacheInfo = action("Clear cache", "Frees space used by saved images and info", false, this::clearCache);
        action("Clear watch history", "Empties Continue Watching", false, () -> {
            LibraryManager.clearList(ctx(), LibraryManager.CONTINUE);
            toast("History cleared");
        });
        action("Clear My List and Favorites", null, true, () -> confirm("Clear your list and favorites?", () -> {
            LibraryManager.clearList(ctx(), LibraryManager.FAVORITES);
            LibraryManager.clearList(ctx(), LibraryManager.WATCHLIST);
            toast("Cleared");
        }));
        action("Reset all settings", "Puts every option back to its default", true, () -> confirm("Reset all settings?", () -> {
            AppPreferences.clearAll(ctx());
            ThemeManager.applyNightMode(ctx());
            requireActivity().recreate();
        }));
    }

    // ------------------------------------------------------------------ row builders

    private View inflate(int layout) {
        View v = LayoutInflater.from(ctx()).inflate(layout, box, false);
        // Rows share view ids; letting Android "restore" their state after a theme change made every switch copy one value.
        v.setSaveFromParentEnabled(false);
        return v;
    }

    private void section(String name) {
        TextView t = (TextView) inflate(R.layout.row_section);
        t.setText(name);
        box.addView(t);
    }

    private void toggle(String title, @Nullable String summary, boolean value, Consumer<Boolean> on) {
        View r = inflate(R.layout.row_switch);
        ((TextView) r.findViewById(R.id.title)).setText(title);
        TextView sum = r.findViewById(R.id.summary);
        if (summary == null) sum.setVisibility(View.GONE); else sum.setText(summary);
        SwitchCompat sw = r.findViewById(R.id.sw);
        sw.setChecked(value);
        sw.setOnCheckedChangeListener((v, checked) -> on.accept(checked));
        r.findViewById(R.id.row).setOnClickListener(v -> sw.toggle());
        box.addView(r);
    }

    private void choice(String title, @Nullable String summary, String[] labels, int selected, IntConsumer onPick) {
        View r = inflate(R.layout.row_choice);
        ((TextView) r.findViewById(R.id.title)).setText(title);
        TextView sum = r.findViewById(R.id.summary);
        if (summary == null) sum.setVisibility(View.GONE); else sum.setText(summary);
        TextView value = r.findViewById(R.id.value);
        final int[] cur = {Math.max(0, Math.min(labels.length - 1, selected))};
        value.setText(labels[cur[0]]);
        r.findViewById(R.id.row).setOnClickListener(v -> new AlertDialog.Builder(ctx())
            .setTitle(title)
            .setSingleChoiceItems(labels, cur[0], (d, which) -> {
                cur[0] = which;
                value.setText(labels[which]);
                onPick.accept(which);
                d.dismiss();
            })
            .setNegativeButton("Cancel", null).show());
        box.addView(r);
    }

    /** A tappable row. Returns the summary TextView so callers can update it. */
    private TextView action(String title, @Nullable String summary, boolean danger, @Nullable Runnable run) {
        View r = inflate(R.layout.row_choice);
        TextView t = r.findViewById(R.id.title);
        t.setText(title);
        if (danger) t.setTextColor(ThemeManager.color(ctx(), R.attr.fhDanger));
        TextView sum = r.findViewById(R.id.summary);
        if (summary == null) sum.setVisibility(View.GONE); else sum.setText(summary);
        r.findViewById(R.id.value).setVisibility(View.GONE);
        if (run != null) r.findViewById(R.id.row).setOnClickListener(v -> run.run());
        box.addView(r);
        return sum;
    }

    private void swatches() {
        View r = inflate(R.layout.row_swatches);
        ((TextView) r.findViewById(R.id.title)).setText("Accent color");
        ((TextView) r.findViewById(R.id.summary)).setText("Pick a palette for buttons, highlights and progress bars");
        LinearLayout row = r.findViewById(R.id.swatches);
        String cur = AppPreferences.getThemeAccent(ctx());
        int d = (int) (44 * getResources().getDisplayMetrics().density);
        int m = (int) (6 * getResources().getDisplayMetrics().density);
        int ring = (int) (3 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < ThemeManager.ACCENTS.length; i++) {
            final String name = ThemeManager.ACCENTS[i];
            View sw = new View(ctx());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(d, d);
            lp.setMargins(m, m, m, m);
            sw.setLayoutParams(lp);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(ContextCompat.getColor(ctx(), ThemeManager.swatchFor(name)));
            if (name.equals(cur)) g.setStroke(ring, ThemeManager.color(ctx(), R.attr.fhText));
            sw.setBackground(g);
            sw.setContentDescription(ThemeManager.ACCENT_LABELS[i]);
            sw.setOnClickListener(v -> {
                AppPreferences.setThemeAccent(ctx(), name);
                requireActivity().recreate();
            });
            row.addView(sw);
        }
        box.addView(r);
    }

    // ------------------------------------------------------------------ actions

    private void refreshStorage() {
        final Context app = ctx().getApplicationContext();
        new Thread(() -> {
            List<DownloadItem> done = AppDatabase.get(app).downloadDao().getCompletedNow();
            long size = 0;
            for (DownloadItem d : done) if (d.localPath != null) size += new File(d.localPath).length();
            String text = done.size() + " videos  ·  " + fmt(size);
            if (getActivity() != null) requireActivity().runOnUiThread(() -> { if (downloadsInfo != null) downloadsInfo.setText(text); });
        }, "fh-settings-storage").start();
        if (cacheInfo != null) cacheInfo.setText("Frees space used by saved images and info  ·  " + fmt(size(ctx().getCacheDir())));
    }

    private void confirmDeleteDownloads() {
        confirm("Delete every downloaded video?", () -> {
            final Context app = ctx().getApplicationContext();
            new Thread(() -> {
                for (DownloadItem d : AppDatabase.get(app).downloadDao().getCompletedNow()) DownloadManager2.delete(app, d);
                if (getActivity() != null) requireActivity().runOnUiThread(() -> { toast("Downloads deleted"); refreshStorage(); });
            }, "fh-delete-downloads").start();
        });
    }

    private void confirmDeleteSavedPages() {
        confirm("Delete all saved pages?", () -> {
            final Context app = ctx().getApplicationContext();
            new Thread(() -> {
                for (OfflineStore.Saved s : OfflineStore.list(app)) OfflineStore.remove(app, s.item.mediaType, s.item.id);
                if (getActivity() != null) requireActivity().runOnUiThread(() -> toast("Saved pages deleted"));
            }, "fh-delete-pages").start();
        });
    }

    private void clearCache() {
        final Context app = ctx().getApplicationContext();
        new Thread(() -> {
            try { com.bumptech.glide.Glide.get(app).clearDiskCache(); } catch (Throwable ignored) {}
            com.fountainhome.streaming.api.TMDBClient.clearCache();
            com.fountainhome.streaming.anime.AniListClient.clearCache();
            if (getActivity() != null) requireActivity().runOnUiThread(() -> {
                try { com.bumptech.glide.Glide.get(app).clearMemory(); } catch (Throwable ignored) {}
                toast("Cache cleared");
                refreshStorage();
            });
        }, "fh-clear-cache").start();
    }

    // ------------------------------------------------------------------ external player

    private String externalSummary() {
        String pkg = AppPreferences.getExternalPlayerPkg(ctx());
        if (pkg.isEmpty() || !com.fountainhome.streaming.download.ExternalPlayers.isInstalled(ctx(), pkg))
            return "Not chosen yet. Tap to pick the app used by \"External player\"";
        return com.fountainhome.streaming.download.ExternalPlayers.labelOf(ctx(), pkg) + "  \u00b7  tap to change";
    }

    private void pickExternalApp() {
        final java.util.List<android.content.pm.ResolveInfo> apps =
            com.fountainhome.streaming.download.ExternalPlayers.installedPlayers(ctx());
        if (apps.isEmpty()) { toast("No other video player is installed on this phone"); return; }
        final android.content.pm.PackageManager pm = ctx().getPackageManager();
        String[] names = new String[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = String.valueOf(apps.get(i).loadLabel(pm));
        new AlertDialog.Builder(ctx()).setTitle("External player")
            .setItems(names, (d, which) -> {
                AppPreferences.setExternalPlayerPkg(ctx(), apps.get(which).activityInfo.packageName);
                if (extInfo != null) extInfo.setText(externalSummary());
                toast("Downloads will open in " + names[which]);
            }).setNegativeButton("Cancel", null).show();
    }

    // ------------------------------------------------------------------ download folder

    private void refreshFolderInfo() {
        if (folderInfo == null) return;
        String t = DownloadFolder.describe(ctx());
        if (DownloadFolder.customUnavailable(ctx())) t += "  ·  waiting for file access, using app storage for now";
        folderInfo.setText(t);
    }

    @Override public void onResume() { super.onResume(); refreshFolderInfo(); }

    private void pickFolder() {
        final java.util.List<String> names = new java.util.ArrayList<>();
        final java.util.List<Runnable> acts = new java.util.ArrayList<>();
        names.add("App storage (default)");
        acts.add(() -> { AppPreferences.setDownloadDir(ctx(), ""); refreshFolderInfo(); });
        File[] vols = DownloadFolder.appVolumes(ctx());
        for (int i = 1; i < vols.length; i++) {
            final File v = vols[i];
            names.add("SD card / external volume");
            acts.add(() -> { AppPreferences.setDownloadDir(ctx(), v.getAbsolutePath()); refreshFolderInfo(); });
        }
        names.add("Choose a folder\u2026");
        acts.add(() -> treePicker.launch(null));
        new AlertDialog.Builder(ctx()).setTitle("Download folder")
            .setItems(names.toArray(new String[0]), (d, which) -> acts.get(which).run())
            .setNegativeButton("Cancel", null).show();
    }

    /** Folders outside the app's own storage need the "All files access" permission. */
    private void askFileAccessIfNeeded() {
        if (DownloadFolder.hasAllFilesAccess(ctx())) return;
        new AlertDialog.Builder(ctx())
            .setTitle("Allow file access")
            .setMessage("To save downloads in a folder you picked, Fountain Home needs permission to manage files. Until you allow it, downloads go to app storage.")
            .setPositiveButton("Allow", (d, w) -> {
                Intent i = DownloadFolder.allFilesAccessIntent(ctx());
                if (i != null) { try { startActivity(i); } catch (Exception e) { toast("Open Settings > Apps > Fountain Home > Permissions"); } }
                else legacyPermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            })
            .setNegativeButton("Not now", null).show();
    }

    private void confirm(String message, Runnable yes) {
        new AlertDialog.Builder(ctx()).setMessage(message)
            .setPositiveButton("Yes", (d, w) -> yes.run()).setNegativeButton("Cancel", null).show();
    }

    private void toast(String m) { Toast.makeText(ctx(), m, Toast.LENGTH_SHORT).show(); }

    private static void delete(File d, boolean self) {
        if (d == null) return;
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) delete(f, true);
        if (self) //noinspection ResultOfMethodCallIgnored
            d.delete();
    }

    private static long size(File d) {
        if (d == null) return 0;
        if (d.isFile()) return d.length();
        long s = 0;
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) s += size(f);
        return s;
    }

    private static String fmt(long v) {
        if (v < 1024) return v + " B";
        if (v < 1048576) return (v / 1024) + " KB";
        if (v < 1073741824L) return (v / 1048576) + " MB";
        return String.format(java.util.Locale.US, "%.1f GB", v / 1073741824.0);
    }

    private static int indexOf(String[] a, String v) { for (int i = 0; i < a.length; i++) if (a[i].equalsIgnoreCase(v)) return i; return 0; }
    private static int indexOf(int[] a, int v) { for (int i = 0; i < a.length; i++) if (a[i] == v) return i; return 0; }
    private static int indexOf(float[] a, float v) { for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - v) < 0.01f) return i; return 2; }
}
