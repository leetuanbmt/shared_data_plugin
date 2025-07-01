import 'package:flutter/material.dart';
import 'package:shared_data_plugin/shared_data_plugin.dart';
import 'dart:io';
import 'package:image_picker/image_picker.dart';
import 'package:shared_data_plugin/shared_data_plugin.g.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Key Inspections',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Shared Data Demo'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> with WidgetsBindingObserver {
  final shareData = ValueNotifier<List<ShareData>>([]);

  @override
  void initState() {
    super.initState();
    getSharedData();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      getSharedData();
    }
  }

  Future<void> getSharedData() async {
    final response = await SharedDataPlugin.instance.receiveAll();
    shareData.value = response;
  }

  Future<void> shareImage() async {
    try {
      final picked = await ImagePicker().pickImage(source: ImageSource.gallery);
      if (picked == null) return;

      await SharedDataPlugin.instance.shareData(
        filePath: picked.path,
        targetPackage: 'com.example.kansuke_app',
        metadata: {
          'text': 'Hello, world!',
          'session': '1234567890',
          'userName': 'John Doe',
          'userAvatar': 'https://via.placeholder.com/150',
          'userEmail': 'john.doe@example.com',
          'userPhone': '1234567890',
          'userAddress': '123 Main St, Anytown, USA',
          'userCity': 'Anytown',
          'userState': 'CA',
        },
      );
      getSharedData();
    } catch (e) {
      Logger.log(e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text('Key Inspection'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: getSharedData,
                    child: const Text('Get'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton(
                    onPressed: shareImage,
                    child: const Text('Share'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton(
                    onPressed: () async {
                      await SharedDataPlugin.instance.clearAll();
                      getSharedData();
                    },
                    child: const Text('Clear'),
                  ),
                ),
              ],
            ),
            Expanded(
              child: ValueListenableBuilder(
                valueListenable: shareData,
                builder: (
                  BuildContext context,
                  List<ShareData> value,
                  Widget? child,
                ) {
                  return ListView.builder(
                    itemCount: value.length,
                    itemBuilder: (context, index) {
                      return ListTile(
                        leading: ImageItem(shareData: value[index]),
                        title: Text(
                          value[index].id ?? '',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        subtitle: Text(
                          value[index].metadata?.toString() ?? '',
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: IconButton(
                          color: Colors.red,
                          onPressed: () {
                            SharedDataPlugin.instance.delete(value[index].id!);
                            getSharedData();
                          },
                          icon: const Icon(Icons.delete),
                        ),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class ImageItem extends StatelessWidget {
  const ImageItem({super.key, required this.shareData});
  final ShareData shareData;
  @override
  Widget build(BuildContext context) {
    if (shareData.filePath == null) return const SizedBox.shrink();
    if (shareData.filePath!.isImage) {
      return SizedBox(
        width: 60,
        height: 60,
        child: Image.file(
          File(shareData.filePath!),
          fit: BoxFit.cover,
        ),
      );
    }
    return const SizedBox.shrink();
  }
}

extension on String? {
  bool get isImage => ['jpg', 'jpeg', 'png', 'gif', 'webp'].contains(
        this?.split('.').last,
      );
}
