package com.example.bestapp.ui.client

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.CreateOrderRequest
import com.example.bestapp.api.models.CreateOrderResponse
import com.example.bestapp.ui.orders.PartEntry
import com.example.bestapp.ui.orders.WorkEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

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
    
    private val _selectedWorks = MutableStateFlow<List<WorkEntry>>(emptyList())
    val selectedWorks: StateFlow<List<WorkEntry>> = _selectedWorks.asStateFlow()
    
    private val _selectedParts = MutableStateFlow<List<PartEntry>>(emptyList())
    val selectedParts: StateFlow<List<PartEntry>> = _selectedParts.asStateFlow()
    
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
    
    fun setSelectedWorks(works: List<WorkEntry>) {
        _selectedWorks.value = works
    }
    
    fun setSelectedParts(parts: List<PartEntry>) {
        _selectedParts.value = parts
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
            
            // Формируем полное описание с выбранными работами и запчастями
            val fullDescription = buildString {
                append(_problemDescription.value)
                
                if (_selectedWorks.value.isNotEmpty() || _selectedParts.value.isNotEmpty()) {
                    if (isNotEmpty()) appendLine().appendLine()
                    
                    if (_selectedWorks.value.isNotEmpty()) {
                        appendLine("Предполагаемые работы:")
                        _selectedWorks.value.forEachIndexed { index, work ->
                            val price = if (work.price != null) String.format(Locale.getDefault(), " (%.0f ₽)", work.price) else ""
                            appendLine("${index + 1}. ${work.description}$price")
                        }
                    }
                    
                    if (_selectedParts.value.isNotEmpty()) {
                        if (isNotEmpty()) appendLine()
                        appendLine("Предполагаемые запчасти:")
                        _selectedParts.value.forEachIndexed { index, part ->
                            val totalCost = part.cost * part.quantity
                            val costStr = if (totalCost > 0) String.format(Locale.getDefault(), " (%.0f ₽)", totalCost) else ""
                            appendLine("${index + 1}. ${part.name} - ${part.quantity} шт.$costStr")
                        }
                    }
                }
            }.trim()
            
            val request = CreateOrderRequest(
                deviceType = _deviceType.value,
                deviceBrand = _deviceBrand.value.takeIf { it.isNotBlank() },
                deviceModel = _deviceModel.value.takeIf { it.isNotBlank() },
                problemDescription = fullDescription,
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
        _selectedWorks.value = emptyList()
        _selectedParts.value = emptyList()
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







