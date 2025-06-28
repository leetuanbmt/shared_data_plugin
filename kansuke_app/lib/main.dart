import 'package:flutter/material.dart';
import 'package:shared_data_plugin/shared_data_plugin.dart';
import 'dart:io';
import 'package:image_picker/image_picker.dart';

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
  Future<void> saveSharedContent() async {
    final plugin = SharedDataPlugin.instance;

    try {
      final images = await ImagePicker().pickImage(source: ImageSource.gallery);
      if (images == null) return;
      final file = File(images.path);
      await plugin.saveFile(
        'shared_file.png',
        file,
        authority: 'com.example.kansuke_app.provider',
      );
      Logger.log('Saving file: ${file.path}, size: ${await file.length()}');
    } catch (e) {
      Logger.log('Error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: ElevatedButton(
          onPressed: saveSharedContent,
          child: const Text('Choose image and save'),
        ),
      ),
    );
  }
}
