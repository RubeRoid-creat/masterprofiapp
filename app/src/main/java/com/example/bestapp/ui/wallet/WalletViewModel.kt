package com.example.bestapp.ui.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiTransaction
import com.example.bestapp.api.models.ApiWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WalletUiState(
    val isLoading: Boolean = false,
    val wallet: ApiWallet? = null,
    val transactions: List<ApiTransaction> = emptyList(),
    val errorMessage: String? = null,
    val isRequestingPayout: Boolean = false,
    val isTopupInProgress: Boolean = false
)

class WalletViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "WalletViewModel"
    }
    
    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    
    init {
        loadWallet()
        loadTransactions()
    }
    
    fun loadWallet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            Log.d(TAG, "Loading wallet...")
            try {
                val result = apiRepository.getWallet()
                result.onSuccess { wallet ->
                    _uiState.value = _uiState.value.copy(
                        wallet = wallet,
                        isLoading = false
                    )
                    Log.d(TAG, "✅ Wallet loaded successfully: balance=${wallet.balance}")
                }.onFailure { error ->
                    val errorMsg = error.message ?: "Ошибка загрузки кошелька"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = errorMsg,
                        isLoading = false
                    )
                    Log.e(TAG, "❌ Error loading wallet: $errorMsg", error)
                    Log.e(TAG, "Error details: ${error.stackTraceToString()}")
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Ошибка загрузки кошелька"
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMsg,
                    isLoading = false
                )
                Log.e(TAG, "❌ Exception loading wallet: $errorMsg", e)
                Log.e(TAG, "Exception details: ${e.stackTraceToString()}")
            }
        }
    }
    
    fun loadTransactions(limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            try {
                val result = apiRepository.getTransactions(limit, offset)
                result.onSuccess { transactions ->
                    _uiState.value = _uiState.value.copy(transactions = transactions)
                    Log.d(TAG, "Transactions loaded: ${transactions.size}")
                }.onFailure { error ->
                    Log.e(TAG, "Error loading transactions", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading transactions", e)
            }
        }
    }
    
    fun requestPayout(amount: Double, payoutMethod: String = "bank") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRequestingPayout = true, errorMessage = null)
            try {
                val result = apiRepository.requestPayout(amount, payoutMethod)
                result.onSuccess { transaction ->
                    _uiState.value = _uiState.value.copy(
                        isRequestingPayout = false
                    )
                    // Обновляем кошелек и транзакции
                    loadWallet()
                    loadTransactions()
                    Log.d(TAG, "Payout requested: ${transaction.id}")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка запроса выплаты",
                        isRequestingPayout = false
                    )
                    Log.e(TAG, "Error requesting payout", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Ошибка запроса выплаты",
                    isRequestingPayout = false
                )
                Log.e(TAG, "Error requesting payout", e)
            }
        }
    }
    
    fun topupWallet(amount: Double, paymentMethod: String = "card", description: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTopupInProgress = true, errorMessage = null)
            try {
                val result = apiRepository.topupWallet(amount, paymentMethod, description)
                result.onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isTopupInProgress = false
                    )
                    // Обновляем кошелек и транзакции
                    loadWallet()
                    loadTransactions()
                    Log.d(TAG, "Wallet topped up: ${response.transaction.id}, new balance: ${response.newBalance}")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка пополнения кошелька",
                        isTopupInProgress = false
                    )
                    Log.e(TAG, "Error topup wallet", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Ошибка пополнения кошелька",
                    isTopupInProgress = false
                )
                Log.e(TAG, "Error topup wallet", e)
            }
        }
    }
    
    fun refresh() {
        loadWallet()
        loadTransactions()
    }
}

