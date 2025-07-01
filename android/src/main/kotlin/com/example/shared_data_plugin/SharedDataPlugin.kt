package com.example.shared_data_plugin

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

/** ShareDataPlugin */
class SharedDataPlugin: FlutterPlugin, ActivityAware, ShareDataApi {
    private lateinit var context: Context
    private var activity: Activity? = null
    private var appGroupId: String = "com.kansuke.app.shared_data"
    internal lateinit var storageManager: StorageManager
    companion object {
        var instance: SharedDataPlugin? = null
        const val EXTRA_DATA_ID = "data_id"
        const val EXTRA_OPERATION = "operation"
        const val EXTRA_SHARED_DATA = "shared_data"
        const val ACTION_CLEAR_SHARED_DATA = "com.kansuke.app.ACTION_CLEAR_SHARED_DATA"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        ShareDataApi.setUp(flutterPluginBinding.binaryMessenger, this)
        storageManager = StorageManager(context, appGroupId)
        instance = this
        context.registerReceiver(dataUpdateReceiver, IntentFilter(ACTION_CLEAR_SHARED_DATA))
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // No need to do anything here as the channel is no longer used
        activity = null
        instance = null

        context.unregisterReceiver(dataUpdateReceiver)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        Log.d("ShareDataPlugin", "onAttachedToActivity: intent=${binding.activity.intent}")
        if (binding.activity.intent != null && binding.activity.intent.action != Intent.ACTION_MAIN) {
            handleIncomingIntent(binding.activity.intent)
        }
    }

