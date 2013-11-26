package com.tstine.marvinas;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by taylor on 11/24/13.
 */
public class BitmapWorker{
    private Resources mResources;
    private Bitmap mLoadingBitmap;
    protected int mImageHeight;
    protected int mImageWidth;
    protected static Object mPauseLock = new Object();
    protected static boolean mPaused = false;
    protected static boolean mExitTask = false;
    private ExecutorService cachedThreadPoolExecutor;
    private BitmapCache mBitmapCache = null;

    public BitmapWorker(Context ctx ){
        this(ctx, 0, 0);
    }
    public BitmapWorker(Context ctx, int width, int height ){
        this(ctx.getResources(), width, height );
    }

    public BitmapWorker( Resources resources, int width, int height ){
        mResources = resources;
        setImageSize(width,height);
        cachedThreadPoolExecutor = Executors.newCachedThreadPool();
        mBitmapCache = new BitmapCache(1024*10);
    }

    public void setImageSize( int width, int height){
        mImageWidth = width;
        mImageHeight = height;
    }

    public void addMemoryCache(int size ){
        mBitmapCache = new BitmapCache(size);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void loadImage(Object dataLocation, ImageView imageView ){
        if( dataLocation == null ){
            //TODO: Load a bitmap here the indicates the bitmap could not be downloaded
            return;
        }
        BitmapDrawable bitmapDrawable = null;
        if( mBitmapCache != null ){
            bitmapDrawable = mBitmapCache.getFromMemoryCache( String.valueOf(dataLocation));
        }
        if( bitmapDrawable != null ){
            imageView.setImageDrawable( bitmapDrawable);
        }
        else if( cancelPotentialWork( dataLocation, imageView )){
            final BitmapWorkerTask task = new BitmapWorkerTask( imageView );
            final AsyncDrawable asyncDrawable =
                new AsyncDrawable( mResources, mLoadingBitmap, task );
            imageView.setImageDrawable( asyncDrawable );
            task.executeOnExecutor( AsyncTask.THREAD_POOL_REJECTION_EXECUTOR, dataLocation);
        }

    }

    public static boolean getPaused(){return mPaused;}

    public static void setPaused(boolean pause){
        synchronized( mPauseLock){
            mPaused = pause;
            if( !mPaused){
                Log.d(Const.TAG,"resuming");
                mPauseLock.notifyAll();
            }else{
                Log.d(Const.TAG, "Paused");
            }
        }
    }

    public static boolean isTaskExited(){return mExitTask;}
    public static void setExitTask(boolean exitTask){
        mExitTask = exitTask;
        setPaused(false);
    }

    public void setLoadingBitmap(Bitmap bmp ){
        mLoadingBitmap = bmp;
    }
    public void setLoadingBitmap( int resource ){
        mLoadingBitmap = BitmapFactory.decodeResource(mResources, resource);
    }

    public static boolean cancelPotentialWork( Object dataLocation, ImageView imageView){
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if( bitmapWorkerTask != null ){
            final Object bitmapDataLocation = bitmapWorkerTask.dataLocation;
            if( bitmapDataLocation == null || !bitmapDataLocation.equals(dataLocation)){
                bitmapWorkerTask.cancel(true);
            }
            else{
                Log.d(Const.TAG, "Work canceled");
                return false;
            }
        }
        return true;
    }

    public static BitmapWorkerTask getBitmapWorkerTask( ImageView imageView ){
        if( imageView != null ){
            final Drawable drawable = imageView.getDrawable();
            if(drawable instanceof AsyncDrawable){
                final AsyncDrawable asyncDrawable = (AsyncDrawable)drawable;
                BitmapWorkerTask task =asyncDrawable.getBitmapWorkerTask();
                return task;
            }
        }
        return null;
    }



    public BitmapDrawable processBitmap( int resId ){
        Bitmap bitmap = decodeSampledBitmapFromResource(mResources, resId, mImageWidth, mImageHeight);
        BitmapDrawable bitmapDrawable = null;
        if( bitmap!= null ){
            bitmapDrawable = new BitmapDrawable(mResources, bitmap);
        }
        return bitmapDrawable;
    }

    public BitmapDrawable processBitmap(Object dataLocation){
        //This is a resources
        if(dataLocation instanceof Integer ){
            return processBitmap(Integer.parseInt(String.valueOf(dataLocation)));
        }
        else if(dataLocation instanceof String){
            String strLoc = String.valueOf(dataLocation);
            //This is a url
            if( strLoc.indexOf("http") != -1 ){
                Bitmap bitmap = getBitmapFromUrl(strLoc);
                BitmapDrawable bitmapDrawable= null;
                if( bitmap != null ){
                    bitmapDrawable = new BitmapDrawable(mResources, bitmap );
                }
                return bitmapDrawable;
            }
            //This is a file
            else{
                Bitmap bitmap = decodeSampledBitmapFromFile(strLoc, mImageWidth, mImageHeight);
                BitmapDrawable bitmapDrawable = null;
                if( bitmap!= null ){
                    bitmapDrawable = new BitmapDrawable( mResources, bitmap);
                }
                return bitmapDrawable;
            }
        }
        return null;
    }

    public static Bitmap getBitmapFromUrl( String src ){
        //TODO: implement sampling so a smaller size of the bitmap is downloaded see below
        URL imageUrl = null;
        HttpURLConnection connection= null;
        InputStream is = null;
        Bitmap bmp = null;
        try{
            imageUrl = new URL(src);
            connection = (HttpURLConnection) imageUrl.openConnection();
            is = new BufferedInputStream(connection.getInputStream());
             bmp = BitmapFactory.decodeStream(is);
        }catch (IOException e) {
            Log.e(Const.TAG, "Exception: + e.printStackTrace()");
        }
        finally{
            if( is != null)
                try{
                    is.close();
                }catch(IOException e ){
                    Log.e(Const.TAG, "Exception: + e.printStackTrace()");
                }
            if( connection != null){
                connection.disconnect();
            }
        }
        return bmp;
    }


    public static Bitmap decodeSampledBitmapFromResource(Resources resources, int resId,
                                                         int containerWidth, int containerHeight ){
        final BitmapFactory.Options options= new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId);
        int bmpHeight = options.outHeight;
        int bmpWidth = options.outWidth;
        int sampleSize = getSampleSize( containerWidth, containerHeight, bmpWidth, bmpHeight);
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeResource(resources, resId);

    }
    public static Bitmap decodeSampledBitmapFromFile(String path, int containerWidth,
                                             int containerHeight ){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int bmpHeight = options.outHeight;
        int bmpWidth = options.outWidth;
        int sampleSize = getSampleSize( containerWidth, containerHeight, bmpWidth, bmpHeight);
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(path, options);
    }

