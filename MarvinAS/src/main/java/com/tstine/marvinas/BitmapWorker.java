package com.tstine.marvinas;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by taylor on 11/24/13.
 */
public class BitmapWorker{
    //TODO: Implement a pre-processing step to look through current files to see if pictures are
    // on the device
    private Resources mResources;
    private Bitmap mLoadingBitmap;
    protected static Object mPauseLock = new Object();
    protected static boolean mPaused = false;
    protected static boolean mExitTask = false;
    private ExecutorService cachedThreadPoolExecutor;
    private BitmapCache mBitmapCache = null;
    private Context mCtx;

    public BitmapWorker(Context ctx ){
        this(ctx, 0, 0);
    }
    public BitmapWorker(Context ctx, int width, int height ){
        this(ctx.getResources(), width, height );
        mCtx = ctx;
    }

    public BitmapWorker( Resources resources, int width, int height ){
        mResources = resources;
        cachedThreadPoolExecutor = Executors.newCachedThreadPool();
        mBitmapCache = new BitmapCache();
    }

    public void addMemoryCache(int size ){
        mBitmapCache.addMemoryCache(size);
    }

    public void addDiskCache(File cacheDir){
        mBitmapCache.addDiskCache(cacheDir);

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ImageLoadRequest loadImage(){return new ImageLoadRequest(this);}
    public void loadImage(Object dataLocation, ImageView imageView,  Object identifier,
                          int width, int height){
        new ImageLoadRequest(this, dataLocation, imageView, identifier, width, height).start();
    }
    public void loadImage(Object dataLocation, ImageView imageView, Object identifier){
        new ImageLoadRequest(this, dataLocation, imageView, identifier, -1, -1).start();
    }
    private void startLoad(ImageLoadRequest loadRequest ){
        if( loadRequest.dataLocation == null || loadRequest.imageView == null ){
            //TODO: Load a bitmap here the indicates the bitmap could not be downloaded
            return;
        }
        BitmapDrawable bitmapDrawable = null;
        if( mBitmapCache != null ){
            bitmapDrawable = mBitmapCache.getFromMemoryCache( String.valueOf(loadRequest.identifier) );
        }
        if( bitmapDrawable != null ){
            Log.d("image in cache");
            loadRequest.imageView.setImageDrawable( bitmapDrawable);
        }
        else if( cancelPotentialWork( loadRequest.dataLocation, loadRequest.imageView )){
            final BitmapWorkerTask task = new BitmapWorkerTask( loadRequest.imageView );
            final AsyncDrawable asyncDrawable =
                new AsyncDrawable( mResources, mLoadingBitmap, task );
            loadRequest.imageView.setImageDrawable( asyncDrawable );
            task.executeOnExecutor( AsyncTask.THREAD_POOL_REJECTION_EXECUTOR,
                loadRequest);
        }
    }

    public static boolean getPaused(){return mPaused;}

    public static void setPaused(boolean pause){
        synchronized( mPauseLock){
            mPaused = pause;
            if( !mPaused){
                mPauseLock.notifyAll();
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
                Log.d("Work canceled");
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
    private BitmapDrawable processBitmap( ImageLoadRequest request ){
        //This is a resources
        Object dataLocation = request.dataLocation;
        BitmapDrawable bitmapDrawable= null;
        Bitmap bitmap = null;
        if(dataLocation instanceof Integer ){
            //it is a resource
            if( request.subSample){
            bitmap = decodeSampledBitmapFromResource(mResources, (Integer)dataLocation, request.mImageWidth, request.mImageHeight );
            }else{
                bitmap = decodeBitmapFromResource( mResources, (Integer)dataLocation);
            }
        }
        else if(dataLocation instanceof String){
            String strLoc = String.valueOf(dataLocation);
            //This is a url
            if( strLoc.indexOf("http") != -1 ){
                if(request.subSample){
                    bitmap = getSampledBitmapFromUrl(strLoc, request.mImageWidth, request.mImageHeight);
                }
                else{
                    bitmap= getBitmapFromUrl(strLoc);
                }
            }
            //This is a file
            else{
                if( request.subSample){
                    bitmap = decodeSampledBitmapFromFile(strLoc, request.mImageWidth, request.mImageHeight);
                }else{
                    bitmap = decodeBitmapFromFile(strLoc);
                }

            }
        }
        if( bitmap!= null ){
            bitmapDrawable = new BitmapDrawable( mResources, bitmap);
        }
        return bitmapDrawable;
    }

    public static Bitmap getBitmapFromUrl(String src){
        return getSampledBitmapFromUrl(src, -1, -1);
    }
    public static Bitmap getSampledBitmapFromUrl( String src, int containerWidth,
                                           int containerHeight ){
        //TODO: implement sampling so a smaller size of the bitmap is downloaded see below
        URL imageUrl = null;
        HttpURLConnection connection= null;
        InputStream is = null;
        Bitmap bmp = null;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        try{
            imageUrl = new URL(src);
            connection = (HttpURLConnection) imageUrl.openConnection();
            is = new BufferedInputStream(connection.getInputStream());
            if(containerHeight!= -1 && containerWidth!=-1){
                options.inJustDecodeBounds = true;
                bmp = BitmapFactory.decodeStream(is, null, options);
                int sampleSize = getSampleSize( containerWidth, containerHeight, options.outWidth,
                    options.outHeight );
                options.inJustDecodeBounds = false;
                options.inSampleSize = sampleSize;
                if( is.markSupported() ){
                    is.reset();
                }
            }
            bmp = BitmapFactory.decodeStream(is, null, options);

        }catch (IOException e) {
            if(e.getMessage().equals("Mark has been invalidated.")){
                try{
                    //TODO: make a cleaner implementation of this
                    connection.disconnect();
                    connection = (HttpURLConnection) imageUrl.openConnection();
                    is = new BufferedInputStream(connection.getInputStream());
                    bmp = BitmapFactory.decodeStream(is, null, options);
                }catch(IOException e2){}
            }
            e.printStackTrace();
        }
        finally{
            if( is != null)
                try{
                    is.close();
                }catch(IOException e ){
                    Log.d("Exception: + e.printStackTrace()");
                }
            if( connection != null){
                connection.disconnect();
            }
        }
        return bmp;
    }


    public static Bitmap decodeBitmapFromResource( Resources resources, int resId){
        return decodeSampledBitmapFromResource(resources, resId, -1, -1);
    }
    public static Bitmap decodeSampledBitmapFromResource(Resources resources, int resId,
                                                         int containerWidth, int containerHeight ){
        if( containerWidth!= -1 && containerHeight!=-1){
            final BitmapFactory.Options options= new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(resources, resId);
            int bmpHeight = options.outHeight;
            int bmpWidth = options.outWidth;
            int sampleSize = getSampleSize( containerWidth, containerHeight, bmpWidth, bmpHeight);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
        }
        return BitmapFactory.decodeResource(resources, resId);

    }
    public static Bitmap decodeBitmapFromFile(String path){
        return decodeSampledBitmapFromFile(path, -1, -1);
    }
    public static Bitmap decodeSampledBitmapFromFile(String path, int containerWidth,
                                             int containerHeight ){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        if( containerWidth!=-1 && containerHeight!= -1){
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            int bmpHeight = options.outHeight;
            int bmpWidth = options.outWidth;
            int sampleSize = getSampleSize( containerWidth, containerHeight, bmpWidth, bmpHeight);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
        }
        return BitmapFactory.decodeFile(path, options);
    }

    private static int getSampleSize( int containerWidth, int containerHeight,
                                      int bmpWidth, int bmpHeight ){
        int sampleSize = 1;
        while( bmpWidth / sampleSize > containerWidth &&
            bmpHeight / sampleSize > containerHeight ){
            sampleSize*=2;
        }
        Log.d("sample size: " + sampleSize);
        return sampleSize;
    }

    public class BitmapWorkerTask extends AsyncTask<ImageLoadRequest, Void, BitmapDrawable> {
        private  WeakReference<ImageView> mImageViewReference;
        private Object dataLocation;
        private Object identifier;
        private ImageLoadRequest request;

        public BitmapWorkerTask(ImageView imageView){
            mImageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected void onPreExecute(){}

        @Override
        public BitmapDrawable doInBackground(ImageLoadRequest... params){
            synchronized (mPauseLock){
                while( mPaused && !isCancelled()){
                    try{
                        //here we are waiting for notification to resume our task
                        mPauseLock.wait();
                    } catch( InterruptedException e ){}
                }
            }
            request = params[0];
            dataLocation = request.dataLocation;
            identifier = request.identifier;
            final String dataString = String.valueOf(dataLocation);
            BitmapDrawable bitmap = null;
            ImageView attached = getAttachedImageView();
            if(mBitmapCache != null && !isCancelled() && attached != null
                && !mExitTask ){
                //TODO:here add a retrieval from the DISK cache
            }

            //if the bitmap is not null then it is is the disk cache
            if( bitmap == null && !isCancelled() && attached != null
                && !mExitTask) {
                bitmap = processBitmap(request);
                //TODO: come up with a better way to handle out of memory errors
                }


            if( bitmap != null && mBitmapCache != null ){
                //TODO: change this to add to cache so this puts the bitmap in both the memory
                // and image cache
                mBitmapCache.addToMemoryCache(String.valueOf(identifier), bitmap);
            }
            return bitmap;
        }

        @Override
        public void onPostExecute(BitmapDrawable bmp ){
            if( isCancelled() || mExitTask){
                bmp = null;
            }
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

    public static class ImageLoadRequest{
        public boolean subSample=false;
        public Object dataLocation=null;
        public ImageView imageView=null;
        public Object identifier=null;
        public BitmapWorker workerReference;
        protected int mImageHeight;
        protected int mImageWidth;

        public ImageLoadRequest(BitmapWorker workerRef){
            this(workerRef,null,null,null,-1,-1);
        }
        public ImageLoadRequest(BitmapWorker workerRef, Object dataLocation, ImageView imageView, Object identifier, int width, int height ){
            workerReference = workerRef;
            withDataLocation(dataLocation);
            withIdentifier(identifier);
            withImageView(imageView);
            if( width!=-1 && height!= -1){
                withSampling(width, height);
            }
        }
        public ImageLoadRequest withDataLocation(Object location){
            dataLocation = location;
            return this;
        }
        public ImageLoadRequest withImageView(ImageView view ){
            imageView = view;
            return this;
        }
        public ImageLoadRequest withIdentifier(Object id){
            identifier = id;
            return this;
        }
        public ImageLoadRequest withSampling(int width, int height){
            subSample = true;
            mImageWidth = width;
            mImageHeight = height;
            return this;
        }

        public void start(){
            workerReference.startLoad(this);
        }
    }
}