package com.fountainhome.streaming.anime;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class AniListClient {
    private static final String URL="https://graphql.anilist.co";
    private static final OkHttpClient client=new OkHttpClient();
    private static final ExecutorService exec=Executors.newCachedThreadPool();
    private static final Handler mh=new Handler(Looper.getMainLooper());
    public interface Callback<T>{void onSuccess(T r);void onError(String e);}
    public static class AnimeItem{public int id,episodes,seasonYear;public String titleEnglish,titleRomaji,coverImage,bannerImage,description,status,season,format;public double averageScore;public List<String>genres=new ArrayList<>();public String displayTitle(){return titleEnglish!=null&&!titleEnglish.isEmpty()?titleEnglish:(titleRomaji!=null?titleRomaji:"");}}
    private static String q(String gql,String vars)throws IOException{String body="{\"query\":\""+gql.replace("\"","\\\"").replace("\n"," ")+"\",\"variables\":"+vars+"}";Request req=new Request.Builder().url(URL).addHeader("Content-Type","application/json").addHeader("Accept","application/json").post(RequestBody.create(body,MediaType.parse("application/json"))).build();try(Response res=client.newCall(req).execute()){if(res.body()!=null)return res.body().string();}return "{}";}
    private static List<AnimeItem>parse(JsonArray arr){List<AnimeItem>items=new ArrayList<>();for(JsonElement el:arr){try{JsonObject o=el.getAsJsonObject();AnimeItem a=new AnimeItem();a.id=gi(o,"id",0);JsonObject t=o.has("title")&&!o.get("title").isJsonNull()?o.getAsJsonObject("title"):null;if(t!=null){a.titleEnglish=gs(t,"english");a.titleRomaji=gs(t,"romaji");}JsonObject ci=o.has("coverImage")&&!o.get("coverImage").isJsonNull()?o.getAsJsonObject("coverImage"):null;if(ci!=null)a.coverImage=gs(ci,"large");a.bannerImage=gs(o,"bannerImage");a.description=gs(o,"description");a.episodes=gi(o,"episodes",0);double sc=gd(o,"averageScore");a.averageScore=sc>0?sc/10.0:0;a.status=gs(o,"status");a.season=gs(o,"season");a.seasonYear=gi(o,"seasonYear",0);a.format=gs(o,"format");if(o.has("genres")&&o.get("genres").isJsonArray())for(JsonElement g:o.getAsJsonArray("genres"))a.genres.add(g.getAsString());items.add(a);}catch(Exception ignored){}}return items;}
    private static String gs(JsonObject o,String k){return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():"";}
    private static int gi(JsonObject o,String k,int d){try{return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsInt():d;}catch(Exception e){return d;}}
    private static double gd(JsonObject o,String k){try{return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsDouble():0;}catch(Exception e){return 0;}}
    private static void run(String gql,String vars,Callback<List<AnimeItem>>cb){exec.execute(()->{try{String res=q(gql,vars);JsonArray arr=JsonParser.parseString(res).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("Page").getAsJsonArray("media");List<AnimeItem>items=parse(arr);mh.post(()->cb.onSuccess(items));}catch(Exception e){mh.post(()->cb.onError(e.getMessage()));}});}
    public static void getTrending(Callback<List<AnimeItem>>cb){run("query{Page(page:1,perPage:20){media(sort:TRENDING_DESC,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}bannerImage description episodes averageScore status season seasonYear format genres}}}","{}",cb);}
    public static void getPopular(Callback<List<AnimeItem>>cb){run("query{Page(page:1,perPage:20){media(sort:POPULARITY_DESC,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format genres}}}","{}",cb);}
    public static void getThisSeason(Callback<List<AnimeItem>>cb){run("query{Page(page:1,perPage:20){media(season:SPRING,seasonYear:2025,type:ANIME,sort:POPULARITY_DESC,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format}}}","{}",cb);}
    public static void search(String kw,Callback<List<AnimeItem>>cb){run("query($s:String){Page(page:1,perPage:20){media(search:$s,type:ANIME,isAdult:false){id title{english romaji}coverImage{large}episodes averageScore status format genres}}}","{\"s\":\""+kw.replace("\"","")+"\"}",cb);}
}
