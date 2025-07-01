package com.example.kansuke_app
import android.util.Log

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.shared_data_plugin.SharedDataPlugin
class MainActivity : FlutterActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("MainActivity", "onNewIntent: intent=$intent")
        SharedDataPlugin.instance?.handleIncomingIntent(intent)  
    }
} 