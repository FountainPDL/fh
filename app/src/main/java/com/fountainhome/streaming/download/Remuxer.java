package com.fountainhome.streaming.download;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Copies the streams of a downloaded .ts file into an .mp4 without re-encoding (fast, no quality loss).
 * Raw MPEG-TS and fragmented MP4 have no seek index, which is why seeking in them fails; a normal MP4 has one.
 */
public final class Remuxer {
    private Remuxer() {}

    public interface Progress { void onProgress(int percent); }

    /** A file that failed to convert once is not tried again on every open (that made playback "load" for ages). */
    private static File failMarker(File f) { return new File(f.getParentFile(), f.getName() + ".noremux"); }
    public static void markFailed(File f) { try { failMarker(f).createNewFile(); } catch (Throwable ignored) { } }
    public static boolean failedBefore(File f) { return failMarker(f).exists(); }

    /**
     * True for files a player can't seek reliably: raw MPEG-TS, and "fragmented" MP4 (what HLS fMP4
     * streams produce: no sample index, just a chain of moof boxes).
     */
    public static boolean needsRemux(File f) {
        if (f == null || !f.isFile() || f.length() == 0 || failedBefore(f)) return false;
        String n = f.getName().toLowerCase();
        if (n.endsWith(".ts") || n.endsWith(".m2ts") || n.endsWith(".mts")) return true;
        if (n.endsWith(".mp4") || n.endsWith(".m4v") || n.endsWith(".mov")) return isFragmented(f);
        return false;
    }

