import os

def w(path, text):
    d = os.path.dirname(path)
    if d: os.makedirs(d, exist_ok=True)
    open(path, 'w').write(text)

S = "app/src/main/java/com/fountainhome/streaming"
print("=== v1.31b: attach resolver WebView, Grid Columns UI, home shimmer ===")

# ── StreamExtractor — new ViewGroup-attached overload. The Context-only overload
#    stays as-is for DownloadManager2 (no live view hierarchy available in the
#    background). PlayerActivity switches to the attached version: a real,
#    properly-measured (if invisible) WebView reads much more like a normal
#    browser tab than one that's never added to any window at all, which is
#    likely what was tripping "is this a real page" checks on some sources. ──
w(f"{S}/service/StreamExtractor.java", r'''package com.fountainhome.streaming.service;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
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
    private ViewGroup container;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final AtomicBoolean found = new AtomicBoolean(false);

    // Background-use overload (e.g. DownloadManager2) — no view hierarchy available.
    public void extract(Context ctx, String embedUrl, int timeoutMs, Callback cb) {
        run(ctx.getApplicationContext(), null, embedUrl, timeoutMs, cb);
    }
    // Live-playback overload — attaches the resolver WebView to a real (invisible)
    // container in the current screen so it behaves like an actual browser tab.
    public void extract(ViewGroup attachTo, String embedUrl, int timeoutMs, Callback cb) {
        run(attachTo.getContext(), attachTo, embedUrl, timeoutMs, cb);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void run(Context ctx, ViewGroup attachTo, String embedUrl, int timeoutMs, Callback cb) {
        found.set(false);
        container = attachTo;
        h.post(() -> {
            wv = new WebView(ctx);
            WebSettings ws = wv.getSettings();
            ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");
            if (container != null) {
                wv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                wv.setVisibility(View.INVISIBLE);
                try { container.addView(wv); } catch (Exception ignored) {}
            }
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
            });
            String html = "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;background:#000}"
                + "iframe{width:100vw;height:100vh;border:none}</style></head>"
                + "<body><iframe src='" + embedUrl + "' allowfullscreen allow='autoplay;fullscreen'></iframe></body></html>";
            wv.loadDataWithBaseURL("https://www.google.com", html, "text/html", "utf-8", null);
            h.postDelayed(() -> { if (!found.get()) { found.set(true); cb.onFailed(); destroy(); } }, timeoutMs);
        });
    }
    private void destroy() {
        h.post(() -> {
            if (wv != null) {
                if (container != null) { try { container.removeView(wv); } catch (Exception ignored) {} }
                wv.stopLoading(); wv.destroy(); wv = null;
            }
        });
    }
}
''')
print("[OK] StreamExtractor.java — added a real-view-attached resolver overload")

# ── PlayerActivity — use the attached overload ──
player_path = f"{S}/ui/player/PlayerActivity.java"
if os.path.exists(player_path):
    content = open(player_path, encoding='utf-8').read()
    old = 'new StreamExtractor().extract(this, url, 8000, new StreamExtractor.Callback() {'
    new = 'new StreamExtractor().extract(b.resolverContainer, url, 8000, new StreamExtractor.Callback() {'
    if old in content:
        content = content.replace(old, new, 1)
        open(player_path, 'w', encoding='utf-8').write(content)
        print("[OK] PlayerActivity.java — resolver now attached to a real view instead of headless")
    elif 'b.resolverContainer' in content:
        print("[SKIP] PlayerActivity.java already patched")
    else:
        print("[WARN] Could not find the expected StreamExtractor().extract(this,... call in PlayerActivity.java.")
else:
    print("[WARN] PlayerActivity.java not found — skipping")

# ── SettingsFragment — add Grid Columns control to the DISPLAY section ──
settings_path = f"{S}/ui/fragment/SettingsFragment.java"
if os.path.exists(settings_path):
    content = open(settings_path, encoding='utf-8').read()
    old = "private void setupUi() {"
    new = (
        "private void setupUi() {\n"
        "        String[] cols = {\"2\", \"3\", \"4\"};\n"
        "        ArrayAdapter<String> ca = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, cols);\n"
        "        ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);\n"
        "        b.gridColsSpinner.setAdapter(ca);\n"
        "        int curCols = AppPreferences.getGridColumns(requireContext());\n"
        "        for (int i = 0; i < cols.length; i++) if (Integer.parseInt(cols[i]) == curCols) { b.gridColsSpinner.setSelection(i); break; }\n"
        "        b.gridColsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {\n"
        "            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setGridColumns(requireContext(), Integer.parseInt(cols[pos])); }\n"
        "            public void onNothingSelected(AdapterView<?> p) {}\n"
        "        });\n"
    )
    if old in content and "gridColsSpinner" not in content:
        content = content.replace(old, new, 1)
        open(settings_path, 'w', encoding='utf-8').write(content)
        print("[OK] SettingsFragment.java — Grid Columns control added")
    elif "gridColsSpinner" in content:
        print("[SKIP] SettingsFragment.java already patched")
    else:
        print("[WARN] Could not find setupUi() in SettingsFragment.java.")
else:
    print("[WARN] SettingsFragment.java not found — skipping")

print()
print("=== v1.31b complete ===")
