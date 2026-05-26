package com.fountainhome.streaming.service;
import java.util.ArrayList;
import java.util.List;
public class SourceGenerator {
    public static class Source{public final String url,label;public Source(String u,String l){url=u;label=l;}}
    public static List<Source> getMovieSources(String imdbId,int tmdbId){
        List<Source>l=new ArrayList<>();boolean has=imdbId!=null&&!imdbId.isEmpty();
        l.add(new Source("https://vsembed.ru/embed/movie?tmdb="+tmdbId,"VidSrc"));
        if(has){l.add(new Source("https://www.2embed.online/2embed.php?id="+imdbId,"2Embed"));l.add(new Source("https://autoembed.cc/movie/imdb/"+imdbId,"AutoEmbed"));l.add(new Source("https://multiembed.mov/directstream.php?video_id="+imdbId,"SuperEmbed VIP"));l.add(new Source("https://multiembed.mov/?video_id="+imdbId,"SuperEmbed"));}
        l.add(new Source("https://www.2embed.online/2embed.php?id="+tmdbId,"2Embed (T)"));l.add(new Source("https://autoembed.cc/movie/tmdb/"+tmdbId,"AutoEmbed (T)"));l.add(new Source("https://multiembed.mov/directstream.php?video_id="+tmdbId+"&tmdb=1","SuperEmbed VIP (T)"));l.add(new Source("https://multiembed.mov/?video_id="+tmdbId+"&tmdb=1","SuperEmbed (T)"));
        return l;
    }
    public static List<Source> getTVSources(String imdbId,int tmdbId,int s,int e){
        List<Source>l=new ArrayList<>();boolean has=imdbId!=null&&!imdbId.isEmpty();
        l.add(new Source("https://vsembed.ru/embed/tv?tmdb="+tmdbId+"&season="+s+"&episode="+e,"VidSrc"));
        if(has){l.add(new Source("https://www.2embed.online/tv-2embed.php?id="+imdbId+"&season="+s+"&episode="+e,"2Embed"));l.add(new Source("https://autoembed.cc/tv/imdb/"+imdbId+"/"+s+"/"+e,"AutoEmbed"));l.add(new Source("https://multiembed.mov/directstream.php?video_id="+imdbId+"&s="+s+"&e="+e,"SuperEmbed VIP"));l.add(new Source("https://multiembed.mov/?video_id="+imdbId+"&s="+s+"&e="+e,"SuperEmbed"));}
        l.add(new Source("https://www.2embed.online/tv-2embed.php?id="+tmdbId+"&season="+s+"&episode="+e,"2Embed (T)"));l.add(new Source("https://autoembed.cc/tv/tmdb/"+tmdbId+"/"+s+"/"+e,"AutoEmbed (T)"));l.add(new Source("https://multiembed.mov/directstream.php?video_id="+tmdbId+"&tmdb=1&s="+s+"&e="+e,"SuperEmbed VIP (T)"));l.add(new Source("https://multiembed.mov/?video_id="+tmdbId+"&tmdb=1&s="+s+"&e="+e,"SuperEmbed (T)"));
        return l;
    }
    public static String imageUrl(String path,String size){if(path==null||path.isEmpty())return "";return "https://image.tmdb.org/t/p/"+size+path;}
}