    /** Walks the top-level MP4 boxes looking for a movie fragment ("moof"). */
    private static boolean isFragmented(File f) {
        try (java.io.RandomAccessFile r = new java.io.RandomAccessFile(f, "r")) {
            long pos = 0, len = r.length();
            byte[] h = new byte[16];
            int guard = 0;
            while (pos + 8 <= len && guard++ < 200) {
                r.seek(pos);
                r.readFully(h, 0, 8);
                long size = ((h[0] & 0xFFL) << 24) | ((h[1] & 0xFFL) << 16) | ((h[2] & 0xFFL) << 8) | (h[3] & 0xFFL);
                String type = new String(h, 4, 4, "ISO-8859-1");
                if (type.equals("moof")) return true;
                if (size == 1) { r.readFully(h, 8, 8); size = 0; for (int i = 8; i < 16; i++) size = (size << 8) | (h[i] & 0xFFL); }
                else if (size == 0) return false;
                if (size < 8) return false;
                pos += size;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Returns the seekable .mp4 if conversion worked (the original is replaced), otherwise the original untouched. */
    public static File makeSeekable(File in, Progress p) { return makeSeekable(in, p, null); }

    /** cancel: set to true from anywhere to stop and keep the original file. */
    public static File makeSeekable(File in, Progress p, java.util.concurrent.atomic.AtomicBoolean cancel) {
        if (!needsRemux(in)) return in;
        String name = in.getName();
        boolean mp4 = name.toLowerCase().endsWith(".mp4") || name.toLowerCase().endsWith(".m4v") || name.toLowerCase().endsWith(".mov");
        File out = mp4 ? in : new File(in.getParentFile(), name.substring(0, name.lastIndexOf('.')) + ".mp4");
        File tmp = new File(in.getParentFile(), name + ".remux.part");
        boolean ok = copy(in, tmp, p, cancel) && tmp.length() > 0;
        if (cancel != null && cancel.get()) { //noinspection ResultOfMethodCallIgnored
            tmp.delete(); return in; }
        // Check the result before trusting it: if sound and picture no longer start together, or the
        // length is off, the converted file would play badly, so the original (which plays right) is kept.
        if (ok && !sameTiming(in, tmp)) ok = false;
        if (ok) {
            if (mp4) {
                if (in.delete() && tmp.renameTo(out)) return out;
            } else if (tmp.renameTo(out)) {
                //noinspection ResultOfMethodCallIgnored
                in.delete();
                return out;
            }
        }
        //noinspection ResultOfMethodCallIgnored
        tmp.delete();
        try { failMarker(in).createNewFile(); } catch (Throwable ignored) { }
        return in;
    }

    /** {videoStartUs, audioStartUs, durationUs}; -1 where unknown. */
    private static long[] starts(File f) {
        long[] r = {-1, -1, -1};
        MediaExtractor ex = new MediaExtractor();
        try {
            ex.setDataSource(f.getAbsolutePath());
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat fm = ex.getTrackFormat(i);
                String mime = fm.getString(MediaFormat.KEY_MIME);
                if (mime == null) continue;
                int slot = mime.startsWith("video/") ? 0 : mime.startsWith("audio/") ? 1 : -1;
                if (slot < 0 || r[slot] >= 0) continue;
                if (fm.containsKey(MediaFormat.KEY_DURATION)) r[2] = Math.max(r[2], fm.getLong(MediaFormat.KEY_DURATION));
                MediaExtractor one = new MediaExtractor();
                try { one.setDataSource(f.getAbsolutePath()); one.selectTrack(i); r[slot] = one.getSampleTime(); }
                catch (Throwable ignored) { } finally { one.release(); }
            }
        } catch (Throwable ignored) { } finally { try { ex.release(); } catch (Throwable ignored) {} }
        return r;
    }

    private static boolean sameTiming(File in, File out) {
        try {
            long[] a = starts(in), b = starts(out);
            if (b[0] < 0) return false;                                   // output has no readable picture
            if (a[1] >= 0 && b[1] < 0) return false;                      // sound got lost
            if (a[0] >= 0 && a[1] >= 0 && b[1] >= 0) {
                long inGap = a[1] - a[0], outGap = b[1] - b[0];
                if (Math.abs(inGap - outGap) > 60_000L) return false;     // lip-sync drifted
            }
            if (a[2] > 0 && b[2] > 0 && (b[2] < a[2] * 9 / 10 || b[2] > a[2] * 11 / 10)) return false;
            return true;
        } catch (Throwable t) { return false; }
    }

    /** Converts every leftover .ts under a folder. Returns the number converted. */
    public static int convertAll(File root, Progress each) {
        int n = 0;
        File[] kids = root == null ? null : root.listFiles();
        if (kids == null) return 0;
        for (File f : kids) {
            if (f.isDirectory()) n += convertAll(f, each);
            else if (needsRemux(f)) {
                File r = makeSeekable(f, each);
                if (r != f) n++;
            }
        }
        return n;
    }

    private static boolean copy(File in, File out, Progress progress, java.util.concurrent.atomic.AtomicBoolean cancel) {
        MediaExtractor ex = new MediaExtractor();
        MediaMuxer mux = null;
        boolean started = false;
        boolean unsupportedAudio = false;
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
                // Try every codec: the muxer refuses the ones MP4 can't hold and we then keep the original.
                try { map[i] = mux.addTrack(f); }
                catch (Throwable t) { if (video) return false; unsupportedAudio = true; continue; }
                ex.selectTrack(i);
                if (video) haveVideo = true; else haveAudio = true;
                if (f.containsKey(MediaFormat.KEY_DURATION)) duration = Math.max(duration, f.getLong(MediaFormat.KEY_DURATION));
            }
            if (!haveVideo) return false;
            // Never produce a silent video: if the only audio can't go into an MP4, keep the original.
            if (unsupportedAudio && !haveAudio) return false;
            mux.start();
            started = true;

            // Where each track really starts: audio and video rarely begin at the same instant, and
            // that difference has to survive the copy or the sound ends up early/late.
            long base = Long.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (map[i] < 0) continue;
                MediaExtractor probe = new MediaExtractor();
                try {
                    probe.setDataSource(in.getAbsolutePath());
                    probe.selectTrack(i);
                    long t0 = probe.getSampleTime();
                    if (t0 >= 0) base = Math.min(base, t0);
                } catch (Throwable ignored) { } finally { probe.release(); }
            }
            if (base == Long.MAX_VALUE) base = 0;

            ByteBuffer buf = ByteBuffer.allocate(8 * 1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int lastPct = -1;
            // Clock restarts (glued HLS segments, ad breaks) start a new "epoch" with one shift shared by
            // audio and video, so the two stay aligned across the jump.
            java.util.ArrayList<Long> shifts = new java.util.ArrayList<>();
            java.util.ArrayList<Long> anchors = new java.util.ArrayList<>();
            shifts.add(0L); anchors.add(base);
            int[] epoch = new int[n];
            long[] lastRaw = new long[n];
            java.util.Arrays.fill(lastRaw, Long.MIN_VALUE);
            long maxOut = 0;
            while (true) {
                if (cancel != null && cancel.get()) return false;
                int size = ex.readSampleData(buf, 0);
                if (size < 0) break;
                int track = ex.getSampleTrackIndex();
                long t = ex.getSampleTime();
                if (map[track] >= 0 && t >= 0) {
                    if (lastRaw[track] != Long.MIN_VALUE && (t < lastRaw[track] - 1_000_000L || t > lastRaw[track] + 30_000_000L)) {
                        int latest = shifts.size() - 1;
                        if (epoch[track] != latest && Math.abs(t - anchors.get(latest)) < 2_000_000L) {
                            epoch[track] = latest;                     // the other track already started this epoch
                        } else {
                            shifts.add(maxOut + 33_000L - (t - base));
                            anchors.add(t);
                            epoch[track] = shifts.size() - 1;
                        }
                    }
                    lastRaw[track] = t;
                    long ts = Math.max(0, t - base + shifts.get(epoch[track]));
                    maxOut = Math.max(maxOut, ts);
                    info.offset = 0;
                    info.size = size;
                    info.presentationTimeUs = ts;
                    info.flags = (ex.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                        ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
                    mux.writeSampleData(map[track], buf, info);
                }
                ex.advance();
                if (progress != null && duration > 0) {
                    int pct = (int) Math.max(0, Math.min(99, (maxOut) * 100 / duration));
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
