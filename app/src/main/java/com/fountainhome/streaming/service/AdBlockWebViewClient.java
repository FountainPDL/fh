package com.fountainhome.streaming.service;
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