    private static int getSampleSize( int containerWidth, int containerHeight,
                                      int bmpWidth, int bmpHeight ){
        int sampleSize = 1;
        while( bmpWidth / sampleSize > containerWidth &&
            bmpHeight / sampleSize > containerHeight ){
            sampleSize*=2;
        }
        return sampleSize;
    }

    public class BitmapWorkerTask extends AsyncTask<Object, Void, BitmapDrawable> {
        private  WeakReference<ImageView> mImageViewReference;
        public Object dataLocation;

        public BitmapWorkerTask(ImageView imageView){
            mImageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected void onPreExecute(){}

        @Override
        public BitmapDrawable doInBackground(Object... params){
            synchronized (mPauseLock){
                while( mPaused && !isCancelled()){
                    try{
                        //here we are waiting for notification to resume our task
                        mPauseLock.wait();
                    } catch( InterruptedException e ){}
                }
            }
            dataLocation = params[0];
            final String dataString = String.valueOf(dataLocation);
            BitmapDrawable bitmap = null;
            ImageView attached = getAttachedImageView();
            if(mBitmapCache != null && !isCancelled() && attached != null
                && !mExitTask ){
                bitmap = mBitmapCache.getFromMemoryCache(String.valueOf(params[0]));
                
            }
            if(bitmap == null){
                Log.d(Const.TAG,"It's in the cache");
            }
            if( bitmap == null && !isCancelled() && attached != null
                && !mExitTask) {
                bitmap = processBitmap(params[0]);
            }

            if( bitmap != null && mBitmapCache != null ){
                mBitmapCache.addToMemoryCache(String.valueOf(dataLocation), bitmap);
            }
            return bitmap;
        }

        @Override
        public void onPostExecute(BitmapDrawable bmp ){
            if( isCancelled() || mExitTask)
                bmp = null;

            final ImageView imageView = getAttachedImageView();
            if( imageView != null && bmp != null ){
                imageView.setImageDrawable( bmp );
            }
        }

        @Override
        public void onCancelled(){
            synchronized(mPauseLock){
                mPauseLock.notifyAll();
            }
        }

        private ImageView getAttachedImageView(){
            final ImageView imageView = mImageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
            if( this.equals(bitmapWorkerTask))
                return imageView;
            return null;
        }

        public boolean equals(Object  o ){
            boolean isEqual = false;
            if( o instanceof BitmapWorkerTask){
                String data = String.valueOf(((BitmapWorkerTask)o).dataLocation);
                String thisData = String.valueOf(this.dataLocation);
                isEqual = data.equals(thisData);
            }
            else{
                isEqual = this.equals(0);
            }
            return isEqual;
        }
    }



    private static class AsyncDrawable extends BitmapDrawable{
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskWeakReference;
        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask task ){
            super( res, bitmap );
            bitmapWorkerTaskWeakReference =
                new WeakReference<BitmapWorkerTask>((BitmapWorkerTask)task);
        }
        public BitmapWorkerTask getBitmapWorkerTask(){
            return bitmapWorkerTaskWeakReference.get();
        }
    }
}