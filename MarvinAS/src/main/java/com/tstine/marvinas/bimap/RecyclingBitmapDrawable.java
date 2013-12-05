package com.tstine.marvinas.bimap;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.tstine.marvinas.util.Log;

import java.io.InputStream;

/**
 * Created by taylor on 12/4/13.
 */
public class RecyclingBitmapDrawable extends BitmapDrawable {
    private int mCacheRefCount = 0;
    private int mDisplayRefCount = 0;
    private boolean mHasBeenDisplayed = false;

    public RecyclingBitmapDrawable(Resources res, Bitmap bitmap) {
        super(res, bitmap);
    }

    public RecyclingBitmapDrawable(Resources res, InputStream is) {
        super(res, is);
    }

    public void setIsDisplayed( boolean isDisplayed ){
        synchronized(this){
            if( isDisplayed ){
                mDisplayRefCount++;
                mHasBeenDisplayed = true;
            }else
                mDisplayRefCount--;
        }
        checkState();
    }

    public void setIsCached(boolean isCached){
        synchronized(this){
            if(isCached){
                mCacheRefCount++;
            }else{
                mCacheRefCount--;
            }
        }
        checkState();
    }

    private synchronized void checkState(){
        if(mCacheRefCount<= 0 && mDisplayRefCount<=0 && mHasBeenDisplayed && hasValidBitmap()){
            Log.d("No longer being used, bitmap is being recycled");
            getBitmap().recycle();
        }
    }

    private synchronized boolean hasValidBitmap(){
        Bitmap bitmap = getBitmap();
        return bitmap != null && !bitmap.isRecycled();
    }
}
