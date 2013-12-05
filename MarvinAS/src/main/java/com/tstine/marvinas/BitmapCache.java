package com.tstine.marvinas;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import com.tstine.marvinas.Log;
import android.support.v4.util.LruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Taylor on 11/26/13
 * This class maintains an LRU cache for Bitmaps.  It can use a memory and a disk cache to save the
 * Images
 */
public class BitmapCache {
    private LruCache<String, BitmapDrawable> mMemoryCache;
    private DiskLruCache mDiskCache;
    private final long DISK_CACHE_SIZE = 1024L * 1024L * 10L;
    private boolean mDiskCacheStarting;
    private Object mDiskCacheLock = new Object();
    private Set<SoftReference<Bitmap>> mReusableBitmaps;
    private RetainFragment mRetainFragment = null;
    private static final String RETAIN_FRAG_TAG="RetainFragment";


    private BitmapCache(float percent){
        initMemoryCache(percent);
    }

    public static BitmapCache getInstance( FragmentManager fm, float percent){
        final RetainFragment mRetainFragment = findOrCreateRetainFragment(fm);
        BitmapCache bitmapCache = (BitmapCache) mRetainFragment.getObject();
        if( bitmapCache == null){
            bitmapCache = new BitmapCache(percent);
            mRetainFragment.setObject(bitmapCache);
        }
        return bitmapCache;
    }

