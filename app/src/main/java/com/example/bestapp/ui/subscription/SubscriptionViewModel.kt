package com.example.bestapp.ui.subscription

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiSubscriptionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val subscriptionInfo: ApiSubscriptionInfo? = null,
    val errorMessage: String? = null,
    val isActivating: Boolean = false,
    val isCanceling: Boolean = false
)

class SubscriptionViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "SubscriptionViewModel"
    }
    
    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()
    
    init {
        loadSubscription()
    }
    
    fun loadSubscription() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = apiRepository.getMySubscription()
                result.onSuccess { info ->
                    _uiState.value = _uiState.value.copy(
                        subscriptionInfo = info,
                        isLoading = false
                    )
                    Log.d(TAG, "✅ Subscription loaded: type=${info.currentType}")
                }.onFailure { error ->
                    val errorMsg = error.message ?: "Ошибка загрузки подписки"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = errorMsg,
                        isLoading = false
                    )
                    Log.e(TAG, "❌ Error loading subscription: $errorMsg", error)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Ошибка загрузки подписки"
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMsg,
                    isLoading = false
                )
                Log.e(TAG, "❌ Exception loading subscription: $errorMsg", e)
            }
        }
    }
    
    fun activateSubscription(subscriptionType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActivating = true, errorMessage = null)
            try {
                val result = apiRepository.activateSubscription(subscriptionType)
                result.onSuccess {
                    Log.d(TAG, "✅ Subscription activated: $subscriptionType")
                    loadSubscription() // Обновляем информацию
                    _uiState.value = _uiState.value.copy(isActivating = false)
                }.onFailure { error ->
                    val errorMsg = error.message ?: "Ошибка активации подписки"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = errorMsg,
                        isActivating = false
                    )
                    Log.e(TAG, "❌ Error activating subscription: $errorMsg", error)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Ошибка активации подписки"
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMsg,
                    isActivating = false
                )
                Log.e(TAG, "❌ Exception activating subscription: $errorMsg", e)
            }
        }
    }
    
    fun cancelSubscription() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCanceling = true, errorMessage = null)
            try {
                val result = apiRepository.cancelSubscription()
                result.onSuccess {
                    Log.d(TAG, "✅ Subscription cancelled")
                    loadSubscription() // Обновляем информацию
                    _uiState.value = _uiState.value.copy(isCanceling = false)
                }.onFailure { error ->
                    val errorMsg = error.message ?: "Ошибка отмены подписки"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = errorMsg,
                        isCanceling = false
                    )
                    Log.e(TAG, "❌ Error canceling subscription: $errorMsg", error)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Ошибка отмены подписки"
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMsg,
                    isCanceling = false
                )
                Log.e(TAG, "❌ Exception canceling subscription: $errorMsg", e)
            }
        }
    }
    
    fun refresh() {
        loadSubscription()
    }
}

