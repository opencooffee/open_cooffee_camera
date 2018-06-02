package com.opencooffeecamera.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;

import com.opencooffeecamera.R;

@SuppressLint("InlinedApi")
public class RuntimePermissionUtils {
	
	//private static final String LOG_TAG = RuntimePermissionUtils.class.getSimpleName();

	public static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
	public static final String PERMISSION_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

	public static final int REQUEST_CAMERA_PERMISSION = 0;
	public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
				
	public static void showMessage(AppCompatActivity appCompatActivity, String message, DialogInterface.OnClickListener listener) {
        
		new AlertDialog.Builder(appCompatActivity)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(appCompatActivity.getResources().getString(R.string.no), listener)
                .setPositiveButton(appCompatActivity.getResources().getString(R.string.agree), listener)
                .create()
                .show();
    }
	
	public static void goToSettings(AppCompatActivity appCompatActivity) {
	    
		Intent intent = new Intent();
	    
		intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
	 
	    Uri uri = Uri.parse("package:" + appCompatActivity.getPackageName());
	    
	    intent.setData(uri);
	    
	    appCompatActivity.startActivity(intent);
	}
		
}