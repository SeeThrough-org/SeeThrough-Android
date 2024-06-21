package com.seethrough.dehazing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DehazeLive extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {
    private static final String TAG = "DehazeLive";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba, mCapturedFrame;
    private String orientationString = "Unknown";
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private final float[] gravity = new float[3];
    private Spinner resolutionSpinner;
    private TextView debugTextView;
    private SwitchCompat switchButton;

    private long lastFrameTime = System.currentTimeMillis();
    private final float[] tempGravity = new float[3];
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dehaze_live);
        setupUI();
        setupSensors();
        checkCameraPermission();
    }

    private void setupUI() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        mOpenCvCameraView = findViewById(R.id.CameraView);
        resolutionSpinner = findViewById(R.id.resolution_spinner);
        debugTextView = findViewById(R.id.debug_text);
        switchButton = findViewById(R.id.activate_dcp);
        findViewById(R.id.capture_Live).setOnClickListener(v -> saveImage());
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.enableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, tempGravity, 0, 3);
            final float alpha = 0.8f;
            for (int i = 0; i < 3; i++) {
                gravity[i] = alpha * gravity[i] + (1 - alpha) * tempGravity[i];
            }

            float x = gravity[0];
            float y = gravity[1];
            float z = gravity[2];

            if (Math.abs(x) > Math.abs(y) && Math.abs(x) > Math.abs(z)) {
                orientationString = x > 0 ? "Reverse Landscape" : "Landscape";
            } else if (Math.abs(y) > Math.abs(x) && Math.abs(y) > Math.abs(z)) {
                orientationString = y > 0 ? "Portrait" : "Reverse Portrait";
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this example
    }

    private void saveImage() {
        if (mCapturedFrame == null) {
            Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = System.currentTimeMillis() + "_Dehazed.png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Dehazed");
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(this, "Failed to create new MediaStore record", Toast.LENGTH_SHORT).show();
                return;
            }

            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    Toast.makeText(this, "Failed to open output stream", Toast.LENGTH_SHORT).show();
                    return;
                }

                Bitmap bitmap = Bitmap.createBitmap(mCapturedFrame.cols(), mCapturedFrame.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mCapturedFrame, bitmap);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                bitmap.recycle();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            }

            Toast.makeText(this, "Image saved successfully in Pictures/Dehazed folder", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error saving image", e);
            Toast.makeText(this, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.setCameraPermissionGranted();
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
            setupResolutionSpinner();
        }
    }

    private void setupResolutionSpinner() {
        List<Size> supportedResolutions = new ArrayList<>();
        supportedResolutions.add(new Size(640, 360));
        supportedResolutions.add(new Size(800, 480));
        supportedResolutions.add(new Size(1280, 720));
        supportedResolutions.add(new Size(1920, 1080));

        List<String> resolutionList = new ArrayList<>();
        for (Size resolution : supportedResolutions) {
            resolutionList.add((int) resolution.height + "p");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resolutionList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionSpinner.setAdapter(adapter);
        resolutionSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                changeCameraResolution(supportedResolutions.get(position));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void changeCameraResolution(Size resolution) {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.setMaxFrameSize((int) resolution.width, (int) resolution.height);
            mOpenCvCameraView.enableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        if (mRgba != null) {
            mRgba.release();
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (switchButton.isChecked()) {
            mRgba = DarkChannelPrior.enhance(mRgba);
        }

        mCapturedFrame = mRgba.clone();
        rotateImageIfNeeded();

        updateDebugInfo();

        return mRgba;
    }

    private void rotateImageIfNeeded() {
        switch (orientationString) {
            case "Landscape":
                Core.rotate(mCapturedFrame, mCapturedFrame, Core.ROTATE_90_CLOCKWISE);
                break;
            case "Reverse Portrait":
                //Core.rotate(mCapturedFrame, mCapturedFrame, Core.ROTATE_180);
                break;
            case "Reverse Landscape":
                Core.rotate(mCapturedFrame, mCapturedFrame, Core.ROTATE_90_COUNTERCLOCKWISE);
                break;
        }
    }

    private void updateDebugInfo() {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastFrameTime;
        lastFrameTime = currentTime;

        int fps = timeDifference > 0 ? (int) (1000 / timeDifference) : 0;

        final String debugData = String.format("FPS: %d", fps);
        runOnUiThread(() -> debugTextView.setText(debugData));
    }
}