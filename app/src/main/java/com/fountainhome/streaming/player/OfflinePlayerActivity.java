package com.fountainhome.streaming.player;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.fountainhome.streaming.databinding.ActivityOfflinePlayerBinding;
import com.fountainhome.streaming.download.AppDatabase;
import com.fountainhome.streaming.download.DownloadItem;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.service.WatchProgress;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;

import java.io.File;

/**
 * Plays a downloaded video from the Offline folder. This is a local file, so a normal native
 * player is the right tool here (the streaming screen uses the provider's own web player).
 */
public class OfflinePlayerActivity extends AppCompatActivity {
    public static final String EXTRA_DOWNLOAD_ID = "download_id";

    private ActivityOfflinePlayerBinding b;
    private ExoPlayer player;
    private DownloadItem item;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityOfflinePlayerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        b.backBtn.setOnClickListener(v -> finish());

        int id = getIntent().getIntExtra(EXTRA_DOWNLOAD_ID, -1);
        item = AppDatabase.get(this).downloadDao().byId(id);
        if (!isPlayable(item)) {
            Toast.makeText(this, "This download is missing. Delete it and download again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override protected void onStart() {
        super.onStart();
        hideBars();
        if (item != null && player == null) startPlayer();
    }

    @Override protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    // ------------------------------------------------------------------

    private static boolean isPlayable(DownloadItem d) {
        return d != null && d.localPath != null && new File(d.localPath).isFile();
    }

    private void startPlayer() {
        b.titleText.setText(label(item));
        player = new ExoPlayer.Builder(this).build();
        b.playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(new File(item.localPath))));
        long resume = WatchProgress.get(this, item.tmdbId, item.type, item.season, item.episode);
        player.prepare();
        if (resume > 5000) player.seekTo(resume);
        player.setPlayWhenReady(true);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) onFinished();
            }
            @Override public void onPlayerError(PlaybackException error) {
                Toast.makeText(OfflinePlayerActivity.this,
                    "Could not play this file: " + error.getErrorCodeName(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void releasePlayer() {
        if (player == null) return;
        saveProgress();
        b.playerView.setPlayer(null);
        player.release();
        player = null;
    }

    private void saveProgress() {
        if (player == null || item == null) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        // Finished (or nearly): forget the position so it starts from the beginning next time.
        if (dur > 0 && pos > dur - 15000) pos = 0;
        ContentItem ci = new ContentItem();
        ci.id = item.tmdbId;
        ci.mediaType = item.type;
        ci.title = item.title;
        ci.posterPath = item.posterPath;
        ci.backdropPath = item.backdropPath;
        ci.rating = item.rating;
        ci.imdbId = item.imdbId;
        if (dur > 0) ci.durationMs = dur;
        LibraryManager.updateProgress(this, ci, item.season, item.episode, pos);
    }

    /** Play the next downloaded episode automatically, if there is one. */
    private void onFinished() {
        if (item == null || "movie".equals(item.type)) return;
        DownloadItem next = AppDatabase.get(this).downloadDao()
            .find(item.tmdbId, item.type, item.season, item.episode + 1);
        if (next != null && DownloadItem.COMPLETED.equals(next.status) && isPlayable(next)) {
            saveProgress();
            item = next;
            b.titleText.setText(label(item));
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(new File(item.localPath))));
            player.prepare();
            player.setPlayWhenReady(true);
        }
    }

    private static String label(DownloadItem d) {
        return "movie".equals(d.type) ? d.title : d.title + "  ·  S" + d.season + " E" + d.episode;
    }

    @SuppressWarnings("deprecation")
    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
