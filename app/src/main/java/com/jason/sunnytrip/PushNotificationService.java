package com.jason.sunnytrip;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by jason on 16/2/1.
 */
public class PushNotificationService extends GcmListenerService {
    private static final String TAG = "PushNotificationService";
    @Override
    public void onMessageReceived(String from, Bundle data) {

        Intent intent = new Intent("my-event");
        intent.putExtras(data);
        Log.d(TAG, "onMessageReceived");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}