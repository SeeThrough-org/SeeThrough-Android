package com.seethrough.dehazing;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] CAMERA_PERMISSION = {Manifest.permission.CAMERA};
    private static final String PREFS_NAME = "AppSettings";
    private static final String NIGHT_MODE_KEY = "NightMode";

    static {
        OpenCVLoader.initLocal();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupEdgeToEdge();
        setupButtons();
        setupThemeSwitch();
        updatePermissionStatusText();
        requestCameraPermissionIfNeeded();
    }

    private void setupEdgeToEdge() {
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupButtons() {
        findViewById(R.id.button_live).setOnClickListener(v -> checkPermissionAndStartActivity(CAMERA_PERMISSION, DehazeLive.class));
        findViewById(R.id.button_image).setOnClickListener(v -> startActivity(new Intent(this, DehazeImage.class)));
    }

    private void setupThemeSwitch() {
        SwitchCompat switchTheme = findViewById(R.id.switch_theme);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean(NIGHT_MODE_KEY, false);

        switchTheme.setChecked(isNightMode);
        updateNightMode(isNightMode);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNightMode(isChecked);
            prefs.edit().putBoolean(NIGHT_MODE_KEY, isChecked).apply();
        });
    }

    private void updateNightMode(boolean isNightMode) {
        AppCompatDelegate.setDefaultNightMode(isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void requestCameraPermissionIfNeeded() {
        if (!hasPermissions(CAMERA_PERMISSION)) {
            ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, PERMISSION_REQUEST_CODE);
        }
    }

    private void checkPermissionAndStartActivity(String[] permissions, Class<?> activityClass) {
        if (hasPermissions(permissions)) {
            startActivity(new Intent(this, activityClass));
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
            showPermissionRationale(permissions[0], activityClass);
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
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

    private void showPermissionRationale(String permission, Class<?> activityClass) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.camera_permission_rationale)
                .setPositiveButton(R.string.grant_permission, (dialog, which) ->
                        ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updatePermissionStatusText() {
        TextView permissionStatus = findViewById(R.id.permission_status);
        String status = getString(R.string.camera_permission_status,
                hasPermissions(CAMERA_PERMISSION) ? getString(R.string.granted) : getString(R.string.denied));
        permissionStatus.setText(status);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.permission_denied_message, Toast.LENGTH_SHORT).show();
            }
            updatePermissionStatusText();
        }
    }
}