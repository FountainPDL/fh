package com.fountainhome.streaming.download;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Copies the streams of a downloaded .ts file into an .mp4 without re-encoding (fast, no quality loss).
 * A raw MPEG-TS file has no seek index, which is why seeking in it is unreliable; an MP4 has one.
 */
public final class Remuxer {
    private Remuxer() {}

    public interface Progress { void onProgress(int percent); }

    /** Returns the .mp4 if it worked (the .ts is deleted), otherwise the original file untouched. */
    public static File makeSeekable(File in, Progress p) {
        if (in == null || !in.getName().toLowerCase().endsWith(".ts")) return in;
        File out = new File(in.getParentFile(), in.getName().substring(0, in.getName().length() - 3) + ".mp4");
        File tmp = new File(out.getPath() + ".part");
        if (copy(in, tmp, p) && tmp.length() > 0 && tmp.renameTo(out)) {
            //noinspection ResultOfMethodCallIgnored
            in.delete();
            return out;
        }
        //noinspection ResultOfMethodCallIgnored
        tmp.delete();
        return in;
    }

    private static boolean copy(File in, File out, Progress progress) {
        MediaExtractor ex = new MediaExtractor();
        MediaMuxer mux = null;
        boolean started = false;
        try {
            ex.setDataSource(in.getAbsolutePath());
            int n = ex.getTrackCount();
            int[] map = new int[n];
            java.util.Arrays.fill(map, -1);
            boolean haveVideo = false, haveAudio = false;
            long duration = 0;
            mux = new MediaMuxer(out.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            for (int i = 0; i < n; i++) {
                MediaFormat f = ex.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime == null) continue;
                boolean video = mime.startsWith("video/"), audio = mime.startsWith("audio/");
                if ((video && haveVideo) || (audio && haveAudio) || (!video && !audio)) continue;
                // MP4 can hold H.264/H.265 and AAC/MP3; anything else is left as a .ts.
                if (video && !(mime.equals("video/avc") || mime.equals("video/hevc"))) return false;
                if (audio && !(mime.equals("audio/mp4a-latm") || mime.equals("audio/mpeg"))) continue;
                map[i] = mux.addTrack(f);
                ex.selectTrack(i);
                if (video) haveVideo = true; else haveAudio = true;
                if (f.containsKey(MediaFormat.KEY_DURATION)) duration = Math.max(duration, f.getLong(MediaFormat.KEY_DURATION));
            }
            if (!haveVideo) return false;
            mux.start();
            started = true;

            ByteBuffer buf = ByteBuffer.allocate(4 * 1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long base = -1;
            int lastPct = -1;
            while (true) {
                int size = ex.readSampleData(buf, 0);
                if (size < 0) break;
                int track = ex.getSampleTrackIndex();
                long t = ex.getSampleTime();
                if (base < 0) base = Math.max(0, t);
                if (map[track] >= 0) {
                    info.offset = 0;
                    info.size = size;
                    info.presentationTimeUs = Math.max(0, t - base);
                    info.flags = (ex.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                        ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
                    mux.writeSampleData(map[track], buf, info);
                }
                ex.advance();
                if (progress != null && duration > 0) {
                    int pct = (int) Math.min(99, (t - base) * 100 / duration);
                    if (pct != lastPct) { lastPct = pct; progress.onProgress(pct); }
                }
            }
            return true;
        } catch (Throwable e) {
            return false;
        } finally {
            try { ex.release(); } catch (Throwable ignored) {}
            if (mux != null) {
                try { if (started) mux.stop(); } catch (Throwable ignored) {}
                try { mux.release(); } catch (Throwable ignored) {}
            }
        }
    }
}
