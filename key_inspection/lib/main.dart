import 'package:flutter/material.dart';
import 'package:shared_data_plugin/shared_data_plugin.dart';
import 'dart:io';
import 'package:path_provider/path_provider.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  Future<File?> getSharedFile() async {
    final dir = await getApplicationDocumentsDirectory();
    final plugin = SharedDataPlugin.instance;
    final file = await plugin.getFile(
      'shared_file.png',
      '${dir.path}/shared_file.png',
      authority: 'com.example.kansuke_app.provider',
    );
    Logger.log('File path: ${file?.path}');
    Logger.log('File exists: ${file != null && await file.exists()}');
    Logger.log('File length: ${file != null ? await file.length() : 0}');
    return file;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: FutureBuilder<File?>(
          future: getSharedFile(),
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const CircularProgressIndicator();
            }
            if (snapshot.data == null) {
              return const Text('No file');
            }
            return Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('File path: ${snapshot.data!.path}'),
                Text(
                  'Exists: ${snapshot.data != null && snapshot.data!.existsSync()}',
                ),
                Text(
                  'Length: ${snapshot.data != null ? snapshot.data!.lengthSync() : 0}',
                ),
                Expanded(
                  child: Image.file(snapshot.data!),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
