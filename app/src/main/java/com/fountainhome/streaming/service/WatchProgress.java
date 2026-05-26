package com.fountainhome.streaming.service;
import android.content.Context;
import android.content.SharedPreferences;
public class WatchProgress {
    private static final String PREFS="fh_progress";
    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREFS,0);}
    public static void save(Context c,int id,String t,int s,int e,long ms){p(c).edit().putLong(key(id,t,s,e),ms).apply();}
    public static long get(Context c,int id,String t,int s,int e){return p(c).getLong(key(id,t,s,e),0L);}
    public static void clear(Context c,int id,String t,int s,int e){p(c).edit().remove(key(id,t,s,e)).apply();}
    private static String key(int id,String t,int s,int e){return t+"_"+id+"_s"+s+"_e"+e;}
}
