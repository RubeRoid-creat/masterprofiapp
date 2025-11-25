package com.example.bestapp.ui.promotion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiPromotionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PromotionUiState(
    val isLoading: Boolean = false,
    val promotionInfo: ApiPromotionInfo? = null,
    val promotionTypes: Map<String, com.example.bestapp.api.models.ApiPromotionType> = emptyMap(),
    val errorMessage: String? = null,
    val isPurchasing: Boolean = false,
    val isCanceling: Boolean = false
)

class PromotionViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "PromotionViewModel"
    }
    
    private val _uiState = MutableStateFlow(PromotionUiState())
    val uiState: StateFlow<PromotionUiState> = _uiState.asStateFlow()
    
    init {
        loadPromotions()
        loadPromotionTypes()
    }
    
    fun loadPromotions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = apiRepository.getMyPromotions()
                result.onSuccess { info ->
                    _uiState.value = _uiState.value.copy(
                        promotionInfo = info,
                        isLoading = false
                    )
                    Log.d(TAG, "✅ Promotions loaded: active=${info.activePromotions?.size ?: 0}")
                }.onFailure { error ->
                    val errorMsg = error.message ?: "Ошибка загрузки продвижений"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = errorMsg,
                        isLoading = false
                    )
                    Log.e(TAG, "❌ Error loading promotions: $errorMsg", error)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Ошибка загрузки продвижений"
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMsg,
                    isLoading = false
                )
                Log.e(TAG, "❌ Exception loading promotions: $errorMsg", e)
            }
        }
    }
    
    fun loadPromotionTypes() {
        viewModelScope.launch {
            try {
                val result = apiRepository.getPromotionTypes()
                result.onSuccess { types ->
                    _uiState.value = _uiState.value.copy(promotionTypes = types)
                    Log.d(TAG, "✅ Promotion types loaded: ${types.size}")
                }.onFailure { error ->
                    Log.e(TAG, "Error loading promotion types", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading promotion types", e)
            }
        }
    }
    
    fun purchasePromotion(promotionType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPurchasing = true, errorMessage = null)
            try {
                val result = apiRepository.purchasePromotion(promotionType)
                result.onSuccess {
                    Log.d(TAG, "✅ Promotion purchased: $promotionType")
                    loadPromotions() // Обновляем информацию
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                }.onFailure { error ->
                    val errorMsg = error.message ?: "Ошибка покупки продвижения"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = errorMsg,
                        isPurchasing = false
                    )
                    Log.e(TAG, "❌ Error purchasing promotion: $errorMsg", error)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Ошибка покупки продвижения"
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMsg,
                    isPurchasing = false
                )
                Log.e(TAG, "❌ Exception purchasing promotion: $errorMsg", e)
            }
        }
    }
    
    fun cancelPromotion(promotionId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCanceling = true, errorMessage = null)
            try {
                val result = apiRepository.cancelPromotion(promotionId)
                result.onSuccess {
                    Log.d(TAG, "✅ Promotion cancelled: $promotionId")
                    loadPromotions() // Обновляем информацию
                    _uiState.value = _uiState.value.copy(isCanceling = false)
                }.onFailure { error ->
                    val errorMsg = error.message ?: "Ошибка отмены продвижения"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = errorMsg,
                        isCanceling = false
                    )
                    Log.e(TAG, "❌ Error canceling promotion: $errorMsg", error)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Ошибка отмены продвижения"
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMsg,
                    isCanceling = false
                )
                Log.e(TAG, "❌ Exception canceling promotion: $errorMsg", e)
            }
        }
    }
    
    fun refresh() {
        loadPromotions()
        loadPromotionTypes()
    }
}

