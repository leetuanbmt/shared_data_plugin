import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartPackageName: 'shared_data_plugin',
    dartOut: 'lib/shared_data_plugin.g.dart',
    dartOptions: DartOptions(),
    kotlinOut:
        'android/src/main/kotlin/com/example/shared_data_plugin/SharedDataPlugin.g.kt',
    kotlinOptions: KotlinOptions(package: 'com.example.shared_data_plugin'),
  ),
)
class ShareData {
  String? id;
  String? filePath;
  String? mimeType;
  Map<String?, String?>? metadata;
}

class ShareResult {
  bool? success;
  String? errorMessage;
  String? sharedDataId;
}

@HostApi()
abstract class ShareDataApi {
  // App Group Configuration
  void configureAppGroup({required String appGroupId});

  // Unified sharing function that handles both text and files
  ShareResult shareData({required ShareData data, String? targetPackage});

  List<ShareData> receiveAll();
  void clearAll();
  void delete(String id);
}
