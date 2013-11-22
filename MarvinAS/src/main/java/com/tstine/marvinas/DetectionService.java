package com.tstine.marvinas;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import static com.tstine.marvinas.Const.*;

public class DetectionService extends IntentService{
	public DetectionService(){
		super("DetectionService");
	}

	@Override 
	protected void onHandleIntent( Intent intent ){
		if( CameraActivity.testConnection( this ) ){
      Request request = (Request) intent.getSerializableExtra(REQUEST_EXTRA_KEY);
			addPictureToBucket( request );
			addRequestToDatabase( request );
      addRequestToQueue( request );
		}

	}
	private void addPictureToBucket( Request request ){
		Intent intent = new Intent( this, AddPictureToBucketService.class);
      intent.putExtra(REQUEST_EXTRA_KEY, request );

		startService( intent );
	}

	private void addRequestToDatabase( Request request ){
		Intent intent = new Intent();
		if( ACTIVE_DB.equals("SIMPLE_DB") )
      intent.setClass(this, AddToSimpleDBService.class );
		else
			intent.setClass(this, AddToDynamoService.class );

		intent.putExtra( REQUEST_EXTRA_KEY, request );
		startService( intent );
	}

	private void addRequestToQueue( Request request ){
		Intent intent = new Intent( this, AddToQueueService.class );
		intent.putExtra(REQUEST_EXTRA_KEY, request );
		startService( intent );
	}
}