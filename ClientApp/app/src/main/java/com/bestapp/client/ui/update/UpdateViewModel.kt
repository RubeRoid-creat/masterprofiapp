package com.bestapp.client.ui.update

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.api.models.VersionCheckResponse
import com.bestapp.client.services.DownloadProgress
import com.bestapp.client.services.UpdateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val isLoading: Boolean = false,
    val versionInfo: VersionCheckResponse? = null,
    val showUpdateDialog: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val errorMessage: String? = null
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val updateService = UpdateService(application)
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()
    
    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val currentVersion = getAppVersion()
                when (val result = updateService.checkForUpdate(currentVersion)) {
                    is com.bestapp.client.data.repository.ApiResult.Success -> {
                        if (result.data.updateRequired) {
                            _uiState.value = _uiState.value.copy(
                                versionInfo = result.data,
                                showUpdateDialog = true,
                                isLoading = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                    }
                    is com.bestapp.client.data.repository.ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun startUpdate() {
        val downloadUrl = _uiState.value.versionInfo?.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "URL загрузки не указан"
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(isDownloading = true, downloadProgress = 0)
        
        viewModelScope.launch {
            updateService.downloadUpdate(downloadUrl)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        errorMessage = e.message ?: "Ошибка загрузки"
                    )
                }
                .collect { progress ->
                    when (progress) {
                        is DownloadProgress.Progress -> {
                            _uiState.value = _uiState.value.copy(
                                downloadProgress = progress.percent
                            )
                        }
                        is DownloadProgress.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isDownloading = false,
                                downloadProgress = 100
                            )
                            try {
                                updateService.installApk(progress.file)
                            } catch (e: Exception) {
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = "Ошибка установки: ${e.message}"
                                )
                            }
                        }
                        is DownloadProgress.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isDownloading = false,
                                errorMessage = progress.message
                            )
                        }
                    }
                }
        }
    }
    
    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showUpdateDialog = false)
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }
}
