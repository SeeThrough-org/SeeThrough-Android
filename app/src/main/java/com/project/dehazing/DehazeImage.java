package com.seethrough.dehazing;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.OutputStream;

public class DehazeImage extends AppCompatActivity {

    private static final String TAG = "DehazeImage";
    private static final String IMAGE_MIME_TYPE = "image/png";
    private static final String[] ACCEPTED_MIME_TYPES = {"image/jpeg", "image/png"};

    private Uri selectedImageUri;
    private ImageView imagePreview;
    private boolean imageProcessed = false;
    private Bitmap dehazedBitmap;
    private SwitchCompat dehazeSwitch;

    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImagePickerResult
    );

    private static WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
        return insets;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dehaze_image);
        setupUI();
    }

    private void setupUI() {
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.DehazeImage), DehazeImage::onApplyWindowInsets);

        imagePreview = findViewById(R.id.ImagePreview);
        dehazeSwitch = findViewById(R.id.EnhanceImage);
        dehazeSwitch.setEnabled(false);

        findViewById(R.id.LoadImage).setOnClickListener(v -> loadImage());
        findViewById(R.id.SaveImage).setOnClickListener(v -> saveImage());
        dehazeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateImagePreview(isChecked)
        );
    }

    private void updateImagePreview(boolean isChecked) {
        if (isChecked) {
            if (!imageProcessed) {
                dehazeImage();
            } else {
                imagePreview.setImageBitmap(dehazedBitmap);
            }
        } else {
            showOriginalImage();
        }
    }

    private void loadImage() {
        try {
            imagePicker.launch("image/*");
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app can handle picking an image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleImagePickerResult(Uri uri) {
        if (uri != null) {
            selectedImageUri = uri;
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imagePreview.setImageBitmap(bitmap);
                imageProcessed = false;
                dehazeSwitch.setEnabled(true);
                dehazeSwitch.setChecked(false);
            } catch (IOException e) {
                Log.e(TAG, "Error loading image", e);
                dehazeSwitch.setEnabled(false);
            }
        } else {
            dehazeSwitch.setEnabled(false);
            dehazeSwitch.setChecked(false);
        }
    }

    private void showOriginalImage() {
        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imagePreview.setImageBitmap(bitmap);
                imageProcessed = false;
                dehazeSwitch.setEnabled(true);
            } catch (IOException e) {
                Log.e(TAG, "Error loading image", e);
                dehazeSwitch.setEnabled(false);
            }
        } else {
            dehazeSwitch.setEnabled(false);
        }
    }

    private void dehazeImage() {
        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                Mat inputMat = new Mat();
                Utils.bitmapToMat(bitmap.copy(Bitmap.Config.ARGB_8888, true), inputMat);
                Mat dehazedMat = DarkChannelPrior.enhance(inputMat);
                dehazedBitmap = Bitmap.createBitmap(dehazedMat.cols(), dehazedMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(dehazedMat, dehazedBitmap);
                imagePreview.setImageBitmap(dehazedBitmap);
                imageProcessed = true;
            } catch (IOException e) {
                Log.e(TAG, "Error dehazing image", e);
            }
        }
    }

    private void saveImage() {
        if (selectedImageUri != null && dehazedBitmap != null) {
            try {
                String newFileName = System.currentTimeMillis() + "_Dehazed.png";
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, IMAGE_MIME_TYPE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Dehazed");
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
                }

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (uri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        dehazedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear();
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        getContentResolver().update(uri, contentValues, null, null);
                    }
                    Toast.makeText(this, "Image saved successfully in the Pictures folder", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to save Image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving image", e);
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
            }
        }
    }
}