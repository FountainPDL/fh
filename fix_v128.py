import os

def w(path, text):
    d = os.path.dirname(path)
    if d: os.makedirs(d, exist_ok=True)
    open(path, 'w').write(text)

S = "app/src/main/java/com/fountainhome/streaming"
print("=== v1.28: restore playback, sync source lists, fix Continue Watching posters ===")

# ── 1. StreamExtractor — remove the shouldOverrideUrlLoading scheme block.
#    This hidden resolver WebView is never shown or touched by the user, so it can't
#    trigger a real intent-hijack popup; blocking non-http(s) navigation was instead
#    breaking legitimate about:blank / redirect steps that some embed pages use
#    before they ever request the real video — which is why every source was
#    failing. Ad-domain resource blocking and popup-window blocking stay, since
#    those don't touch normal same-page navigation at all. ──
w(f"{S}/service/StreamExtractor.java", r'''package com.fountainhome.streaming.service;
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
                // No shouldOverrideUrlLoading override here on purpose — this WebView is
                // never shown or touched by the user, so it can't be hijacked the way a
                // visible one could. Letting it navigate freely is what lets embed pages
                // complete their normal redirect chain down to the real video request.
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
''')
print("[OK] StreamExtractor.java — removed the overly aggressive navigation block")

# ── 2. SettingsFragment — full source list, matching what SourceGenerator actually tries ──
w(f"{S}/ui/fragment/SettingsFragment.java", r'''package com.fountainhome.streaming.ui.fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.databinding.FragmentSettingsBinding;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.MainActivity;
import java.io.File;
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentSettingsBinding.inflate(i, c, false); return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        b.backBtn.setOnClickListener(x -> requireActivity().onBackPressed());
        setupAccent(); setupSource(); setupPlayer(); setupGestures(); setupSubtitles();
        setupAnime(); setupDownloads(); setupUi(); setupStorage();
    }
    private void setupAccent() {
        String[][] cols = {{AppPreferences.COLOR_PURPLE},{AppPreferences.COLOR_BLUE},{AppPreferences.COLOR_RED},{AppPreferences.COLOR_GREEN},{AppPreferences.COLOR_ORANGE},{AppPreferences.COLOR_PINK},{AppPreferences.COLOR_TEAL}};
        View[] sw = {b.colorPurple, b.colorBlue, b.colorRed, b.colorGreen, b.colorOrange, b.colorPink, b.colorTeal};
        String cur = AppPreferences.getAccent(requireContext());
        for (int i = 0; i < sw.length; i++) {
            final String hex = cols[i][0];
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(hex));
            sw[i].setBackground(gd);
            boolean sel = hex.equalsIgnoreCase(cur);
            sw[i].setAlpha(sel ? 1f : 0.4f); sw[i].setScaleX(sel ? 1.25f : 1f); sw[i].setScaleY(sel ? 1.25f : 1f);
            sw[i].setOnClickListener(vv -> {
                AppPreferences.setAccent(requireContext(), hex);
                for (View s2 : sw) { s2.setAlpha(0.4f); s2.setScaleX(1f); s2.setScaleY(1f); }
                vv.setAlpha(1f); vv.setScaleX(1.25f); vv.setScaleY(1.25f);
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).applyAccent();
                Toast.makeText(getContext(), "Accent updated", Toast.LENGTH_SHORT).show();
            });
        }
    }
    private void setupSource() {
        String[] srcs = {"VidSrc","VidLink","VidRock","VIDEASY","VidFast","VidKing","111Movies","Peachify","SuperFlix","VidNest","2Embed","AutoEmbed","SuperEmbed VIP","SuperEmbed"};
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, srcs);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.sourceSpinner.setAdapter(a);
        String cur = AppPreferences.getSource(requireContext());
        for (int i = 0; i < srcs.length; i++) if (srcs[i].equals(cur)) { b.sourceSpinner.setSelection(i); break; }
        b.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setSource(requireContext(), srcs[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupPlayer() {
        b.autoplaySwitch.setChecked(AppPreferences.getAutoplay(requireContext()));
        b.autoplaySwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setAutoplay(requireContext(), c));
        b.pipSwitch.setChecked(AppPreferences.getPiP(requireContext()));
        b.pipSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setPiP(requireContext(), c));
        b.hwAccelSwitch.setChecked(AppPreferences.getHwAccel(requireContext()));
        b.hwAccelSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setHwAccel(requireContext(), c));
        b.keepScreenSwitch.setChecked(AppPreferences.getKeepScreenOn(requireContext()));
        b.keepScreenSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setKeepScreenOn(requireContext(), c));
        String[] speeds = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"};
        float[] speedVals = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        ArrayAdapter<String> sp = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, speeds);
        sp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.speedSpinner.setAdapter(sp);
        float cur = AppPreferences.getPlaybackSpeed(requireContext());
        for (int i = 0; i < speedVals.length; i++) if (speedVals[i] == cur) { b.speedSpinner.setSelection(i); break; }
        b.speedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setPlaybackSpeed(requireContext(), speedVals[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupGestures() {
        b.gestureSwitch.setChecked(AppPreferences.getGestureControls(requireContext()));
        b.gestureSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setGestureControls(requireContext(), c));
        String[] dt = {"5s", "10s", "15s", "30s"};
        int[] dtVals = {5, 10, 15, 30};
        ArrayAdapter<String> dta = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, dt);
        dta.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.dtSeekSpinner.setAdapter(dta);
        int curDt = AppPreferences.getDoubleTapSeek(requireContext());
        for (int i = 0; i < dtVals.length; i++) if (dtVals[i] == curDt) { b.dtSeekSpinner.setSelection(i); break; }
        b.dtSeekSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setDoubleTapSeek(requireContext(), dtVals[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        b.showSkipIntroSwitch.setChecked(AppPreferences.getShowSkipIntro(requireContext()));
        b.showSkipIntroSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setShowSkipIntro(requireContext(), c));
        b.autoSkipIntroSwitch.setChecked(AppPreferences.getAutoSkipIntro(requireContext()));
        b.autoSkipIntroSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setAutoSkipIntro(requireContext(), c));
        b.bgPlaybackSwitch.setChecked(AppPreferences.getBackgroundPlayback(requireContext()));
        b.bgPlaybackSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setBackgroundPlayback(requireContext(), c));
    }
    private void setupSubtitles() {
        b.subEnabledSwitch.setChecked(AppPreferences.getSubEnabled(requireContext()));
        b.subEnabledSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setSubEnabled(requireContext(), c));
        String[] langs = {"English", "Spanish", "French", "German", "Japanese", "Korean", "Arabic"};
        String[] codes = {"en", "es", "fr", "de", "ja", "ko", "ar"};
        ArrayAdapter<String> la = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, langs);
        la.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.subLangSpinner.setAdapter(la);
        String cl = AppPreferences.getSubLang(requireContext());
        for (int i = 0; i < codes.length; i++) if (codes[i].equals(cl)) { b.subLangSpinner.setSelection(i); break; }
        b.subLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setSubLang(requireContext(), codes[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupAnime() {
        String[] ds = {"Sub (Subbed)", "Dub (Dubbed)"};
        ArrayAdapter<String> da = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, ds);
        da.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.dubSubSpinner.setAdapter(da);
        b.dubSubSpinner.setSelection("dub".equals(AppPreferences.getAnimeDubSub(requireContext())) ? 1 : 0);
        b.dubSubSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setAnimeDubSub(requireContext(), pos == 1 ? "dub" : "sub"); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupDownloads() {
        b.wifiOnlySwitch.setChecked(AppPreferences.getWifiOnly(requireContext()));
        b.wifiOnlySwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setWifiOnly(requireContext(), c));
        b.dlSubSwitch.setChecked(AppPreferences.getSubWithDownload(requireContext()));
        b.dlSubSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setSubWithDownload(requireContext(), c));
        String[] qs = {"360p", "480p", "720p", "1080p"};
        ArrayAdapter<String> qa = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, qs);
        qa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.qualitySpinner.setAdapter(qa);
        String cq = AppPreferences.getDlQuality(requireContext());
        for (int i = 0; i < qs.length; i++) if (qs[i].equals(cq)) { b.qualitySpinner.setSelection(i); break; }
        b.qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setDlQuality(requireContext(), qs[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupUi() {
        b.showRatingSwitch.setChecked(AppPreferences.getShowRating(requireContext()));
        b.showRatingSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setShowRating(requireContext(), c));
        b.showContinueSwitch.setChecked(AppPreferences.getShowContinue(requireContext()));
        b.showContinueSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setShowContinue(requireContext(), c));
    }
    private void setupStorage() {
        long sz = ds(requireContext().getCacheDir());
        b.storageText.setText("Cache: " + fmt(sz));
        b.clearCacheBtn.setOnClickListener(v -> { cd(requireContext().getCacheDir()); b.storageText.setText("Cache: 0 B"); Toast.makeText(getContext(), "Cache cleared", Toast.LENGTH_SHORT).show(); });
        b.clearHistoryBtn.setOnClickListener(v -> { LibraryManager.clearList(requireContext(), LibraryManager.CONTINUE); Toast.makeText(getContext(), "History cleared", Toast.LENGTH_SHORT).show(); });
        b.clearAllBtn.setOnClickListener(v -> { LibraryManager.clearList(requireContext(), LibraryManager.FAVORITES); LibraryManager.clearList(requireContext(), LibraryManager.WATCHLIST); LibraryManager.clearList(requireContext(), LibraryManager.CONTINUE); AppPreferences.clearAll(requireContext()); Toast.makeText(getContext(), "All cleared", Toast.LENGTH_SHORT).show(); });
    }
    private void cd(File d) { if (d!=null&&d.isDirectory()){File[]fs=d.listFiles();if(fs!=null)for(File f:fs)cd(f);}if(d!=null)d.delete(); }
    private long ds(File d) { long s=0;if(d!=null&&d.isDirectory()){File[]fs=d.listFiles();if(fs!=null)for(File f:fs)s+=f.length();}return s; }
    private String fmt(long bv) { if(bv<1024)return bv+" B";if(bv<1048576)return(bv/1024)+" KB";return(bv/1048576)+" MB"; }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
''')
print("[OK] SettingsFragment.java — full 14-source list")

