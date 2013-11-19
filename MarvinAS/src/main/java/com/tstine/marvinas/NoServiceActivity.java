package com.tstine.marvinas;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import android.view.Gravity;
import android.view.View;
import android.content.Intent;

public class NoServiceActivity extends Activity{
	@Override
	public void onCreate( Bundle iceThunder ){
		super.onCreate( iceThunder );
		setContentView( R.layout.no_connection );
	}

	@Override
	protected void onStart( ){
		super.onStart();
		if( CameraActivity.testConnection(this) )
			startActivity( new Intent( this, CameraActivity.class ) );
	}

	public void refreshClick(View v ){
		Toast notification = Toast.makeText( this, "", Toast.LENGTH_SHORT);
		notification.setGravity( Gravity.CENTER, 0, 0 );
		if( CameraActivity.testConnection( this ) ){
			notification.setText("Found connection!");
			notification.show();
			startActivity( new Intent( this, CameraActivity.class ) );
		}
		else{
			notification.setText("Sorry still nothing");
			notification.show();
		}
	}
}