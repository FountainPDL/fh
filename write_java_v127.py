import os

def w(path, text):
    d = os.path.dirname(path)
    if d: os.makedirs(d, exist_ok=True)
    open(path, 'w').write(text)

S = "app/src/main/java/com/fountainhome/streaming"
print("=== v1.27: no WebView player, fixed controls, new sources, stable signing ===")

# ── AdBlockWebViewClient — BLOCK list exposed for StreamExtractor to reuse ──
w(f"{S}/service/AdBlockWebViewClient.java", r'''package com.fountainhome.streaming.service;
import android.net.Uri;
import android.webkit.*;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
public class AdBlockWebViewClient extends WebViewClient {
    static final List<String> BLOCK = Arrays.asList(
        "doubleclick.net","googlesyndication.com","googleadservices.com","outbrain.com","taboola.com",
        "exoclick.com","juicyads.com","trafficjunky.net","popads.net","popcash.net","adsterra.com",
        "propellerads.com","criteo.com","media.net","pubmatic.com","appnexus.com","clickadu.com",
        "coinzilla.com","adfoc.us","bc.vc","shorte.st","ouo.io","linkvertise.com","onclickalgo.com",
        "adnium.com","propellerclick.com","clickagy.com","smartadserver.com","yllix.com","adcash.com",
        "revcontent.com","mgid.com","zedo.com","adroll.com","hilltopads.net","adskeeper.com"
    );
    private static final List<String> ALLOWED_HOSTS = Arrays.asList(
        "vsembed.ru","2embed.online","vidlink.pro","vidrock.ru","videasy.net","vidfast.pro","vidking.net",
        "111movies.net","peachify.pro","superflixapi.pro","vidnest.fun","autoembed.cc","multiembed.mov",
        "vidsrc.xyz","vidsrc.me","themoviedb.org","image.tmdb.org","2anime.xyz","yugen.to","graphql.anilist.co"
    );
    private static final WebResourceResponse EMPTY =
        new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));

    @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        String url = r.getUrl().toString().toLowerCase();
        for (String b : BLOCK) if (url.contains(b)) return EMPTY;
        return null;
    }
    @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        Uri uri = r.getUrl();
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) return true;
        String url = uri.toString().toLowerCase();
        for (String b : BLOCK) if (url.contains(b)) return true;
        String host = uri.getHost();
        if (host == null) return true;
        for (String ok : ALLOWED_HOSTS) if (host.contains(ok)) return false;
        if (host.contains("embed") || host.contains("vid") || host.contains("stream") || host.contains("play")) return false;
        return true;
    }
}
''')

# ── StreamExtractor — hidden resolver WebView, hardened, never shown to the user ──
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
''')

# ── SourceGenerator — every source from servers.txt, used purely as a URL resolver ──
w(f"{S}/service/SourceGenerator.java", r'''package com.fountainhome.streaming.service;
import java.util.ArrayList;
import java.util.List;
public class SourceGenerator {
    public static class Source {
        public final String url, label;
        public Source(String u, String l) { url = u; label = l; }
    }

    public static List<Source> getMovieSources(String imdbId, int tmdbId) {
        List<Source> l = new ArrayList<>();
        boolean has = imdbId != null && !imdbId.isEmpty();
        l.add(new Source("https://vsembed.ru/embed/movie?tmdb=" + tmdbId, "VidSrc"));
        l.add(new Source("https://vidlink.pro/movie/" + tmdbId, "VidLink"));
        l.add(new Source("https://vidrock.ru/movie/" + tmdbId, "VidRock"));
        l.add(new Source("https://player.videasy.net/movie/" + tmdbId, "VIDEASY"));
        l.add(new Source("https://vidfast.pro/movie/" + tmdbId, "VidFast"));
        l.add(new Source("https://www.vidking.net/embed/movie/" + tmdbId, "VidKing"));
        l.add(new Source("https://111movies.net/movie/" + tmdbId, "111Movies"));
        l.add(new Source("https://peachify.pro/embed/movie/" + tmdbId, "Peachify"));
        l.add(new Source("https://superflixapi.pro/filme/" + tmdbId, "SuperFlix"));
        l.add(new Source("https://vidnest.fun/movie/" + tmdbId, "VidNest"));
        l.add(new Source("https://www.2embed.online/embed/movie/" + tmdbId, "2Embed"));
        l.add(new Source("https://autoembed.cc/movie/tmdb/" + tmdbId, "AutoEmbed"));
        l.add(new Source("https://multiembed.mov/directstream.php?video_id=" + tmdbId + "&tmdb=1", "SuperEmbed VIP"));
        l.add(new Source("https://multiembed.mov/?video_id=" + tmdbId + "&tmdb=1", "SuperEmbed"));
        if (has) {
            l.add(new Source("https://autoembed.cc/movie/imdb/" + imdbId, "AutoEmbed(I)"));
            l.add(new Source("https://multiembed.mov/directstream.php?video_id=" + imdbId, "SuperEmbed VIP(I)"));
        }
        return l;
    }

