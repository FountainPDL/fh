package com.fountainhome.streaming;
import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
@GlideModule
public class GlideConfig extends AppGlideModule {
    @Override public void applyOptions(@NonNull Context c, @NonNull GlideBuilder b) {
        b.setMemoryCache(new LruResourceCache(20*1024*1024));
        b.setDiskCache(new InternalCacheDiskCacheFactory(c,"image_cache",100*1024*1024));
    }
}
