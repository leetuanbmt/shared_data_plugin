package com.example.shared_data_plugin

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import io.flutter.embedding.engine.plugins.FlutterPlugin
import java.io.ByteArrayOutputStream
import android.util.Log

class SharedDataPlugin : FlutterPlugin, SharedDataApi {
    private lateinit var context: Context

    companion object {
        private const val AUTHORITY = "com.example.kansuke_app.provider"
        private val DATA_URI = Uri.parse("content://$AUTHORITY/data")
        private val FILE_URI = Uri.parse("content://$AUTHORITY/file")
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        SharedDataApi.setUp(binding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}

    private fun getDataUri(authority: String): Uri = Uri.parse("content://$authority/data")
    private fun getFileUri(authority: String): Uri = Uri.parse("content://$authority/file")

    override fun getSharedData(request: SharedDataRequest): SharedDataResponse {
        val authority = request.authority ?: AUTHORITY
        val dataKey = request.key ?: run {
            Log.d("SharedDataPlugin", "getSharedData: key is null")
            return SharedDataResponse(exists = false)
        }
        Log.d("SharedDataPlugin", "getSharedData: authority = $authority, key = $dataKey")
        var data: String? = null
        var fileContent: ByteArray? = null
        // Lấy dữ liệu
        context.contentResolver.query(
            getDataUri(authority),
            null,
            "key = ?",
            arrayOf(dataKey),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                data = cursor.getString(cursor.getColumnIndexOrThrow("data"))
                Log.d("SharedDataPlugin", "getSharedData: data found for key $dataKey")
            } else {
                Log.d("SharedDataPlugin", "getSharedData: no data for key $dataKey")
            }
        } ?: Log.d("SharedDataPlugin", "getSharedData: query data cursor is null for key $dataKey")
        // Lấy file
        val fileUri = Uri.withAppendedPath(getFileUri(authority), dataKey)
        try {
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                val byteArrayOutputStream = ByteArrayOutputStream()
                input.copyTo(byteArrayOutputStream)
                fileContent = byteArrayOutputStream.toByteArray()
                Log.d("SharedDataPlugin", "getSharedData: file found for key $dataKey, size = ${fileContent?.size}")
            } ?: Log.d("SharedDataPlugin", "getSharedData: file not found for key $dataKey")
        } catch (e: Exception) {
            Log.e("SharedDataPlugin", "getSharedData: error reading file for key $dataKey", e)
        }
        Log.d("SharedDataPlugin", "getSharedData: return data=${data != null}, fileContent=${fileContent != null}")
        return SharedDataResponse(
            data = data,
            fileContent = fileContent,
            exists = (data != null || fileContent != null)
        )
    }

    override fun saveSharedData(request: SharedDataRequest, data: String?, fileContent: ByteArray?) {
        val authority = request.authority ?: AUTHORITY
        val saveKey = request.key ?: run {
            Log.d("SharedDataPlugin", "saveSharedData: key is null")
            return
        }
        Log.d("SharedDataPlugin", "saveSharedData: authority = $authority, key = $saveKey, data = ${data != null}, fileContent = ${fileContent != null}")
        if (data != null) {
            val values = ContentValues().apply {
                put("key", saveKey)
                put("data", data)
            }
            val uri = context.contentResolver.insert(getDataUri(authority), values)
            Log.d("SharedDataPlugin", "saveSharedData: data inserted, uri = $uri")
        }
        if (fileContent != null) {
            val fileUri = Uri.withAppendedPath(getFileUri(authority), saveKey)
            val values = ContentValues().apply {
                put("key", saveKey)
                put("fileContent", fileContent)
            }
            val uri = context.contentResolver.insert(fileUri, values)
            Log.d("SharedDataPlugin", "saveSharedData: file inserted, uri = $uri")
        }
        Log.d("SharedDataPlugin", "saveSharedData: fileContent size = ${fileContent?.size}")
    }

    override fun deleteSharedData(request: SharedDataRequest) {
        val authority = request.authority ?: AUTHORITY
        val deleteKey = request.key ?: run {
            Log.d("SharedDataPlugin", "deleteSharedData: key is null")
            return
        }
        Log.d("SharedDataPlugin", "deleteSharedData: authority = $authority, key = $deleteKey")
        val dataRows = context.contentResolver.delete(
            getDataUri(authority),
            "key = ?",
            arrayOf(deleteKey)
        )
        Log.d("SharedDataPlugin", "deleteSharedData: data rows deleted = $dataRows")
        val fileRows = context.contentResolver.delete(
            getFileUri(authority),
            "key = ?",
            arrayOf(deleteKey)
        )
        Log.d("SharedDataPlugin", "deleteSharedData: file rows deleted = $fileRows")
    }

    override fun checkSharedData(request: SharedDataRequest): SharedDataResponse {
        val authority = request.authority ?: AUTHORITY
        val checkKey = request.key ?: run {
            Log.d("SharedDataPlugin", "checkSharedData: key is null")
            return SharedDataResponse(exists = false)
        }
        Log.d("SharedDataPlugin", "checkSharedData: authority = $authority, key = $checkKey")
        var exists = false
        context.contentResolver.query(
            getDataUri(authority),
            null,
            "key = ?",
            arrayOf(checkKey),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                exists = true
                Log.d("SharedDataPlugin", "checkSharedData: data exists for key $checkKey")
            }
        } ?: Log.d("SharedDataPlugin", "checkSharedData: query data cursor is null for key $checkKey")
        if (!exists) {
            val fileUri = Uri.withAppendedPath(getFileUri(authority), checkKey)
            try {
                context.contentResolver.openInputStream(fileUri)?.use {
                    exists = true
                    Log.d("SharedDataPlugin", "checkSharedData: file exists for key $checkKey")
                } ?: Log.d("SharedDataPlugin", "checkSharedData: file not found for key $checkKey")
            } catch (e: Exception) {
                Log.e("SharedDataPlugin", "checkSharedData: error reading file for key $checkKey", e)
            }
        }
        Log.d("SharedDataPlugin", "checkSharedData: exists = $exists")
        return SharedDataResponse(exists = exists)
    }
}