package com.example.bestapp.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "RegistrationViewModel"
    }
    
    private val _uiState = MutableStateFlow<RegUiState>(RegUiState.Idle)
    val uiState: StateFlow<RegUiState> = _uiState
    
    init {
        // Инициализируем RetrofitClient при создании ViewModel
        RetrofitClient.initialize(application)
    }
    
    fun register(fullName: String, email: String, phone: String, spec: String, pwd: String, pwdConfirm: String) {
        if (!validate(fullName, email, phone, spec, pwd, pwdConfirm)) return
        
        _uiState.value = RegUiState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting registration for: $email")
                
                // Регистрируем через API
                val result = apiRepository.register(
                    email = email.trim(),
                    password = pwd,
                    name = fullName.trim(),
                    phone = phone.trim(),
                    role = "master"
                )
                
                result.onSuccess { response ->
                    Log.d(TAG, "Registration successful: ${response.user.name}, id: ${response.user.id}")
                    // Токен уже сохранен в ApiRepository.register()
                    _uiState.value = RegUiState.Success(response.user.id)
                }.onFailure { error ->
                    Log.e(TAG, "Registration failed: ${error.message}")
                    _uiState.value = RegUiState.Error(error.message ?: "Ошибка регистрации")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error", e)
                _uiState.value = RegUiState.Error(e.message ?: "Ошибка регистрации")
            }
        }
    }
    
    private fun validate(name: String, email: String, phone: String, spec: String, pwd: String, confirm: String): Boolean {
        when {
            name.isBlank() || email.isBlank() || phone.isBlank() || spec.isBlank() || pwd.isBlank() -> {
                _uiState.value = RegUiState.Error("Заполните все поля")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = RegUiState.Error("Неверный формат email")
                return false
            }
            pwd.length < 6 -> {
                _uiState.value = RegUiState.Error("Пароль должен быть минимум 6 символов")
                return false
            }
            pwd != confirm -> {
                _uiState.value = RegUiState.Error("Пароли не совпадают")
                return false
            }
            !phone.matches(Regex("^\\+?7\\d{10}$|^\\d{10}$")) -> {
                _uiState.value = RegUiState.Error("Неверный формат телефона")
                return false
            }
        }
        return true
    }
}

sealed class RegUiState {
    object Idle : RegUiState()
    object Loading : RegUiState()
    data class Success(val userId: Long) : RegUiState()
    data class Error(val message: String) : RegUiState()
}

