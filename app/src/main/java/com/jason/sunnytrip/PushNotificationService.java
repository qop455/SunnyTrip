package com.jason.sunnytrip;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by jason on 16/2/1.
 */
public class PushNotificationService extends GcmListenerService {
    private static final String TAG = "PushNotificationService";
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        Log.d(TAG,"onMessageReceived:"+message);
    }
}