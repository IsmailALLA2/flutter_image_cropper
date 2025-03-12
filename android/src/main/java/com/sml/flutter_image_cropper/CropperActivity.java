package com.sml.flutter_image_cropper;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import me.pqpo.smartcropperlib.SmartCropper;
import me.pqpo.smartcropperlib.view.CropImageView;

public class CropperActivity extends AppCompatActivity {

    private CropImageView ivCrop;
    private ImageView ivPreview;
    private Button btnCrop, btnDone, btnReset, btnRotate, btnCancel;
    private LinearLayout preCropActions, postCropActions;
    private Bitmap originalBitmap;
    private Bitmap croppedBitmap;
    private int rotationAngle = 0;
    private boolean isCropped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SmartCropper.buildImageDetector(this);

        // Set the activity to fullscreen
        setFullScreenMode();

        setContentView(R.layout.activity_cropper);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        ivCrop = findViewById(R.id.iv_crop);
        ivPreview = findViewById(R.id.iv_preview);
        btnCrop = findViewById(R.id.btn_crop);
        btnDone = findViewById(R.id.btn_done);
        btnReset = findViewById(R.id.btn_reset);
        btnRotate = findViewById(R.id.btn_rotate);
        btnCancel = findViewById(R.id.btn_cancel);
        preCropActions = findViewById(R.id.pre_crop_actions);
        postCropActions = findViewById(R.id.post_crop_actions);

        // Set background colors to ensure no white backgrounds
        ivCrop.setBackgroundColor(android.graphics.Color.BLACK);
        ivPreview.setBackgroundColor(android.graphics.Color.BLACK);

        // Get image path from intent
        String imagePath = getIntent().getStringExtra("imagePath");
        if (imagePath == null) {
            Toast.makeText(this, "No image path provided", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Load the image from path - handle both file paths and content URIs
        try {
            Uri imageUri;
            if (imagePath.startsWith("content://")) {
                // Already a content URI
                imageUri = Uri.parse(imagePath);
            } else {
                // File path
                imageUri = Uri.fromFile(new java.io.File(imagePath));
            }

            originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            if (originalBitmap == null) {
                throw new IOException("Failed to decode bitmap");
            }
            ivCrop.setImageToCrop(originalBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Initially hide the Done button in pre-crop mode
        btnDone.setVisibility(View.GONE);

        // Crop Image
        btnCrop.setOnClickListener(v -> {
            croppedBitmap = ivCrop.crop();
            if (croppedBitmap != null) {
                // Switch to post-crop UI
                switchToPostCropUI();
                Toast.makeText(CropperActivity.this, "Image cropped", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CropperActivity.this, "Failed to crop image", Toast.LENGTH_SHORT).show();
            }
        });

        // Rotate Image
        btnRotate.setOnClickListener(v -> {
            if (croppedBitmap != null) {
                rotationAngle = (rotationAngle + 90) % 360;
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                croppedBitmap = Bitmap.createBitmap(croppedBitmap, 0, 0,
                        croppedBitmap.getWidth(), croppedBitmap.getHeight(), matrix, true);
                ivPreview.setImageBitmap(croppedBitmap);
            }
        });

        // Done button - return the image path and finish
        btnDone.setOnClickListener(v -> {
            if (croppedBitmap != null) {
                // Save the image and get its path
                String resultPath = saveImageToGallery();

                // Return the result to the calling activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("imagePath", resultPath);
                setResult(RESULT_OK, resultIntent);

                // Finish this activity
                finish();
            }
        });

        // Reset to original cropping state
        btnReset.setOnClickListener(v -> {
            switchToPreCropUI();
        });

        // Cancel button (in the top bar)
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void setFullScreenMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void switchToPostCropUI() {
        // Hide crop container and show preview container
        findViewById(R.id.crop_container).setVisibility(View.GONE);
        findViewById(R.id.preview_container).setVisibility(View.VISIBLE);

        // Set the preview image
        ivPreview.setImageBitmap(croppedBitmap);

        // Hide pre-crop buttons and show post-crop buttons
        preCropActions.setVisibility(View.GONE);
        postCropActions.setVisibility(View.VISIBLE);

        // Show Done button in top bar
        btnDone.setVisibility(View.VISIBLE);

        // Change top left button text to "Back"
        btnCancel.setText("Back");

        // Set flag
        isCropped = true;
    }

    private void switchToPreCropUI() {
        // Show crop container and hide preview container
        findViewById(R.id.crop_container).setVisibility(View.VISIBLE);
        findViewById(R.id.preview_container).setVisibility(View.GONE);

        // Reset the cropper with the original image
        if (originalBitmap != null) {
            ivCrop.setImageToCrop(originalBitmap);
        }

        // Show pre-crop buttons and hide post-crop buttons
        preCropActions.setVisibility(View.VISIBLE);
        postCropActions.setVisibility(View.GONE);

        // Hide Done button in top bar
        btnDone.setVisibility(View.GONE);

        // Change top left button text back to "Cancel"
        btnCancel.setText("Cancel");

        // Reset state
        rotationAngle = 0;
        isCropped = false;
    }

    private String saveImageToGallery() {
        // Implementation to save the image to gallery and return its path
        if (croppedBitmap == null) {
            return null;
        }

        String title = "CroppedImage_" + System.currentTimeMillis();
        String description = "Image cropped by FlutterImageCropper";

        // Save the image and get its URI
        String imagePath = null;
        Uri imageUri = null;

        try {
            // Insert the image into the MediaStore
            imageUri = Uri.parse(MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    croppedBitmap,
                    title,
                    description));

            if (imageUri != null) {
                // Convert URI to file path
                imagePath = getRealPathFromURI(imageUri);
                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return imagePath;
    }

    // Helper method to get file path from URI
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);

        if (cursor == null) {
            return contentUri.getPath();
        }

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();

        return result;
    }

    @Override
    public void onBackPressed() {
        if (isCropped) {
            // If we're in post-crop mode, go back to pre-crop mode
            switchToPreCropUI();
        } else {
            // Otherwise, cancel and exit
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }
}
