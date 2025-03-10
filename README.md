# Flutter Image Cropper

A Flutter plugin for cropping images using native Android implementation.

## Installation

Add to your `pubspec.yaml`:

```yaml
dependencies:
#this plugin is not exist on pub.dev yet while do this :
  flutter_image_cropper:
      path: {PLACE_HERE_THE_PLUGIN_PATH}
  image_picker: ^1.1.2
  permission_handler: ^11.4.0
```

## Android Setup

1. Set minimum SDK version in `android/app/build.gradle`:

```gradle
minSdkVersion 21
```

2. Add permissions to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.CAMERA" />
```

## Basic Usage

```dart
final croppedPath = await FlutterImageCropper.cropImage(image.path);
    if (croppedPath != null) {
      // Use croppedPath
    }

```

```dart
import 'package:flutter_image_cropper/flutter_image_cropper.dart';
import 'package:image_picker/image_picker.dart';
import 'package:permission_handler/permission_handler.dart';

// From gallery
Future<void> cropImageFromGallery() async {
  await Permission.storage.request();

  final picker = ImagePicker();
  final image = await picker.pickImage(source: ImageSource.gallery);

  if (image != null) {
    final croppedPath = await FlutterImageCropper.cropImage(image.path);
    if (croppedPath != null) {
      // Use croppedPath
    }
  }
}

// From camera
Future<void> cropImageFromCamera() async {
  await Permission.camera.request();

  final picker = ImagePicker();
  final image = await picker.pickImage(source: ImageSource.camera);

  if (image != null) {
    final croppedPath = await FlutterImageCropper.cropImage(image.path);
    if (croppedPath != null) {
      // Use croppedPath
    }
  }
}
```

## Example Implementation

```dart
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_image_cropper/flutter_image_cropper.dart';
import 'package:image_picker/image_picker.dart';
import 'package:permission_handler/permission_handler.dart';

class ImageCropperDemo extends StatefulWidget {
  @override
  _ImageCropperDemoState createState() => _ImageCropperDemoState();
}

class _ImageCropperDemoState extends State<ImageCropperDemo> {
  String? _croppedImagePath;
  final ImagePicker _picker = ImagePicker();

  Future<void> _pickImage(ImageSource source) async {
    // Request permission
    if (source == ImageSource.camera) {
      await Permission.camera.request();
    } else {
      await Permission.storage.request();
    }

    // Pick image
    final XFile? image = await _picker.pickImage(source: source);
    if (image == null) return;

    // Crop image
    final croppedPath = await FlutterImageCropper.cropImage(image.path);
    if (croppedPath != null) {
      setState(() {
        _croppedImagePath = croppedPath;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Image Cropper')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (_croppedImagePath != null)
              Image.file(
                File(_croppedImagePath!),
                height: 300,
                width: 300,
              )
            else
              Text('No image selected'),

            SizedBox(height: 20),

            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ElevatedButton(
                  onPressed: () => _pickImage(ImageSource.camera),
                  child: Text('Camera'),
                ),
                SizedBox(width: 20),
                ElevatedButton(
                  onPressed: () => _pickImage(ImageSource.gallery),
                  child: Text('Gallery'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
```
