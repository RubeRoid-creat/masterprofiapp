package com.example.bestapp.ui.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiWorkReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow as FlowStateFlow

data class ReportsListUiState(
    val isLoading: Boolean = false,
    val reports: List<ApiWorkReport> = emptyList(),
    val errorMessage: String? = null
)

class ReportsListViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "ReportsListViewModel"
    }
    
    private val _uiState = MutableStateFlow(ReportsListUiState())
    val uiState: StateFlow<ReportsListUiState> = _uiState.asStateFlow()
    
    val reports = _uiState.asStateFlow().map { it.reports }
    
    init {
        loadReports()
    }
    
    fun loadReports(orderId: Long? = null, status: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = apiRepository.getReports(orderId, status)
                result.onSuccess { reportsList ->
                    _uiState.value = _uiState.value.copy(
                        reports = reportsList,
                        isLoading = false
                    )
                    Log.d(TAG, "Reports loaded: ${reportsList.size}")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка загрузки отчетов",
                        isLoading = false
                    )
                    Log.e(TAG, "Error loading reports", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Ошибка загрузки отчетов",
                    isLoading = false
                )
                Log.e(TAG, "Error loading reports", e)
            }
        }
    }
}

