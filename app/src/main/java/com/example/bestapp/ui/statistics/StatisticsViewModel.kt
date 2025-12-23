package com.example.bestapp.ui.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val period: String = "month", // 'day', 'week', 'month', 'all'
    val stats: Map<String, Any>? = null,
    val errorMessage: String? = null
)

class StatisticsViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "StatisticsViewModel"
    }
    
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()
    
    init {
        loadStatistics()
    }
    
    fun loadStatistics(period: String? = null) {
        val selectedPeriod = period ?: _uiState.value.period
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, period = selectedPeriod)
            try {
                val result = apiRepository.getMasterStats(selectedPeriod)
                result.onSuccess { stats ->
                    _uiState.value = _uiState.value.copy(
                        stats = stats,
                        isLoading = false
                    )
                    Log.d(TAG, "Statistics loaded for period: $selectedPeriod")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка загрузки статистики",
                        isLoading = false
                    )
                    Log.e(TAG, "Error loading statistics", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Ошибка загрузки статистики",
                    isLoading = false
                )
                Log.e(TAG, "Error loading statistics", e)
            }
        }
    }
    
    fun setPeriod(period: String) {
        if (_uiState.value.period != period) {
            loadStatistics(period)
        }
    }
    
    fun refresh() {
        loadStatistics()
    }
}



