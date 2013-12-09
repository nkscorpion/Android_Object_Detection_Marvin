package com.tstine.marvinas.camera;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.view.Gravity;
import android.widget.Toast;

import com.tstine.marvinas.R;
import com.tstine.marvinas.util.Log;

public class MarvinCamera{

    private static Camera mCamera = null;

    public static int getCameraId() {
        return mCameraId;
    }
    private static int mCameraId = 0;

    public static Camera getCamera(){return mCamera;}

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static Camera initCamera(Context ctx){
        if( mCamera == null ){
            if(Camera.getNumberOfCameras() < 0 )
                return null;
            mCameraId = findRearFacingCamera();
            try{
                mCamera = Camera.open(mCameraId);
            }
            catch(Exception e){
                Toast warning = Toast.makeText(ctx, ctx.getResources().getString(R.string.camera_occupied), Toast.LENGTH_SHORT);
                warning.setGravity(Gravity.CENTER, 0, 0);
                warning.show();
            }
        }
        return mCamera;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static int findRearFacingCamera(){
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();

        for(int i=0; i < numberOfCameras; i++){
            cameraId = i;
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo( i, info );
            if( info.facing == CameraInfo.CAMERA_FACING_BACK )
                break;
        }
        return cameraId;
    }

    public static void stopAndRelease(){
        if( mCamera != null ){
            mCamera.stopPreview();
            releaseCamera();
        }
    }

    public static void releaseCamera(){
        if( mCamera!=null){
            mCamera.release();
            mCamera=null;
        }
    }

    public static void takePicture( PictureCallback callback ){mCamera.takePicture( null, null, callback );}
}

