package com.example.bestapp.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.RetrofitClient
import com.example.bestapp.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val apiRepository = ApiRepository()
    private val appContext = application
    
    companion object {
        private const val TAG = "LoginViewModel"
    }
    
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState
    
    init {
        // Инициализируем RetrofitClient при создании ViewModel
        RetrofitClient.initialize(application)
    }
    
    fun login(email: String, password: String) {
        if (!validateInput(email, password)) return
        
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting login for: $email")
                val result = apiRepository.login(email.trim(), password)
                result.onSuccess { loginResponse ->
                    Log.d(TAG, "Login successful: ${loginResponse.user.name}, role: ${loginResponse.user.role}")
                    // Токен уже сохранен в ApiRepository.login()
                    // Сохраняем userId в AuthManager
                    val authManager = AuthManager(appContext)
                    authManager.saveUserId(loginResponse.user.id)
                    Log.d(TAG, "UserId saved: ${loginResponse.user.id}")
                    _uiState.value = LoginUiState.Success(loginResponse.user.id)
                }.onFailure { error ->
                    Log.e(TAG, "Login failed: ${error.message}")
                    _uiState.value = LoginUiState.Error(error.message ?: "Ошибка входа")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                _uiState.value = LoginUiState.Error(e.message ?: "Ошибка входа")
            }
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Заполните все поля")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = LoginUiState.Error("Неверный формат email")
            return false
        }
        return true
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userId: Long) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

