package com.bestapp.client.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.repository.ApiResult
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentsUiState(
    val isLoading: Boolean = false,
    val payments: List<PaymentItem> = emptyList(),
    val errorMessage: String? = null
)

class PaymentsViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentsUiState())
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    fun loadPayments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.getMyPayments()) {
                is ApiResult.Success -> {
                    val paymentItems = result.data.map { payment ->
                        PaymentItem(
                            id = payment.id,
                            orderId = payment.orderId,
                            orderNumber = payment.orderNumber,
                            amount = payment.amount,
                            paymentMethod = payment.paymentMethod,
                            paymentStatus = payment.paymentStatus,
                            deviceType = payment.deviceType,
                            createdAt = payment.createdAt
                        )
                    }
                    _uiState.value = PaymentsUiState(payments = paymentItems)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}







