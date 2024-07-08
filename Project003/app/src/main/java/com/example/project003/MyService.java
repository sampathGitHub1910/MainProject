package com.example.project003;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {

    private static final String TAG = "MyService";
    private boolean isServiceRunning = false;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        MyService getService() {

            return MyService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        isServiceRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        Log.d(TAG, "Service onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean isServiceRunning() {
        return isServiceRunning;
    }
}