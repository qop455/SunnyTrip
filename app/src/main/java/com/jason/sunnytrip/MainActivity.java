package com.jason.sunnytrip;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    public static final int RATE_STRING = 1;
    public static final int TOKEN_RESPONSE = 2;
    public static final int COUNTRY_SWITCH = 3;
    private static final int JAPAN = 11;
    private static final int CHINA = 12;
    private static final int AMERICA = 13;
    private static final String TAG = "MainActivity";
    private static final String PROJECT_NUMBER = "947587828981";
    public int xLast, yLast, xC, yC;
    WindowManager windowManager = null;
    LinearLayout linearLayout;
    TextView textView;
    ImageView imageView;
    WindowManager.LayoutParams layoutParams = null;
    private int country = JAPAN;
    private boolean isMoving = false;
    private boolean isFirst = true;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RATE_STRING:
                    try {
                        JSONObject jsonObject = null;
                        jsonObject = new JSONObject(msg.getData().getString("result"));
                        String mTo = jsonObject.getString("to");
                        String mRate = jsonObject.getString("rate");
                        String mFrom = jsonObject.getString("from");
                        Log.d(TAG, "To: " + mTo);
                        Log.d(TAG, ",Rate: " + mRate);
                        Log.d(TAG, ",From: " + mFrom);
                        textView.setText(mRate);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case TOKEN_RESPONSE:
                    textView.setText(msg.getData().getString("response"));
                    break;
                case COUNTRY_SWITCH:
                    textView.setText("waiting..");
                    switch (msg.getData().getInt("country")) {
                        case JAPAN:
                            imageView.setImageResource(R.drawable.japanpic);
                            break;
                        case CHINA:
                            imageView.setImageResource(R.drawable.chinapic);
                            break;
                        case AMERICA:
                            imageView.setImageResource(R.drawable.americapic);
                            break;
                    }
                    break;
            }
        }
    };
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            switch (intent.getStringExtra("country")){
                case "JAPAN":
                    setCountry(JAPAN);
                    break;
                case "CHINA":
                    setCountry(CHINA);
                    break;
                case "AMERICA":
                    setCountry(AMERICA);
                    break;
            }
        }
    };

    private void setCountry(int c) {
        if (c != JAPAN && c != CHINA && c != AMERICA) {
            Log.d(TAG, "setCountry failed with country undefined. ('" + c + "')");
            return;
        }
        this.country = c;

        Bundle bundle = new Bundle();
        bundle.putInt("country", country);

        Message msg = new Message();
        msg.what = COUNTRY_SWITCH;
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        new HttpThread().start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if (windowManager == null) {
            createView();
        }
        if (textView != null) {
            HttpThread httpThread = new HttpThread();
            httpThread.start();
        }
        GCMClientManager pushClientManager = new GCMClientManager(this, PROJECT_NUMBER);
        pushClientManager.registerIfNeeded(new GCMClientManager.RegistrationCompletedHandler() {
            @Override
            public void onSuccess(String registrationId, boolean isNewRegistration) {
                if (!isNewRegistration) return;
                final String url = "http://140.113.72.19/GCM/add.php?token=" + registrationId;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String response = RequestHttp(url);
                        Bundle bundle = new Bundle();
                        bundle.putString("response", response);

                        Message msg = new Message();
                        msg.what = TOKEN_RESPONSE;
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }
                }).start();
                Log.d(TAG, "Registration id: " + registrationId);
            }

            @Override
            public void onFailure(String ex) {
                super.onFailure(ex);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("GCM_UPDATE"));
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    private void createView() {

        linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        linearLayout.setBackgroundColor(Color.RED);

        imageView = new ImageView(getApplicationContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        imageView.setImageResource(R.drawable.japanpic);
        imageView.getLayoutParams().height = 100;
        imageView.getLayoutParams().width = 140;
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    isMoving = true;
                    if (!isFirst) {
                        xC = xC + (int) event.getRawX() - xLast;
                        yC = yC + (int) event.getRawY() - yLast;
                        layoutParams.x = xC;
                        layoutParams.y = yC;
                        windowManager.updateViewLayout(linearLayout, layoutParams);
                    }
                    xLast = (int) event.getRawX();
                    yLast = (int) event.getRawY();
                    isFirst = false;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    isMoving = false;
                    isFirst = true;
                }
                return false;
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMoving) {
                    switch (country) {
                        case JAPAN:
                            setCountry(CHINA);
                            break;
                        case CHINA:
                            setCountry(AMERICA);
                            break;
                        case AMERICA:
                            setCountry(JAPAN);
                            break;
                    }
                }
            }
        });
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Todo
                if (!isMoving) {
                    if (windowManager != null) {
                        windowManager.removeView(linearLayout);
                        windowManager = null;
                    }
                    Log.d(TAG, "windowManager.removeView");
                    MainActivity.this.finish();
                    int pid = android.os.Process.myPid();
                    Log.d(TAG, "pid: " + pid + ", kill process.");
                    android.os.Process.killProcess(pid);
                }
                return true;
            }
        });

        textView = new TextView(getApplicationContext());
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        textView.setText("waiting..");
        textView.setTextSize(25);
        Typeface typeface = Typeface.createFromAsset(getAssets(),
                "fonts/orange-juice-2.0.ttf");
        textView.setTypeface(typeface);
        textView.setPadding(15, 0, 0, 0);
        linearLayout.addView(imageView);
        linearLayout.addView(textView);

        windowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER | Gravity.TOP;
        windowManager.addView(linearLayout, layoutParams);
    }

    public String RequestHttp(String url) {
        StringBuilder stringBuilder = new StringBuilder();
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            StatusLine statusLine = httpResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity httpEntity = httpResponse.getEntity();
                InputStream inputStream = httpEntity.getContent();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                    Log.d(TAG, "BufferedReader.readLine : " + line);
                }
                return stringBuilder.toString();
            } else if (statusCode >= 400) {
                Log.d(TAG, "Client Error, with status: " + statusCode);
                return "Client Error!";
            } else if (statusCode >= 500) {
                Log.d(TAG, "Server Error, with status: " + statusCode);
                return "Server Error!";
            } else {
                Log.d(TAG, "Error, with status: " + statusCode);
                return "Error!";
            }
        } catch (ClientProtocolException e) {
            Log.d(TAG, "Error:" + e);
        } catch (IOException e) {
            Log.d(TAG, "Error:" + e);
        }
        return "Exception!";
    }

    class HttpThread extends Thread {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            Log.d(TAG, "HttpThread");
            try {
                String url = "http://rate-exchange-1.appspot.com/currency?to=TWD&from=";
                if (country == JAPAN) {
                    url += "JPY";
                } else if (country == CHINA) {
                    url += "CNY";
                } else {
                    url += "USD";
                }
                String result = RequestHttp(url);
                Bundle bundle = new Bundle();
                bundle.putString("result", result);

                Message msg = new Message();
                msg.what = RATE_STRING;
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