    /**
     * Adds a memory cache to track bitmaps
     * @param percent percentage of disk space to use
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void initMemoryCache(float percent){
        if(Utils.hasHoneycomb()){
            mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
        }
        int size = Math.round(percent * Runtime.getRuntime().maxMemory()/1024);
        mMemoryCache = new LruCache<String,BitmapDrawable>(size){
            /**
             * Notifies the removed entry is no longer cached
             */
            @Override
            protected void entryRemoved( boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue ){
                if(evicted){
                    Log.d("item evicted from cachce");
                }
                if( RecyclingBitmapDrawable.class.isInstance(oldValue)){
                    ((RecyclingBitmapDrawable)oldValue).setIsCached(false);
                }else{
                    if(Utils.hasHoneycomb())
                        mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
                }
            }
            /**
             * This measures an items size, the overriding method does so in Kilobytes rather than
             default of just returning one
             * @param key key in the cache
             * @param value value of the item
             * @return size of the bitmap in kilobytes
             */
            @Override
            protected int sizeOf(String key, BitmapDrawable value ){
                final int bitmapSize = getBitmapSize(value) / 1024;
                return bitmapSize == 0 ? 1 : bitmapSize;
            }
        };

    }

    //TODO:This is whole lifecycle is unimplemented
    public void addDiskCache(File cacheDir){
        mDiskCacheStarting=true;
        new InitDiskCacheTask().execute(cacheDir);
    }

    //TODO: finish implementing this method
    private class InitDiskCacheTask extends AsyncTask<File, Void, Void>{
        @Override
        public Void doInBackground(File... params){
            synchronized(mDiskCacheLock){
                File cacheDir = params[0];
                try{
                    mDiskCache = DiskLruCache.open(cacheDir,1,1, DISK_CACHE_SIZE);
                }catch(IOException e ){e.printStackTrace(); }
                mDiskCacheStarting = false;
                mDiskCacheLock.notifyAll();
            }
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void addToMemoryCache(String key, BitmapDrawable bitmap){
        if( key == null || bitmap == null)
            return;

        //add it to the memory cache
        if( mMemoryCache != null ){
            if( RecyclingBitmapDrawable.class.isInstance(bitmap)){
                //The entry we are adding is recycling drawable, so notify it
                //that it has been added to the cache
                ((RecyclingBitmapDrawable)bitmap).setIsCached(true);
            }
                mMemoryCache.put(key,bitmap);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void clearMemoryCache(){
        if( mMemoryCache != null ){
            mMemoryCache.evictAll();
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
                }catch(IOException e ){ e.printStackTrace();}
            }
        }
        return null;
    }

    //TODO:finishi implementing this disk cache
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static File getDiskCacheDir( Context ctx, String name){
        final String cachePath =
            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable() ?
                getExternalCacheDir(ctx).getPath() : ctx.getCacheDir().getPath();
        return new File( cachePath + File.separator + name );
    }

    /**
     * Determines whether the inBitmap option can be used to decode the candidate.
     * @param candidate the bitmap to test the validity of as a candidate for the inBitmap option
     * @param targetOptions the options of the bitmap for which this bitmap will be used with inBitmap
     *                      note that all out*** values must be populated
     * @return boolean if this bitmap can be used with in bitmap for the given options
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean canUseForInBitmap(
        Bitmap candidate, BitmapFactory.Options targetOptions){
        if( !Utils.hasKitKat()){
            return candidate.getWidth() == targetOptions.outWidth
                && candidate.getHeight() == targetOptions.outHeight
                && targetOptions.inSampleSize == 1;
        }

        int width = targetOptions.outWidth / targetOptions.inSampleSize;
        int height = targetOptions.outHeight / targetOptions.inSampleSize;
        int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
        return byteCount <= candidate.getAllocationByteCount();
    }


    //TODO:Finish this implementation
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static File getExternalCacheDir( Context context ){
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File( Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    /**
     * Returns the number of bytes pre pixel given a Bitmap.Config configuration
     * @param config the bitmaps Config
     * @return an integer representing the number of bytes per pixel
     */
    private static int getBytesPerPixel(Bitmap.Config config){
        if(config == Bitmap.Config.ARGB_8888){
            //8 * 4 = 32 / 8  = 4 bit bytes
            return 4;
        }else if(config == Bitmap.Config.RGB_565){
            //5 + 6 + 5 = 16 /8 = 2
            return 2;
        }else if(config == Bitmap.Config.ARGB_4444){
            //4 * 4 = 16 / 8 = 2
            return 2;
        }else if(config == Bitmap.Config.ALPHA_8){
            //8 / 8 = 1
            return 1;
        }
        return 1;
    }

    /**
     * returns the size of the given bitmap drawable in bytes
     * @param value BitmapDrawable to query the size of
     * @return an integer representing the number of bytes in the bitmap
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static int getBitmapSize( BitmapDrawable value ){
        Bitmap bitmap = value.getBitmap();
        //KitKat and above we can use the getAllocationByteCount() method - this is more accurate
        if(Utils.hasKitKat()){
            return bitmap.getAllocationByteCount();
        }
        //Honeycomb and above we can use the geByteCount() method
        if(Utils.hasHoneycombMR1()){
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * returns a bitmap from the SoftReference HashSet for the given reusable bitmap memory cache
     * @param options BitmapFactory.Options with all out* values populated
     * @return Bitmap that can be used for inBitmap
     */
    protected Bitmap getBitmapFromReusableSet(BitmapFactory.Options options){
        Bitmap bitmap = null;
        if( mReusableBitmaps != null && !mReusableBitmaps.isEmpty()){
            synchronized(mReusableBitmaps){
                final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
                Bitmap item;
                while(iterator.hasNext()){
                    item = iterator.next().get();
                    if( item != null && item.isMutable() ){
                        //Check to see if the item can be used for inBitmap
                        if(canUseForInBitmap(item, options)){
                            bitmap = item;
                            iterator.remove();
                        }
                    }else{
                        iterator.remove();
                    }
                }
            }
        }
        return bitmap;
    }


    /**
     * Find and isntance of the RetainFragment if it exists in the FragmentManager
     * If it does not, create it and return it
     * @param fm
     * @return
     */
    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm){
        RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(RETAIN_FRAG_TAG);
        if( mRetainFragment == null ){
            mRetainFragment = new RetainFragment();
            fm.beginTransaction().add(mRetainFragment, RETAIN_FRAG_TAG).commitAllowingStateLoss();
        }
        return mRetainFragment;
    }
    //TODO:Add a retain fragment to each instance of the cache, so that if the view is change
    //TODO: or the lifecycle runs it's course the cache is maintained
    public static class RetainFragment extends Fragment {
        //The fragment stores one object
        private Object mObject;
        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            //save the fragment if across Activity re-creation (i.e. from a configuration change)
            setRetainInstance(true);
        }
        public void setObject(Object object){ mObject = object;}
        public Object getObject(){ return mObject;}
    }

}
