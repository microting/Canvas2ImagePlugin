package org.devgeeks.Canvas2ImagePlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;


/**
 * Canvas2ImagePlugin.java
 *
 * Android implementation of the Canvas2ImagePlugin for iOS.
 * Inspirated by Joseph's "Save HTML5 Canvas Image to Gallery" plugin
 * http://jbkflex.wordpress.com/2013/06/19/save-html5-canvas-image-to-gallery-phonegap-android-plugin/
 *
 * @author Vegard Løkken <vegard@headspin.no>
 */
public class Canvas2ImagePlugin extends CordovaPlugin {
	public static final String ACTION = "saveImageDataToLibrary";
    private final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final int WRITE_PERM_REQUEST_CODE = 1;
    private CallbackContext callbackContext;
    private Bitmap bmp;
		private String extension;
		private Stirng strQuality;
		private String picFolder;

	@Override
	public boolean execute(String action, JSONArray data,
			CallbackContext callbackContext) throws JSONException {

		if (action.equals(ACTION)) {

			String base64 = data.optString(0);
			this.extension = data.optString(1);
			this.strQuality = data.optString(2);
			this.picfolder = Environment.DIRECTORY_PICTURES;
			boolean add2Galery = true; // Why is this here, it's never used?
			if (data.length() > 3) picfolder = data.optString(3);
			if (data.length() > 4) add2Galery = Boolean.valueOf(data.optString(4)); // Why is this here, it's never used?
			if (base64.equals("")) // isEmpty() requires API level 9
				callbackContext.error("Missing base64 string");

			// Create the bitmap from the base64 string
			Log.d("Canvas2ImagePlugin", base64);
			byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
			Bitmap bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
			if (bmp == null) {
				callbackContext.error("The image could not be decoded");
			} else {

                this.bmp = bmp;
                this.callbackContext = callbackContext;
				// Save the image
				askPermissionAndSave();

			}

			return true;
		} else {
			return false;
		}
	}

  private void askPermissionAndSave() {

		if (PermissionHelper.hasPermission(this, WRITE_EXTERNAL_STORAGE)) {
    	Log.d("SaveImage", "Permissions already granted, or Android version is lower than 6");
			savePhoto();
		} else {
			Log.d("SaveImage", "Requesting permissions for WRITE_EXTERNAL_STORAGE");
			PermissionHelper.requestPermission(this, WRITE_PERM_REQUEST_CODE, WRITE_EXTERNAL_STORAGE);
		}
	}
	
	private int getQuality(String strQuality){
		int result=100;
		try {
		    result=Integer.valueOf(strQuality);
		    if (result> 100) result=100;
		    if (result < 1) result=1;
		} catch (Exception e){}		
		return result;
	}

	private void savePhoto() {
    int quality = getQuality(this.strQuality);        
		File image = null;
        
        Bitmap bmp = this.bmp;
        CallbackContext callbackContext = this.callbackContext;

		try {
			Calendar c = Calendar.getInstance();
			String date = "" + c.get(Calendar.DAY_OF_MONTH)
					+ c.get(Calendar.MONTH)
					+ c.get(Calendar.YEAR)
					+ c.get(Calendar.HOUR_OF_DAY)
					+ c.get(Calendar.MINUTE)
					+ c.get(Calendar.SECOND)
					+ c.get(Calendar.MILLISECOND);

			String deviceVersion = Build.VERSION.RELEASE;
			Log.i("Canvas2ImagePlugin", "Android version " + deviceVersion);
			int check = deviceVersion.compareTo("2.3.3");

			File folder;
			/*
			 * File path = Environment.getExternalStoragePublicDirectory(
			 * Environment.DIRECTORY_PICTURES ); //this throws error in Android
			 * 2.2
			 */
			if (check >= 1) {
				
				folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

				if (picfolder != folder) {
					folder = new File(picfolder);
				}				

				if(!folder.exists()) {
					folder.mkdirs();
				}
			} else {
				folder = Environment.getExternalStorageDirectory();
			}

			File imageFile = new File(folder, "c2i_" + date.toString() + extension);

			CompressFormat compressFormat = (extension.equals(".jpg")) ? Bitmap.CompressFormat.JPEG :Bitmap.CompressFormat.PNG;
			FileOutputStream out = new FileOutputStream(imageFile);			
			bmp.compress(compressFormat, quality, out);
			out.flush();
			out.close();

			image = imageFile;
		} catch (Exception e) {
			Log.e("Canvas2ImagePlugin", "An exception occured while saving image: "
					+ e.toString());
		}

		if (image == null) {
			callbackContext.error("Error while saving image");
		} else {
			// Update image gallery
			scanPhoto(image);
			callbackContext.success(image.toString());
		}


	}

	/* Invoke the system's media scanner to add your photo to the Media Provider's database,
	 * making it available in the Android Gallery application and to other apps. */
	private void scanPhoto(File imageFile)
	{
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    Uri contentUri = Uri.fromFile(imageFile);
	    mediaScanIntent.setData(contentUri);
	    cordova.getActivity().sendBroadcast(mediaScanIntent);
	}

    /**
     * Callback from PermissionHelper.requestPermission method
     */
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d("SaveImage", "Permission not granted by the user");
                callbackContext.error("Permissions denied");
                return;
            }
        }

        switch (requestCode) {
            case WRITE_PERM_REQUEST_CODE:
                Log.d("SaveImage", "User granted the permission for WRITE_EXTERNAL_STORAGE");
				savePhoto();
                break;
        }
    }
}
