package com.sml.flutter_image_cropper;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

public class FlutterImageCropperPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, ActivityResultListener {

    private static final int CROP_IMAGE_REQUEST_CODE = 7890;

    private MethodChannel channel;
    private Activity activity;
    private Result pendingResult;

    private MethodChannel utilsChannel;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        // Main channel
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_image_cropper");
        channel.setMethodCallHandler(this);

        // Utilities channel
        utilsChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_image_cropper/utils");
        utilsChannel.setMethodCallHandler((call, result) -> {
            if (call.method.equals("getSystemProperty")) {
                String property = call.argument("property");
                if (property != null) {
                    try {
                        String value = System.getProperty(property);
                        result.success(value);
                    } catch (Exception e) {
                        result.error("SYSTEM_PROPERTY_ERROR",
                                "Error getting system property: " + e.getMessage(), null);
                    }
                } else {
                    result.error("INVALID_ARGUMENT", "Property name cannot be null", null);
                }
            } else {
                result.notImplemented();
            }
        });
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("cropImage")) {
            if (activity == null) {
                result.error("ACTIVITY_NULL", "Activity is null", null);
                return;
            }

            String imagePath = call.argument("imagePath");
            if (imagePath == null) {
                result.error("INVALID_ARGUMENT", "Image path cannot be null", null);
                return;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { // Android 13+
                if (activity.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 1001);
                    result.error("PERMISSION_DENIED", "Storage permission is required to access images", null);
                    return;
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // Android 6-12
                if (activity.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
                    result.error("PERMISSION_DENIED", "Storage permission is required to access images", null);
                    return;
                }
            }

            pendingResult = result;

            // Launch the CropperActivity with the image path
            try {
                Intent intent = new Intent(activity, CropperActivity.class);
                intent.putExtra("imagePath", imagePath);
                activity.startActivityForResult(intent, CROP_IMAGE_REQUEST_CODE);
            } catch (Exception e) {
                pendingResult.error("ACTIVITY_START_ERROR",
                        "Failed to start cropper activity: " + e.getMessage(), null);
                pendingResult = null;
            }
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        if (utilsChannel != null) {
            utilsChannel.setMethodCallHandler(null);
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CROP_IMAGE_REQUEST_CODE && pendingResult != null) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String croppedImagePath = data.getStringExtra("imagePath");
                pendingResult.success(croppedImagePath);
            } else {
                pendingResult.success(null); // Cancelled or error
            }
            pendingResult = null;
            return true;
        }
        return false;
    }

    
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry cropping
            } else {
                pendingResult.error("PERMISSION_DENIED", "Storage permission is required to access images", null);
            }
        }
    }

}
