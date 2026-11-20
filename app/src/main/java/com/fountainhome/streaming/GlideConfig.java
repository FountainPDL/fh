package com.fountainhome.streaming;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.fountainhome.streaming.service.AppPreferences;

/** Images: a large on-disk cache (size is a setting) and a memory cache scaled to the phone's RAM. */
@GlideModule
public class GlideConfig extends AppGlideModule {
    @Override public void applyOptions(@NonNull Context c, @NonNull GlideBuilder b) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        int heapMb = am != null ? am.getMemoryClass() : 128;
        long mem = Math.max(32L, Math.min(128L, heapMb / 4)) * 1024 * 1024;
        long disk = Math.max(50, AppPreferences.getImageCacheMb(c)) * 1024L * 1024L;
        b.setMemoryCache(new LruResourceCache(mem));
        b.setDiskCache(new InternalCacheDiskCacheFactory(c, "image_cache", disk));
    }

    @Override public boolean isManifestParsingEnabled() { return false; }
}
