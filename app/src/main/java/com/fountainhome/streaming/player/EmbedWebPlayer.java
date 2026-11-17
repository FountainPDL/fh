package com.fountainhome.streaming.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.Iterator;

/**
 * Owns the one WebView that shows an embed provider's own player.
 *
 * Design rules (each one exists because of a bug in the previous player):
 *  - The provider's player is shown, visibly, exactly as the provider built it. Nothing is
 *    scraped, hidden or re-wrapped in another player.
 *  - The provider page is hosted in a plain {@code <iframe>} with NO {@code sandbox} attribute
 *    (sandboxed frames break many providers, and some print "sandbox not supported").
 *  - Pop-ups, pop-unders, top-frame redirects, JS dialogs and ad hosts are blocked.
 *  - HTML5 fullscreen works (onShowCustomView / onHideCustomView are implemented).
 *  - {@link #release()} really destroys the WebView so audio can never outlive the screen.
 */
public class EmbedWebPlayer {

    public interface Listener {
        /** The provider frame finished loading its document. */
        void onFrameLoaded();
        /** Progress reported by the provider via postMessage (when the provider supports it). */
        void onProgress(long positionMs, long durationMs);
        /** Provider reported the video ended. */
        void onEnded();
        /** The page asked for / left HTML5 fullscreen. */
        void onFullscreenChanged(boolean fullscreen);
        /** The WebView render process died; the owner should call load() again. */
        void onRenderGone();
    }

    /** Never fetched: only used as the document origin / referrer for the wrapper page. */
    private static final String BASE_URL = "https://fountainhome.app/";