    public static List<Source> getTVSources(String imdbId, int tmdbId, int s, int e) {
        List<Source> l = new ArrayList<>();
        boolean has = imdbId != null && !imdbId.isEmpty();
        l.add(new Source("https://vsembed.ru/embed/tv?tmdb=" + tmdbId + "&season=" + s + "&episode=" + e, "VidSrc"));
        l.add(new Source("https://vidlink.pro/tv/" + tmdbId + "/" + s + "/" + e, "VidLink"));
        l.add(new Source("https://vidrock.ru/tv/" + tmdbId + "/" + s + "/" + e, "VidRock"));
        l.add(new Source("https://player.videasy.net/tv/" + tmdbId + "/" + s + "/" + e, "VIDEASY"));
        l.add(new Source("https://vidfast.pro/tv/" + tmdbId + "/" + s + "/" + e, "VidFast"));
        l.add(new Source("https://www.vidking.net/embed/tv/" + tmdbId + "/" + s + "/" + e, "VidKing"));
        l.add(new Source("https://111movies.net/tv/" + tmdbId + "/" + s + "/" + e, "111Movies"));
        l.add(new Source("https://peachify.pro/embed/tv/" + tmdbId + "/" + s + "/" + e, "Peachify"));
        l.add(new Source("https://superflixapi.pro/serie/" + tmdbId + "/" + s + "/" + e, "SuperFlix"));
        l.add(new Source("https://vidnest.fun/tv/" + tmdbId + "/" + s + "/" + e, "VidNest"));
        l.add(new Source("https://www.2embed.online/embed/tv/" + tmdbId + "/" + s + "/" + e, "2Embed"));
        l.add(new Source("https://autoembed.cc/tv/tmdb/" + tmdbId + "/" + s + "/" + e, "AutoEmbed"));
        l.add(new Source("https://multiembed.mov/directstream.php?video_id=" + tmdbId + "&tmdb=1&s=" + s + "&e=" + e, "SuperEmbed VIP"));
        l.add(new Source("https://multiembed.mov/?video_id=" + tmdbId + "&tmdb=1&s=" + s + "&e=" + e, "SuperEmbed"));
        if (has) {
            l.add(new Source("https://autoembed.cc/tv/imdb/" + imdbId + "/" + s + "/" + e, "AutoEmbed(I)"));
            l.add(new Source("https://multiembed.mov/directstream.php?video_id=" + imdbId + "&s=" + s + "&e=" + e, "SuperEmbed VIP(I)"));
        }
        return l;
    }

    // Anime uses its own separate source list — never the movie/TV ones.
    // idMal (MyAnimeList ID, resolved on demand via AniList's own idMal field) is required
    // for VidLink, our preferred/default anime source. Pass 0 if it isn't known yet;
    // VidLink is simply skipped for that attempt.
    public static List<Source> getAnimeSources(int anilistId, int idMal, int season, int episode) {
        List<Source> l = new ArrayList<>();
        if (idMal > 0) {
            l.add(new Source("https://vidlink.pro/anime/" + idMal + "/" + episode + "/sub", "VidLink Sub"));
            l.add(new Source("https://vidlink.pro/anime/" + idMal + "/" + episode + "/dub?fallback=true", "VidLink Dub"));
        }
        l.add(new Source("https://player.videasy.net/anime/" + anilistId + "/" + episode, "VIDEASY"));
        l.add(new Source("https://superflixapi.pro/serie/" + anilistId + "/" + season + "/" + episode, "SuperFlix"));
        l.add(new Source("https://autoembed.cc/anime/anilist/" + anilistId + "/" + episode, "AutoEmbed Sub"));
        l.add(new Source("https://autoembed.cc/anime/anilist/" + anilistId + "/" + episode + "?dubbed=1", "AutoEmbed Dub"));
        l.add(new Source("https://2anime.xyz/embed/" + anilistId + "/" + episode, "2Anime"));
        l.add(new Source("https://yugen.to/embed/" + anilistId + "/" + episode, "Yugen"));
        return l;
    }

