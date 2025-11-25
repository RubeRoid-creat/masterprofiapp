package com.example.bestapp.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.RetrofitClient
import com.example.bestapp.api.models.ApiUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _currentUser = MutableStateFlow<ApiUser?>(null)
    val currentUser: StateFlow<ApiUser?> = _currentUser.asStateFlow()
    
    init {
        // Проверяем, есть ли сохраненный токен
        checkAuthToken()
    }
    
    private fun checkAuthToken() {
        val token = RetrofitClient.getAuthToken()
        if (token != null) {
            _isLoggedIn.value = true
            Log.d(TAG, "Found saved auth token")
        }
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = apiRepository.login(email, password)
            result.onSuccess { response ->
                _isLoggedIn.value = true
                _currentUser.value = response.user
                _errorMessage.value = null
                Log.d(TAG, "Login successful: ${response.user.name}")
            }.onFailure { error ->
                _isLoggedIn.value = false
                _errorMessage.value = error.message ?: "Ошибка входа"
                Log.e(TAG, "Login failed", error)
            }
            
            _isLoading.value = false
        }
    }
    
    fun register(email: String, password: String, name: String, phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = apiRepository.register(email, password, name, phone, "master")
            result.onSuccess { response ->
                _isLoggedIn.value = true
                _currentUser.value = response.user
                _errorMessage.value = null
                Log.d(TAG, "Registration successful: ${response.user.name}")
            }.onFailure { error ->
                _isLoggedIn.value = false
                _errorMessage.value = error.message ?: "Ошибка регистрации"
                Log.e(TAG, "Registration failed", error)
            }
            
            _isLoading.value = false
        }
    }
    
    fun logout() {
        RetrofitClient.setAuthToken(null)
        _isLoggedIn.value = false
        _currentUser.value = null
        Log.d(TAG, "Logged out")
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    // Быстрый вход для тестирования
    fun quickLoginAsMaster(masterIndex: Int = 0) {
        val credentials = listOf(
            "smirnov@example.com" to "password123",
            "kuznetsov@example.com" to "password123",
            "popov@example.com" to "password123"
        )
        
        val (email, password) = credentials.getOrElse(masterIndex) { credentials[0] }
        login(email, password)
    }
}







