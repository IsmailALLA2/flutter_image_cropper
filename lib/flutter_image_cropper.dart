import 'dart:async';
import 'package:flutter/services.dart';

class FlutterImageCropper {
  static const MethodChannel _channel = MethodChannel('flutter_image_cropper');

  /// Launches the native image cropper UI.
  ///
  /// [imagePath] is the path to the image to be cropped.
  ///
  /// Returns the path to the cropped image, or null if the operation was canceled.
  static Future<String?> cropImage(String imagePath) async {
    try {
      final String? result = await _channel.invokeMethod('cropImage', {
        'imagePath': imagePath,
      });
      return result;
    } on PlatformException catch (e) {
      print('Error cropping image: ${e.message}');
      return null;
    }
  }

  /// Launches the native camera UI to take a picture and then crops it.
  ///
  /// Returns the path to the cropped image, or null if the operation was canceled.
  static Future<String?> takePictureAndCrop() async {
    try {
      final String? result = await _channel.invokeMethod('takePictureAndCrop');
      return result;
    } on PlatformException catch (e) {
      print('Error taking picture and cropping: ${e.message}');
      return null;
    }
  }
}
