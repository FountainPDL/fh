package com.fountainhome.streaming.service;
import android.content.Context;
import android.content.SharedPreferences;
import com.fountainhome.streaming.ui.viewmodel.ContentItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
public class LibraryManager {
    public static final String FAVORITES="favorites",WATCHLIST="watchlist",CONTINUE="continue";
    private static final Gson gson=new Gson();
    private static SharedPreferences p(Context c){return c.getSharedPreferences("fh_library",0);}
    public static void add(Context c,ContentItem item,String list){List<ContentItem>items=get(c,list);items.removeIf(i->i.id==item.id&&eq(i.mediaType,item.mediaType));items.add(0,item);if(CONTINUE.equals(list)&&items.size()>100)items=items.subList(0,100);p(c).edit().putString(list,gson.toJson(items)).apply();}
    public static void remove(Context c,int id,String mt,String list){List<ContentItem>items=get(c,list);items.removeIf(i->i.id==id&&eq(i.mediaType,mt));p(c).edit().putString(list,gson.toJson(items)).apply();}
    public static boolean isIn(Context c,int id,String mt,String list){for(ContentItem i:get(c,list))if(i.id==id&&eq(i.mediaType,mt))return true;return false;}
    public static List<ContentItem>get(Context c,String list){String json=p(c).getString(list,"[]");Type type=new TypeToken<List<ContentItem>>(){}.getType();try{List<ContentItem>items=gson.fromJson(json,type);return items!=null?items:new ArrayList<>();}catch(Exception e){return new ArrayList<>();}}
    public static void clearList(Context c,String list){p(c).edit().remove(list).apply();}
    public static void toggleFavorite(Context c,ContentItem item){if(isIn(c,item.id,item.mediaType,FAVORITES))remove(c,item.id,item.mediaType,FAVORITES);else add(c,item,FAVORITES);}
    public static void updateProgress(Context c,ContentItem item,int s,int e,long ms){item.lastSeason=s;item.lastEpisode=e;item.lastPositionMs=ms;add(c,item,CONTINUE);WatchProgress.save(c,item.id,item.mediaType,s,e,ms);}
    private static boolean eq(String a,String b){return a==null?b==null:a.equals(b);}
}
