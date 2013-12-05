package com.tstine.marvinas.util;

import com.tstine.marvinas.BuildConfig;

/**
 * Created by taylor on 12/1/13.
 */
public class Log {
    public static final String TAG="MarvinLog";
    public static void d(String str ){
        if(BuildConfig.DEBUG){
            android.util.Log.d(TAG, str);
        }
    }
}
