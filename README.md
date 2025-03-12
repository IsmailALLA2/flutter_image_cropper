# Flutter Image Cropper

A Flutter plugin for Android that provides native camera capture and image cropping functionality with a seamless user experience. This plugin is built using [SmartCropper](https://github.com/pqpo/SmartCropper) for powerful, intelligent edge detection and cropping capabilities.

## Features

- 📷 Native Android camera UI with a circular capture button
- ✂️ Powerful image cropping with auto-detection (powered by SmartCropper)
- 🔄 Image rotation functionality
- 🔦 Camera flash control (on/off)

## Credits

This plugin is built with the following technologies:
- [SmartCropper](https://github.com/pqpo/SmartCropper) v2.1.3 by [pqpo](https://github.com/pqpo) - An intelligent image cropping library for Android
- Android CameraX API for the native camera functionality

## Installation

Since this plugin is not available on pub.dev, you'll need to install it directly from GitHub:

### Step 1: Add the plugin to your pubspec.yaml

```yaml
dependencies:
  flutter_image_cropper:
    git:
      url: https://github.com/IsmailALLA2/flutter_image_cropper.git
      ref: main # or specify your branch/tag

  # Required dependencies
  image_picker: ^1.1.2
  permission_handler: ^11.4.0
```

### Step 2: Run flutter pub get

```
flutter pub get
```

## Android Setup

### 1. Set minimum SDK version

In your `android/app/build.gradle`, ensure you have:

```gradle
android {
    defaultConfig {
        minSdkVersion 21
        // other settings...
    }
}
```

### 2. Add permissions to AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Storage permissions -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- Camera permission -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="true" />

    <!-- Rest of your manifest -->
</manifest>
```

### 3. Add FileProvider configuration

Create a file at `android/app/src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="external_files" path="."/>
    <external-cache-path name="external_cache" path="."/>
    <external-files-path name="external_files" path="."/>
    <cache-path name="cache" path="."/>
    <files-path name="files" path="."/>
</paths>
```

Register the provider in your application's AndroidManifest.xml inside the `<application>` tag:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

## Usage

The plugin offers three main approaches to capture and crop images:

### 1. Native Camera with Crop (All-in-One)

Use the plugin's built-in native camera UI with integrated cropping:

```dart
// Launch the plugin's native camera and cropper in one flow
final String? croppedImagePath = await FlutterImageCropper.takePictureAndCrop();

if (croppedImagePath != null) {
  // Use the cropped image path
  setState(() {
    _imagePath = croppedImagePath;
  });
}
```

### 2. Flutter Camera with Crop (Two-Step)

Use Flutter's image_picker for camera capture, then send to the cropper:

```dart
// First use Flutter's image_picker to take a photo
final XFile? image = await ImagePicker().pickImage(source: ImageSource.camera);

if (image != null) {
  // Then send to the plugin's cropper
  final String? croppedPath = await FlutterImageCropper.cropImage(image.path);
  
  if (croppedPath != null) {
    // Use the cropped image path
    setState(() {
      _imagePath = croppedPath;
    });
  }
}
```

### 3. Gallery with Crop

Pick an existing image from the gallery, then crop it:

```dart
// First pick an image from gallery
final XFile? image = await ImagePicker().pickImage(source: ImageSource.gallery);

if (image != null) {
  // Then send to cropper
  final String? croppedPath = await FlutterImageCropper.cropImage(image.path);
  
  if (croppedPath != null) {
    // Use the cropped image path
    setState(() {
      _imagePath = croppedPath;
    });
  }
}
```

## Complete Example

Here's a complete example showing how to implement all three options with proper permission handling:

```dart
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_image_cropper/flutter_image_cropper.dart';
import 'package:image_picker/image_picker.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Image Cropper Demo',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String? _croppedImagePath;
  bool _isProcessing = false;

  // Method 1: Use native camera and cropper (all-in-one)
  Future<void> _takePictureAndCropNative() async {
    setState(() {
      _isProcessing = true;
    });

    try {
      // Request permissions
      var cameraStatus = await Permission.camera.request();

      // Handle storage permissions based on Android version
      PermissionStatus storageStatus;
      if (Platform.isAndroid) {
        try {
          int sdkInt = int.parse(
            (await Process.run('getprop', ['ro.build.version.sdk'])).stdout.trim(),
          );

          if (sdkInt >= 33) {
            // Android 13+ uses photos permission
            storageStatus = await Permission.photos.request();
          } else {
            // Older Android versions use storage permission
            storageStatus = await Permission.storage.request();
          }
        } catch (e) {
          // Fallback - request both permissions
          await Permission.storage.request();
          storageStatus = await Permission.photos.request();
        }
      } else {
        storageStatus = await Permission.photos.request();
      }

      if (cameraStatus.isDenied || storageStatus.isDenied) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Permissions required'))
        );
        return;
      }

      // Launch native camera & cropper
      final String? croppedPath = await FlutterImageCropper.takePictureAndCrop();

      if (croppedPath != null && mounted) {
        setState(() {
          _croppedImagePath = croppedPath;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: ${e.toString()}')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
      }
    }
  }

  // Method 2: Use Flutter camera and then crop (two-step)
  Future<void> _takePhotoWithFlutterCamera() async {
    setState(() {
      _isProcessing = true;
    });

    try {
      // Request permissions
      await Permission.camera.request();
      
      // Handle storage permissions
      if (Platform.isAndroid) {
        if (await _requestStoragePermission() == false) {
          return;
        }
      }

      // First take a photo with Flutter's image_picker
      final XFile? photo = await ImagePicker().pickImage(source: ImageSource.camera);
      
      if (photo != null) {
        // Check if file exists
        final File file = File(photo.path);
        if (!await file.exists()) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Image file does not exist')),
            );
          }
          return;
        }
        
        // Then send to cropper
        final String? croppedPath = await FlutterImageCropper.cropImage(photo.path);

        if (croppedPath != null && mounted) {
          setState(() {
            _croppedImagePath = croppedPath;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: ${e.toString()}')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
      }
    }
  }

  // Method 3: Pick from gallery and crop
  Future<void> _pickFromGalleryAndCrop() async {
    setState(() {
      _isProcessing = true;
    });

    try {
      // Request storage permission
      if (Platform.isAndroid) {
        if (await _requestStoragePermission() == false) {
          return;
        }
      }

      // Pick image from gallery
      final XFile? image = await ImagePicker().pickImage(source: ImageSource.gallery);

      if (image != null) {
        // Check if file exists
        final File file = File(image.path);
        if (!await file.exists()) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Selected image file does not exist')),
            );
          }
          return;
        }

        // Send to cropper
        final String? croppedPath = await FlutterImageCropper.cropImage(image.path);

        if (croppedPath != null && mounted) {
          setState(() {
            _croppedImagePath = croppedPath;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: ${e.toString()}')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
      }
    }
  }

  // Helper method for storage permission
  Future<bool> _requestStoragePermission() async {
    try {
      int sdkInt = int.parse(
        (await Process.run('getprop', ['ro.build.version.sdk'])).stdout.trim(),
      );
      
      PermissionStatus status;
      if (sdkInt >= 33) {
        status = await Permission.photos.request();
      } else {
        status = await Permission.storage.request();
      }
      
      if (status.isDenied && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Storage permission required'))
        );
        return false;
      }
      return true;
    } catch (e) {
      // Fallback - request both permissions
      await Permission.storage.request();
      await Permission.photos.request();
      return true;
    }
  }

  // Show selection dialog with all three options
  void _showImageSourceOptions() {
    showModalBottomSheet(
      context: context,
      builder: (context) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.camera_enhance),
              title: const Text('Native Camera'),
              subtitle: const Text('One-step process with built-in camera'),
              onTap: () {
                Navigator.of(context).pop();
                _takePictureAndCropNative();
              },
            ),
            ListTile(
              leading: const Icon(Icons.camera_alt),
              title: const Text('Flutter Camera'),
              subtitle: const Text('Use Flutter\'s camera then crop'),
              onTap: () {
                Navigator.of(context).pop();
                _takePhotoWithFlutterCamera();
              },
            ),
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('Gallery'),
              subtitle: const Text('Choose existing image'),
              onTap: () {
                Navigator.of(context).pop();
                _pickFromGalleryAndCrop();
              },
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Flutter Image Cropper Demo')),
      body: _isProcessing
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text('Processing image...'),
                ],
              ),
            )
          : Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (_croppedImagePath != null) ...[
                    const Text(
                      'Cropped Image:',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      height: 300,
                      width: 300,
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.grey),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: Image.file(
                          File(_croppedImagePath!),
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    ElevatedButton(
                      onPressed: _showImageSourceOptions,
                      child: const Text('Crop Another Image'),
                    ),
                  ] else ...[
                    const Icon(Icons.image, size: 100, color: Colors.grey),
                    const SizedBox(height: 20),
                    const Text(
                      'No image cropped yet',
                      style: TextStyle(fontSize: 16),
                    ),
                    const SizedBox(height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        ElevatedButton.icon(
                          onPressed: _takePictureAndCropNative,
                          icon: const Icon(Icons.camera_enhance),
                          label: const Text('Native Camera'),
                          style: ElevatedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 10,
                            ),
                          ),
                        ),
                        const SizedBox(width: 20),
                        ElevatedButton.icon(
                          onPressed: _takePhotoWithFlutterCamera,
                          icon: const Icon(Icons.camera_alt),
                          label: const Text('Flutter Camera'),
                          style: ElevatedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 10,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      onPressed: _pickFromGalleryAndCrop,
                      icon: const Icon(Icons.photo_library),
                      label: const Text('Gallery'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 10,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
      floatingActionButton: _croppedImagePath == null && !_isProcessing
          ? FloatingActionButton(
              onPressed: _showImageSourceOptions,
              tooltip: 'Select Image',
              child: const Icon(Icons.add_photo_alternate),
            )
          : null,
    );
  }
}
```

## Key Implementation Details

### Option 1: Native Camera Interface

- Built-in Android camera UI using CameraX API
- Features a circular capture button in the center
- Includes flash toggle (on/off) button in the top-right corner
- Automatically sends captured photos to the cropper
- Single method call: `FlutterImageCropper.takePictureAndCrop()`

### Option 2: Flutter Camera

- Uses Flutter's image_picker package with camera source
- Uses the system camera UI
- Requires two steps: capture with image_picker, then send to cropper
- More flexibility but less integrated experience

### Option 3: Gallery Picker

- Uses Flutter's image_picker package with gallery source
- Allows selecting existing images for cropping
- Perfect for when you already have images you want to crop

### SmartCropper Integration

This plugin uses [SmartCropper](https://github.com/pqpo/SmartCropper) v2.1.3 by pqpo, which provides:

- Intelligent edge detection for documents
- Automatic corner detection
- Manual adjustment of crop points
- High-quality image processing

### Permissions Handling

The plugin handles permissions based on Android version:

- Android 13+ (SDK 33+): Uses READ_MEDIA_IMAGES permission
- Android 12 and below: Uses READ_EXTERNAL_STORAGE permission
- Camera permission is requested as needed

## Troubleshooting

### Image Orientation Issues

The plugin automatically fixes image orientation by reading the EXIF data and correcting the rotation.

### Permission Denied Errors

Make sure to request all required permissions before using the plugin. The example code shows how to handle this for different Android versions.

### Images Don't Appear After Cropping

Check that you're properly handling the returned path and that your app has storage permissions.

## Contributing

Feel free to contribute to this project by opening issues or submitting pull requests on GitHub.
