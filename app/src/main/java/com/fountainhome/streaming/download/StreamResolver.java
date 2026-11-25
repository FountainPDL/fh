package com.fountainhome.streaming.download;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fountainhome.streaming.player.AdBlocker;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Used ONLY for downloads (never for playback): loads a server's embed page in an off-screen
 * WebView, watches its network traffic for the real video URL (.m3u8 / .mp4) and reports it.
 *
 * Safety rules:
 *  - media segments are never fetched here, so nothing can play or make sound;
 *  - the WebView is always destroyed (on success, failure, timeout or cancel()).
 * All methods must be called on the main thread.
 */
public class StreamResolver {

    public static final class Found {
        public String url;
        public Map<String, String> headers = new HashMap<>();
        /** Subtitle files the page loaded while we were looking (any language). */
        public java.util.List<SubtitleFetcher.Track> subs = new java.util.ArrayList<>();
    }

    public interface Callback {
        void onResult(Found found);   // null when nothing was found
    }

    private static final Pattern SUBFILE = Pattern.compile("\\.(vtt|srt|ass|ssa)(\\?|$)");
    private final java.util.List<SubtitleFetcher.Track> subs = new java.util.ArrayList<>();
    private Found pending;
    private static final Pattern SEGMENT = Pattern.compile("\\.(ts|m4s|aac)(\\?|$)");
    private static final String CLICK_JS =
        "(function(){try{"
        + "['.jw-icon-display','.vjs-big-play-button','.plyr__control--overlaid','.play-button','.play','#play','button[aria-label=\"Play\"]']"
        + ".forEach(function(s){var e=document.querySelector(s);if(e)e.click();});"
        + "document.querySelectorAll('video').forEach(function(v){v.muted=true;try{v.play();}catch(x){}});"
        + "var d=document.elementFromPoint(window.innerWidth/2,window.innerHeight/2);if(d)d.click();"
        + "}catch(e){}})();";

    private final Handler main = new Handler(Looper.getMainLooper());
    private WebView web;
    private boolean done = false;
    private String embedUrl;
    private Callback callback;

    @SuppressLint("SetJavaScriptEnabled")
    public void resolve(Context ctx, String embedUrl, int timeoutMs, Callback cb) {
        this.embedUrl = embedUrl;
        this.callback = cb;
        Uri eu = Uri.parse(embedUrl);
        final String embedHost = eu.getHost() == null ? "" : eu.getHost();

        WebView w = new WebView(ctx.getApplicationContext());
        web = w;
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setAllowFileAccess(false);
        String ua = s.getUserAgentString();
        if (ua != null) s.setUserAgentString(ua.replace("; wv", "").replace("Version/4.0 ", ""));
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        // Give the detached WebView a real size so players that measure themselves start up.
        w.measure(android.view.View.MeasureSpec.makeMeasureSpec(1280, android.view.View.MeasureSpec.EXACTLY),
                  android.view.View.MeasureSpec.makeMeasureSpec(720, android.view.View.MeasureSpec.EXACTLY));
        w.layout(0, 0, 1280, 720);

        w.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onCreateWindow(WebView v, boolean d, boolean g, android.os.Message m) { return false; }
            @Override public boolean onJsAlert(WebView v, String u, String m, android.webkit.JsResult r) { r.cancel(); return true; }
            @Override public boolean onJsConfirm(WebView v, String u, String m, android.webkit.JsResult r) { r.cancel(); return true; }
        });
        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (!r.isForMainFrame()) return false;
                if (r.isRedirect() && !AdBlocker.isBlocked(r.getUrl())) return false;
                return !com.fountainhome.streaming.player.EmbedWebPlayer.sameSite(r.getUrl().getHost(), embedHost);
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (AdBlocker.isBlocked(u)) return empty();
                String url = u.toString();
                String low = url.toLowerCase();
                boolean m3u8 = low.contains(".m3u8");
                boolean mp4 = low.contains(".mp4") && !SEGMENT.matcher(low).find();
                if (SUBFILE.matcher(low).find() && subs.size() < 12) subs.add(new SubtitleFetcher.Track(url, u.getLastPathSegment()));
                if ((m3u8 || mp4) && !done && pending == null) {
                    Found f = new Found();
                    f.url = url;
                    Map<String, String> rh = r.getRequestHeaders();
                    if (rh != null) f.headers.putAll(rh);
                    if (!f.headers.containsKey("Referer")) f.headers.put("Referer", embedHostUrl(embedUrl));
                    f.subs = subs;
                    // Players ask for their subtitle files around the same time: wait a moment so we see them.
                    main.post(() -> { pending = f; main.postDelayed(() -> finish(f), 2500); });
                    return empty();   // do not let the page load the stream itself
                }
                // Never load media data: no video, no audio, nothing to leak.
                if (SEGMENT.matcher(low).find()) return empty();
                return null;
            }

            @Override public void onPageFinished(WebView v, String url) {
                scheduleClicks();
            }
        });

        w.loadUrl(embedUrl);
        main.postDelayed(() -> finish(null), timeoutMs);
    }

    /** Stop right now without reporting anything. */
    public void cancel() {
        done = true;
        callback = null;
        destroy();
    }

    private void scheduleClicks() {
        for (int i = 1; i <= 4; i++) {
            main.postDelayed(() -> {
                if (!done && web != null) {
                    try { web.evaluateJavascript(CLICK_JS, null); } catch (Throwable ignored) {}
                }
            }, i * 2000L);
        }
    }

    private void finish(Found f) {
        if (done) return;
        done = true;
        Callback cb = callback;
        callback = null;
        destroy();
        if (cb != null) cb.onResult(f);
    }

    private void destroy() {
        main.removeCallbacksAndMessages(null);
        WebView w = web;
        web = null;
        if (w == null) return;
        try {
            w.stopLoading();
            w.setWebViewClient(new WebViewClient());
            w.setWebChromeClient(null);
            w.loadUrl("about:blank");
            w.onPause();
            w.removeAllViews();
            w.destroy();
        } catch (Throwable ignored) {}
    }

    private static WebResourceResponse empty() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
    }

    private static String embedHostUrl(String u) {
        Uri x = Uri.parse(u);
        return x.getScheme() + "://" + x.getHost() + "/";
    }
}
