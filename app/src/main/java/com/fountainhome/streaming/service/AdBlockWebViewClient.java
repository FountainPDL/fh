package com.fountainhome.streaming.service;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

public class AdBlockWebViewClient extends WebViewClient {

    // Blocked ad/tracker domains
    private static final List<String> BLOCK_LIST = Arrays.asList(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google", "ads.yahoo.com", "amazon-adsystem.com",
        "outbrain.com", "taboola.com", "exoclick.com", "juicyads.com",
        "trafficjunky.net", "popads.net", "popcash.net", "hilltopads.net",
        "adsterra.com", "adskeeper.co.uk", "propellerads.com",
        "revcontent.com", "content.ad", "mgid.com", "adsco.re",
        "adnxs.com", "advertising.com", "quantserve.com", "scorecardresearch.com",
        "moatads.com", "yieldmo.com", "media.net", "criteo.com",
        "smartadserver.com", "openx.net", "rubiconproject.com",
        "pubmatic.com", "33across.com", "appnexus.com", "sovrn.com",
        "lijit.com", "sharethrough.com", "bidswitch.net",
        // Popup/redirect domains common on streaming sites
        "go.oclasrv.com", "adskeeper", "adfoc.us", "bc.vc",
        "shorte.st", "ouo.io", "linkvertise.com", "lnkr.js",
        "clickadu.com", "coinzilla.com", "a-ads.com",
        "trafficfactory.biz", "pornhub", "xvideos", "xhamster"
    );

    private static final WebResourceResponse EMPTY =
        new WebResourceResponse("text/plain", "utf-8",
            new ByteArrayInputStream("".getBytes()));

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view,
            WebResourceRequest request) {
        String url = request.getUrl().toString().toLowerCase();

        // Block ad domains
        for (String blocked : BLOCK_LIST) {
            if (url.contains(blocked)) return EMPTY;
        }

        // Block pop-up scripts
        if (url.contains("pop") && (url.contains("ad") || url.contains("click")))
            return EMPTY;

        return null; // allow
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString().toLowerCase();

        // Block redirects to known ad/scam domains
        for (String blocked : BLOCK_LIST) {
            if (url.contains(blocked)) return true; // block navigation
        }

        // Block redirects away from known player domains
        String host = request.getUrl().getHost();
        if (host == null) return false;

        List<String> ALLOWED_HOSTS = Arrays.asList(
            "vsembed.ru", "2embed.online", "2embed.cc",
            "autoembed.cc", "multiembed.mov", "vidsrc.xyz",
            "vidsrc.me", "vidsrc.net", "vidsrc.pro",
            "themoviedb.org", "image.tmdb.org",
            "googleapis.com", "gstatic.com", "google.com",
            "cdnjs.cloudflare.com", "jwplayer.com",
            "plyr.io", "hls.js", "vidcloud.co", "embedsito.com",
            "cdn.", "stream.", "media.", "player."
        );

        for (String allowed : ALLOWED_HOSTS) {
            if (host.contains(allowed)) return false; // allow
        }

        // Allow same-site navigation (subdomains of embed providers)
        if (host.contains("embed") || host.contains("vid") || host.contains("stream"))
            return false;

        // Block everything else (popups, redirects)
        return url.contains("redirect") || url.contains("click") || url.contains("popup");
    }
}
