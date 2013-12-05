package com.tstine.marvinas.fragment;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.Gravity;
import android.view.View;
import android.content.Intent;

import com.tstine.marvinas.R;
import com.tstine.marvinas.activity.CameraActivity;

public class NoServiceFragment extends Fragment {
    @Override
    public void onCreate( Bundle iceThunder ){
        super.onCreate( iceThunder );
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        return inflater.inflate(R.layout.no_connection, null, false);
    }
    @Override
    public void onStart( ){
        super.onStart();

        if( CameraActivity.testConnection(getActivity()) )
            getFragmentManager().beginTransaction().remove(this).commit();
    }

    public void refreshClick(View v ){
        Toast notification = Toast.makeText( getActivity(), "", Toast.LENGTH_SHORT);
        notification.setGravity( Gravity.CENTER, 0, 0 );
        if( CameraActivity.testConnection( getActivity() ) ){
            notification.setText("Found connection!");
            notification.show();
            getFragmentManager().beginTransaction().remove(this).commit();
        }
        else{
            notification.setText("Sorry still nothing");
            notification.show();
        }
    }
}