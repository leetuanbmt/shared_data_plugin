package com.example.shared_data_plugin

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import java.io.File

class SharedDataProvider : ContentProvider() {
    companion object {
        fun getAuthority(context: Context?): String = context?.packageName + ".shared_data_provider"
        fun getContentUri(context: Context?): Uri = Uri.parse("content://" + getAuthority(context) + "/data")
        private const val CODE_DATA = 1
        private var uriMatcher: UriMatcher? = null
        fun getUriMatcher(context: Context?): UriMatcher {
            if (uriMatcher == null && context != null) {
                uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
                    addURI(getAuthority(context), "data", CODE_DATA)
                }
            }
            return uriMatcher!!
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (getUriMatcher(context).match(uri) == CODE_DATA) {
            val file = File(context?.filesDir, "shared_data.json")
            val data = if (file.exists()) file.readText() else ""
            val cursor = MatrixCursor(arrayOf("data"))
            cursor.addRow(arrayOf(data))
            return cursor
        }
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (getUriMatcher(context).match(uri) == CODE_DATA) {
            val data = values?.getAsString("data") ?: return null
            val file = File(context?.filesDir, "shared_data.json")
            file.writeText(data)
            return getContentUri(context)
        }
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        if (getUriMatcher(context).match(uri) == CODE_DATA) {
            val file = File(context?.filesDir, "shared_data.json")
            if (file.exists()) file.delete()
            return 1
        }
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = null
} 