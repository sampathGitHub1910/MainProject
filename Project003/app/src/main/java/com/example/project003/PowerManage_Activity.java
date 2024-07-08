package com.example.project003;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PowerManage_Activity extends AppCompatActivity {

    private Button startServiceButton;
    private Button stopServiceButton;
    private TextView statusText;
    private TextView statusTextView;
    private Handler handler = new Handler();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power);

        startServiceButton = findViewById(R.id.start_service_button);
        stopServiceButton = findViewById(R.id.stop_service_button);
        statusText= findViewById(R.id.text_view);
        statusTextView = findViewById(R.id.status_text_view);

        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPowerManagementService();
            }
        });

        stopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPowerManagementService();
            }
        });
    }

    private void startPowerManagementService() {
        Intent serviceIntent = new Intent(this, PowerManagementService.class);
        startService(serviceIntent);

        statusText.setText("Power Management Active");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                statusTextView.setText("Minimize the battery usage");
            }
        }, 2000); // 2000 milliseconds = 2 seconds
    }
    private void stopPowerManagementService() {
        Intent serviceIntent = new Intent(this, PowerManagementService.class);
        stopService(serviceIntent);

        statusText.setText("Power Management Inactive");
        statusTextView.setText("");
    }

}