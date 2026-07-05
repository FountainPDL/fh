package com.fountainhome.streaming.service;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.*;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
public class StreamExtractor {
    public interface Callback { void onFound(String url, Map<String, String> headers); void onFailed(); }
    private static final String[] PATTERNS = {".m3u8", "master.m3u8", "playlist.m3u8", "/hls/", "index.m3u8", ".mp4?", ".mp4&", ".mpd"};
    private static final WebResourceResponse EMPTY = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    private WebView wv;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final AtomicBoolean found = new AtomicBoolean(false);

    @SuppressLint("SetJavaScriptEnabled")
    public void extract(Context ctx, String embedUrl, int timeoutMs, Callback cb) {
        found.set(false);
        h.post(() -> {
            wv = new WebView(ctx.getApplicationContext());
            WebSettings ws = wv.getSettings();
            ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setJavaScriptCanOpenWindowsAutomatically(false);
            ws.setSupportMultipleWindows(false);
            ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");
            wv.setWebChromeClient(new WebChromeClient() {
                @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) { return false; }
            });
            wv.setWebViewClient(new WebViewClient() {
                @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                    if (found.get()) return null;
                    String url = r.getUrl().toString().toLowerCase();
                    for (String b : AdBlockWebViewClient.BLOCK) if (url.contains(b)) return EMPTY;
                    for (String p : PATTERNS) {
                        if (url.contains(p)) {
                            found.set(true);
                            Map<String, String> hd = new HashMap<>(r.getRequestHeaders());
                            hd.put("Referer", embedUrl);
                            h.post(() -> { cb.onFound(r.getUrl().toString(), hd); destroy(); });
                            break;
                        }
                    }
                    return null;
                }
                @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                    String scheme = r.getUrl().getScheme();
                    return scheme == null || !(scheme.equals("http") || scheme.equals("https"));
                }
            });
            String html = "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;background:#000}"
                + "iframe{width:100vw;height:100vh;border:none}</style></head>"
                + "<body><iframe src='" + embedUrl + "' allowfullscreen allow='autoplay;fullscreen'></iframe></body></html>";
            wv.loadDataWithBaseURL("https://www.google.com", html, "text/html", "utf-8", null);
            h.postDelayed(() -> { if (!found.get()) { found.set(true); cb.onFailed(); destroy(); } }, timeoutMs);
        });
    }
    private void destroy() { h.post(() -> { if (wv != null) { wv.stopLoading(); wv.destroy(); wv = null; } }); }
}
