package com.project.dehazing;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;

public class DehazeImage extends AppCompatActivity {

    private Uri selectedImageUri;
    private ImageView imagePreview;
    private boolean imageProcessed = false;
    private Bitmap dehazedBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dehaze_image);
        setupUI();
    }
    private void setupUI() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        imagePreview = findViewById(R.id.ImagePreview);
        Switch dehazeSwitch = findViewById(R.id.DehazeImage);
        findViewById(R.id.LoadImage).setOnClickListener(v -> loadImage());
        findViewById(R.id.SaveImage).setOnClickListener(v -> saveImage());
        dehazeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!imageProcessed) {
                    dehazeImage();
                } else {
                    imagePreview.setImageBitmap(dehazedBitmap);
                }
            } else {
                originalImage();
            }
        });
    }

    private final ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        imagePreview.setImageBitmap(bitmap);
                        imageProcessed = false;
                    } catch (IOException e) {
                        Log.e("DehazeImage", "Error loading image", e);
                    }
                }
            });


    private void originalImage(){
        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imagePreview.setImageBitmap(bitmap);
                imageProcessed = false;
            } catch (IOException e) {
                Log.e("DehazeImage", "Error loading image", e);
            }
        }
    }

    private void loadImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png"});
        try {
            if (intent.resolveActivity(getPackageManager()) != null) {
                imagePicker.launch(intent);
            } else {
                Toast.makeText(this,"No app can handle picking an image.", Toast.LENGTH_SHORT).show();
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,"No activity found to handle image picking.", Toast.LENGTH_SHORT).show();
        }
    }

    private void dehazeImage() {
        double kernel = 0.01;
        double minAtmosphericLight = 240.0;
        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                Mat inputMat = new Mat();
                Utils.bitmapToMat(bitmap.copy(Bitmap.Config.ARGB_8888, true), inputMat);
                Mat dehazedMat = DarkChannelPrior.enhance(inputMat, kernel, minAtmosphericLight);
                dehazedBitmap = ConvertMatToBitmap(dehazedMat);
                imagePreview.setImageBitmap(dehazedBitmap);
                imageProcessed = true;
            } catch (IOException e) {
                Log.e("DehazeImage", "Error dehazing image", e);
            }
        }
    }

    private Bitmap ConvertMatToBitmap(Mat inputMat) {
        Bitmap bitmap = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputMat, bitmap);
        return bitmap;
    }

    private void saveImage() {
        if (selectedImageUri != null) {
            try {
                String newFileName = System.currentTimeMillis() + "_Dehazed";
                Uri savedImageUri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), dehazedBitmap, newFileName, "Dehazed Image"));
                if (savedImageUri != null) {
                    Toast.makeText(this, "Image saved successfully at Pictures Folder", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to save Image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("DehazeImage", "Error saving image", e);
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
            }
        }
    }

}