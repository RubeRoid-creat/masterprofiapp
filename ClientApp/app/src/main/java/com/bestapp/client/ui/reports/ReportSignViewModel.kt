package com.bestapp.client.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.repository.ApiResult
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportSignViewModel(
    private val reportId: Long,
    private val apiRepository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReportSignUiState>(ReportSignUiState.Loading)
    val uiState: StateFlow<ReportSignUiState> = _uiState.asStateFlow()
    
    fun loadReport() {
        viewModelScope.launch {
            try {
                _uiState.value = ReportSignUiState.Loading
                when (val result = apiRepository.getReportById(reportId)) {
                    is ApiResult.Success -> {
                        val report = result.data
                        android.util.Log.d("ReportSignViewModel", "Отчет загружен: id=${report.id}, workDescription=${report.workDescription}, totalCost=${report.totalCost}")
                        _uiState.value = ReportSignUiState.Success(
                            report = report,
                            isSigning = false,
                            signSuccess = false
                        )
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("ReportSignViewModel", "Ошибка загрузки отчета: ${result.message}")
                        _uiState.value = ReportSignUiState.Error(result.message)
                    }
                    else -> {
                        android.util.Log.e("ReportSignViewModel", "Неизвестный результат загрузки отчета")
                        _uiState.value = ReportSignUiState.Error("Неизвестная ошибка")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportSignViewModel", "Исключение при загрузке отчета", e)
                _uiState.value = ReportSignUiState.Error(e.message ?: "Ошибка загрузки отчета")
            }
        }
    }
    
    fun signReport(signature: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ReportSignUiState.Success) {
                _uiState.value = currentState.copy(isSigning = true)
                
                when (val result = apiRepository.signReport(reportId, signature)) {
                    is ApiResult.Success -> {
                        _uiState.value = currentState.copy(
                            isSigning = false,
                            signSuccess = true
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = ReportSignUiState.Error(result.message)
                    }
                    else -> {
                        _uiState.value = currentState.copy(isSigning = false)
                    }
                }
            }
        }
    }
    
    companion object {
        fun provideFactory(reportId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReportSignViewModel(reportId) as T
                }
            }
        }
    }
}

sealed class ReportSignUiState {
    object Loading : ReportSignUiState()
    data class Success(
        val report: com.bestapp.client.data.api.models.WorkReportDto,
        val isSigning: Boolean = false,
        val signSuccess: Boolean = false
    ) : ReportSignUiState()
    data class Error(val message: String) : ReportSignUiState()
}

