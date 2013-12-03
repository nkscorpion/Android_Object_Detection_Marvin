package com.tstine.marvinas;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by taylor on 11/26/13
 */
public class BitmapCache {
    private LruCache<String, BitmapDrawable> mMemoryCache;
    private DiskLruCache mDiskCache;
    private final long DISK_CACHE_SIZE = 1024L * 1024L * 10L;
    private boolean mDiskCacheStarting;
    private Object mDiskCacheLock = new Object();
    private static ArrayList<BitmapCache> sInstances = new ArrayList<BitmapCache>();

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void addMemoryCache(int size ){
        mMemoryCache = new LruCache<String,BitmapDrawable>(size);
        sInstances.add(this);
    }

    public void addDiskCache(File cacheDir){
        mDiskCacheStarting=true;
        new InitDiskCacheTask().execute(cacheDir);
    }

    private class InitDiskCacheTask extends AsyncTask<File, Void, Void>{
        @Override
        public Void doInBackground(File... params){
            synchronized(mDiskCacheLock){
                File cacheDir = params[0];
                try{
                    mDiskCache = DiskLruCache.open(cacheDir,1,1, DISK_CACHE_SIZE);
                }catch(IOException e ){
                    Log.e(Const.TAG, "Could not create disk cache " + e);
                }
                mDiskCacheStarting = false;
                mDiskCacheLock.notifyAll();
            }
            return null;
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void addToMemoryCache(String key, BitmapDrawable bitmap){
        if( mMemoryCache != null ){
            if( mMemoryCache.get(key) == null ){
                mMemoryCache.put(key,bitmap);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public BitmapDrawable getFromMemoryCache( String key ){
        BitmapDrawable bitmap=null;
        if( mMemoryCache != null ){
            bitmap=mMemoryCache.get(key);
        }
        return bitmap;
    }

    public void clearMemoryCache(){
        if( mMemoryCache != null ){
            mMemoryCache.evictAll();
        }
    }

    public static void clearAllMemoryCaches(){
        for(BitmapCache cache : sInstances){
            cache.clearMemoryCache();
        }
    }


    //TODO:Finish this implementation of a get from disk cache method
    public BitmapDrawable getFromDiskCache(String key ){
        BitmapDrawable bitmapDrawable = null;
        while(mDiskCacheStarting){
            try{
                mDiskCacheLock.wait();
            }catch(InterruptedException e){}
            if( mDiskCache != null ){
                InputStream inputStream = null;
                try{
                    final DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
                    if(snapshot != null){
                        inputStream = snapshot.getInputStream(0);
                        if( inputStream != null ){
                            FileDescriptor fd = ((FileInputStream) inputStream).getFD();
                            //TODO:continue the implementation here
                        }
                    }
                }catch(IOException e ){
                    Log.e(Const.TAG, "Error retreiving from cache: " + e);
                    bitmapDrawable = null;
                }
            }
        }
        return null;
    }

    public static File getDiskCacheDir( Context ctx, String name){
        final String cachePath =
            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable() ?
                getExternalCacheDir(ctx).getPath() : ctx.getCacheDir().getPath();
        return new File( cachePath + File.separator + name );
    }

    public static File getExternalCacheDir( Context context ){
        return context.getExternalCacheDir();
        /*
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File( Environment.getExternalStorageDirectory().getPath() + cacheDir);
         */
    }

}
