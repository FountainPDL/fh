package com.fountainhome.streaming.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Turns a stream URL into one playable video file.
 *
 *  - HLS (.m3u8): picks a variant for the chosen quality, downloads every segment (4 in
 *    parallel, written in order), decrypts AES-128 segments, and joins them into a single
 *    file: ".ts" for MPEG-TS streams, ".mp4" for fragmented-MP4 streams.
 *  - Plain video URLs (.mp4 etc.): streamed straight to disk.
 */
public final class HlsDownloader {
    private HlsDownloader() {}

    public interface Progress {
        void onProgress(int percent);
        boolean isCancelled();
        /** Which quality was really chosen (e.g. "720p (480p is not offered by this server)"). */
        default void onInfo(String info) { }
    }

    private static final int PARALLEL = 4;

    /** Downloads {@code url} into {@code dir}/{@code baseName}.ext and returns the finished file. */
    public static File download(OkHttpClient http, String url, Map<String, String> headers,
                                String quality, File dir, String baseName, Progress cb) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create folder " + dir);
        String lower = url.toLowerCase();
        if (lower.contains("m3u8")) return downloadHls(http, url, headers, quality, dir, baseName, cb);
        if (cb != null) cb.onInfo("direct file, one quality only");
        String ext = lower.contains(".webm") ? "webm" : (lower.contains(".mkv") ? "mkv" : "mp4");
        return downloadFile(http, url, headers, new File(dir, baseName + "." + ext), cb);
    }

    // ------------------------------------------------------------------ plain file

    private static File downloadFile(OkHttpClient http, String url, Map<String, String> headers,
                                     File out, Progress cb) throws IOException {
        File part = new File(out.getPath() + ".part");
        long have = part.isFile() ? part.length() : 0;
        Request.Builder rb = request(url, headers).newBuilder();
        if (have > 0) rb.header("Range", "bytes=" + have + "-");
        try (Response r = http.newCall(rb.build()).execute()) {
            boolean complete = have > 0 && r.code() == 416;   // everything was already there
            if (!complete) {
                if (!r.isSuccessful() || r.body() == null) throw new IOException("Server answered HTTP " + r.code());
                boolean resumed = have > 0 && r.code() == 206;
                if (!resumed) have = 0;                           // server ignored Range: start over
                long total = r.body().contentLength();
                if (total > 0) total += have;
                long done = have;
                int lastPct = -1;
                try (InputStream in = r.body().byteStream(); OutputStream os = new FileOutputStream(part, resumed)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        if (cb.isCancelled()) throw new IOException("Cancelled");
                        os.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            int pct = (int) (done * 100 / total);
                            if (pct != lastPct) { lastPct = pct; cb.onProgress(pct); }
                        }
                    }
                }
            }
        }
        if (part.length() < 100 * 1024) { part.delete(); throw new IOException("Downloaded file is too small to be a video"); }
        if (out.exists()) out.delete();
        if (!part.renameTo(out)) throw new IOException("Could not finish file");
        return out;
    }

    // ------------------------------------------------------------------ HLS

    private static final class Segment {
        String url;
        String keyUrl;     // null = not encrypted
        byte[] iv;         // explicit IV, or null = derive from sequence number
        long sequence;
    }

    private static File downloadHls(OkHttpClient http, String playlistUrl, Map<String, String> headers,
                                    String quality, File dir, String baseName, Progress cb) throws IOException {
        String url = playlistUrl;
        String text = new String(getBytes(http, url, headers), "UTF-8");

        // Master playlist -> choose one variant, then fetch its media playlist.
        boolean master = false;
        for (int hop = 0; hop < 3 && text.contains("#EXT-X-STREAM-INF"); hop++) {
            master = true;
            String[] info = new String[1];
            url = pickVariant(url, text, quality, info);
            if (info[0] != null && cb != null) cb.onInfo(info[0]);
            text = new String(getBytes(http, url, headers), "UTF-8");
        }
        if (!text.contains("#EXTM3U")) throw new IOException("Not a valid playlist");
        if (cb != null && !text.contains("#EXT-X-STREAM-INF") && !master) cb.onInfo("this server offers one quality only");
        if (!text.contains("#EXT-X-ENDLIST")) throw new IOException("Live streams cannot be downloaded");

        List<Segment> segments = new ArrayList<>();
        String initUrl = null;
        String keyUrl = null;
        byte[] keyIv = null;
        long seq = 0;
        boolean pendingSegment = false;

        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                seq = parseLong(line.substring("#EXT-X-MEDIA-SEQUENCE:".length()), 0);
            } else if (line.startsWith("#EXT-X-KEY:")) {
                String method = attr(line, "METHOD");
                if ("NONE".equals(method)) { keyUrl = null; keyIv = null; }
                else if ("AES-128".equals(method)) {
                    String uri = attr(line, "URI");
                    if (uri == null) throw new IOException("Encrypted stream without key");
                    keyUrl = resolve(url, uri);
                    String iv = attr(line, "IV");
                    keyIv = iv != null ? hex(iv) : null;
                } else throw new IOException("Unsupported encryption (" + method + ")");
            } else if (line.startsWith("#EXT-X-MAP:")) {
                String uri = attr(line, "URI");
                if (uri != null) initUrl = resolve(url, uri);
            } else if (line.startsWith("#EXT-X-BYTERANGE")) {
                throw new IOException("Byte-range streams are not supported");
            } else if (line.startsWith("#EXTINF")) {
                pendingSegment = true;
            } else if (!line.startsWith("#") && pendingSegment) {
                Segment s = new Segment();
                s.url = resolve(url, line);
                s.keyUrl = keyUrl;
                s.iv = keyIv;
                s.sequence = seq++;
                segments.add(s);
                pendingSegment = false;
            }
        }
        if (segments.isEmpty()) throw new IOException("Playlist has no video segments");

        String ext = initUrl != null ? "mp4" : "ts";
        File out = new File(dir, baseName + "." + ext);
        File part = new File(out.getPath() + ".part");
        File stateFile = new File(out.getPath() + ".part.state");
        Map<String, byte[]> keys = new HashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL);
        final int total = segments.size();
        // Resume: the partial file holds the first N segments in order; the state file says how many and how long.
        int resumeAt = 0;
        long bytes = 0;
        if (part.isFile() && stateFile.isFile()) {
            try {
                String[] st = readSmall(stateFile).trim().split("\\n");
                if (st.length >= 3 && Integer.parseInt(st[0]) == total) {
                    int w = Integer.parseInt(st[1]);
                    long len = Long.parseLong(st[2]);
                    if (w > 0 && w <= total && part.length() >= len) {
                        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(part, "rw")) { raf.setLength(len); }
                        resumeAt = w; bytes = len;
                    }
                }
            } catch (Exception ignored) { resumeAt = 0; bytes = 0; }
        }
        boolean fresh = resumeAt == 0;
        try (OutputStream os = new FileOutputStream(part, !fresh)) {
            if (fresh) {
                if (initUrl != null) { byte[] init = getBytes(http, initUrl, headers); os.write(init); bytes += init.length; }
            }
            List<Future<byte[]>> window = new ArrayList<>();
            int next = resumeAt, written = resumeAt, lastPct = -1;
            if (resumeAt > 0) { lastPct = resumeAt * 100 / total; cb.onProgress(lastPct); }
            while (written < total) {
                if (cb.isCancelled()) throw new IOException("Cancelled");
                // keep PARALLEL*2 segments in flight, but write strictly in order
                while (next < total && window.size() < PARALLEL * 2) {
                    final Segment s = segments.get(next++);
                    window.add(pool.submit(() -> fetchSegment(http, s, headers, keys)));
                }
                byte[] data;
                try {
                    data = window.remove(0).get();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted");
                } catch (ExecutionException ee) {
                    Throwable c = ee.getCause();
                    throw c instanceof IOException ? (IOException) c : new IOException(String.valueOf(c));
                }
                os.write(data);
                bytes += data.length;
                written++;
                writeState(stateFile, total, written, bytes);
                int pct = written * 100 / total;
                if (pct != lastPct) { lastPct = pct; cb.onProgress(pct); }
            }
        } catch (IOException | RuntimeException e) {
            pool.shutdownNow();              // the partial file and its state stay so the download can continue later
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        }
        pool.shutdownNow();
        if (out.exists()) out.delete();
        if (!part.renameTo(out)) throw new IOException("Could not finish file");
        stateFile.delete();
        return out;
    }

    private static String readSmall(File f) throws IOException {
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[512]; int n;
            while ((n = in.read(b)) > 0 && bo.size() < 4096) bo.write(b, 0, n);
            return bo.toString("UTF-8");
        }
    }

    private static void writeState(File f, int total, int written, long bytes) {
        try (FileOutputStream o = new FileOutputStream(f)) {
            o.write((total + "\n" + written + "\n" + bytes + "\n").getBytes("UTF-8"));
        } catch (Exception ignored) { }
    }

    private static byte[] fetchSegment(OkHttpClient http, Segment s, Map<String, String> headers,
                                       Map<String, byte[]> keys) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                byte[] data = getBytes(http, s.url, headers);
                if (s.keyUrl == null) return data;
                byte[] key;
                synchronized (keys) {
                    key = keys.get(s.keyUrl);
                    if (key == null) {
                        key = getBytes(http, s.keyUrl, headers);
                        keys.put(s.keyUrl, key);
                    }
                }
                byte[] iv = s.iv;
                if (iv == null) {           // default IV = media sequence number, big endian
                    iv = new byte[16];
                    long q = s.sequence;
                    for (int i = 15; i >= 8; i--) { iv[i] = (byte) (q & 0xff); q >>= 8; }
                } else if (iv.length != 16) {
                    byte[] p = new byte[16];
                    System.arraycopy(iv, 0, p, 16 - Math.min(16, iv.length), Math.min(16, iv.length));
                    iv = p;
                }
                Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
                return c.doFinal(data);
            } catch (IOException e) {
                last = e;
            } catch (Exception e) {
                throw new IOException("Could not decrypt video: " + e.getMessage());
            }
            try { Thread.sleep(600L * (attempt + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        throw last != null ? last : new IOException("Segment failed");
    }

    // ------------------------------------------------------------------ playlist helpers

    /**
     * Pick the variant matching the wanted quality: the best one not above it, or the lowest one
     * if every variant is above it. Height is judged by the larger of the real height and the
     * 16:9-equivalent of the width (so 1920x800 counts as 1080p). Variants without a RESOLUTION are
     * ranked by BANDWIDTH against typical bitrates for each tier.
     */
    private static String pickVariant(String base, String master, String quality, String[] info) throws IOException {
        int want = 720;
        boolean best = quality == null || quality.toLowerCase().startsWith("best");
        if (best) want = 100000;
        else try { want = Integer.parseInt(quality.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
        long wantBw = want >= 100000 ? Long.MAX_VALUE : want <= 360 ? 1_000_000L : want <= 480 ? 1_800_000L : want <= 720 ? 3_500_000L : Long.MAX_VALUE;
        String[] lines = master.split("\n");
        String bestUrl = null, lowestUrl = null;
        int bestH = 0, lowestH = 0, maxH = 0;
        java.util.TreeSet<Integer> offered = new java.util.TreeSet<>();
        long bestKey = -1, lowestKey = Long.MAX_VALUE;
        Pattern res = Pattern.compile("RESOLUTION=(\\d+)x(\\d+)");
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (!l.startsWith("#EXT-X-STREAM-INF")) continue;
            String uri = null;
            for (int j = i + 1; j < lines.length; j++) {
                String n = lines[j].trim();
                if (n.isEmpty() || n.startsWith("#")) continue;
                uri = n;
                break;
            }
            if (uri == null) continue;
            long bw = parseLong(attr(l, "AVERAGE-BANDWIDTH"), 0);
            if (bw <= 0) bw = parseLong(attr(l, "BANDWIDTH"), 0);
            Matcher m = res.matcher(l);
            boolean hasRes = m.find();
            long key;       // comparable "size" of the variant
            boolean fits;
            if (hasRes) {
                int w = Integer.parseInt(m.group(1)), h = Integer.parseInt(m.group(2));
                int eff = Math.max(h, Math.round(w * 9f / 16f));
                key = eff * 100_000_000L + bw / 1000;
                fits = eff <= want + 8;
                offered.add(eff);
            } else {
                key = bw;
                fits = bw <= wantBw;
            }
            String abs = resolve(base, uri);
            int effH = hasRes ? Math.max(Integer.parseInt(m.group(2)), Math.round(Integer.parseInt(m.group(1)) * 9f / 16f)) : 0;
            if (fits && key > bestKey) { bestKey = key; bestUrl = abs; bestH = effH; }
            if (key < lowestKey) { lowestKey = key; lowestUrl = abs; lowestH = effH; }
        }
        String pick = bestUrl != null ? bestUrl : lowestUrl;
        if (pick == null) throw new IOException("No video variants in playlist");
        int gotH = bestUrl != null ? bestH : lowestH;
        if (gotH > 0) {
            StringBuilder sb = new StringBuilder().append(gotH).append("p");
            if (!best && offered.size() > 1 && Math.abs(gotH - want) > 24) sb.append(" (closest to ").append(want).append("p)");
            else if (best) sb.append(" (best of ").append(offered.size()).append(")");
            info[0] = sb.toString();
        } else info[0] = best ? "best available" : null;
        return pick;
    }

    static String attr(String line, String name) {
        Matcher m = Pattern.compile("(?:^|[:,])" + name + "=(\"[^\"]*\"|[^,]*)").matcher(line);
        if (!m.find()) return null;
        String v = m.group(1);
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) v = v.substring(1, v.length() - 1);
        return v;
    }

    static String resolve(String base, String ref) throws IOException {
        HttpUrl b = HttpUrl.parse(base);
        HttpUrl r = b != null ? b.resolve(ref) : null;
        if (r == null) throw new IOException("Bad playlist URL: " + ref);
        return r.toString();
    }

    private static byte[] hex(String s) {
        String h = s.startsWith("0x") || s.startsWith("0X") ? s.substring(2) : s;
        if (h.length() % 2 == 1) h = "0" + h;
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    // ------------------------------------------------------------------ http

    private static Request request(String url, Map<String, String> headers) {
        Request.Builder rb = new Request.Builder().url(url);
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                String k = e.getKey();
                if (k == null || e.getValue() == null) continue;
                String kl = k.toLowerCase();
                if (kl.equals("host") || kl.equals("range") || kl.equals("accept-encoding")
                    || kl.equals("content-length") || kl.equals("connection")) continue;
                try { rb.header(k, e.getValue()); } catch (IllegalArgumentException ignored) {}
            }
        }
        return rb.build();
    }

    static byte[] getBytes(OkHttpClient http, String url, Map<String, String> headers) throws IOException {
        try (Response r = http.newCall(request(url, headers)).execute()) {
            if (!r.isSuccessful() || r.body() == null) throw new IOException("Server answered HTTP " + r.code());
            return r.body().bytes();
        }
    }
}
