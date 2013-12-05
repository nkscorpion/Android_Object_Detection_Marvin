package com.tstine.marvinas.fragment;

import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import java.io.File;

import android.widget.EditText;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.app.Activity;

import com.tstine.marvinas.R;
import com.tstine.marvinas.activity.CameraActivity;

public class UserInputDialog extends DialogFragment{
	private File mPicFile;

	public UserInputDialog( File file ){
		mPicFile = file;
	}
	@Override
	public void onCreate( Bundle savedInstance ){
		super.onCreate( savedInstance );
	}

	@Override
	public Dialog onCreateDialog( Bundle savedInstanceState ){
		Activity activity = getActivity();
		final EditText input = new EditText( activity );
		return new AlertDialog.Builder(activity )
			.setView( input )
			.setTitle( activity.getString( R.string.user_input_dialog_title) )
			.setPositiveButton(
				R.string.alert_dialog_ok,
				new DialogInterface.OnClickListener(){
					public void onClick( DialogInterface dialog, int button ){
						((CameraActivity)getActivity()).doPositiveClick(input.getText().toString(), mPicFile);
					}
				})
			.setNegativeButton(
				R.string.alert_dialog_cancel,
				new DialogInterface.OnClickListener(){
					public void onClick( DialogInterface dialog, int button ){
						((CameraActivity)getActivity()).doNegativeClick();
					}
				})
			.create();
	}

}