package com.tstine.marvinas.bimap;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Taylor Stine on 12/4/13.
 * based on BitmapFun sample from the android sdk sample
 * sub-class of ImageView which automatically notifies the drawable when it is being displayed
 */
public class RecyclingImageView extends ImageView {

    public RecyclingImageView(Context context) {
        super(context);
    }

    public RecyclingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclingImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDetachedFromWindow(){
        setImageDrawable(null);
        super.onDetachedFromWindow();
    }

    @Override
    public void setImageDrawable(Drawable drawable){
        //Keep hold of the previous Drawable
        final Drawable previousDrawable = getDrawable();

        //Call super method
        super.setImageDrawable(drawable);

        //Notify the drawable it is being displayed
        notifyDrawable(drawable, true);

        //Notify the old Drawable that it is no longer being displayed
        notifyDrawable( previousDrawable, false );

    }

    /**
     * Notifies the drawable that it's displayed state has changed
     * @param drawable
     */
    private static void notifyDrawable(Drawable drawable, final boolean isDisplayed){
        if( drawable instanceof RecyclingBitmapDrawable){
            //The drawable is a RecyclingBitmapDrawable so notify it of it's state change
            ((RecyclingBitmapDrawable) drawable).setIsDisplayed(isDisplayed);
        }else if( drawable instanceof LayerDrawable){
            //recurse on each layer and notify
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            for( int i = 0, z = layerDrawable.getNumberOfLayers(); i<z; i++ ){
                notifyDrawable(layerDrawable.getDrawable(i), isDisplayed);
            }
        }
    }
}
