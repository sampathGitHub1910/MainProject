package com.example.project002;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, SensorEventListener {

    private static final String TAG = "MainActivity";
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView capturedImageView;
    private boolean previewing = false;
    private byte[] previousFrame;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int MOTION_THRESHOLD = 15000; // Adjust as necessary
    private static final int ACCELERATION_THRESHOLD = 10; // Adjust as necessary

    // Sensor variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isAccelerometerAvailable = false;

    // Handler for delayed photo capture
    private Handler handler = new Handler();
    private boolean isCapturing = false;
    private Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (camera != null && isCapturing) {
                try {
                    camera.takePicture(null, null, pictureCallback);
                } catch (Exception e) {
                    Log.e(TAG, "Error taking picture: " + e.getMessage());
                    e.printStackTrace();
                }
                handler.postDelayed(this, 2000); // Capture every 2 seconds
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.cameraPreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        capturedImageView = findViewById(R.id.capturedImageView);

        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera != null) {
                    try {
                        camera.takePicture(null, null, pictureCallback);
                    } catch (Exception e) {
                        Log.e(TAG, "Error taking picture: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });

        // Initialize sensor manager and accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            isAccelerometerAvailable = accelerometer != null;
        }

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register accelerometer sensor listener
        if (isAccelerometerAvailable) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister accelerometer sensor listener and release camera
        if (isAccelerometerAvailable) {
            sensorManager.unregisterListener(this);
        }
        releaseCamera();
        handler.removeCallbacks(captureRunnable);
        isCapturing = false;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (camera == null) {
            openCamera();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (previewing) {
            try {
                camera.stopPreview();
                previewing = false;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping camera preview: " + e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(this);
            camera.startPreview();
            previewing = true;
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
                previewing = false;
            } catch (Exception e) {
                Log.e(TAG, "Error releasing camera: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            camera = Camera.open();
            Camera.Parameters params = camera.getParameters();
            List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
            Camera.Size previewSize = getOptimalPreviewSize(previewSizes, surfaceView.getWidth(), surfaceView.getHeight());
            if (previewSize != null) {
                params.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(params);
            } else {
                Log.e(TAG, "Error: No suitable preview size found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap != null) {
                capturedImageView.setImageBitmap(bitmap);
                capturedImageView.setVisibility(View.VISIBLE);
                saveCompressedImage(bitmap);
            }
            camera.startPreview(); // Restart preview for continuous capture
        }
    };

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (previousFrame != null) {
            int diff = calculateFrameDifference(data, previousFrame);
            if (diff > MOTION_THRESHOLD) {
                Log.d(TAG, "Motion detected. Starting capture.");
                startCapturing();
            }
        }
        previousFrame = data.clone();
    }

    private int calculateFrameDifference(byte[] currentFrame, byte[] previousFrame) {
        int diff = 0;
        for (int i = 0; i < currentFrame.length; i++) {
            diff += Math.abs((currentFrame[i] & 0xFF) - (previousFrame[i] & 0xFF));
        }
        return diff;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Detect vibration based on accelerometer data
            float[] values = event.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z);

            // Adjust threshold as per your testing and requirements
            if (acceleration > ACCELERATION_THRESHOLD) {
                Log.d(TAG, "Vibration detected. Starting capture.");
                startCapturing();
            }
        }
    }

    private void startCapturing() {
        if (!isCapturing) {
            isCapturing = true;
            handler.post(captureRunnable);
        }
    }

    private void saveCompressedImage(Bitmap bitmap) {
        FileOutputStream fos = null;
        try {
            File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "AutoSurveillance");
            if (!storageDir.exists()) {
                if (!storageDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory");
                    return;
                }
            }
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            File file = new File(storageDir, fileName);
            fos = new FileOutputStream(file);

            // Compress the image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] compressedData = baos.toByteArray();

            fos.write(compressedData);
            Log.d(TAG, "Image saved: " + file.getAbsolutePath());

            // Request media scan
            MediaScannerConnection.scanFile(MainActivity.this,
                    new String[]{file.getAbsolutePath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i(TAG, "Scanned " + path + ":");
                            Log.i(TAG, "-> uri=" + uri);
                        }
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing FileOutputStream: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }
}
