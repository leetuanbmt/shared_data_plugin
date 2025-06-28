package com.example.kansuke_app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import android.util.Log

class SharedDataProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "com.example.kansuke_app.provider"
        private const val FILE = 1
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "file/*", FILE)
        }
    }

    override fun onCreate(): Boolean = true

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.d("SharedDataProvider", "Insert uri: $uri, values: $values")
        return when (uriMatcher.match(uri)) {
            FILE -> {
                val key = values?.getAsString("key")
                val fileContent = values?.getAsByteArray("fileContent")
                Log.d("SharedDataProvider", "key: $key, fileContent null: ${fileContent == null}")
                if (key != null && fileContent != null) {
                    val file = File(context?.filesDir, key)
                    Log.d("SharedDataProvider", "Insert file: $key, exists before write: ${file.exists()}")
                    try {
                        FileOutputStream(file).use { it.write(fileContent) }
                        Log.d("SharedDataProvider", "Insert file: $key, exists after write: ${file.exists()}")
                        Log.d("SharedDataProvider", "Insert file: $key, size: ${file.length()}")
                        return Uri.parse("content://$AUTHORITY/file/$key")
                    } catch (e: Exception) {
                        Log.e("SharedDataProvider", "Error writing file: $key", e)
                    }
                }
                null
            }
            else -> null
        }
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        // Nếu bạn cần query data, xử lý ở đây
        return null
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            FILE -> "application/octet-stream" // hoặc xác định MIME type theo file
            else -> null
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            FILE -> {
                val key = uri.lastPathSegment ?: return 0
                val file = File(context?.filesDir, key)
                if (file.exists()) {
                    file.delete()
                    1
                } else 0
            }
            else -> 0
        }
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): android.os.ParcelFileDescriptor? {
        // Cho phép app khác đọc file qua ContentProvider
        val key = uri.lastPathSegment ?: return null
        val file = File(context?.filesDir, key)
        return android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
    }
}