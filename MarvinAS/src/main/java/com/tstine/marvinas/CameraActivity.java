package com.tstine.marvinas;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.view.OrientationEventListener;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.tstine.marvinas.Const.*;


public class CameraActivity extends Activity{
    public OrientationEventListener orientationEventListener;
	@Override
	public void onCreate( Bundle savedInstanceState ){
		super.onCreate( savedInstanceState );
		if( !testConnection( this ) )
			startActivity( new Intent( this, NoServiceActivity.class ) );
		else
			setContentView( R.layout.camera );
      Installation.setContext( this );
      ButterKnife.inject(this);
	}

    @Override
    public void onStart(){
        super.onStart();
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                Log.d("orientation changed: " + orientation);
            }
        };
        //orientationEventListener.enable();
    }

	@Override	
	protected void onResume(){
		super.onResume();
		if( MarvinCamera.getCamera() == null &&
				MarvinCamera.initCamera() == null ) {
			Toast.makeText( this, "Error starting camera", Toast.LENGTH_SHORT )
				.show();
		}
    FrameLayout preview = (FrameLayout) findViewById( R.id.camera_preview );
    if( preview != null ){
        preview.removeAllViews();
        preview.addView( new CameraPreview( this, MarvinCamera.getCamera(),
            MarvinCamera.getCameraId() ),
            0 );
		}
	}

	@Override
	public void onPause(){
		super.onPause();
		MarvinCamera.stopAndRelease();
	}

    @Override
    public void onStop(){
        super.onStop();
        //orientationEventListener.disable();
    }

    @OnClick(R.id.button_capture)
    public void onClickPicture( ){
        MarvinCamera.takePicture( new PictureTaker(this) );
    }

    @OnClick(R.id.see_results)
    public void onClickResults(){
        Intent intent = new Intent(this, ResultsActivity.class);
        startActivity(intent);
    }

	public void doPositiveClick(String userInput, File picFile ){

    //addToPollQueueService(userInput);
    Request request = new Request(
        picFile.getPath(),
        picFile.getName(),
        Installation.getId(),
        Installation.getTimestamp(),
        userInput
    );
      Intent detectionIntent = new Intent( this, DetectionService.class);
      detectionIntent.putExtra( REQUEST_EXTRA_KEY, request);
      if(Const.SEND_TO_AWS)
          startService( detectionIntent );
      Intent showListIntent = new Intent(this, ResultsActivity.class );
      showListIntent.putExtra( REQUEST_EXTRA_KEY, request );
      startActivity( showListIntent );

	}

	public void doNegativeClick(){
		
	}

	private void addToPollQueueService(String message){
		//Intent pollQueueIntent = new Intent( this, PollResponseService.class );
		//pollQueueIntent.putExtra( Const.USER_MESSAGE_KEY, message );
	}

	public static boolean testConnection( Context ctx ){
		ConnectivityManager cm = (ConnectivityManager)
			ctx.getSystemService( Context.CONNECTIVITY_SERVICE );
		NetworkInfo net = cm.getActiveNetworkInfo();
		if( net == null )
			return false;
		return net.isConnectedOrConnecting();
	}



}