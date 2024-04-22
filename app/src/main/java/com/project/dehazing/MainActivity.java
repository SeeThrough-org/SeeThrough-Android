package com.seethrough.dehazing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] CAMERA_PERMISSION = {Manifest.permission.CAMERA};

    static {
        OpenCVLoader.initLocal();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViewById(R.id.button_live).setOnClickListener(v -> checkPermissionAndStartActivity(CAMERA_PERMISSION, DehazeLive.class));
        findViewById(R.id.button_image).setOnClickListener(v -> startActivity(new Intent(this, DehazeImage.class)));

        updatePermissionStatusText();
    }

    private void checkPermissionAndStartActivity(String[] permissions, Class<?> activityClass) {
        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            startActivity(new Intent(this, activityClass));
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void updatePermissionStatusText() {
        TextView permissionStatus = findViewById(R.id.permission_status);
        permissionStatus.setText("");

        appendPermissionStatus(permissionStatus, CAMERA_PERMISSION, "Camera");
    }

    private void appendPermissionStatus(TextView textView, String[] permissions, String permissionType) {
        textView.append(permissionType + ": " + (hasPermissions(permissions) ? "Granted" : "Denied") + "\n");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionStatusText();
        }
    }
}