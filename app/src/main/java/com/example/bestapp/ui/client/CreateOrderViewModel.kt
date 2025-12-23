package com.example.bestapp.ui.client

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.CreateOrderRequest
import com.example.bestapp.api.models.CreateOrderResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateOrderViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "CreateOrderViewModel"
    }
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _orderCreated = MutableStateFlow<CreateOrderResponse?>(null)
    val orderCreated: StateFlow<CreateOrderResponse?> = _orderCreated.asStateFlow()
    
    // Поля формы
    private val _deviceType = MutableStateFlow("")
    val deviceType: StateFlow<String> = _deviceType.asStateFlow()
    
    private val _deviceBrand = MutableStateFlow("")
    val deviceBrand: StateFlow<String> = _deviceBrand.asStateFlow()
    
    private val _deviceModel = MutableStateFlow("")
    val deviceModel: StateFlow<String> = _deviceModel.asStateFlow()
    
    private val _problemDescription = MutableStateFlow("")
    val problemDescription: StateFlow<String> = _problemDescription.asStateFlow()
    
    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()
    
    private val _arrivalTime = MutableStateFlow("")
    val arrivalTime: StateFlow<String> = _arrivalTime.asStateFlow()
    
    private val _isUrgent = MutableStateFlow(false)
    val isUrgent: StateFlow<Boolean> = _isUrgent.asStateFlow()
    
    fun setDeviceType(value: String) {
        _deviceType.value = value
    }
    
    fun setDeviceBrand(value: String) {
        _deviceBrand.value = value
    }
    
    fun setDeviceModel(value: String) {
        _deviceModel.value = value
    }
    
    fun setProblemDescription(value: String) {
        _problemDescription.value = value
    }
    
    fun setAddress(value: String) {
        _address.value = value
    }
    
    fun setArrivalTime(value: String) {
        _arrivalTime.value = value
    }
    
    fun setIsUrgent(value: Boolean) {
        _isUrgent.value = value
    }
    
    fun createOrder(latitude: Double = 56.859611, longitude: Double = 35.911896) {
        viewModelScope.launch {
            // Валидация
            if (_deviceType.value.isBlank()) {
                _errorMessage.value = "Укажите тип техники"
                return@launch
            }
            
            if (_problemDescription.value.isBlank()) {
                _errorMessage.value = "Опишите проблему"
                return@launch
            }
            
            if (_address.value.isBlank()) {
                _errorMessage.value = "Укажите адрес"
                return@launch
            }
            
            _isLoading.value = true
            _errorMessage.value = null
            
            val request = CreateOrderRequest(
                deviceType = _deviceType.value,
                deviceBrand = _deviceBrand.value.takeIf { it.isNotBlank() },
                deviceModel = _deviceModel.value.takeIf { it.isNotBlank() },
                problemDescription = _problemDescription.value,
                address = _address.value,
                latitude = latitude,
                longitude = longitude,
                arrivalTime = _arrivalTime.value.takeIf { it.isNotBlank() },
                orderType = if (_isUrgent.value) "urgent" else "regular"
            )
            
            val result = apiRepository.createOrder(request)
            result.onSuccess { response ->
                Log.d(TAG, "Order created: ${response.order.id}")
                _orderCreated.value = response
                clearForm()
            }.onFailure { error ->
                Log.e(TAG, "Failed to create order", error)
                _errorMessage.value = error.message ?: "Ошибка создания заказа"
            }
            
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearForm() {
        _deviceType.value = ""
        _deviceBrand.value = ""
        _deviceModel.value = ""
        _problemDescription.value = ""
        _address.value = ""
        _arrivalTime.value = ""
        _isUrgent.value = false
    }
    
    // Быстрое создание тестового заказа
    fun createTestOrder() {
        _deviceType.value = "Холодильник"
        _deviceBrand.value = "Samsung"
        _deviceModel.value = "RF23R62E3SR"
        _problemDescription.value = "Не морозит, издаёт странные звуки"
        _address.value = "Тверь, ул. Вагжанова, д. 15, кв. 42"
        _arrivalTime.value = "14:00 - 16:00"
        _isUrgent.value = true
        
        createOrder(56.856111, 35.924444)
    }
}







