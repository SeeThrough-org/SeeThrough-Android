package com.project.dehazing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 101;
    private static final String[] PERMISSION_EXTERNAL = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String[] PERMISSION_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE};

    static {
        OpenCVLoader.initDebug();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);


        Button liveButton = findViewById(R.id.button_live);
        Button imageButton = findViewById(R.id.button_image);

        liveButton.setEnabled(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        liveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DehazeLive.class));

            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DehazeImage.class));
            }
        });


        checkCameraPermission();
        checkStoragePermission();
    }

    private void checkCameraPermission() {
        TextView permission = findViewById(R.id.permission_status);
        // Check camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                // Permission denied, show rationale
                permission.append("\nCamera: Denied");
            } else {
                // Permission not yet requested, request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            }
        } else {
            // Permission already granted
            permission.append("\nCamera: Granted");
        }
    }

    private void checkStoragePermission() {
        TextView permission = findViewById(R.id.permission_status);
        // Check storage permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // For Android 10 and below
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Permission denied, show rationale
                    permission.append("\nStorage: Dnied");
                } else {
                    // Permission not yet requested, request it
                    ActivityCompat.requestPermissions(this,
                            PERMISSION_EXTERNAL,
                            STORAGE_PERMISSION_REQUEST_CODE);
                }
            } else {
                // Permission already granted
                permission.append("\nStorage: Granted");
            }
        } else {
            // For Android 11 and above
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
                    // Permission denied, show rationale
                    permission.append("\nStorage: Denied");
                } else {
                    // Permission not yet requested, request it
                    ActivityCompat.requestPermissions(this,
                            PERMISSION_STORAGE,
                            STORAGE_PERMISSION_REQUEST_CODE);
                }
            } else {
                // Permission already granted
                permission.append("\nStorage: Granted");
            }
        }
    }

    // Override onRequestPermissionsResult to handle permission request response
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
                TextView permission = findViewById(R.id.permission_status);
                permission.setText("Camera: Granted");
            } else {
                // Camera permission denied
                TextView permission = findViewById(R.id.permission_status);
                permission.setText("Camera: Denied");
            }
        }
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Storage permission granted
                TextView permission = findViewById(R.id.permission_status);
                permission.append("\nStorage: Granted");
            } else {
                // Storage permission denied
                TextView permission = findViewById(R.id.permission_status);
                permission.append("\nStorage: Denied");


            }
        }
    }
}
