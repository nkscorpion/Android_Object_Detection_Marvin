package com.tstine.marvinas.camera;
import java.lang.RuntimeException;
import java.lang.Integer;
import java.lang.Math;
import java.io.IOException; 
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.Surface;

import com.tstine.marvinas.util.Log;

public class CameraPreview
	extends SurfaceView implements SurfaceHolder.Callback{
	
	private Camera mCamera;
    private int mCameraId;
	private boolean mPreviewRunning=false;
	private SurfaceHolder mHolder;
	private Context mCtx;

	public CameraPreview( Context ctx, Camera camera, int id ){
		super( ctx );
		mHolder = getHolder();
		mHolder.addCallback( this );
		mCamera = camera;
		mCtx = ctx;;
      mCameraId = id;
	}

	@Override
	public void surfaceCreated( SurfaceHolder holder ){
	}

	@Override
	public void surfaceChanged( SurfaceHolder holder, int format,
															int surfaceWidth, int surfaceHeight){
      Log.d("onsurfacechanged");
      setCameraDisplayOrientation(mCtx, mCameraId, mCamera);
      setCameraPreviewSize(mCamera, surfaceWidth, surfaceHeight);

      Camera.Parameters params = mCamera.getParameters();
      if( params.getSupportedFocusModes()
          .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
          params.setFocusMode( Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE );
      mCamera.setParameters( params );
      startPreview();
	}

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void setCameraDisplayOrientation(Context context, int cameraId,
                                                   Camera camera){
        Camera.CameraInfo info = new Camera.CameraInfo();
        camera.getCameraInfo(cameraId, info);
        int rotation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        int degreesCW = 0;
        switch(rotation){
            case Surface.ROTATION_0: degrees = 0; degreesCW = 0; break;
            case Surface.ROTATION_90: degrees = 90; degreesCW = 270; break;
            case Surface.ROTATION_180: degrees = 180; degreesCW = 180; break;
            case Surface.ROTATION_270: degrees = 270; degreesCW = 90; break;
        }
        int result;
        int cameraRotation = 0;
        if( info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ){
            result = (info.orientation + degrees ) % 360;
            result = (360 - result ) % 360;
            cameraRotation = (info.orientation - degreesCW + 360 ) % 360;
        }else{
            result = (info.orientation - degrees + 360 ) % 360;
            cameraRotation = (info.orientation + degreesCW ) % 360;
        }

        Log.d("info orientation: " + info.orientation + " result: " + result + " degrees " +
            degrees + " camera roation: " + cameraRotation);
        camera.setDisplayOrientation(result);
        Camera.Parameters params = camera.getParameters();
        params.setRotation(cameraRotation);
        camera.setParameters(params);
     }

    public static void setCameraPreviewSize( Camera camera, int surfaceWidth, int surfaceHeight){
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size bestSize = findBestSize( sizes, surfaceWidth, surfaceHeight );
        params.setPreviewSize( bestSize.width, bestSize.height );
        camera.setParameters(params);
    }
	@Override
	public void surfaceDestroyed( SurfaceHolder holder ){
	}

	public void startPreview(){
		if( mHolder != null ){
			try{
				mCamera.setPreviewDisplay( mHolder );
			}catch( IOException e ){
				throw new RuntimeException( e.getMessage() );
			}
			mCamera.startPreview();
			mPreviewRunning = true;
		}
	}

	public static Camera.Size findBestSize( List<Camera.Size> sizes, int width, int height ){
		float surfAspect = 0.0f;
		if( width > height )
			surfAspect = (float)width/height;
		else
			surfAspect = (float)height/width;
		
		float aspect = -1, bestAspectDiff = Integer.MAX_VALUE, aspectDiff;
		Camera.Size bestSize=null;
		for( Camera.Size size: sizes ){
			aspect = (float)size.width/size.height;
			aspectDiff = Math.abs(aspect - surfAspect);
			if( aspectDiff < bestAspectDiff ){
				bestSize = size;
				bestAspectDiff = aspectDiff;
			}
		}
		return bestSize;
	}


}
