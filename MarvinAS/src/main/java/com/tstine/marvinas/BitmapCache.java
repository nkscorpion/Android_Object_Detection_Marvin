package com.tstine.marvinas;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.LruCache;

/**
 * Created by taylor on 11/26/13.
 */
public class BitmapCache {
    private LruCache<String, BitmapDrawable> mMemoryCache;
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public BitmapCache(int size ){
        mMemoryCache = new LruCache<String,BitmapDrawable>(size);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void addToMemoryCache(String key, BitmapDrawable bitmap){
        if( mMemoryCache.get(key) == null ){
            mMemoryCache.put(key,bitmap);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public BitmapDrawable getFromMemoryCache( String key ){
        BitmapDrawable bitmap=null;
        bitmap=mMemoryCache.get(key);
        return bitmap;
    }
}
