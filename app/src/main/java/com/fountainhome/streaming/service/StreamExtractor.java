package com.fountainhome.streaming.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads an embed URL in a hidden WebView, intercepts network requests,
 * and extracts the actual video stream URL (m3u8/mp4).
 * This is what allows native ExoPlayer playback like the OnStream screenshot.
 */
public class StreamExtractor {

    public interface Callback {
        void onFound(String streamUrl, Map<String, String> headers);
        void onFailed();
    }

    // Known direct-stream URL patterns
    private static final String[] STREAM_PATTERNS = {
        ".m3u8", "master.m3u8", "index.m3u8", "playlist.m3u8",
        "/hls/", "/stream/", ".mp4?", "video.mp4", "/manifest"
    };

    // Domains that typically serve video CDN content
    private static final String[] VIDEO_DOMAINS = {
        "delivery", "stream", "video", "cdn", "media",
        "fastly", "cloudfront", "akamai", "storage"
    };

    private WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean found = new AtomicBoolean(false);

    @SuppressLint("SetJavaScriptEnabled")
    public void extract(Context context, String embedUrl, int timeoutMs, Callback callback) {
        found.set(false);

        mainHandler.post(() -> {
            webView = new WebView(context.getApplicationContext());
            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setLoadWithOverviewMode(true);
            ws.setUseWideViewPort(true);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view,
                        WebResourceRequest request) {
                    if (found.get()) return null;
                    String url = request.getUrl().toString().toLowerCase();
                    if (isStreamUrl(url)) {
                        found.set(true);
                        Map<String, String> headers = new HashMap<>(request.getRequestHeaders());
                        headers.put("Referer", embedUrl);
                        headers.put("Origin", getOrigin(embedUrl));
                        mainHandler.post(() -> {
                            callback.onFound(request.getUrl().toString(), headers);
                            destroyWebView();
                        });
                    }
                    return null;
                }
            });

            // Load embed wrapped in iframe
            String html = "<!DOCTYPE html><html><head>"
                + "<style>*{margin:0;padding:0;background:#000}"
                + "iframe{width:100vw;height:100vh;border:none}</style></head>"
                + "<body><iframe src='" + embedUrl + "' "
                + "allowfullscreen allow='autoplay;fullscreen;encrypted-media'>"
                + "</iframe></body></html>";
            webView.loadDataWithBaseURL(
                "https://www.google.com", html, "text/html", "utf-8", null);

            // Timeout — fall back to WebView embed mode
            mainHandler.postDelayed(() -> {
                if (!found.get()) {
                    found.set(true);
                    callback.onFailed();
                    destroyWebView();
                }
            }, timeoutMs);
        });
    }

    private boolean isStreamUrl(String url) {
        for (String p : STREAM_PATTERNS) {
            if (url.contains(p)) return true;
        }
        return false;
    }

    private String getOrigin(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getProtocol() + "://" + u.getHost();
        } catch (Exception e) {
            return "https://vidsrc.xyz";
        }
    }

    private void destroyWebView() {
        mainHandler.post(() -> {
            if (webView != null) {
                webView.stopLoading();
                webView.destroy();
                webView = null;
            }
        });
    }
}
