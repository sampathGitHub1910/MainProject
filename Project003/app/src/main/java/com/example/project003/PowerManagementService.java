package com.example.project003;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class PowerManagementService extends Service {

    private static final String TAG = "PowerManagementService";
    private boolean isVehicleParked = false;

    private Handler handler;
    private Runnable checkParkedStatusRunnable = new Runnable() {
        @Override
        public void run() {
            // Simulate checking if vehicle is parked (e.g., based on GPS or sensor data)
            boolean vehicleParked = checkVehicleParkedStatus();

            // If parked status changes, update and manage power accordingly
            if (vehicleParked != isVehicleParked) {
                isVehicleParked = vehicleParked;
                managePowerBasedOnParkedStatus();
            }

            // Schedule next check after a delay (e.g., 5 minutes)
            handler.postDelayed(this, 5 * 60 * 1000); // 5 minutes in milliseconds
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PowerManagementService onCreate");
        handler = new Handler();
        handler.post(checkParkedStatusRunnable); // Start periodic status checks
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "PowerManagementService onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PowerManagementService onDestroy");
        handler.removeCallbacks(checkParkedStatusRunnable); // Stop periodic checks
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean checkVehicleParkedStatus() {

        return Math.random() < 0.5; // Simulate 50% chance of being parked
    }

    private void managePowerBasedOnParkedStatus() {
        if (isVehicleParked) {

            Log.d(TAG, "Vehicle is parked. Implementing power saving measures.");

        } else {
            Log.d(TAG, "Vehicle is not parked.");
        }
    }
}