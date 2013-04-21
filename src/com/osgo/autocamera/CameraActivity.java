package com.osgo.autocamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
public class CameraActivity extends Activity {

	protected Camera mCamera;
	protected CameraPreview mPreview;
	protected MediaRecorder mMediaRecorder;
	protected Picturetask takePictures;
	protected SharedPreferences prefs;
	protected Context mContext;
	protected FrameLayout preview;

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	public static final String PREFS = "CAMERA_APP";
	private static final String BROADCAST_PREVIEW = "refresh";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		prefs = getSharedPreferences(PREFS, 0);
		long timeout = prefs.getLong("TIMEOUT", 5);

		// Create an instance of Camera
		if(checkCameraHardware(this)){
			mCamera = getCameraInstance();
		}

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		// Add a listener to the Capture button
		final Button captureButton = (Button) findViewById(R.id.button_capture);
		captureButton.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// get an image from the camera
				takePictures.execute(null);
				captureButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// get an image from the camera
						takePictures.cancelPicture();
						captureButton.setOnClickListener(null);
					}
				});
			}
		});
		
		takePictures = new Picturetask(timeout);
		
	}
	
	protected void resetPreview(){
		mCamera.stopPreview();
		mPreview = new CameraPreview(this, mCamera);
		preview.addView(mPreview);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		takePictures.cancelPicture();
//		takePictures.cancel(true);
		releaseMediaRecorder();       // if you are using MediaRecorder, release it first
		releaseCamera();              // release the camera immediately on pause event
		
	}
	
	@Override
	public void onStart() {
        super.onStart();
        registerReceiver(refreshPreview, new IntentFilter(BROADCAST_PREVIEW));
    }

	protected void releaseMediaRecorder(){
		if (mMediaRecorder != null) {
			mMediaRecorder.reset();   // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			mCamera.lock();           // lock camera for later use
		}
	}

	protected void releaseCamera(){
		if (mCamera != null){
			mCamera.release();        // release the camera for other applications
			mCamera = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/** Check if this device has a camera */
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(){
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		}
		catch (Exception e){
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	protected PictureCallback mPicture = new PictureCallback() {

		private String TAG = "PictureCallBack";

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			if (pictureFile == null){
				Log.d(TAG, "Error creating media file, check storage permissions: ");
				return;
			}

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
				
				// Tell the media scanner about the new file so that it is
		        // immediately available to the user.
				MediaScannerConnection.scanFile(mContext,
						new String[] { pictureFile.toString() }, null,
						new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
						Log.i("ExternalStorage", "Scanned " + path + ":");
						Log.i("ExternalStorage", "-> uri=" + uri);
					}
				});
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
		}
	};
	
	private class Picturetask extends AsyncTask<String, Void, Boolean> {

		private long timeout;
		private volatile boolean wait = true; 
		
		public Picturetask(long timeout){
			
			this.timeout = 1000 * timeout;
		}
		
		@Override
		protected Boolean doInBackground(String... arg0) {
			takePicture();
			return null;
		}
		
		private void takePicture(){
			
			while(wait){
				
/*				SurfaceView view = new SurfaceView(mContext);
				try {
					CameraActivity.this.mCamera.setPreviewDisplay(view.getHolder());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				CameraActivity.this.mCamera.startPreview();*/
				
				CameraActivity.this.mCamera.takePicture(null, null, mPicture);

				Intent i = new Intent(BROADCAST_PREVIEW);
				sendBroadcast(i);
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}
		}	
		
		public void cancelPicture(){
			this.wait = false;
		}
	}

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
		return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "CameraApp");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"IMG_"+ timeStamp + ".jpg");
		} else if(type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"VID_"+ timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}
	
	private BroadcastReceiver refreshPreview = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
//			mPreview = new CameraPreview(mContext, mCamera);
//			preview = (FrameLayout) findViewById(R.id.camera_preview);
//			preview.addView(mPreview);
			
/*			mPreview = new CameraPreview(mContext, mCamera);*/
			mPreview.surfaceChanged(null, 0, 0, 0);
	/*		preview.removeAllViews();
			preview.addView(mPreview);*/
//				mCamera.setPreviewDisplay(mPreview.getHolder());
//				mCamera.startPreview();
			
		}
	};

}
