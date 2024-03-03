package com.project.dehazing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;

import org.opencv.core.Mat;


public class DehazeLive extends AppCompatActivity implements CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dehaze_live);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mOpenCvCameraView = findViewById(R.id.CameraView);
        if (ContextCompat.checkSelfPermission(DehazeLive.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DehazeLive.this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            startCamera();
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

    }
    @Override
    public void onCameraViewStopped() {
    }


    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        final double kernelRatio = 0.01; // set kernel ratio
        final double minAtmosLight = 240.0; // set minimum atmospheric light
        final double eps = 0.000001;
        Mat dehazed = DarkChannelPrior.enhance(frame, kernelRatio, minAtmosLight, eps);

        return dehazed;
    }

}