import 'dart:developer' as dev;

import 'shared_data_plugin.g.dart';

class Logger {
  static void log(String message) {
    dev.log(message);
  }
}

// Lớp ngoại lệ tùy chỉnh
class SharedDataPlugin {
  static final SharedDataPlugin _instance = SharedDataPlugin._();
  static SharedDataPlugin get instance => _instance;

  SharedDataPlugin._();

  static final ShareDataApi _api = ShareDataApi();

  /// Unified share function for both text and files
  Future<ShareResult> shareData({
    String? filePath,
    Map<String?, String?>? metadata,
    String? targetPackage,
  }) async {
    return await _api.shareData(
      data: ShareData(
        filePath: filePath,
        mimeType: getMimeType(filePath),
        metadata: metadata,
      ),
      targetPackage: targetPackage,
    );
  }

  /// Receive all shared data
  Future<List<ShareData>> receiveAll() async {
    return await _api.receiveAll();
  }

  /// Clear all shared data
  Future<void> clearAll() async {
    await _api.clearAll();
  }

  /// Delete specific data by ID
  Future<void> delete(String id) async {
    await _api.delete(id);
  }
}

String getMimeType(String? filePath) {
  if (filePath == null) return '';
  final extension = filePath.split('.').last;
  return switch (extension) {
    'jpg' || 'jpeg' => 'image/jpeg',
    'png' => 'image/png',
    'gif' => 'image/gif',
    'mp4' => 'video/mp4',
    'mp3' => 'audio/mpeg',
    'doc' => 'application/msword',
    'docx' =>
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'xls' => 'application/vnd.ms-excel',
    'xlsx' =>
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'ppt' => 'application/vnd.ms-powerpoint',
    'pptx' =>
      'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    _ => 'application/octet-stream',
  };
}
