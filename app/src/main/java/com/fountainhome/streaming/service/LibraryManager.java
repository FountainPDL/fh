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

    public static final String FAVORITES  = "favorites";
    public static final String WATCHLIST  = "watchlist";
    public static final String CONTINUE   = "continue";
    public static final String DOWNLOADED = "downloaded";

    private static final Gson gson = new Gson();

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("fh_library", Context.MODE_PRIVATE);
    }

    public static void add(Context ctx, ContentItem item, String list) {
        List<ContentItem> items = get(ctx, list);
        items.removeIf(i -> i.id == item.id && eq(i.mediaType, item.mediaType));
        items.add(0, item);
        if (list.equals(CONTINUE) && items.size() > 50) items = items.subList(0, 50);
        prefs(ctx).edit().putString(list, gson.toJson(items)).apply();
    }

    public static void remove(Context ctx, int id, String mediaType, String list) {
        List<ContentItem> items = get(ctx, list);
        items.removeIf(i -> i.id == id && eq(i.mediaType, mediaType));
        prefs(ctx).edit().putString(list, gson.toJson(items)).apply();
    }

    public static boolean isIn(Context ctx, int id, String mediaType, String list) {
        for (ContentItem i : get(ctx, list))
            if (i.id == id && eq(i.mediaType, mediaType)) return true;
        return false;
    }

    public static List<ContentItem> get(Context ctx, String list) {
        String json = prefs(ctx).getString(list, "[]");
        Type type = new TypeToken<List<ContentItem>>(){}.getType();
        List<ContentItem> items = gson.fromJson(json, type);
        return items != null ? items : new ArrayList<>();
    }

    public static void clearList(Context ctx, String list) {
        prefs(ctx).edit().remove(list).apply();
    }

    public static void updateProgress(Context ctx, ContentItem item, int season, int episode) {
        item.lastSeason  = season;
        item.lastEpisode = episode;
        add(ctx, item, CONTINUE);
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
