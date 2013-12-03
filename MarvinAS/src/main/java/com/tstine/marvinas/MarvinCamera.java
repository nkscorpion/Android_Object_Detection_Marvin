package com.tstine.marvinas;
import java.util.List;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.view.SurfaceHolder;
import java.io.IOException;
import java.lang.RuntimeException;
import static com.tstine.marvinas.Const.*;

public class MarvinCamera{
	
	private static Camera mCamera = null;

    public static int getCameraId() {
        return mCameraId;
    }
    private static int mCameraId = 0;

	public static Camera getCamera(){return mCamera;}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
  public static Camera initCamera(){
		if( mCamera == null ){
			if(Camera.getNumberOfCameras() < 0 )
				return null;
		
			mCameraId = findRearFacingCamera();
			Camera cam = null;
			try{
				mCamera = Camera.open(mCameraId);
			}
			catch(Exception e){
				Log.d("Camera could not be opened");
			}
		}
		return mCamera;
	}

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

	public static void takePicture( PictureCallback callback ){
		mCamera.takePicture( null, null, callback );


	}
}

