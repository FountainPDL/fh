package com.fountainhome.streaming.player;

import android.net.Uri;

/**
 * Host-suffix based blocklist for ad / popup networks that embed providers load.
 * Only dedicated ad-network domains are listed so that blocking can never break a
 * provider's own player.
 */
public final class AdBlocker {
    private AdBlocker() {}

    private static final String[] HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "outbrain.com", "taboola.com", "exoclick.com", "juicyads.com", "trafficjunky.net",
        "popads.net", "popcash.net", "adsterra.com", "highperformanceformat.com",
        "profitableratecpm.com", "propellerads.com", "propellerclick.com", "criteo.com",
        "media.net", "pubmatic.com", "appnexus.com", "clickadu.com", "coinzilla.com",
        "adfoc.us", "bc.vc", "shorte.st", "ouo.io", "linkvertise.com", "onclickalgo.com",
        "adnium.com", "clickagy.com", "smartadserver.com", "yllix.com", "adcash.com",
        "revcontent.com", "mgid.com", "zedo.com", "adroll.com", "hilltopads.net",
        "adskeeper.com", "monetag.com", "a-ads.com", "realsrv.com", "tsyndicate.com",
        "ad-maven.com", "admaven.com"
    };

    /** True when the host is (a sub-domain of) a known ad network. */
    public static boolean isBlockedHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        for (String b : HOSTS) {
            if (h.equals(b) || h.endsWith("." + b)) return true;
        }
        return false;
    }

    public static boolean isBlocked(Uri uri) {
        return uri != null && isBlockedHost(uri.getHost());
    }
}