    override fun onDetachedFromActivityForConfigChanges() { activity = null }
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) { 
        activity = binding.activity
     }




       // Broadcast receiver for real-time data updates
    private val dataUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("ShareDataPlugin", "dataUpdateReceiver: intent=$intent")
            when (intent?.action) {
                ACTION_CLEAR_SHARED_DATA -> {
                    val dataId = intent.getStringExtra(EXTRA_DATA_ID)
                    val operation = intent.getStringExtra(EXTRA_OPERATION)
                    Log.d("ShareDataPlugin", "Received data update: $operation for ID: $dataId")
                    if(operation == "clear_all") {
                        storageManager.clearAll()
                    } else if(operation == "delete_item" && dataId != null) {
                        storageManager.deleteData(dataId)
                    }
                    
                }
            }
        }
    }
    override fun onDetachedFromActivity() { activity = null }

    override fun configureAppGroup(appGroupId: String) {
        this.appGroupId = appGroupId
        storageManager = StorageManager(context, appGroupId)
    }

    override fun shareData(data: ShareData, targetPackage: String?): ShareResult {
        Log.d("ShareDataPlugin", "shareData: data=$data, targetPackage=$targetPackage")
        return try {
            val finalData = ensureDataHasId(data)
            if(targetPackage != null) {
                Log.d("ShareDataPlugin", "shareData: sharing to target package $targetPackage")
                shareToTargetPackage(finalData, targetPackage)
            } else {
                Log.d("ShareDataPlugin", "shareData: sharing via system intent")
                storageManager.saveData(finalData)
                ShareResult(true, null, finalData.id)
            }
        } catch (e: Exception) {
            logError("SHARE_DATA_ERROR", e)
            ShareResult(false, e.message, data.id)
        }
    }

    override fun receiveAll(): List<ShareData> = storageManager.getAllData()

    override fun clearAll() {
        storageManager.clearAll()
        clearAllSharedData(context, null, "clear_all")
    }
    override fun delete(id: String) {
        storageManager.deleteData(id)
        clearAllSharedData(context, id, "delete_item")
    }


    private fun clearAllSharedData(context: Context, dataId: String?, operation: String) {
        val intent = Intent(ACTION_CLEAR_SHARED_DATA)
        intent.putExtra(EXTRA_DATA_ID, dataId)
        intent.putExtra(EXTRA_OPERATION, operation)
        context.sendBroadcast(intent)
    }


     fun handleIncomingIntent(intent: Intent?) {
        Log.d("ShareDataPlugin", "handleIncomingIntent: intent=$intent")
        val data = storageManager.createShareDataFromIntent(intent)
        if (data != null) {
            Log.d("ShareDataPlugin", "handleIncomingIntent: saving data id=${data.id}")
            storageManager.saveData(data)
        } else {
            Log.d("ShareDataPlugin", "handleIncomingIntent: no data extracted from intent")
        }
    }

    private fun ensureDataHasId(data: ShareData): ShareData =
        if (data.id == null) data.copy(id = UUID.randomUUID().toString()) else data


    private fun logError(tag: String, e: Exception) {
        Log.e("ShareDataPlugin-$tag", e.message ?: "Unknown error", e)
    }

    // Định nghĩa lại các hàm chia sẻ intent
    private fun buildShareIntent(data: ShareData): Pair<Intent, Boolean> {
        val intent = Intent(Intent.ACTION_SEND)
        var hasContent = false
        try {
            Log.d("ShareDataPlugin", "buildShareIntent: data=$data")
            val metadataJson = if (data.metadata != null && data.metadata.isNotEmpty()) {
                org.json.JSONObject(data.metadata).toString()
            } else null

            // Nếu có file: luôn truyền kèm metadata dạng JSON
            if (data.filePath != null) {
                val file = File(data.filePath!!)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    if (!metadataJson.isNullOrEmpty()) {
                        intent.putExtra(Intent.EXTRA_TEXT, metadataJson)
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.type = data.mimeType ?: data.metadata?.get("mimeType") ?: "application/octet-stream"
                    hasContent = true
                } else {
                    Log.e("ShareDataPlugin", "buildShareIntent: file does not exist: ${data.filePath}")
                }
            } else if (!metadataJson.isNullOrEmpty()) {
                // Nếu không có file, chỉ truyền metadata dạng JSON
                intent.putExtra(Intent.EXTRA_TEXT, metadataJson)
                intent.type = "application/json"
                hasContent = true
            }
            Log.d("ShareDataPlugin", "buildShareIntent: hasContent=$hasContent, intent=$intent")
            return Pair(intent, hasContent)
        } catch (e: Exception) {
            Log.e("ShareDataPlugin", "buildShareIntent: Exception: ${e.message}", e)
            return Pair(intent, false)
        }
    }


    private fun isAppInstalled(targetPackage: String): Boolean {
        val pm = context.packageManager
         try {
            pm.getApplicationInfo(targetPackage, 0)
          return  true
        } catch (e: Exception) {
          return  false
        }
    }

    // Hàm kiểm tra package đã cài, nếu chưa thì mở Google Play Store
    private fun checkAndPromptInstallTargetApp(targetPackage: String): Boolean {
        if (!isAppInstalled(targetPackage)) {
            Log.e("ShareDataPlugin", "Target package $targetPackage is not installed")
            try {
                val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetPackage"))
                playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(playIntent)
            } catch (ex: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$targetPackage"))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            }
            return false
        }
        return true
    }

    private fun shareToTargetPackage(data: ShareData, targetPackage: String): ShareResult {
        Log.d("ShareDataPlugin", "shareToTargetPackage: data=$data, targetPackage=$targetPackage")
        return try {
            // // Kiểm tra targetPackage đã cài chưa, nếu chưa thì mở Google Play
            // if (!checkAndPromptInstallTargetApp(targetPackage)) {
            //     return ShareResult(false, "Target app is not installed", data.id)
            // }
            val (intent, hasContent) = buildShareIntent(data)
            if (!hasContent) {
                Log.d("ShareDataPlugin", "shareToTargetPackage: no content to share")
                return ShareResult(false, "No content to share", data.id)
            }
            intent.setPackage(targetPackage)
            intent.putExtra("uid", data.id)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            storageManager.saveData(data)
            Log.d("ShareDataPlugin", "shareToTargetPackage: shared successfully")
            ShareResult(true, null, data.id)
        } catch (e: Exception) {
            logError("SHARE_TO_TARGET", e)
            ShareResult(false, e.message, data.id)
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "json" -> "application/json"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> "application/octet-stream"
        }
    }

}

// ShareDataSerializer: Chuyển đổi giữa ShareData và JSON
object ShareDataSerializer {
    fun toJson(data: ShareData): String {
        val map = mutableMapOf<String, Any?>()
        data.id?.let { map["id"] = it }
        data.filePath?.let { map["filePath"] = it }
        data.mimeType?.let { map["mimeType"] = it }
        data.metadata?.let { map["metadata"] = it }
        return JSONObject(map).toString()
    }
    fun fromJson(json: String, id: String): ShareData? {
        return try {
            val jsonObject = JSONObject(json)
            ShareData(
                id = id,
                filePath = jsonObject.optString("filePath").takeIf { it.isNotEmpty() },
                mimeType = jsonObject.optString("mimeType").takeIf { it.isNotEmpty() },
                metadata = jsonObject.optJSONObject("metadata")?.let { metadataObj ->
                    val metadataMap = mutableMapOf<String?, String?>()
                    val keys = metadataObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        metadataMap[key] = metadataObj.optString(key)
                    }
                    metadataMap as Map<String?, String?>?
                }
            )
        } catch (e: Exception) {
            Log.e("ShareDataSerializer", "Error parsing JSON: ${e.message}")
            null
        }
    }
}