    private final Activity activity;
    private final FrameLayout container;
    private final FrameLayout fullscreenHost;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());

    private WebView web;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean released = false;
    private boolean directMode = false;
    private String directHost = "";

    public EmbedWebPlayer(Activity activity, FrameLayout container, FrameLayout fullscreenHost, Listener listener) {
        this.activity = activity;
        this.container = container;
        this.fullscreenHost = fullscreenHost;
        this.listener = listener;
    }

    // ------------------------------------------------------------------ public API

    /**
     * Load a provider embed.
     *
     * @param embedUrl the provider's embed URL
     * @param direct   false = host it in an un-sandboxed iframe (default, enables progress
     *                 reporting and strict navigation blocking); true = load the provider page
     *                 itself as the top document (compatibility mode for providers that refuse
     *                 to be framed).
     */
    public void load(String embedUrl, boolean direct) {
        if (released || embedUrl == null) return;
        ensureWebView();
        directMode = direct;
        Uri u = Uri.parse(embedUrl);
        directHost = u.getHost() != null ? u.getHost() : "";
        if (direct) {
            web.loadUrl(embedUrl);
        } else {
            web.loadDataWithBaseURL(BASE_URL, wrapperHtml(embedUrl), "text/html", "utf-8", null);
        }
    }

    /** Stop everything the page is doing (video, audio, timers) but keep the WebView for reuse. */
    public void unload() {
        if (web == null) return;
        try {
            web.stopLoading();
            web.loadUrl("about:blank");
            web.onPause();
        } catch (Throwable ignored) {}
    }

    /** Undo the WebView.onPause() done by unload(); call before load() when coming back. */
    public void prepareResume() {
        if (web != null) {
            try { web.onResume(); } catch (Throwable ignored) {}
        }
    }

    public boolean isFullscreen() { return customView != null; }

    public void exitFullscreen() { hideCustomView(); }

    /** Evaluate a tiny script in the top document (used to ask the wrapper to reload). */
    public void reload() {
        if (web != null) {
            try { web.reload(); } catch (Throwable ignored) {}
        }
    }

    /**
     * Fully tear the player down. Idempotent. After this returns the WebView is destroyed, so
     * no provider script, video or audio can keep running.
     */
    public void release() {
        if (released) return;
        released = true;
        main.removeCallbacksAndMessages(null);
        hideCustomViewQuiet();
        WebView w = web;
        web = null;
        if (w == null) return;
        try {
            w.stopLoading();
            w.setWebChromeClient(null);
            w.setWebViewClient(new WebViewClient());
            w.removeJavascriptInterface("FHPlayer");
            w.loadUrl("about:blank");
            w.onPause();
        } catch (Throwable ignored) {}
        try { container.removeView(w); } catch (Throwable ignored) {}
        try { w.removeAllViews(); } catch (Throwable ignored) {}
        try { w.destroy(); } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------------ WebView setup

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void ensureWebView() {
        if (web != null) return;
        WebView w = new WebView(activity);
        w.setBackgroundColor(Color.BLACK);
        w.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(true);      // so onCreateWindow can refuse pop-ups
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        // Look like a normal mobile Chrome: drop the "; wv" / "Version/x.y" WebView markers.
        String ua = s.getUserAgentString();
        if (ua != null) s.setUserAgentString(ua.replace("; wv", "").replace("Version/4.0 ", ""));

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(w, true);

        w.addJavascriptInterface(new Bridge(), "FHPlayer");
        w.setWebViewClient(new Client());
        w.setWebChromeClient(new Chrome());

        container.addView(w);
        web = w;
    }

    /** Un-sandboxed wrapper page: one full-bleed iframe + a postMessage forwarder. */
    private String wrapperHtml(String embedUrl) {
        String src = TextUtils.htmlEncode(embedUrl);
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
            + "<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}"
            + "iframe{position:fixed;top:0;left:0;width:100%;height:100%;border:0;background:#000}</style>"
            + "</head><body>"
            + "<iframe id=\"p\" src=\"" + src + "\" allowfullscreen webkitallowfullscreen mozallowfullscreen"
            + " allow=\"autoplay; fullscreen; encrypted-media; picture-in-picture; accelerometer; gyroscope\"></iframe>"
            + "<script>(function(){var f=document.getElementById('p');"
            + "f.addEventListener('load',function(){try{FHPlayer.frameLoaded();}catch(e){}});"
            + "window.addEventListener('message',function(e){try{"
            + "if(e.source!==f.contentWindow)return;"
            + "var d=e.data;if(typeof d!=='string')d=JSON.stringify(d);"
            + "FHPlayer.message(d);}catch(x){}});})();</script>"
            + "</body></html>";
    }

    // ------------------------------------------------------------------ fullscreen

    private void showCustomView(View view, WebChromeClient.CustomViewCallback cb) {
        if (customView != null) {
            try { cb.onCustomViewHidden(); } catch (Throwable ignored) {}
            return;
        }
        customView = view;
        customViewCallback = cb;
        fullscreenHost.addView(view, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fullscreenHost.setVisibility(View.VISIBLE);
        listener.onFullscreenChanged(true);
    }

    private void hideCustomView() {
        if (customView == null) return;
        hideCustomViewQuiet();
        listener.onFullscreenChanged(false);
    }

    private void hideCustomViewQuiet() {
        if (customView == null) return;
        try { fullscreenHost.removeView(customView); } catch (Throwable ignored) {}
        fullscreenHost.setVisibility(View.GONE);
        customView = null;
        WebChromeClient.CustomViewCallback cb = customViewCallback;
        customViewCallback = null;
        if (cb != null) {
            try { cb.onCustomViewHidden(); } catch (Throwable ignored) {}
        }
    }

    // ------------------------------------------------------------------ clients

    private final class Client extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            // The wrapper page never legitimately navigates, so ANY top-frame navigation
            // (ad redirects, top.location = ..., market:// intents) is refused.
            // In direct mode only same-site navigation is allowed.
            Uri u = r.getUrl();
            if (AdBlocker.isBlocked(u)) return true;
            if (directMode && r.isForMainFrame()) {
                if (r.isRedirect()) return false;   // server-side 30x of the provider itself
                String scheme = u.getScheme();
                boolean http = "http".equals(scheme) || "https".equals(scheme);
                return !(http && sameSite(u.getHost(), directHost));
            }
            return r.isForMainFrame();
        }

        @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
            if (AdBlocker.isBlocked(r.getUrl())) {
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
            }
            return null;
        }

        @Override public void onPageFinished(WebView v, String url) {
            if (directMode && url != null && !"about:blank".equals(url)) main.post(listener::onFrameLoaded);
        }

        @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
            // Sub-resource errors are normal for ad-heavy pages; nothing to do.
        }

        @Override public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
            // Without this override a WebView renderer crash takes the whole app down.
            main.post(() -> {
                if (released) return;
                WebView dead = web;
                web = null;
                if (dead != null) {
                    try { container.removeView(dead); } catch (Throwable ignored) {}
                    try { dead.destroy(); } catch (Throwable ignored) {}
                }
                listener.onRenderGone();
            });
            return true;
        }
    }

    private final class Chrome extends WebChromeClient {
        @Override public void onShowCustomView(View view, CustomViewCallback callback) { showCustomView(view, callback); }
        @Override public void onHideCustomView() { hideCustomView(); }

        // Refuse every pop-up window (returning false = no new window is created).
        @Override public boolean onCreateWindow(WebView v, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) { return false; }

        // Ad scripts love alert()/confirm()/prompt() spam.
        @Override public boolean onJsAlert(WebView v, String url, String message, JsResult result) { result.cancel(); return true; }
        @Override public boolean onJsConfirm(WebView v, String url, String message, JsResult result) { result.cancel(); return true; }
        @Override public boolean onJsPrompt(WebView v, String url, String message, String defaultValue, JsPromptResult result) { result.cancel(); return true; }
        @Override public boolean onJsBeforeUnload(WebView v, String url, String message, JsResult result) { result.cancel(); return true; }

        @Override public void onPermissionRequest(PermissionRequest request) {
            // Only DRM (protected media) is useful to a video player; deny camera/mic/etc.
            boolean wantsDrm = false;
            for (String res : request.getResources()) {
                if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(res)) wantsDrm = true;
            }
            if (wantsDrm) request.grant(new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});
            else request.deny();
        }

        /** Some devices crash when the default poster is null. */
        @Override public Bitmap getDefaultVideoPoster() {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }

    /** Called from the WebView's JS thread; everything is bounced to the main thread. */
    private final class Bridge {
        @JavascriptInterface public void frameLoaded() {
            main.post(() -> { if (!released) listener.onFrameLoaded(); });
        }

        @JavascriptInterface public void message(String json) {
            if (json == null || json.length() > 20000) return;
            try {
                final long[] out = parseProgress(json);
                final boolean ended = out[2] == 1;
                main.post(() -> {
                    if (released) return;
                    if (out[0] >= 0 && out[1] > 0) listener.onProgress(out[0], out[1]);
                    if (ended) listener.onEnded();
                });
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Understands the postMessage shapes used by the providers (all best effort):
     *   {"type":"PLAYER_EVENT","data":{"event":"timeupdate","currentTime":12.3,"duration":5400}}
     *   {"type":"MEDIA_DATA","data":{"<id>":{"progress":{"watched":12.3,"duration":5400}}}}
     * Returns {positionMs or -1, durationMs or -1, ended(1/0)}.
     */
    static long[] parseProgress(String json) throws Exception {
        long pos = -1, dur = -1;
        boolean ended = false;
        JSONObject o = new JSONObject(json);
        JSONObject d = o.optJSONObject("data");
        JSONObject src = d != null ? d : o;
        String ev = src.optString("event", "");
        if ("ended".equals(ev)) ended = true;
        if (src.has("currentTime") && src.has("duration")) {
            pos = (long) (src.optDouble("currentTime", -1) * 1000);
            dur = (long) (src.optDouble("duration", -1) * 1000);
        } else if ("MEDIA_DATA".equals(o.optString("type")) && d != null) {
            Iterator<String> keys = d.keys();
            while (keys.hasNext()) {
                JSONObject m = d.optJSONObject(keys.next());
                JSONObject p = m != null ? m.optJSONObject("progress") : null;
                if (p != null && p.has("watched") && p.has("duration")) {
                    pos = (long) (p.optDouble("watched", -1) * 1000);
                    dur = (long) (p.optDouble("duration", -1) * 1000);
                }
            }
        }
        return new long[]{pos, dur, ended ? 1 : 0};
    }

    public static boolean sameSite(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return false;
        a = a.toLowerCase();
        b = b.toLowerCase();
        return a.equals(b) || a.endsWith("." + b) || b.endsWith("." + a)
            || registrable(a).equals(registrable(b));
    }

    private static String registrable(String host) {
        String[] p = host.split("\\.");
        if (p.length <= 2) return host;
        return p[p.length - 2] + "." + p[p.length - 1];
    }
}
