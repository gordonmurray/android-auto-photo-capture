package com.osgo.autocamera;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

public class TakePictureService extends Service {
    private static final String PREFS_NAME = "VerifiPrefs";
    private SharedPreferences settings;
    private String API_KEY;
    private String API;
    private ApiService api;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	
    	Bundle extras = intent.getExtras();
    	
    	String latLang = extras.getString("latLang");
    	settings = getSharedPreferences(PREFS_NAME, 0);
        API_KEY = settings.getString("API_KEY", "");
        API = settings.getString("API", "");
    	api = new ApiService(API_KEY,API, this);
    	
    	new AlertTask().execute(latLang);
    	
        return Service.START_FLAG_REDELIVERY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @SuppressWarnings("unused")
	private class AlertTask extends AsyncTask<String, Void, Boolean> {
        private static final String DEBUG_TAG = "SafetyAlertService$AlertTask";
        @Override
        protected Boolean doInBackground(String... params) {
        	
        	
        	return false;
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
        	// TODO Auto-generated method stub
        	super.onPostExecute(result);
        	SharedPreferences.Editor editor = settings.edit();
        	editor.putBoolean("sendAlert", false);
        	editor.commit();
        }
    }
}
