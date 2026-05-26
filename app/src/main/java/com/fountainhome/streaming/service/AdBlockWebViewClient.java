package com.fountainhome.streaming.service;
import android.webkit.*;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
public class AdBlockWebViewClient extends WebViewClient {
    private static final List<String> BLOCK=Arrays.asList("doubleclick.net","googlesyndication.com","googleadservices.com","outbrain.com","taboola.com","exoclick.com","juicyads.com","trafficjunky.net","popads.net","popcash.net","adsterra.com","propellerads.com","criteo.com","media.net","pubmatic.com","appnexus.com","clickadu.com","coinzilla.com","adfoc.us","bc.vc","shorte.st","ouo.io","linkvertise.com");
    private static final WebResourceResponse EMPTY=new WebResourceResponse("text/plain","utf-8",new ByteArrayInputStream("".getBytes()));
    @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){String url=r.getUrl().toString().toLowerCase();for(String b:BLOCK)if(url.contains(b))return EMPTY;return null;}
    @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String url=r.getUrl().toString().toLowerCase();for(String b:BLOCK)if(url.contains(b))return true;String host=r.getUrl().getHost();if(host==null)return false;List<String>ALLOWED=Arrays.asList("vsembed.ru","2embed.online","2embed.cc","autoembed.cc","multiembed.mov","vidsrc.xyz","vidsrc.me","themoviedb.org","image.tmdb.org");for(String a:ALLOWED)if(host.contains(a))return false;if(host.contains("embed")||host.contains("vid")||host.contains("stream"))return false;return url.contains("redirect")||url.contains("popup");}
}
