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

    /**
     * True for files a player can't seek reliably: raw MPEG-TS, and "fragmented" MP4 (what HLS fMP4
     * streams produce: no sample index, just a chain of moof boxes).
     */
    public static boolean needsRemux(File f) {
        if (f == null || !f.isFile() || f.length() == 0) return false;
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
    public static File makeSeekable(File in, Progress p) {
        if (!needsRemux(in)) return in;
        String name = in.getName();
        boolean mp4 = name.toLowerCase().endsWith(".mp4") || name.toLowerCase().endsWith(".m4v") || name.toLowerCase().endsWith(".mov");
        File out = mp4 ? in : new File(in.getParentFile(), name.substring(0, name.lastIndexOf('.')) + ".mp4");
        File tmp = new File(in.getParentFile(), name + ".remux.part");
        if (copy(in, tmp, p) && tmp.length() > 0) {
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
        return in;
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

    private static boolean copy(File in, File out, Progress progress) {
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

            ByteBuffer buf = ByteBuffer.allocate(8 * 1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long base = -1;
            int lastPct = -1;
            long[] shift = new long[n], lastOut = new long[n];
            while (true) {
                int size = ex.readSampleData(buf, 0);
                if (size < 0) break;
                int track = ex.getSampleTrackIndex();
                long t = ex.getSampleTime();
                if (base < 0) base = Math.max(0, t);
                if (map[track] >= 0) {
                    info.offset = 0;
                    info.size = size;
                    // Segments glued together can restart their clock; keep time moving forward.
                    long out = Math.max(0, t - base) + shift[track];
                    if (out < lastOut[track] - 1_000_000L) { shift[track] += lastOut[track] - out + 33_000L; out = lastOut[track] + 33_000L; }
                    lastOut[track] = Math.max(lastOut[track], out);
                    info.presentationTimeUs = out;
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
