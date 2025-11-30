package com.example.bestapp.ui.mlm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiMLMStatistics
import kotlinx.coroutines.launch

class MLMViewModel(private val apiRepository: ApiRepository) : ViewModel() {
    
    private val _statistics = MutableLiveData<ApiMLMStatistics?>()
    val statistics: LiveData<ApiMLMStatistics?> = _statistics
    
    private val _referralCode = MutableLiveData<String?>()
    val referralCode: LiveData<String?> = _referralCode
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadMLMData() {
        viewModelScope.launch {
            _error.value = null
            
            // Загружаем статистику
            apiRepository.getMLMStatistics().onSuccess { response ->
                _statistics.value = response.statistics
                _error.value = null
            }.onFailure { exception ->
                val errorMessage = exception.message ?: "Неизвестная ошибка"
                if (errorMessage.contains("404") || errorMessage.contains("не найден")) {
                    // Если данные не найдены, показываем пустую статистику
                    _statistics.value = null
                    _error.value = "MLM данные еще не доступны. Начните приглашать мастеров в свою команду!"
                } else {
                    _error.value = "Ошибка загрузки статистики: $errorMessage"
                }
            }
            
            // Загружаем реферальный код
            apiRepository.getMLMReferralCode().onSuccess { response ->
                _referralCode.value = response.referralCode
            }.onFailure { exception ->
                val errorMessage = exception.message ?: "Неизвестная ошибка"
                if (errorMessage.contains("404") || errorMessage.contains("не найден")) {
                    // Генерируем код на основе user_id, если API недоступен
                    _referralCode.value = "Загрузка..."
                    _error.value = _error.value ?: "Реферальный код будет доступен после полной регистрации"
                } else {
                    _error.value = _error.value?.let { "$it\nОшибка загрузки кода: $errorMessage" }
                        ?: "Ошибка загрузки реферального кода: $errorMessage"
                }
            }
        }
    }
}

class MLMViewModelFactory(private val apiRepository: ApiRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MLMViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MLMViewModel(apiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

