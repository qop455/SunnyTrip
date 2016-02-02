package com.jason.sunnytrip;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by jason on 16/2/1.
 */
public class PushNotificationService extends GcmListenerService {
    private static final String TAG = "PushNotificationService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "onMessageReceived");

        Intent intent = new Intent("GCM_EVENT");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        String title = data.getString("title");
        String text = data.getString("text");
        String subtext = data.getString("subtext");
        String ticker = data.getString("ticker");

        Log.d(TAG, "title:" + title);
        Log.d(TAG, "text:" + text);
        Log.d(TAG, "subtext:" + subtext);
        Log.d(TAG, "ticker:" + ticker);

        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.drawable.japanpic)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text)
                .setSubText(subtext)
                .setTicker(ticker)
                .setDefaults(Notification.DEFAULT_ALL);
        int pid = android.os.Process.myPid();

        notificationManager.notify(pid, builder.build());


    }
}