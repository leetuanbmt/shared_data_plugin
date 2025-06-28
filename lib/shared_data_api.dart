import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartPackageName: 'shared_data_plugin',
    dartOut: 'lib/shared_data_plugin.g.dart',
    dartOptions: DartOptions(),
    kotlinOut:
        'android/src/main/kotlin/com/example/shared_data_plugin/SharedDataApi.kt',
    kotlinOptions: KotlinOptions(package: 'com.example.shared_data_plugin'),
  ),
)
class SharedDataRequest {
  String? authority; // Authority của ContentProvider
  String? key; // Key để xác định dữ liệu hoặc file
}

class SharedDataResponse {
  String? data; // Dữ liệu dạng chuỗi
  Uint8List? fileContent; // Nội dung file dạng bytes
  bool? exists; // Kiểm tra dữ liệu/file có tồn tại
}

@HostApi()
abstract class SharedDataApi {
  SharedDataResponse getSharedData(SharedDataRequest request);
  void saveSharedData(
    SharedDataRequest request,
    String? data,
    Uint8List? fileContent,
  );
  void deleteSharedData(SharedDataRequest request);
  SharedDataResponse checkSharedData(SharedDataRequest request);
}
