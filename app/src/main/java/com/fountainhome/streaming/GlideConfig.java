package com.fountainhome.streaming;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

@GlideModule
public class GlideConfig extends AppGlideModule {

    private static final int  MEMORY_CACHE_MB = 20;
    private static final long DISK_CACHE_MB   = 100;

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setMemoryCache(
            new LruResourceCache(MEMORY_CACHE_MB * 1024 * 1024));
        builder.setDiskCache(
            new InternalCacheDiskCacheFactory(context, "image_cache",
                DISK_CACHE_MB * 1024 * 1024));
        builder.setDefaultRequestOptions(
            new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL));
    }
}
