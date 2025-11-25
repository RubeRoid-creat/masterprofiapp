package com.bestapp.client.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object MediaUtils {
    
    /**
     * Создает MultipartBody.Part из URI файла
     */
    suspend fun createMultipartPart(
        context: Context,
        uri: Uri,
        partName: String = "files"
    ): MultipartBody.Part? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null
            
            // Создаем временный файл
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            // Определяем MIME тип
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            
            MultipartBody.Part.createFormData(partName, getFileName(context, uri), requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Получает имя файла из URI
     */
    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        val finalResult = result ?: uri.path?.let {
            val cut = it.lastIndexOf('/')
            if (cut != -1) {
                it.substring(cut + 1)
            } else {
                it
            }
        } ?: "file"
        return finalResult
    }
    
    /**
     * Определяет тип медиа по URI
     */
    fun getMediaType(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        return when {
            mimeType.startsWith("image/") -> "photo"
            mimeType.startsWith("video/") -> "video"
            else -> "photo" // По умолчанию
        }
    }
}

