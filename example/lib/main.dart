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
  final ImagePicker _picker = ImagePicker();
  bool _isProcessing = false;

  // Take photo and crop directly with native implementation
  Future<void> _takePictureAndCropNative() async {
    setState(() {
      _isProcessing = true;
    });

    try {
      // Request required permissions
      await _requestPermissions(isCamera: true);

      // Use the new native camera + cropper flow
      final String? croppedPath =
          await FlutterImageCropper.takePictureAndCrop();

      if (croppedPath != null && mounted) {
        setState(() {
          _croppedImagePath = croppedPath;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Error: ${e.toString()}')));
      }
    } finally {
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
      }
    }
  }

  // Request all required permissions
  Future<bool> _requestPermissions({bool isCamera = false}) async {
    // Request Camera Permission if needed
    if (isCamera) {
      final cameraStatus = await Permission.camera.request();
      if (!cameraStatus.isGranted) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Camera permission denied')),
          );
        }
        return false;
      }
    }

    // Request Storage/Photos Permission based on Android Version
    PermissionStatus storageStatus;
    if (Platform.isAndroid) {
      int sdkInt = 0;
      try {
        sdkInt = int.parse(
          (await Process.run('getprop', [
            'ro.build.version.sdk',
          ])).stdout.trim(),
        );
      } catch (e) {
        // Default to using both permissions if we can't determine SDK version
        await Permission.storage.request();
        await Permission.photos.request();
        return true;
      }

      if (sdkInt >= 33) {
        // Android 13+ uses photos permission
        storageStatus = await Permission.photos.request();
      } else {
        // Older Android versions use storage permission
        storageStatus = await Permission.storage.request();
      }
    } else {
      storageStatus = await Permission.photos.request();
    }

    if (!storageStatus.isGranted) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Storage permission denied')),
        );
      }
      return false;
    }

    return true;
  }

  Future<void> _pickAndCropImage(ImageSource source) async {
    setState(() {
      _isProcessing = true;
    });

    try {
      // Request permissions
      final hasPermissions = await _requestPermissions(
        isCamera: source == ImageSource.camera,
      );
      if (!hasPermissions) {
        return;
      }

      // Pick Image from selected source
      final XFile? image = await ImagePicker().pickImage(source: source);

      if (image != null) {
        final File file = File(image.path);
        if (!await file.exists()) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Selected image file does not exist'),
              ),
            );
          }
          return;
        }

        final String? croppedPath = await FlutterImageCropper.cropImage(
          image.path,
        );

        if (croppedPath != null && mounted) {
          setState(() {
            _croppedImagePath = croppedPath;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Error: ${e.toString()}')));
      }
    } finally {
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
      }
    }
  }

  // Show a modal bottom sheet to select image source
  void _showImageSourceOptions() {
    showModalBottomSheet(
      context: context,
      builder:
          (context) => SafeArea(
            child: Wrap(
              children: [
                ListTile(
                  leading: const Icon(Icons.camera_alt_outlined),
                  title: const Text('Native Camera + Crop'),
                  subtitle: const Text('One-step process with native UI'),
                  onTap: () {
                    Navigator.of(context).pop();
                    _takePictureAndCropNative();
                  },
                ),
                const Divider(),
                ListTile(
                  leading: const Icon(Icons.photo_camera),
                  title: const Text('Take a Photo'),
                  subtitle: const Text('Using Flutter camera'),
                  onTap: () {
                    Navigator.of(context).pop();
                    _pickAndCropImage(ImageSource.camera);
                  },
                ),
                ListTile(
                  leading: const Icon(Icons.photo_library),
                  title: const Text('Choose from Gallery'),
                  onTap: () {
                    Navigator.of(context).pop();
                    _pickAndCropImage(ImageSource.gallery);
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

      body:
          _isProcessing
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
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
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
                      ElevatedButton.icon(
                        onPressed: _takePictureAndCropNative,
                        icon: const Icon(Icons.camera_enhance),
                        label: const Text('Native Camera + Crop'),
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 12,
                          ),
                        ),
                      ),
                      const SizedBox(height: 12),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          ElevatedButton.icon(
                            onPressed:
                                () => _pickAndCropImage(ImageSource.camera),
                            icon: const Icon(Icons.camera_alt),
                            label: const Text('Camera'),
                            style: ElevatedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 10,
                              ),
                            ),
                          ),
                          const SizedBox(width: 20),
                          ElevatedButton.icon(
                            onPressed:
                                () => _pickAndCropImage(ImageSource.gallery),
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
                      ),
                    ],
                  ],
                ),
              ),
      floatingActionButton:
          _croppedImagePath == null && !_isProcessing
              ? FloatingActionButton(
                onPressed: _showImageSourceOptions,
                tooltip: 'Select Image',
                child: const Icon(Icons.add_photo_alternate),
              )
              : null,
    );
  }
}
