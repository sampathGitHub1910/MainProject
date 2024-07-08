package com.example.project001;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;

public class MotionDetector implements Camera.PreviewCallback {

    public void start(SurfaceView surfaceView) {

    }

    public interface MotionDetectionListener {
        void onMotionDetected();
    }

    private static final String TAG = "MotionDetector";
    private static final int MOTION_THRESHOLD = 10000; // Adjust threshold as needed
    private MotionDetectionListener listener;

    private int[] previousFrame;
    private int previewWidth;
    private int previewHeight;

    public void setMotionDetectionListener(MotionDetectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (listener == null) return;

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();

        if (previousFrame == null) {
            previewWidth = size.width;
            previewHeight = size.height;
            previousFrame = new int[previewWidth * previewHeight];
        }

        int[] currentFrame = convertYUV420SPtoRGB(data, previewWidth, previewHeight);

        if (previousFrame != null) {
            int diff = frameDifference(previousFrame, currentFrame);
            Log.d(TAG, "Motion detected, difference: " + diff);
            if (diff > MOTION_THRESHOLD) {
                listener.onMotionDetected();
            }
        }

        System.arraycopy(currentFrame, 0, previousFrame, 0, currentFrame.length);
    }

    private int[] convertYUV420SPtoRGB(byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int[] rgb = new int[frameSize];

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }

        return rgb;
    }

    private int frameDifference(int[] previousFrame, int[] currentFrame) {
        int difference = 0;
        for (int i = 0; i < previousFrame.length; i++) {
            difference += Math.abs(previousFrame[i] - currentFrame[i]);
        }
        return difference;
    }
}