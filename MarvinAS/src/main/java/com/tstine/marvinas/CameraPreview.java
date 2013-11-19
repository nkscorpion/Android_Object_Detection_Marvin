package com.tstine.marvinas;
import java.lang.RuntimeException;
import java.lang.Integer;
import java.lang.Math;
import java.io.IOException; 
import java.util.List;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.Display;
import android.view.WindowManager;
import android.view.Surface;
import android.content.Context;

public class CameraPreview
	extends SurfaceView implements SurfaceHolder.Callback{
	
	private Camera mCamera;
	private boolean mPreviewRunning=false;
	private SurfaceHolder mHolder;
	private Context mCtx;
	
	public CameraPreview( Context ctx, Camera camera ){
		super( ctx );
		mHolder = getHolder();
		mHolder.addCallback( this );
		mCamera = camera;
		mCtx = ctx;;
	}

	@Override
	public void surfaceCreated( SurfaceHolder holder ){
	}

	@Override
	public void surfaceChanged( SurfaceHolder holder, int format,
															int surfaceWidth, int surfaceHeight){
		Camera.Parameters params = mCamera.getParameters();
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		Camera.Size bestSize = findBestSize( sizes, surfaceWidth,
																		 surfaceHeight );
		params.setPreviewSize( bestSize.width, bestSize.height );

      if( params.getSupportedFocusModes()
          .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
          params.setFocusMode( Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE );

		Display display = ((WindowManager) mCtx.getSystemService( Context.WINDOW_SERVICE))
			.getDefaultDisplay();

		switch( display.getRotation()){
		case Surface.ROTATION_0:
			mCamera.setDisplayOrientation(90);
			params.setRotation( 90 );
			break;
		case Surface.ROTATION_90:
			break;
		case Surface.ROTATION_180:
			break;
		case Surface.ROTATION_270:
			mCamera.setDisplayOrientation( 180 );
			params.setRotation( 180 );
			break;
		}
		
		mCamera.setParameters( params );		
		startPreview();
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

	private Camera.Size findBestSize( List<Camera.Size> sizes, int width, int height ){
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
