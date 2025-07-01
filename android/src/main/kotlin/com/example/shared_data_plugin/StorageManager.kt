package com.example.shared_data_plugin

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import android.content.Intent
import android.os.Environment
import android.content.ContentValues
import android.database.Cursor

// StorageManager: Quản lý lưu trữ dữ liệu
class StorageManager(private val context: Context, private val appGroupId: String) {

    fun saveData(data: ShareData) {
        Log.d("StorageManager", "saveData: data=$data")
        val allData = getAllData().toMutableList()
        allData.removeAll { it.id == data.id }
        allData.add(data)
        writeAllData(allData)
    }

    fun getAllData(): List<ShareData> {
        return try {
            val cursor: Cursor? = context.contentResolver.query(
                SharedDataProvider.getContentUri(context), null, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val jsonString = it.getString(it.getColumnIndexOrThrow("data"))
                    if (!jsonString.isNullOrEmpty()) {
                        val jsonArray = JSONArray(jsonString)
                        val dataList = mutableListOf<ShareData>()
                        for (i in 0 until jsonArray.length()) {
                            val jsonStringItem = jsonArray.getString(i)
                            val jsonObject = JSONObject(jsonStringItem)
                            val id = jsonObject.optString("id")
                            val data = ShareDataSerializer.fromJson(jsonStringItem, id)
                            data?.let { dataList.add(it) }
                        }
                        return dataList
                    }
                }
            }
            emptyList()
        } catch (e: Exception) {
            Log.e("StorageManager", "Error reading from provider: ${e.message}")
            emptyList()
        }
    }

    fun deleteData(id: String) {
        val allData = getAllData().toMutableList()
        allData.removeAll { it.id == id }
        writeAllData(allData)
    }

    fun clearAll() {
        try {
            context.contentResolver.delete(SharedDataProvider.getContentUri(context), null, null)
        } catch (e: Exception) {
            Log.e("StorageManager", "Error clearing provider: ${e.message}")
        }
    }

    private fun writeAllData(dataList: List<ShareData>) {
        try {
            val jsonArray = JSONArray()
            for (item in dataList) {
                jsonArray.put(ShareDataSerializer.toJson(item))
            }
            val values = ContentValues().apply {
                put("data", jsonArray.toString())
            }
            context.contentResolver.insert(SharedDataProvider.getContentUri(context), values)
        } catch (e: Exception) {
            Log.e("StorageManager", "Error writing to provider: ${e.message}")
        }
    }

    fun copySharedFileToAppStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = getFileName(uri)
            val file = File(context.filesDir, fileName)

            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            Log.e("ShareDataPlugin", "Error copying shared file: ${e.message}")
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "shared_file_${System.currentTimeMillis()}"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        return fileName
    }

    fun createShareDataFromIntent(intent: Intent?): ShareData? {
        return try {
            if (intent?.action == Intent.ACTION_SEND) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val sharedFileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val metadata = mutableMapOf<String?, String?>()
                val uid = intent.getStringExtra("uid")
                Log.d("StorageManager", "createShareDataFromIntent: sharedText=$sharedText, sharedFileUri=$sharedFileUri")

                // Nếu EXTRA_TEXT là json, parse vào metadata
                if (!sharedText.isNullOrEmpty()) {
                    try {
                        val jsonObject = org.json.JSONObject(sharedText)
                        jsonObject.keys().forEach { key ->
                            metadata[key] = jsonObject.optString(key)
                        }
                    } catch (e: Exception) {
                        // Nếu không phải json, lưu text thường
                        metadata["text"] = sharedText
                    }
                }

                var filePath: String? = null
                var fileName: String? = null
                var mimeType: String? = null
                if (sharedFileUri != null) {
                    filePath = copySharedFileToAppStorage(sharedFileUri)
                    fileName = getFileName(sharedFileUri)
                    mimeType = context.contentResolver.getType(sharedFileUri)
                }

                if (sharedText != null || sharedFileUri != null) {
                    return ShareData(
                        id = uid,
                        filePath = filePath,
                        mimeType = mimeType,
                        metadata = metadata
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e("StorageManager", "Error creating ShareData from intent: ${e.message}", e)
            null
        }
    }
}
