import 'dart:developer' as dev;

import 'dart:io';
import 'package:encrypt/encrypt.dart' as encrypt;

import 'shared_data_plugin.g.dart';

class Logger {
  static void log(String message) {
    dev.log(message);
  }
}

// Lớp ngoại lệ tùy chỉnh
class SharedDataException implements Exception {
  final String message;
  SharedDataException(this.message);
}

class SharedDataPlugin {
  static final SharedDataPlugin _instance = SharedDataPlugin._();
  static SharedDataPlugin get instance => _instance;

  SharedDataPlugin._();

  final SharedDataApi _api = SharedDataApi();
  // Cấu hình mã hóa
  final _key = encrypt.Key.fromUtf8('your-32-char-key-here!!!!!!!');
  final _iv = encrypt.IV.fromLength(16);
  late final encrypt.Encrypter _encrypter;

  SharedDataPlugin() {
    _encrypter = encrypt.Encrypter(encrypt.AES(_key));
  }

  /// Lưu dữ liệu dạng chuỗi (ví dụ: session token, JSON)
  Future<void> saveData(String key, String data, {bool encrypt = true}) async {
    try {
      final request = SharedDataRequest()..key = key;
      final dataToSave = encrypt ? _encryptData(data) : data;
      await _api.saveSharedData(request, dataToSave, null);
    } catch (e) {
      throw SharedDataException('Failed to save data: $e');
    }
  }

  /// Lưu file
  Future<void> saveFile(String key, File file, {String? authority}) async {
    try {
      final request = SharedDataRequest(
        key: key,
        authority: authority,
      );
      final fileContent = await file.readAsBytes();
      await _api.saveSharedData(
        request,
        null,
        fileContent,
      );
    } catch (e) {
      throw SharedDataException('Failed to save file: $e');
    }
  }

  /// Đọc dữ liệu dạng chuỗi
  Future<String?> getData(String key, {bool decrypt = true}) async {
    try {
      final request = SharedDataRequest()..key = key;
      final response = await _api.getSharedData(request);
      if (response.data == null) return null;
      return decrypt ? _decryptData(response.data!) : response.data;
    } catch (e) {
      throw SharedDataException('Failed to get data: $e');
    }
  }

  /// Đọc file và lưu vào đường dẫn cục bộ
  Future<File?> getFile(String key, String destinationPath,
      {String? authority}) async {
    try {
      final request = SharedDataRequest(
        key: key,
        authority: authority,
      );
      final response = await _api.getSharedData(request);
      if (response.fileContent == null) return null;
      final file = File(destinationPath);
      await file.writeAsBytes(response.fileContent!);
      return file;
    } catch (e) {
      throw SharedDataException('Failed to get file: $e');
    }
  }

  /// Xóa dữ liệu hoặc file
  Future<void> deleteData(String key) async {
    try {
      final request = SharedDataRequest()..key = key;
      await _api.deleteSharedData(request);
    } catch (e) {
      throw SharedDataException('Failed to delete data: $e');
    }
  }

  /// Kiểm tra dữ liệu/file có tồn tại
  Future<bool> checkDataExists(String key) async {
    try {
      final request = SharedDataRequest()..key = key;
      final response = await _api.checkSharedData(request);
      return response.exists ?? false;
    } catch (e) {
      throw SharedDataException('Failed to check data: $e');
    }
  }

  /// Mã hóa dữ liệu
  String _encryptData(String data) {
    return _encrypter.encrypt(data, iv: _iv).base64;
  }

  /// Giải mã dữ liệu
  String _decryptData(String encrypted) {
    return _encrypter.decrypt64(encrypted, iv: _iv);
  }
}
