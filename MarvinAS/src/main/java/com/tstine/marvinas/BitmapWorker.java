package com.tstine.marvinas;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.FragmentManager;
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
    // TODO: on the device

    //Resources for the current context
    private Resources mResources;
    //Bitmap to show while we are running a BitmapWorkerTask
    private Bitmap mLoadingBitmap;

    //Variable to tell all of the currently processing BitmapWorkerTasks to pause
    protected static boolean mPaused = false;
    protected static Object mPauseLock = new Object();

    //tells the BitmapWorkerTasks to exit
    protected static boolean mExitTask = false;

    //Instance of the bitmapcache
    private BitmapCache mBitmapCache = null;

    public BitmapWorker(Context ctx ){
        this(ctx, 0, 0);
    }
    public BitmapWorker(Context ctx, int width, int height ){
        this(ctx.getResources(), width, height );
    }

    public BitmapWorker( Resources resources, int width, int height ){
        mResources = resources;
    }

    /**
     * Adds a memory cache to this instance of the bitmap worker
     * @param percent the percent of memory that should be used for the cache
     */
    public void addMemoryCache(float percent, FragmentManager fm ){
        mBitmapCache = BitmapCache.getInstance(fm, percent);
    }

    /**
     * loads an image using the builder paradigm.  An example implementation would be:
     * loadImage()
     * .withDataLocation("http://www.google.com/example.jpg")
     * .withImageView(imageView)
     * .withIdentifier("example.jpg")
     * .withSampleSize( 250, 250)
     * .start()
     *
     * @return ImageLoadRequest that can be used with a builder paradigm to add elements to
     * the request
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ImageLoadRequest loadImage(){return new ImageLoadRequest(this);}

    /**
     * Initiates a load from the given ImageLoadRequest.  This method first checks the cache
     * if it exsists for the drawable.  If the cache exists and there is an image in it, then
     * the bitmap is set from there.  If there is no cache or the drawable is not in the
     * cache, the method creates a new AsyncTask to download the bitmap
     * @param loadRequest The ImageLoadRequest request to be processed
     */
    private void startLoad(ImageLoadRequest loadRequest ){
        if( loadRequest.dataLocation == null || loadRequest.imageView == null ){
            //TODO: Load a bitmap here the indicates the bitmap could not be downloaded
            return;
        }
        BitmapDrawable bitmapDrawable = null;
        //If the cache exists, search it for the bitmap
        if( mBitmapCache != null ){
            bitmapDrawable = mBitmapCache.getFromMemoryCache( String.valueOf(loadRequest.identifier) );
        }
        //If the bitmap is not null, then it was in the cache
        if( bitmapDrawable != null ){
            Log.d("image in cache");
            loadRequest.imageView.setImageDrawable( bitmapDrawable);
        }
        //If the bitmap was not in the cache, check to see if we can cancel the work
        else if( cancelPotentialWork( loadRequest.dataLocation, loadRequest.imageView )){
            //Create a new task to load the bitmap
            final BitmapWorkerTask task = new BitmapWorkerTask( loadRequest.imageView );
            //Set a drawable that has a reference to the task
            final AsyncDrawable asyncDrawable =
                new AsyncDrawable( mResources, mLoadingBitmap, task );
            //set the image drawable to the AsycDrawable
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
    private Bitmap processBitmap( ImageLoadRequest request ){
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
                    bitmap = getSampledBitmapFromUrl(strLoc, request.mImageWidth, request.mImageHeight, mBitmapCache);
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
        return bitmap;
    }

    public static Bitmap getBitmapFromUrl(String src){
        return getSampledBitmapFromUrl(src, -1, -1, null);
    }
    public static Bitmap getSampledBitmapFromUrl( String src, int containerWidth,
                                           int containerHeight, BitmapCache cache ){
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
                options.inSampleSize= getSampleSize( containerWidth, containerHeight, options.outWidth,
                    options.outHeight );
                //TODO: here we test to see if we can use inBitmap, implement similar functionality
                //TODO: to all of the get***BitmapFrom***() methods
                if(Utils.hasHoneycomb()){
                    addInBitmapOptions(options, cache);
                }
                options.inJustDecodeBounds = false;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void addInBitmapOptions(BitmapFactory.Options options, BitmapCache cache){
        //inBitmap only works with mutable bitmaps so force the decoder to return
        //mutable bitmaps
        options.inMutable = true;
        if( cache != null ){
            //Try and find a bitmap to use for inBitmap
            //TODO:implement getBitmapFromReusableSet to retrieve bitmap from HashSet
            Bitmap inBitmap = cache.getBitmapFromReusableSet(options);
            if( inBitmap!= null ){
                options.inBitmap = inBitmap;
            }
        }
    }

    /**
     * The AsyncTask that asynchronously process the image
     */
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
            //Wiat here if the work is paused and the task hasn't been cancelled
            synchronized (mPauseLock){
                while( mPaused && !isCancelled()){
                    try{
                        mPauseLock.wait();
                    } catch( InterruptedException e ){}
                }
            }
            request = params[0];
            dataLocation = request.dataLocation;
            identifier = request.identifier;
            final String dataString = String.valueOf(dataLocation);
            Bitmap bitmap = null;
            BitmapDrawable drawable = null;

            ImageView attached = getAttachedImageView();
            //If there is an image cache and the task is not cancelled
            //and the ImageView that was originally bound is still bound to this task
            //and we have not been told to exit early, the get the image from the Disk cache
            if(mBitmapCache != null && !isCancelled() && attached != null
                && !mExitTask ){
                //TODO:here add a retrieval from the DISK cache
            }

            //if the bitmap was not found in the cache, and the task has not been cancelled
            // and the attached image view is still valid, and we have not been told to
            // exit early, then call the main process bitmap method
            if( bitmap == null && !isCancelled() && attached != null
                && !mExitTask) {
                bitmap = processBitmap(request);
                //TODO: move this implementation to a subclass and make BitmapWorker abstract
                }

            //If the bitmap was successfully processed and there is a cache available, add the bitmap
            //to the cache for future use.
            if( bitmap != null && mBitmapCache != null ){
                if(Utils.hasHoneycomb()){
                    //If we're running on Honeycomb put it in a standard BitmapDrawable
                    drawable = new BitmapDrawable(mResources, bitmap);
                }else{
                    //TODO:is this correct? should you put it in a RecyclingBitampDrawable everytime?
                    //Running on Gingerbread or older, put it in a RecyclingBitmapDrawable
                    drawable = new RecyclingBitmapDrawable(mResources, bitmap);

                }
                //TODO: change this to add to cache so this puts the bitmap in both the memory
                // and image cache
                mBitmapCache.addToMemoryCache(String.valueOf(identifier), drawable);
            }
            return drawable;
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