package com.tstine.marvinas.activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.KeyEventCompat;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.tstine.marvinas.aws.AWSWorker;
import com.tstine.marvinas.camera.CameraPreview;
import com.tstine.marvinas.util.Const;
import com.tstine.marvinas.util.Installation;
import com.tstine.marvinas.camera.MarvinCamera;
import com.tstine.marvinas.fragment.NoServiceFragment;
import com.tstine.marvinas.camera.PictureTaker;
import com.tstine.marvinas.R;
import com.tstine.marvinas.aws.Request;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.tstine.marvinas.util.Const.*;


public class CameraActivity extends FragmentActivity {
    @Override
	public void onCreate( Bundle savedInstanceState ){
		super.onCreate( savedInstanceState );
		if( !testConnection( this ) )
			startActivity( new Intent( this, NoServiceFragment.class ) );
		else
			setContentView( R.layout.camera );
      Installation.setContext(this);
      ButterKnife.inject(this);
	}

    @Override
    public void onStart(){
        super.onStart();
    }

	@Override	
	protected void onResume(){
      super.onResume();
      if( MarvinCamera.getCamera() == null &&
          MarvinCamera.initCamera(this) == null ) {
          finish();
      }else{
          FrameLayout preview = (FrameLayout) findViewById( R.id.camera_preview );
          if( preview != null ){
              preview.removeAllViews();
              preview.addView( new CameraPreview( this, MarvinCamera.getCamera(),
                  MarvinCamera.getCameraId() ),
                  0 );
          }
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
      if(Const.SEND_TO_AWS)
          AWSWorker.uploadToAWS(request);
      Intent showListIntent = new Intent(this, ResultsActivity.class );
      showListIntent.putExtra( REQUEST_EXTRA_KEY, request );
      startActivity( showListIntent );

	}

	public void doNegativeClick(){
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