package com.seethrough.dehazing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import java.io.OutputStream;


public class DehazeLive extends AppCompatActivity implements CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private long lastFrameTime = System.currentTimeMillis();
    private Mat mRgba;

    private Mat mCapturedFrame;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dehaze_live);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mOpenCvCameraView =  findViewById(R.id.CameraView);
        if (ContextCompat.checkSelfPermission(DehazeLive.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DehazeLive.this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            startCamera();
        }
        findViewById(R.id.capture_Live).setOnClickListener(v ->
                saveImage());
    }
    private void saveImage() {
        if (mCapturedFrame != null) {
            try {
                String newFileName = System.currentTimeMillis() + "_Dehazed.png";
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Dehazed");
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
                }

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (uri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        if (outputStream != null) {
                            Bitmap bitmap = Bitmap.createBitmap(mCapturedFrame.cols(), mCapturedFrame.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(mCapturedFrame, bitmap);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                            bitmap.recycle();
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear();
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        getContentResolver().update(uri, contentValues, null, null);
                    }

                    Toast.makeText(this, "Image saved successfully in the Pictures/Dehazed folder", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("DehazeImage", "Error saving image", e);
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void startCamera() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.setCameraPermissionGranted();
            mOpenCvCameraView.setMaxFrameSize(856, 480); // Set the maximum frame size of the camera view
            mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
    }
    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Size resolution = mRgba.size();
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastFrameTime;
        lastFrameTime = currentTime;

        int fps = timeDifference > 0 ? (int) (1000 / timeDifference) : 0;

        String debugData = String.format("FPS: %d Resolution:%.1fx%.1f", fps, resolution.width, resolution.height);

        runOnUiThread(() -> {
            TextView debugTextView = findViewById(R.id.debug_text);
            debugTextView.setText(debugData);
        });

        SwitchCompat switchButton = findViewById(R.id.activate_dcp);
        boolean dcpActivated = switchButton.isChecked();

        if (dcpActivated) {
            final double kernelRatio = 0.01;
            final double minAtmosLight = 240.0;
            mRgba = DarkChannelPrior.enhance(mRgba, kernelRatio, minAtmosLight);
        }
        mCapturedFrame = mRgba.clone();
        Core.flip(mCapturedFrame.t(), mCapturedFrame, 1);
        return mRgba;
    }

}