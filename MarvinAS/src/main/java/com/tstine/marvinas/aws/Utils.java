package com.tstine.marvinas.aws;

import android.os.Build;

/**
 * Created by taylor on 12/4/13.
 */
public class Utils {

    public static boolean hasFroyo(){
       return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }
    public static boolean hasGingerbread(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }
    public static boolean hasHoneycomb(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
    public static boolean hasHoneycombMR1(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }
    public static boolean hasJellyBean(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
    public static boolean hasKitKat(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
}
