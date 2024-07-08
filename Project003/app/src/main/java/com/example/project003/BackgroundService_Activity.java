package com.example.project003;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BackgroundService_Activity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button startServiceButton;
    private Button stopServiceButton;
    private TextView serviceStatusTextView;

    private MyService myService;
    private boolean isServiceBound;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            myService = binder.getService();
            isServiceBound = true;
            updateServiceStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power);

        startServiceButton = findViewById(R.id.start_service_button);
        stopServiceButton = findViewById(R.id.stop_service_button);
        serviceStatusTextView = findViewById(R.id.service_status_textview);

        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMyService();
            }
        });

        stopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMyService();
            }
        });
    }

    private void startMyService() {
        Intent serviceIntent = new Intent(this, MyService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        updateServiceStatus(); // Update status immediately after starting service
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
    }

    private void stopMyService() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        Intent serviceIntent = new Intent(this, MyService.class);
        stopService(serviceIntent);
        serviceStatusTextView.setText("Service stopped");
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateServiceStatus() {
        if (isServiceBound && myService != null && myService.isServiceRunning()) {
            serviceStatusTextView.setText("Service is running");
        } else {
            serviceStatusTextView.setText("Service is not running");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}

