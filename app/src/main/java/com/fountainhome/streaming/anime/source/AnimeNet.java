package com.fountainhome.streaming.anime.source;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.fountainhome.streaming.download.StreamResolver;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** HTTP and stream-finding helpers shared by every anime site (blocking: call from a worker thread). */
public final class AnimeNet {
    private AnimeNet() {}

    public static final String UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
        + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    static final OkHttpClient HTTP = new OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true).followSslRedirects(true).build();

    private static Context app;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void init(Context c) { app = c.getApplicationContext(); }

    public static String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder rb = new Request.Builder().url(url).header("User-Agent", UA);
        if (headers != null) for (Map.Entry<String, String> e : headers.entrySet()) rb.header(e.getKey(), e.getValue());
        try (Response r = HTTP.newCall(rb.build()).execute()) {
            String body = r.body() != null ? r.body().string() : "";
            if (!r.isSuccessful()) throw new IOException("HTTP " + r.code() + " for " + url);
            return body;
        }
    }

    public static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    public static String enc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    public static String origin(String url) {
        try { URI u = new URI(url); return u.getScheme() + "://" + u.getAuthority(); } catch (Exception e) { return url; }
    }

    public static boolean isDirect(String url) {
        String p = url.split("[?#]")[0].toLowerCase();
        return p.endsWith(".m3u8") || p.endsWith(".mp4");
    }

    // ------------------------------------------------------------------ packed JS (p.a.c.k.e.r)

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern PACKED = Pattern.compile(
        "\\}\\('((?:[^'\\\\]|\\\\.)*)',\\s*(\\d+),\\s*(\\d+),\\s*'((?:[^'\\\\]|\\\\.)*)'\\.split\\('\\|'\\)");
    private static final Pattern TOKEN = Pattern.compile("\\b\\w+\\b");

    private static int decode(String v, int radix) {
        int n = 0;
        for (char ch : v.toCharArray()) {
            int d = ALPHABET.indexOf(ch);
            if (d < 0 || d >= radix) return -1;
            n = n * radix + d;
        }
        return n;
    }

    /** Many embed players hide their stream URL inside one of these packed scripts. */
    public static List<String> unpackAll(String js) {
        List<String> out = new ArrayList<>();
        Matcher m = PACKED.matcher(js);
        while (m.find()) {
            String payload = m.group(1).replace("\\'", "'").replace("\\\\", "\\");
            int radix;
            try { radix = Integer.parseInt(m.group(2)); } catch (Exception e) { continue; }
            if (radix < 2 || radix > 62) continue;
            String[] words = m.group(4).split("\\|", -1);
            Matcher t = TOKEN.matcher(payload);
            StringBuffer sb = new StringBuffer();
            while (t.find()) {
                int idx = decode(t.group(), radix);
                String w = idx >= 0 && idx < words.length && !words[idx].isEmpty() ? words[idx] : t.group();
                t.appendReplacement(sb, Matcher.quoteReplacement(w));
            }
            t.appendTail(sb);
            out.add(sb.toString());
        }
        return out;
    }

    private static final Pattern STREAM = Pattern.compile(
        "https?://[^\\s\"'<>\\\\)]+?\\.(?:m3u8|mp4)(?:\\?[^\\s\"'<>\\\\)]*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMBED = Pattern.compile(
        "https?://[^\\s\"'<>\\\\]+/(?:v|e|embed|player)/[A-Za-z0-9_\\-]{6,}[^\\s\"'<>\\\\]*", Pattern.CASE_INSENSITIVE);

    public static String findStreamUrl(String text) {
        String cleaned = text.replace("\\/", "/").replace("&amp;", "&");
        Matcher m = STREAM.matcher(cleaned);
        String first = null;
        while (m.find()) {
            if (m.group().toLowerCase().contains(".m3u8")) return m.group();
            if (first == null) first = m.group();
        }
        return first;
    }

    /** Opens an embed page and looks for a direct stream: HTML, unpacked scripts, then nested iframes (2 levels). */
    public static AnimeSource.Stream resolveEmbed(String pageUrl, String referer, int depth) throws IOException {
        String html = get(pageUrl, map("Referer", referer));
        List<String> texts = new ArrayList<>();
        texts.add(html);
        texts.addAll(unpackAll(html));
        for (String t : texts) {
            String s = findStreamUrl(t);
            if (s == null) continue;
            String o = origin(pageUrl);
            return new AnimeSource.Stream(s, map("Referer", o + "/", "Origin", o));
        }
        if (depth < 2) {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html, pageUrl);
            java.util.LinkedHashSet<String> next = new java.util.LinkedHashSet<>();
            for (org.jsoup.nodes.Element e : doc.select("iframe[src], iframe[data-src]")) {
                String u = e.absUrl("src");
                next.add(u.isEmpty() ? e.absUrl("data-src") : u);
            }
            for (org.jsoup.nodes.Element e : doc.select("[data-embed], [data-video], [data-url], [data-link]")) {
                for (String a : new String[]{"data-embed", "data-video", "data-url", "data-link"}) {
                    String u = e.absUrl(a);
                    if (u.startsWith("http")) next.add(u);
                }
            }
            Matcher m = EMBED.matcher(html.replace("\\/", "/"));
            while (m.find()) next.add(m.group());
            for (String c : next) {
                if (!c.startsWith("http") || c.equals(pageUrl)) continue;
                try { return resolveEmbed(c, pageUrl, depth + 1); } catch (IOException ignored) { }
            }
        }
        throw new IOException("No playable stream found on " + origin(pageUrl));
    }

    /** Last resort: load the page in a hidden WebView and watch its requests (players that build the URL with JS). */
    public static AnimeSource.Stream webResolve(String pageUrl) throws IOException {
        if (app == null) throw new IOException("Not ready");
        final StreamResolver.Found[] holder = new StreamResolver.Found[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final StreamResolver r = new StreamResolver();
        MAIN.post(() -> {
            try {
                r.resolve(app, pageUrl, 25000, found -> { holder[0] = found; latch.countDown(); });
            } catch (Throwable t) { latch.countDown(); }
        });
        try { latch.await(40, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        MAIN.post(r::cancel);
        if (holder[0] == null || holder[0].url == null) throw new IOException("The player did not reveal a stream");
        return new AnimeSource.Stream(holder[0].url, new HashMap<>(holder[0].headers));
    }

    /** Embed page or direct file to a playable stream: direct, then HTML scraping, then the hidden WebView. */
    public static AnimeSource.Stream toStream(String url, String referer) throws IOException {
        if (isDirect(url)) return new AnimeSource.Stream(url, map("Referer", referer, "Origin", origin(referer)));
        try { return resolveEmbed(url, referer, 0); }
        catch (IOException first) {
            try { return webResolve(url); } catch (IOException second) { throw first; }
        }
    }
}
