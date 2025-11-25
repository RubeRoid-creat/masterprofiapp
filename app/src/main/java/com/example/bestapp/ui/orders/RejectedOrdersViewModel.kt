package com.example.bestapp.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiRejectedAssignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RejectedOrdersViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    private val _uiState = MutableStateFlow<RejectedOrdersUiState>(RejectedOrdersUiState.Loading)
    val uiState: StateFlow<RejectedOrdersUiState> = _uiState.asStateFlow()
    
    init {
        loadRejectedOrders()
    }
    
    fun loadRejectedOrders() {
        viewModelScope.launch {
            _uiState.value = RejectedOrdersUiState.Loading
            try {
                val result = apiRepository.getRejectedAssignments()
                result.fold(
                    onSuccess = { rejectedAssignments ->
                        _uiState.value = RejectedOrdersUiState.Success(rejectedAssignments)
                    },
                    onFailure = { error ->
                        _uiState.value = RejectedOrdersUiState.Error(error.message ?: "Неизвестная ошибка")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = RejectedOrdersUiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }
    
    fun refresh() {
        loadRejectedOrders()
    }
}

sealed class RejectedOrdersUiState {
    object Loading : RejectedOrdersUiState()
    data class Success(val rejectedAssignments: List<ApiRejectedAssignment>) : RejectedOrdersUiState()
    data class Error(val message: String) : RejectedOrdersUiState()
}