# ── 3. PlayerActivity — accept posterPath/rating extras so Continue Watching
#    entries keep their real poster instead of getting overwritten with a blank one. ──
player_path = f"{S}/ui/player/PlayerActivity.java"
if os.path.exists(player_path):
    content = open(player_path, encoding='utf-8').read()
    anchor = 'cur.id = tmdbId; cur.mediaType = type; cur.title = titleStr; cur.imdbId = imdbId;'
    replacement = (
        'cur.id = tmdbId; cur.mediaType = type; cur.title = titleStr; cur.imdbId = imdbId;\n'
        '        cur.posterPath = getIntent().getStringExtra("posterPath");\n'
        '        cur.rating = getIntent().getDoubleExtra("rating", 0);'
    )
    if anchor in content and 'cur.posterPath' not in content:
        content = content.replace(anchor, replacement, 1)
        open(player_path, 'w', encoding='utf-8').write(content)
        print("[OK] PlayerActivity.java — now carries posterPath/rating through to Continue Watching")
    elif 'cur.posterPath' in content:
        print("[SKIP] PlayerActivity.java already patched")
    else:
        print("[WARN] Could not find the expected cur=new ContentItem() block in PlayerActivity.java.")
        print("       Add these two lines manually right after 'cur.imdbId = imdbId;' in onCreate():")
        print('         cur.posterPath = getIntent().getStringExtra("posterPath");')
        print('         cur.rating = getIntent().getDoubleExtra("rating", 0);')
