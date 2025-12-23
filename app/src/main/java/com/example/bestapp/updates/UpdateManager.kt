package com.example.bestapp.updates

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.bestapp.BuildConfig
import com.example.bestapp.api.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Менеджер обновлений приложения
 * Поддерживает:
 * - Проверку версий через API
 * - Автоматическое скачивание APK
 * - Установку обновлений
 * - Google Play In-App Updates (опционально)
 */
class UpdateManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "UpdateManager"
        private const val UPDATE_FILE_NAME = "app-update.apk"
    }

    private val apiRepository = ApiRepository()

    // Информация об обновлении
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    // Прогресс скачивания
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    // Статус проверки обновлений
    private val _updateCheckStatus = MutableStateFlow<UpdateCheckStatus>(UpdateCheckStatus.Idle)
    val updateCheckStatus: StateFlow<UpdateCheckStatus> = _updateCheckStatus.asStateFlow()

    /**
     * Проверить наличие обновлений
     */
    fun checkForUpdates() {
        scope.launch {
            try {
                _updateCheckStatus.value = UpdateCheckStatus.Checking
                Log.d(TAG, "🔍 Проверка обновлений...")

                val currentVersion = BuildConfig.VERSION_NAME
                Log.d(TAG, "Текущая версия: $currentVersion")

                val result = apiRepository.checkAppVersion(
                    platform = "android_master",
                    appVersion = currentVersion
                )

                result.onSuccess { response ->
                    val updateRequired = response["update_required"] as? Boolean ?: false
                    val forceUpdate = response["force_update"] as? Boolean ?: false
                    val newVersion = response["current_version"] as? String
                    val releaseNotes = response["release_notes"] as? String
                    val downloadUrl = response["download_url"] as? String

                    Log.d(TAG, "Update required: $updateRequired, force: $forceUpdate")
                    Log.d(TAG, "New version: $newVersion, URL: $downloadUrl")

                    if (updateRequired && newVersion != null) {
                        val updateInfo = UpdateInfo(
                            currentVersion = currentVersion,
                            newVersion = newVersion,
                            forceUpdate = forceUpdate,
                            releaseNotes = releaseNotes ?: "Доступно новое обновление",
                            downloadUrl = downloadUrl
                        )
                        _updateInfo.value = updateInfo
                        _updateCheckStatus.value = UpdateCheckStatus.UpdateAvailable(updateInfo)
                        Log.d(TAG, "✅ Доступно обновление: $currentVersion -> $newVersion")
                    } else {
                        _updateCheckStatus.value = UpdateCheckStatus.NoUpdateAvailable
                        Log.d(TAG, "✅ Приложение обновлено до последней версии")
                    }
                }

                result.onFailure { error ->
                    Log.e(TAG, "❌ Ошибка проверки обновлений: ${error.message}")
                    _updateCheckStatus.value = UpdateCheckStatus.Error(error.message ?: "Ошибка проверки")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка проверки обновлений", e)
                _updateCheckStatus.value = UpdateCheckStatus.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Скачать и установить обновление
     */
    fun downloadAndInstall(downloadUrl: String) {
        scope.launch {
            try {
                _downloadProgress.value = DownloadProgress(0, "Начало загрузки...")
                Log.d(TAG, "📥 Начало загрузки обновления: $downloadUrl")

                val apkFile = withContext(Dispatchers.IO) {
                    downloadApk(downloadUrl)
                }

                if (apkFile != null) {
                    _downloadProgress.value = DownloadProgress(100, "Загрузка завершена")
                    Log.d(TAG, "✅ Обновление скачано: ${apkFile.absolutePath}")
                    installApk(apkFile)
                } else {
                    _downloadProgress.value = null
                    _updateCheckStatus.value = UpdateCheckStatus.Error("Не удалось скачать обновление")
                    Log.e(TAG, "❌ Не удалось скачать обновление")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка загрузки обновления", e)
                _downloadProgress.value = null
                _updateCheckStatus.value = UpdateCheckStatus.Error("Ошибка загрузки: ${e.message}")
            }
        }
    }

    /**
     * Скачать APK файл
     */
    private suspend fun downloadApk(downloadUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection()
            connection.connect()

            val fileLength = connection.contentLength
            val inputStream = connection.getInputStream()

            // Сохраняем во внутреннее хранилище
            val outputFile = File(context.cacheDir, UPDATE_FILE_NAME)
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(4096)
            var total: Long = 0
            var count: Int

            while (inputStream.read(buffer).also { count = it } != -1) {
                total += count
                outputStream.write(buffer, 0, count)

                // Обновляем прогресс
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toInt()
                    _downloadProgress.value = DownloadProgress(
                        progress,
                        "Загрузка: ${total / 1024 / 1024} MB / ${fileLength / 1024 / 1024} MB"
                    )
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Log.d(TAG, "✅ APK скачан: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка скачивания APK", e)
            null
        }
    }

    /**
     * Установить APK
     */
    private fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ требует FileProvider
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive")

            Log.d(TAG, "🚀 Запуск установки APK")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка установки APK", e)
            _updateCheckStatus.value = UpdateCheckStatus.Error("Ошибка установки: ${e.message}")
        }
    }

    /**
     * Открыть страницу приложения в Google Play
     */
    fun openGooglePlay() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Если Google Play не установлен, открываем в браузере
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Сбросить статус проверки
     */
    fun resetStatus() {
        _updateCheckStatus.value = UpdateCheckStatus.Idle
        _updateInfo.value = null
        _downloadProgress.value = null
    }
}

/**
 * Информация об обновлении
 */
data class UpdateInfo(
    val currentVersion: String,
    val newVersion: String,
    val forceUpdate: Boolean,
    val releaseNotes: String,
    val downloadUrl: String?
)

/**
 * Прогресс загрузки
 */
data class DownloadProgress(
    val progress: Int,
    val message: String
)

/**
 * Статус проверки обновлений
 */
sealed class UpdateCheckStatus {
    object Idle : UpdateCheckStatus()
    object Checking : UpdateCheckStatus()
    object NoUpdateAvailable : UpdateCheckStatus()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckStatus()
    data class Error(val message: String) : UpdateCheckStatus()
}
