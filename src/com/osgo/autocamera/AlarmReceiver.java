package com.osgo.autocamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String DEBUG_TAG = "AlarmReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(DEBUG_TAG, "Sending safety alert.");
        // start the download
        
        String latLang = intent.getExtras().getString("latLang");
        
        Intent safetyAlert = new Intent(context, TakePictureService.class);
        safetyAlert.putExtra("latLang", latLang);
        context.startService(safetyAlert);
    }
}
