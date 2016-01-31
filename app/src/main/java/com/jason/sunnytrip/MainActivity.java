package com.jason.sunnytrip;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private static final int JSON_STRING = 1;
    private static final String URL = "http://rate-exchange-1.appspot.com/currency?from=JPY&to=TWD";
    private TextView textView;

    WindowManager windowManager = null;
    WindowManager.LayoutParams layoutParams = null;
    public int xLast;
    public int yLast;
    public int xC;
    public int yC;
    private boolean isMoving = false;
    private boolean isFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (windowManager == null) {
            createView();
        }
        if (textView != null) {
            HttpThread httpThread = new HttpThread();
            httpThread.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void createView() {
        textView = new TextView(getApplicationContext());
        textView.setText("waiting..");
        windowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        layoutParams.format = 1;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER | Gravity.TOP;
        windowManager.addView(textView, layoutParams);

        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    isMoving = true;
                    if (!isFirst) {
                        xC = xC + (int) event.getRawX() - xLast;
                        yC = yC + (int) event.getRawY() - yLast;
                        layoutParams.x = xC;
                        layoutParams.y = yC;
                        windowManager.updateViewLayout(textView, layoutParams);
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
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMoving) {
                    //do something here
                    textView.setText("waiting..");
                    new HttpThread().start();
                    Log.d("onClick","HttpThread.start");
                }
            }
        });
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Todo
                if (!isMoving) {
                    if (windowManager!=null){
                        windowManager.removeView(textView);
                        windowManager=null;
                    }
                    Log.d("onLongClick","windowManager.removeView");
                    MainActivity.this.finish();
                }
                return true;
            }
        });
    }

    class HttpThread extends Thread {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            try {
                String result = mRequestHttp(URL);
                Bundle bundle = new Bundle();
                bundle.putString("result", result);

                Message msg = new Message();
                msg.what = JSON_STRING;
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case JSON_STRING:
                    try {
                        JSONObject jsonObject = new JSONObject(msg.getData().getString("result"));
                        Log.d(MainActivity.class.getName(), jsonObject.getString("to"));
                        Log.d(MainActivity.class.getName(), jsonObject.getString("rate"));
                        Log.d(MainActivity.class.getName(), jsonObject.getString("from"));
                        String mTo = jsonObject.getString("to");
                        String mRate = jsonObject.getString("rate");
                        String mFrom = jsonObject.getString("from");

                        textView.setText(mRate);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    public String mRequestHttp(String url) {
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
                    Log.d("mRequestHttp", "BufferedReader.readLine : " + line);
                }
            } else if (statusCode >= 400) {
                Log.d("mRequestHttp", "Client Error, with status: " + statusCode);
            } else if (statusCode >= 500) {
                Log.d("mRequestHttp", "Server Error, with status: " + statusCode);
            } else {
                Log.d("mRequestHttp", "Error, with status: " + statusCode);
            }
        } catch (ClientProtocolException e) {
            Log.d("ClientProtocolException", "Error:" + e);
        } catch (IOException e) {
            Log.d("IOException", "Error:" + e);
        }
        return stringBuilder.toString();
    }
}