else:
    print("[WARN] PlayerActivity.java not found — skipping")

# ── 4. WatchActivity — expand its per-title source spinner + pass posterPath/rating
#    to PlayerActivity when opening it. ──
watch_path = f"{S}/ui/WatchActivity.java"
if os.path.exists(watch_path):
    content = open(watch_path, encoding='utf-8').read()
    changed = False

    old_srcs = '{"VidSrc","2Embed","AutoEmbed","SuperEmbed VIP"}'
    new_srcs = '{"VidSrc","VidLink","VidRock","VIDEASY","VidFast","VidKing","111Movies","Peachify","SuperFlix","VidNest","2Embed","AutoEmbed","SuperEmbed VIP","SuperEmbed"}'
    if old_srcs in content:
        content = content.replace(old_srcs, new_srcs, 1)
        changed = True
        print("[OK] WatchActivity.java — source spinner expanded to match the player")
    elif '"VidNest"' in content:
        print("[SKIP] WatchActivity.java source spinner already expanded")
    else:
        print("[WARN] Could not find the expected source array in WatchActivity.java.")
        print("       Replace it manually with:", new_srcs)

    anchor_extra = 'i.putExtra("imdbId",ci.imdbId!=null?ci.imdbId:"");'
    addition_extra = 'i.putExtra("posterPath",ci.posterPath!=null?ci.posterPath:"");i.putExtra("rating",ci.rating);'
    if anchor_extra in content and 'putExtra("posterPath"' not in content:
        content = content.replace(anchor_extra, anchor_extra + addition_extra, 1)
        changed = True
        print("[OK] WatchActivity.java — openPlayer now passes posterPath/rating")
    elif 'putExtra("posterPath"' in content:
        print("[SKIP] WatchActivity.java openPlayer already patched")
    else:
        print("[WARN] Could not find the expected imdbId extra in WatchActivity.java's openPlayer.")
        print("       Add this manually right after the imdbId putExtra line:")
        print("        ", addition_extra)

    if changed:
        open(watch_path, 'w', encoding='utf-8').write(content)
else:
    print("[WARN] WatchActivity.java not found — skipping")

# ── 5. AnimeDetailActivity — pass posterPath/rating to PlayerActivity too ──
anime_path = f"{S}/ui/AnimeDetailActivity.java"
if os.path.exists(anime_path):
    content = open(anime_path, encoding='utf-8').read()
    anchor = 'i.putExtra("title", title); i.putExtra("imdbId", "");'
    addition = '\n        i.putExtra("posterPath", cover); i.putExtra("rating", getIntent().getDoubleExtra("rating", 0));'
    if anchor in content and 'putExtra("posterPath"' not in content:
        content = content.replace(anchor, anchor + addition, 1)
        open(anime_path, 'w', encoding='utf-8').write(content)
        print("[OK] AnimeDetailActivity.java — openPlayer now passes posterPath/rating")
    elif 'putExtra("posterPath"' in content:
        print("[SKIP] AnimeDetailActivity.java already patched")
    else:
        print("[WARN] Could not find the expected title/imdbId extras in AnimeDetailActivity.java's openPlayer.")
        print("       Add this manually right after those two putExtra calls:")
        print("        ", addition.strip())
else:
    print("[WARN] AnimeDetailActivity.java not found — skipping")

print()
print("=== v1.28 complete ===")