    public static String imageUrl(String path, String size) {
        if (path == null || path.isEmpty()) return "";
        if (path.startsWith("http")) return path;
        return "https://image.tmdb.org/t/p/" + size + path;
    }
}
''')

# ── AniListClient — same as before, plus getMalId() so VidLink can be used for anime
#    without needing MyAnimeList's own OAuth API at all. ──
w(f"{S}/anime/AniListClient.java", r'''package com.fountainhome.streaming.anime;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class AniListClient {
    private static final String URL = "https://graphql.anilist.co";
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final Handler mh = new Handler(Looper.getMainLooper());
    public interface Callback<T> { void onSuccess(T r); void onError(String e); }
    public interface IntCallback { void onResult(int value); }
    public static class AnimeItem {
        public int id, episodes, seasonYear;
        public String titleEnglish, titleRomaji, coverImage, bannerImage, description, status, season, format;
        public double averageScore;
        public List<String> genres = new ArrayList<>();
        public String displayTitle() {
            return titleEnglish != null && !titleEnglish.isEmpty() ? titleEnglish : (titleRomaji != null ? titleRomaji : "");
        }
    }
    private static String query(String gql, String vars) throws IOException {
        String q = gql.replace("\n", " ");
        String body = "{\"query\":\"" + q + "\",\"variables\":" + vars + "}";
        Request req = new Request.Builder().url(URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(RequestBody.create(body, MediaType.parse("application/json"))).build();
        try (Response res = client.newCall(req).execute()) {
            if (res.body() != null) return res.body().string();
        }
        return "{}";
    }
    private static List<AnimeItem> parse(JsonArray arr) {
        List<AnimeItem> items = new ArrayList<>();
        for (JsonElement el : arr) {
            try {
                JsonObject o = el.getAsJsonObject();
                AnimeItem a = new AnimeItem();
                a.id = gi(o, "id", 0);
                JsonObject t = o.has("title") && !o.get("title").isJsonNull() ? o.getAsJsonObject("title") : null;
                if (t != null) { a.titleEnglish = gs(t, "english"); a.titleRomaji = gs(t, "romaji"); }
                JsonObject ci = o.has("coverImage") && !o.get("coverImage").isJsonNull() ? o.getAsJsonObject("coverImage") : null;
                if (ci != null) a.coverImage = gs(ci, "large");
                a.bannerImage = gs(o, "bannerImage");
                a.description = gs(o, "description");
                a.episodes = gi(o, "episodes", 0);
                double sc = gd(o, "averageScore");
                a.averageScore = sc > 0 ? sc / 10.0 : 0;
                a.status = gs(o, "status");
                a.season = gs(o, "season");
                a.seasonYear = gi(o, "seasonYear", 0);
                a.format = gs(o, "format");
                if (o.has("genres") && o.get("genres").isJsonArray())
                    for (JsonElement g : o.getAsJsonArray("genres")) a.genres.add(g.getAsString());
                items.add(a);
            } catch (Exception ignored) {}
        }
        return items;
    }
    private static String gs(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    private static int gi(JsonObject o, String k, int d) { try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : d; } catch (Exception e) { return d; } }
    private static double gd(JsonObject o, String k) { try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0; } catch (Exception e) { return 0; } }
    private static void run(String gql, String vars, Callback<List<AnimeItem>> cb) {
        exec.execute(() -> {
            try {
                String res = query(gql, vars);
                JsonArray arr = JsonParser.parseString(res).getAsJsonObject()
                    .getAsJsonObject("data").getAsJsonObject("Page").getAsJsonArray("media");
                mh.post(() -> cb.onSuccess(parse(arr)));
            } catch (Exception e) { mh.post(() -> cb.onError(e.getMessage())); }
        });
    }
    public static void getTrending(Callback<List<AnimeItem>> cb) {
        run("query{Page(page:1,perPage:20){media(sort:TRENDING_DESC,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}bannerImage description episodes averageScore status season seasonYear format genres}}}", "{}", cb);
    }
    public static void getPopular(Callback<List<AnimeItem>> cb) {
        run("query{Page(page:1,perPage:20){media(sort:POPULARITY_DESC,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format genres}}}", "{}", cb);
    }
    public static void getThisSeason(Callback<List<AnimeItem>> cb) {
        run("query{Page(page:1,perPage:20){media(season:SPRING,seasonYear:2025,type:ANIME,sort:POPULARITY_DESC,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format}}}", "{}", cb);
    }
    public static void search(String kw, Callback<List<AnimeItem>> cb) {
        String safeKw = kw.replace("\"", "").replace("\\", "");
        run("query($s:String){Page(page:1,perPage:20){media(search:$s,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format genres}}}", "{\"s\":\"" + safeKw + "\"}", cb);
    }
    // Resolves the MyAnimeList ID for a given AniList ID, straight from AniList's own
    // graph (Media.idMal) — no separate MyAnimeList API key or OAuth needed.
    public static void getMalId(int anilistId, IntCallback cb) {
        exec.execute(() -> {
            try {
                String res = query("query($id:Int){Media(id:$id,type:ANIME){idMal}}", "{\"id\":" + anilistId + "}");
                JsonObject media = JsonParser.parseString(res).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("Media");
                int malId = media.has("idMal") && !media.get("idMal").isJsonNull() ? media.get("idMal").getAsInt() : 0;
                mh.post(() -> cb.onResult(malId));
            } catch (Exception e) { mh.post(() -> cb.onResult(0)); }
        });
    }
}
''')

# ── PlayerActivity — WebView-free. Anime build is async: resolve MAL id first
#    (so VidLink can lead), then resolve the rest of the source list. ──
w(f"{S}/ui/player/PlayerActivity.java", r'''package com.fountainhome.streaming.ui.player;
import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.anime.AniListClient;
import com.fountainhome.streaming.databinding.ActivityPlayerBinding;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import java.util.*;

