package com.bestapp.client.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.repository.ApiResult
import com.bestapp.client.di.AppContainer
import com.bestapp.client.services.FcmService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository,
    private val fcmService: FcmService = AppContainer.fcmService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            
            when (val result = repository.login(email, password)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState(isSuccess = true)
                    // Регистрируем FCM токен после успешного входа
                    android.util.Log.d("AuthViewModel", "Вход успешен, регистрируем FCM токен...")
                    fcmService.registerToken(viewModelScope)
                }
                is ApiResult.Error -> {
                    _uiState.value = AuthUiState(errorMessage = result.message)
                }
                ApiResult.Loading -> {
                    _uiState.value = AuthUiState(isLoading = true)
                }
            }
        }
    }

    fun register(name: String, email: String, password: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            
            when (val result = repository.register(name, email, password, phone)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState(isSuccess = true)
                    // Регистрируем FCM токен после успешной регистрации
                    android.util.Log.d("AuthViewModel", "Регистрация успешна, регистрируем FCM токен...")
                    fcmService.registerToken(viewModelScope)
                }
                is ApiResult.Error -> {
                    _uiState.value = AuthUiState(errorMessage = result.message)
                }
                ApiResult.Loading -> {
                    _uiState.value = AuthUiState(isLoading = true)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}

