package com.example.bestapp.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.auth.AuthManager
import com.example.bestapp.auth.SimpleUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)
    private val userRepository = SimpleUserRepository(application)
    
    private val _uiState = MutableStateFlow<RegUiState>(RegUiState.Idle)
    val uiState: StateFlow<RegUiState> = _uiState
    
    fun register(fullName: String, email: String, phone: String, spec: String, pwd: String, pwdConfirm: String) {
        if (!validate(fullName, email, phone, spec, pwd, pwdConfirm)) return
        
        _uiState.value = RegUiState.Loading
        viewModelScope.launch {
            try {
                if (userRepository.isEmailExists(email.trim())) {
                    _uiState.value = RegUiState.Error("Email уже зарегистрирован")
                    return@launch
                }
                
                val userId = userRepository.register(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    passwordHash = authManager.hashPassword(pwd),
                    specialization = spec.trim()
                )
                authManager.saveUserId(userId)
                _uiState.value = RegUiState.Success(userId)
            } catch (e: Exception) {
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

