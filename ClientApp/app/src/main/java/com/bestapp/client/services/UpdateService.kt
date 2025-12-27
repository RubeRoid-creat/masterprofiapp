package com.bestapp.client.services

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.bestapp.client.data.api.models.VersionCheckResponse
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.repository.ApiResult
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

class UpdateService(private val context: Context) {
    
    private val apiRepository: ApiRepository = AppContainer.apiRepository
    private var downloadId: Long = -1
    
    suspend fun checkForUpdate(currentVersion: String): ApiResult<VersionCheckResponse> {
        return withContext(Dispatchers.IO) {
            apiRepository.checkVersion(currentVersion)
        }
    }
    
    fun downloadUpdate(downloadUrl: String): Flow<DownloadProgress> {
        return flow {
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = Uri.parse(downloadUrl)
                
                // Создаем директорию для сохранения APK если её нет
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) 
                    ?: context.filesDir
                downloadDir?.mkdirs()
                
                val fileName = "app-update-${System.currentTimeMillis()}.apk"
                val destinationFile = File(downloadDir ?: context.cacheDir, fileName)
                
                // Удаляем старые APK файлы
                downloadDir?.listFiles()?.filter { it.name.startsWith("app-update-") }?.forEach { it.delete() }
                
                val request = DownloadManager.Request(uri)
                    .setTitle("Обновление приложения")
                    .setDescription("Загрузка новой версии...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationUri(Uri.fromFile(destinationFile))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                
                downloadId = downloadManager.enqueue(request)
                
                // Отслеживаем прогресс загрузки через polling
                var downloadComplete = false
                
                while (!downloadComplete && downloadId != -1L) {
                    val query = DownloadManager.Query()
                    query.setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        
                        val status = cursor.getInt(statusIndex)
                        val downloaded = cursor.getLong(downloadedIndex)
                        val total = cursor.getLong(totalIndex)
                        
                        if (status == DownloadManager.STATUS_RUNNING && total > 0) {
                            val progress = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            emit(DownloadProgress.Progress(progress))
                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloadComplete = true
                            if (destinationFile.exists()) {
                                emit(DownloadProgress.Success(destinationFile))
                                break
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloadComplete = true
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = cursor.getInt(reasonIndex)
                            emit(DownloadProgress.Error("Ошибка загрузки: $reason"))
                            break
                        }
                    }
                    cursor.close()
                    
                    delay(500) // Проверяем каждые 500мс
                }
            } catch (e: Exception) {
                emit(DownloadProgress.Error(e.message ?: "Ошибка начала загрузки"))
            }
        }
    }
    
    fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("UpdateService", "Ошибка установки APK", e)
            throw e
        }
    }
    
}

sealed class DownloadProgress {
    data class Progress(val percent: Int) : DownloadProgress()
    data class Success(val file: File) : DownloadProgress()
    data class Error(val message: String) : DownloadProgress()
}

