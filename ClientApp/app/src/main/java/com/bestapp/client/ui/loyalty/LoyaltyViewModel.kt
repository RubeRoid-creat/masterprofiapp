package com.bestapp.client.ui.loyalty

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

class LoyaltyViewModel(
    private val apiRepository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LoyaltyUiState>(LoyaltyUiState.Loading)
    val uiState: StateFlow<LoyaltyUiState> = _uiState.asStateFlow()
    
    fun loadLoyaltyData() {
        viewModelScope.launch {
            _uiState.value = LoyaltyUiState.Loading
            
            // Загружаем баланс и историю параллельно
            val balanceResult = apiRepository.getLoyaltyBalance()
            val historyResult = apiRepository.getLoyaltyHistory(50)
            val infoResult = apiRepository.getLoyaltyInfo()
            
            when {
                balanceResult is ApiResult.Success && 
                historyResult is ApiResult.Success && 
                infoResult is ApiResult.Success -> {
                    _uiState.value = LoyaltyUiState.Success(
                        balance = balanceResult.data.balance,
                        config = balanceResult.data.config,
                        history = historyResult.data,
                        info = infoResult.data
                    )
                }
                balanceResult is ApiResult.Error -> {
                    _uiState.value = LoyaltyUiState.Error(balanceResult.message)
                }
                historyResult is ApiResult.Error -> {
                    _uiState.value = LoyaltyUiState.Error(historyResult.message)
                }
                infoResult is ApiResult.Error -> {
                    _uiState.value = LoyaltyUiState.Error(infoResult.message)
                }
                else -> {
                    _uiState.value = LoyaltyUiState.Error("Неизвестная ошибка")
                }
            }
        }
    }
    
    fun usePoints(points: Int, orderId: Long? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is LoyaltyUiState.Success) {
                _uiState.value = currentState.copy(isUsingPoints = true)
                
                when (val result = apiRepository.useLoyaltyPoints(points, orderId)) {
                    is ApiResult.Success -> {
                        // Обновляем данные после использования баллов
                        loadLoyaltyData()
                    }
                    is ApiResult.Error -> {
                        _uiState.value = currentState.copy(
                            isUsingPoints = false,
                            errorMessage = result.message
                        )
                    }
                    else -> {
                        _uiState.value = currentState.copy(isUsingPoints = false)
                    }
                }
            }
        }
    }
    
    fun clearError() {
        val currentState = _uiState.value
        if (currentState is LoyaltyUiState.Success) {
            _uiState.value = currentState.copy(errorMessage = null)
        }
    }
    
    companion object {
        fun provideFactory(): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoyaltyViewModel() as T
                }
            }
        }
    }
}

sealed class LoyaltyUiState {
    object Loading : LoyaltyUiState()
    data class Success(
        val balance: Int,
        val config: com.bestapp.client.data.api.models.LoyaltyConfigDto,
        val history: com.bestapp.client.data.api.models.LoyaltyHistoryDto,
        val info: com.bestapp.client.data.api.models.LoyaltyInfoDto,
        val isUsingPoints: Boolean = false,
        val errorMessage: String? = null
    ) : LoyaltyUiState()
    data class Error(val message: String) : LoyaltyUiState()
}

