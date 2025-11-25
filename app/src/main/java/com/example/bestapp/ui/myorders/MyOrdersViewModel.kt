package com.example.bestapp.ui.myorders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.data.Order
import com.example.bestapp.data.RepairStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MyOrdersViewModel : ViewModel() {
    
    private val apiRepository = ApiRepository()
    
    // Поисковый запрос
    private val _searchQuery = MutableStateFlow("")
    
    // Состояние загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Мои заказы (принятые в работу) - in_progress и completed
    private val _myOrders = MutableStateFlow<List<Order>>(emptyList())
    val myOrders: StateFlow<List<Order>> = _myOrders.asStateFlow()
    
    // Отфильтрованные мои заказы
    private val _filteredMyOrders = MutableStateFlow<List<Order>>(emptyList())
    val filteredMyOrders: StateFlow<List<Order>> = _filteredMyOrders.asStateFlow()
    
    companion object {
        private const val TAG = "MyOrdersViewModel"
    }
    
    init {
        // Загружаем заказы при инициализации
        loadMyOrders()
        
        // Применяем поиск
        viewModelScope.launch {
            combine(myOrders, _searchQuery) { orders, query ->
                if (query.isBlank()) {
                    orders
                } else {
                    orders.filter { order ->
                        order.clientName.contains(query, ignoreCase = true) ||
                        order.deviceModel.contains(query, ignoreCase = true) ||
                        order.deviceBrand.contains(query, ignoreCase = true) ||
                        order.problemDescription.contains(query, ignoreCase = true) ||
                        order.id.toString().contains(query)
                    }
                }
            }.collect { filtered ->
                _filteredMyOrders.value = filtered
            }
        }
    }
    
    fun loadMyOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading my orders (in_progress and completed)")
                
                // Загружаем заказы in_progress
                val inProgressResult = apiRepository.getOrders(status = "in_progress")
                val inProgressOrders = inProgressResult.getOrElse { emptyList() }
                
                // Загружаем заказы completed
                val completedResult = apiRepository.getOrders(status = "completed")
                val completedOrders = completedResult.getOrElse { emptyList() }
                
                // Объединяем и конвертируем в Order
                val allApiOrders = inProgressOrders + completedOrders
                val orders = allApiOrders.map { apiOrder ->
                    Order(
                        id = apiOrder.id,
                        clientId = apiOrder.clientId,
                        clientName = apiOrder.clientName,
                        clientPhone = apiOrder.clientPhone,
                        clientAddress = apiOrder.address,
                        latitude = apiOrder.latitude,
                        longitude = apiOrder.longitude,
                        deviceType = apiOrder.deviceType,
                        deviceBrand = apiOrder.deviceBrand ?: "",
                        deviceModel = apiOrder.deviceModel ?: "",
                        problemDescription = apiOrder.problemDescription,
                        requestStatus = com.example.bestapp.data.OrderRequestStatus.NEW,
                        orderType = if (apiOrder.orderType == "urgent" || apiOrder.priority == "urgent") 
                            com.example.bestapp.data.OrderType.URGENT 
                        else 
                            com.example.bestapp.data.OrderType.REGULAR,
                        arrivalTime = apiOrder.arrivalTime,
                        status = when(apiOrder.repairStatus) {
                            "new" -> RepairStatus.NEW
                            "in_progress" -> RepairStatus.IN_PROGRESS
                            "completed" -> RepairStatus.COMPLETED
                            "cancelled" -> RepairStatus.CANCELLED
                            else -> RepairStatus.NEW
                        },
                        estimatedCost = apiOrder.estimatedCost,
                        createdAt = java.util.Date() // TODO: Parse from apiOrder.createdAt
                    )
                }
                
                _myOrders.value = orders
                Log.d(TAG, "Loaded ${orders.size} my orders (${inProgressOrders.size} in_progress, ${completedOrders.size} completed)")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading my orders", e)
                _myOrders.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshOrders() {
        loadMyOrders()
    }
    
    fun searchOrders(query: String) {
        _searchQuery.value = query
    }
}



