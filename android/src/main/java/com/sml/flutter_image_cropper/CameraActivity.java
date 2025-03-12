package com.sml.flutter_image_cropper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final int CROP_IMAGE_REQUEST_CODE = 7890;

    private PreviewView previewView;
    private FrameLayout cameraContainer;
    private Button captureButton;
    private ImageButton flashButton;
    private Button closeButton;

    private CameraSelector cameraSelector;
    private ImageCapture imageCapture;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private Executor executor = Executors.newSingleThreadExecutor();

    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Hide action bar and set fullscreen
        setFullScreenMode();

        // Initialize views
        previewView = findViewById(R.id.preview_view);
        cameraContainer = findViewById(R.id.camera_container);
        captureButton = findViewById(R.id.capture_button);
        flashButton = findViewById(R.id.flash_button);
        closeButton = findViewById(R.id.close_button);

        // Always use back camera
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Set up button listeners
        captureButton.setOnClickListener(v -> captureImage());
        closeButton.setOnClickListener(v -> finish());
        flashButton.setOnClickListener(v -> toggleFlash());

        // Update flash button icon
        updateFlashButtonIcon();

        // Check and request camera permission
        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void setFullScreenMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }

        // Unbind previous use cases
        cameraProvider.unbindAll();

        // Set up preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Set up image capture with current flash mode
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()) // Set target rotation
                .build();

        try {
            // Bind to lifecycle
            camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);

            // Update flash button visibility based on if flash is available
            boolean hasFlash = camera.getCameraInfo().hasFlashUnit();
            flashButton.setVisibility(hasFlash ? View.VISIBLE : View.GONE);

        } catch (Exception e) {
            Toast.makeText(this, "Error binding camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFlash() {
        isFlashOn = !isFlashOn;

        // Update the flash icon
        updateFlashButtonIcon();

        // Update the image capture use case
        if (imageCapture != null) {
            imageCapture.setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
        }

        // Show toast feedback
        Toast.makeText(this, "Flash: " + (isFlashOn ? "On" : "Off"), Toast.LENGTH_SHORT).show();
    }

    private void updateFlashButtonIcon() {
        // Use our custom vector drawables for flash
        if (isFlashOn) {
            // Set flash on icon
            flashButton.setImageResource(R.drawable.ic_flash_on);
            // Set a text label on the button
            flashButton.setContentDescription("Flash On");
        } else {
            // Set flash off icon
            flashButton.setImageResource(R.drawable.ic_flash_off);
            // Set a text label on the button
            flashButton.setContentDescription("Flash Off");
        }
    }

    private void captureImage() {
        if (imageCapture == null) {
            return;
        }

        // Create output file
        File photoFile = createImageFile();
        if (photoFile == null) {
            return;
        }

        // Create output options object
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Take the picture
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> {
                    try {
                        // Fix image orientation
                        String correctedImagePath = fixImageOrientation(photoFile.getAbsolutePath());

                        // Send corrected image to cropper
                        launchCropper(correctedImagePath);
                    } catch (IOException e) {
                        Toast.makeText(CameraActivity.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "Error capturing image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String fixImageOrientation(String imagePath) throws IOException {
        // Read EXIF orientation
        ExifInterface exif = new ExifInterface(imagePath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        // If orientation is normal, just return the original path
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return imagePath;
        }

        // Load the bitmap
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        // Create a matrix for rotation
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return imagePath; // No rotation needed
        }

        // Rotate the bitmap
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // If no rotation performed, return original
        if (bitmap == rotatedBitmap) {
            return imagePath;
        }

        // Create a new file for the rotated image
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        String rotatedFileName = "ROT_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File rotatedFile = new File(storageDir, rotatedFileName);

        // Save the rotated bitmap
        FileOutputStream out = new FileOutputStream(rotatedFile);
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();

        // Recycle the bitmaps
        bitmap.recycle();
        rotatedBitmap.recycle();

        return rotatedFile.getAbsolutePath();
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
            String imageFileName = "IMG_" + timeStamp;
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return new File(storageDir, imageFileName + ".jpg");
        } catch (Exception e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void launchCropper(String imagePath) {
        Intent intent = new Intent(this, CropperActivity.class);
        intent.putExtra("imagePath", imagePath);
        startActivityForResult(intent, CROP_IMAGE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CROP_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String croppedImagePath = data.getStringExtra("imagePath");

                // Return the cropped image path to Flutter
                Intent resultIntent = new Intent();
                resultIntent.putExtra("imagePath", croppedImagePath);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled cropping, restart camera preview
                Toast.makeText(this, "Cropping cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