public class PlayerActivity extends AppCompatActivity {
    private ActivityPlayerBinding b;
    private ExoPlayer exo;
    private List<SourceGenerator.Source> sources = new ArrayList<>();
    private int currentSourceIdx = 0;
    private String imdbId = "", type, titleStr;
    private int tmdbId, season = 1, episode = 1;
    private boolean fs = false, ctrlVisible = true, locked = false, seekTracking = false;
    private ContentItem cur;
    private final Handler ph = new Handler(Looper.getMainLooper());
    private Runnable pr, hideCtrl, upNextCountdown;
    private AudioManager audioManager;
    private GestureDetector gestureDetector;
    private float dragStartY, dragStartX, startBrightness, startVolume;
    private boolean isDragging = false, isBrightnessZone = false;
    private boolean introHandledThisEp = false, upNextShown = false, upNextCancelled = false;
    private static final float[] SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};

    private final Runnable hideIndicatorRunnable = () -> { if (b != null) b.bvIndicator.setVisibility(View.GONE); };

    private final Runnable posUpdater = new Runnable() {
        @Override public void run() {
            if (b != null && exo != null) {
                long pos = exo.getCurrentPosition(), dur = exo.getDuration();
                if (dur > 0) {
                    b.seekBar.setMax(1000);
                    if (!seekTracking) b.seekBar.setProgress((int) (pos * 1000 / dur));
                    b.currentTimeText.setText(fmtTime(pos));
                    b.durationText.setText(fmtTime(dur));
                }
                checkSkipIntro(pos);
                checkUpNext(pos, dur);
            }
            ph.postDelayed(this, 500);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (AppPreferences.getKeepScreenOn(this)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        type = getIntent().getStringExtra("type");
        tmdbId = getIntent().getIntExtra("id", 0);
        imdbId = getIntent().getStringExtra("imdbId");
        titleStr = getIntent().getStringExtra("title");
        season = getIntent().getIntExtra("season", 1);
        episode = getIntent().getIntExtra("episode", 1);
        if (imdbId == null) imdbId = "";
        cur = new ContentItem();
        cur.id = tmdbId; cur.mediaType = type; cur.title = titleStr; cur.imdbId = imdbId;
        b.titleText.setText(titleStr != null ? titleStr : "");
        updateLabel();

        b.playerView.setUseController(false);
        try {
            b.seekBar.getProgressDrawable().setTint(AppPreferences.getAccentColor(this));
            b.seekBar.getThumb().setTint(AppPreferences.getAccentColor(this));
        } catch (Exception ignored) {}
        b.speedBtn.setText(fmtSpeed(AppPreferences.getPlaybackSpeed(this)));

        hideCtrl = this::hideControls;
        setupGestures();

        b.backBtn.setOnClickListener(v -> { save(); finish(); });
        b.fullscreenBtn.setOnClickListener(v -> { if (fs) exitFs(); else enterFs(); resetHide(); });
        b.lockBtn.setOnClickListener(v -> setLocked(true));
        b.lockIndicator.setOnClickListener(v -> setLocked(false));
        b.moreBtn.setOnClickListener(v -> showMoreMenu());
        b.speedBtn.setOnClickListener(v -> showSpeedMenu());
        b.playPauseBtn.setOnClickListener(v -> { if (exo == null) return; if (exo.isPlaying()) exo.pause(); else exo.play(); updatePlayPauseIcon(); resetHide(); });
        b.skipBackBtn.setOnClickListener(v -> { if (exo != null) exo.seekTo(Math.max(0, exo.getCurrentPosition() - 10000)); resetHide(); });
        b.skipForwardBtn.setOnClickListener(v -> { if (exo != null) exo.seekTo(exo.getCurrentPosition() + 10000); resetHide(); });
        b.skipIntroPill.setOnClickListener(v -> { if (exo != null) exo.seekTo(90000); b.skipIntroPill.setVisibility(View.GONE); });
        b.retrySourcesBtn.setOnClickListener(v -> { b.noStreamCard.setVisibility(View.GONE); build(); });

        b.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser && exo != null && exo.getDuration() > 0) b.currentTimeText.setText(fmtTime((long) p * exo.getDuration() / 1000));
            }
            public void onStartTrackingTouch(SeekBar sb) { seekTracking = true; ph.removeCallbacks(hideCtrl); }
            public void onStopTrackingTouch(SeekBar sb) {
                if (exo != null && exo.getDuration() > 0) exo.seekTo((long) sb.getProgress() * exo.getDuration() / 1000);
                seekTracking = false; resetHide();
            }
        });

        if ("tv".equals(type) || "anime".equals(type)) {
            b.tvControls.setVisibility(View.VISIBLE);
            b.prevBtn.setOnClickListener(v -> {
                save();
                if (episode > 1) episode--; else if (season > 1) { season--; episode = 1; }
                updateLabel(); resetEpisodeState(); build(); resetHide();
            });
            b.nextBtn.setOnClickListener(v -> { save(); episode++; updateLabel(); resetEpisodeState(); build(); resetHide(); });
        } else b.tvControls.setVisibility(View.GONE);

        build(); startSaving(); enterFs();
        ph.post(posUpdater);
        showControls();
    }

    // ---------- Gestures: tap / double-tap seek / brightness & volume swipe ----------
    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapConfirmed(MotionEvent e) { toggleControls(); return true; }
            @Override public boolean onDoubleTap(MotionEvent e) {
                if (exo == null) return false;
                int seekMs = AppPreferences.getDoubleTapSeek(PlayerActivity.this) * 1000;
                boolean isLeft = e.getX() < b.touchInterceptor.getWidth() / 2f;
                long pos = exo.getCurrentPosition();
                exo.seekTo(isLeft ? Math.max(0, pos - seekMs) : pos + seekMs);
                showSeekFlash(isLeft);
                return true;
            }
        });
        b.touchInterceptor.setOnTouchListener((v, event) -> {
            if (locked) return true;
            gestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: onTouchDown(event); break;
                case MotionEvent.ACTION_MOVE: onTouchMove(event); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: onTouchUp(); break;
            }
            return true;
        });
    }
    private void onTouchDown(MotionEvent e) {
        dragStartY = e.getY(); dragStartX = e.getX(); isDragging = false;
        isBrightnessZone = e.getX() < b.touchInterceptor.getWidth() / 2f;
        float br = getWindow().getAttributes().screenBrightness;
        startBrightness = br < 0 ? 0.5f : br;
        startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }
    private void onTouchMove(MotionEvent e) {
        if (!AppPreferences.getGestureControls(this)) return;
        float dy = dragStartY - e.getY();
        float dx = Math.abs(e.getX() - dragStartX);
        if (Math.abs(dy) < 24 || dx > 120) return;
        isDragging = true;
        float screenH = Math.max(b.touchInterceptor.getHeight(), 1);
        float delta = dy / screenH;
        if (isBrightnessZone) {
            float newB = clamp(startBrightness + delta, 0.02f, 1f);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = newB;
            getWindow().setAttributes(lp);
            showIndicator(true, newB);
        } else {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int newVol = (int) clamp(startVolume + delta * maxVol, 0, maxVol);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
            showIndicator(false, maxVol > 0 ? newVol / (float) maxVol : 0);
        }
    }
    private void onTouchUp() {
        if (isDragging) { ph.removeCallbacks(hideIndicatorRunnable); ph.postDelayed(hideIndicatorRunnable, 600); }
        isDragging = false;
    }
    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private void showIndicator(boolean isBrightness, float level) {
        b.bvIndicator.setVisibility(View.VISIBLE);
        b.bvIcon.setImageResource(isBrightness ? R.drawable.ic_brightness : R.drawable.ic_volume);
        b.bvLevelBar.setProgress((int) (level * 100));
        b.bvText.setText(((int) (level * 100)) + "%");
    }
    private void showSeekFlash(boolean isLeft) {
        TextView flash = isLeft ? b.flashLeft : b.flashRight;
        flash.setText((isLeft ? "-" : "+") + AppPreferences.getDoubleTapSeek(this) + "s");
        flash.setAlpha(1f); flash.setVisibility(View.VISIBLE);
        flash.animate().alpha(0f).setDuration(450).setStartDelay(150)
            .withEndAction(() -> flash.setVisibility(View.GONE)).start();
    }

    // ---------- Lock ----------
    private void setLocked(boolean l) {
        locked = l;
        b.lockIndicator.setVisibility(l ? View.VISIBLE : View.GONE);
        if (l) {
            ph.removeCallbacks(hideCtrl);
            b.topControls.setVisibility(View.GONE);
            b.controlsBar.setVisibility(View.GONE);
            b.centerControls.setVisibility(View.GONE);
            b.skipIntroPill.setVisibility(View.GONE);
            b.upNextCard.setVisibility(View.GONE);
        } else {
            showControls();
        }
    }

    // ---------- Menus ----------
    private void showMoreMenu() {
        PopupMenu menu = new PopupMenu(this, b.moreBtn);
        menu.getMenu().add("Picture-in-Picture");
        menu.getMenu().add("Download");
        menu.getMenu().add("Share");
        menu.setOnMenuItemClickListener(item -> {
            String t = item.getTitle().toString();
            if ("Picture-in-Picture".equals(t)) pip();
            else if ("Download".equals(t)) {
                DownloadManager2.downloadVideo(this, cur, season, episode);
                Toast.makeText(this, "Download started \u2014 check the notification for progress", Toast.LENGTH_LONG).show();
            } else if ("Share".equals(t)) {
                Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, (titleStr != null ? titleStr : "") + " \u2014 Fountain Home");
                startActivity(Intent.createChooser(i, "Share"));
            }
            resetHide();
            return true;
        });
        menu.show();
    }
    private void showSpeedMenu() {
        PopupMenu menu = new PopupMenu(this, b.speedBtn);
        for (float sp : SPEEDS) menu.getMenu().add(fmtSpeed(sp));
        menu.setOnMenuItemClickListener(item -> {
            String label = item.getTitle().toString();
            float speed = Float.parseFloat(label.replace("x", ""));
            AppPreferences.setPlaybackSpeed(this, speed);
            if (exo != null) exo.setPlaybackSpeed(speed);
            b.speedBtn.setText(label);
            resetHide();
            return true;
        });
        menu.show();
    }
    private String fmtSpeed(float sp) {
        String num = (sp == Math.floor(sp)) ? String.valueOf((int) sp) : String.valueOf(sp);
        return num + "x";
    }
    private String fmtTime(long ms) {
        long totalSec = Math.max(0, ms) / 1000;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, sec = totalSec % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, sec) : String.format(Locale.US, "%d:%02d", m, sec);
    }
    private void updatePlayPauseIcon() {
        if (exo == null) return;
        b.playPauseBtn.setImageResource(exo.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    // ---------- Skip intro / Up next ----------
    private void checkSkipIntro(long pos) {
        if (!("tv".equals(type) || "anime".equals(type)) || locked) { b.skipIntroPill.setVisibility(View.GONE); return; }
        boolean show = AppPreferences.getShowSkipIntro(this);
        boolean auto = AppPreferences.getAutoSkipIntro(this);
        if (pos < 3000 || pos > 90000) { b.skipIntroPill.setVisibility(View.GONE); return; }
        if (auto && !introHandledThisEp) {
            introHandledThisEp = true;
            exo.seekTo(90000);
            b.skipIntroPill.setVisibility(View.GONE);
            return;
        }
        if (show) b.skipIntroPill.setVisibility(View.VISIBLE);
    }
    private void checkUpNext(long pos, long dur) {
        if (!("tv".equals(type) || "anime".equals(type)) || dur <= 0 || locked) return;
        if (!AppPreferences.getAutoplay(this)) return;
        if (dur - pos < 15000 && dur - pos > 500 && !upNextShown && !upNextCancelled) {
            upNextShown = true;
            showUpNext();
        }
    }
    private void showUpNext() {
        b.upNextCard.setVisibility(View.VISIBLE);
        b.upNextTitle.setText("Up Next \u2014 Episode " + (episode + 1));
        final int[] secs = {5};
        b.upNextCountdown.setText(secs[0] + "s");
        upNextCountdown = new Runnable() {
            @Override public void run() {
                if (upNextCancelled || b == null) return;
                secs[0]--;
                b.upNextCountdown.setText(Math.max(secs[0], 0) + "s");
                if (secs[0] <= 0) { advanceEpisode(); return; }
                ph.postDelayed(this, 1000);
            }
        };
        ph.postDelayed(upNextCountdown, 1000);
        b.upNextCancel.setOnClickListener(v -> {
            upNextCancelled = true;
            b.upNextCard.setVisibility(View.GONE);
            if (upNextCountdown != null) ph.removeCallbacks(upNextCountdown);
        });
        b.upNextPlayNow.setOnClickListener(v -> {
            if (upNextCountdown != null) ph.removeCallbacks(upNextCountdown);
            advanceEpisode();
        });
    }
    private void advanceEpisode() {
        if (b != null) b.upNextCard.setVisibility(View.GONE);
        save(); episode++; updateLabel(); resetEpisodeState(); build();
    }
    private void resetEpisodeState() {
        upNextShown = false; upNextCancelled = false; introHandledThisEp = false;
        if (upNextCountdown != null) { ph.removeCallbacks(upNextCountdown); upNextCountdown = null; }
    }

    // ---------- Controls visibility (cancel + state-guard so a stale fade-out
    //            callback can never re-hide controls after a later show) ----------
    private void toggleControls() { if (ctrlVisible) hideControls(); else showControls(); }
    private void showControls() {
        ctrlVisible = true;
        b.controlsBar.animate().cancel();
        b.topControls.animate().cancel();
        b.centerControls.animate().cancel();
        b.controlsBar.setVisibility(View.VISIBLE);
        b.topControls.setVisibility(View.VISIBLE);
        b.centerControls.setVisibility(View.VISIBLE);
        b.controlsBar.setAlpha(1f); b.topControls.setAlpha(1f); b.centerControls.setAlpha(1f);
        scheduleHide();
    }
    private void hideControls() {
        if (!ctrlVisible) return;
        ctrlVisible = false;
        b.controlsBar.animate().cancel();
        b.topControls.animate().cancel();
        b.centerControls.animate().cancel();
        b.controlsBar.animate().alpha(0f).setDuration(400)
            .withEndAction(() -> { if (!ctrlVisible) b.controlsBar.setVisibility(View.INVISIBLE); }).start();
        b.topControls.animate().alpha(0f).setDuration(400)
            .withEndAction(() -> { if (!ctrlVisible) b.topControls.setVisibility(View.INVISIBLE); }).start();
        b.centerControls.animate().alpha(0f).setDuration(400)
            .withEndAction(() -> { if (!ctrlVisible) b.centerControls.setVisibility(View.INVISIBLE); }).start();
    }
    private void scheduleHide() { ph.removeCallbacks(hideCtrl); ph.postDelayed(hideCtrl, 3500); }
    private void resetHide() { showControls(); }
    private void startSaving() {
        pr = new Runnable() { @Override public void run() { save(); ph.postDelayed(this, 10000); } };
        ph.postDelayed(pr, 10000);
    }
    private void save() {
        try { long pos = exo != null ? exo.getCurrentPosition() : 0; LibraryManager.updateProgress(this, cur, season, episode, pos); }
        catch (Exception ignored) {}
    }

    // ---------- Source resolution / playback — every source is just a URL resolver ----------
    private void build() {
        currentSourceIdx = 0;
        resetEpisodeState();
        b.noStreamCard.setVisibility(View.GONE);
        if ("anime".equals(type)) {
            showLoad(true);
            AniListClient.getMalId(tmdbId, malId -> {
                sources = SourceGenerator.getAnimeSources(tmdbId, malId, season, episode);
                finishBuild();
            });
        } else {
            sources = "movie".equals(type) ? SourceGenerator.getMovieSources(imdbId, tmdbId) : SourceGenerator.getTVSources(imdbId, tmdbId, season, episode);
            finishBuild();
        }
    }
    private void finishBuild() {
        String pref = AppPreferences.getSource(this);
        List<SourceGenerator.Source> ro = new ArrayList<>();
        for (SourceGenerator.Source src : sources) if (src.label.startsWith(pref)) ro.add(0, src);
        for (SourceGenerator.Source src : sources) if (!src.label.startsWith(pref)) ro.add(src);
        sources = ro;
        List<String> labels = new ArrayList<>();
        for (SourceGenerator.Source src : sources) labels.add(src.label);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.sourceSpinner.setAdapter(a);
        b.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean init = true;
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { if (init) { init = false; return; } currentSourceIdx = pos; play(pos); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        if (!sources.isEmpty()) play(0); else showNoStreamFound();
        long saved = WatchProgress.get(this, tmdbId, type, season, episode);
        if (saved > 5000) ph.postDelayed(() -> { if (exo != null) exo.seekTo(saved); }, 2000);
    }
    private void tryNextSource() {
        currentSourceIdx++;
        if (currentSourceIdx < sources.size()) { b.sourceSpinner.setSelection(currentSourceIdx); play(currentSourceIdx); }
        else showNoStreamFound();
    }
    private void play(int idx) {
        if (idx < 0 || idx >= sources.size()) { showNoStreamFound(); return; }
        String url = sources.get(idx).url;
        showLoad(true); freeExo();
        b.playerView.setVisibility(View.GONE);
        b.noStreamCard.setVisibility(View.GONE);
        // The source is used purely to resolve a playable URL. Whatever it hands back
        // plays only in our own ExoPlayer-based UI below — nothing from the source is
        // ever rendered directly.
        new StreamExtractor().extract(this, url, 8000, new StreamExtractor.Callback() {
            @Override public void onFound(String su, Map<String, String> h) { runOnUiThread(() -> playExo(su, h)); }
            @Override public void onFailed() { runOnUiThread(() -> tryNextSource()); }
        });
    }
    private void playExo(String url, Map<String, String> headers) {
        showLoad(false);
        b.playerView.setVisibility(View.VISIBLE);
        DefaultHttpDataSource.Factory dsf = new DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 Chrome/120.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(headers).setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000).setReadTimeoutMs(8000);
        exo = new ExoPlayer.Builder(this).build();
        b.playerView.setPlayer(exo);
        MediaItem mi = MediaItem.fromUri(url);
        if (url.contains(".m3u8") || url.contains("/hls/")) exo.setMediaSource(new HlsMediaSource.Factory(dsf).createMediaSource(mi));
        else exo.setMediaSource(new ProgressiveMediaSource.Factory(dsf).createMediaSource(mi));
        exo.addListener(new androidx.media3.common.Player.Listener() {
            @Override public void onPlayerError(PlaybackException e) { runOnUiThread(() -> tryNextSource()); }
            @Override public void onIsPlayingChanged(boolean isPlaying) { updatePlayPauseIcon(); }
            @Override public void onPlaybackStateChanged(int st) {
                if (st == androidx.media3.common.Player.STATE_ENDED && ("tv".equals(type) || "anime".equals(type))
                        && AppPreferences.getAutoplay(PlayerActivity.this) && !upNextShown) {
                    advanceEpisode();
                }
            }
        });
        exo.prepare();
        exo.setPlaybackSpeed(AppPreferences.getPlaybackSpeed(this));
        exo.play();
        updatePlayPauseIcon();
    }
    private void showNoStreamFound() {
        showLoad(false);
        b.noStreamCard.setVisibility(View.VISIBLE);
    }
    private void enterFs() {
        fs = true;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    private void exitFs() {
        fs = false;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
    private void pip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppPreferences.getPiP(this)) {
            try { enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16, 9)).build()); }
            catch (Exception ignored) {}
        }
    }
    @Override public void onPictureInPictureModeChanged(boolean inPip) {
        if (b == null) return;
        int vis = inPip ? View.GONE : View.VISIBLE;
        b.controlsBar.setVisibility(vis);
        b.topControls.setVisibility(vis);
        b.centerControls.setVisibility(vis);
        if (inPip && exo != null) exo.play();
    }
    @Override protected void onUserLeaveHint() { super.onUserLeaveHint(); if (AppPreferences.getPiP(this)) pip(); }
    private void showLoad(boolean show) {
        b.loadingBar.setVisibility(show ? View.VISIBLE : View.GONE);
        b.extractingText.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    private void updateLabel() {
        if ("tv".equals(type) || "anime".equals(type)) {
            b.episodeLabel.setVisibility(View.VISIBLE);
            b.episodeLabel.setText("S" + season + "\u00b7E" + episode);
        } else b.episodeLabel.setVisibility(View.GONE);
    }
    private void freeExo() { if (exo != null) { exo.release(); exo = null; } }
    @Override public void onBackPressed() { if (fs) { exitFs(); return; } save(); super.onBackPressed(); }
    @Override protected void onPause() {
        super.onPause(); save();
        if (exo != null && (!AppPreferences.getBackgroundPlayback(this) || isFinishing())) exo.pause();
    }
    @Override protected void onResume() { super.onResume(); if (exo != null) exo.play(); }
    @Override protected void onDestroy() { save(); ph.removeCallbacksAndMessages(null); freeExo(); super.onDestroy(); }
}
''')

# ── SettingsFragment — expand the Default Source list to include the new sources ──
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
        String[] srcs = {"VidSrc", "VidLink", "VidRock", "VIDEASY", "VidFast", "VidKing", "2Embed", "AutoEmbed", "SuperEmbed VIP"};
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

print("Java files written")
