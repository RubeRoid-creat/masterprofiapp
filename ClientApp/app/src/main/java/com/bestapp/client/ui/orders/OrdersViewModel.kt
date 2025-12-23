package com.bestapp.client.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.api.models.CreateOrderRequest
import com.bestapp.client.data.api.models.OrderDto
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.repository.ApiResult
import com.bestapp.client.data.websocket.WebSocketEvent
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderDto> = emptyList(),
    val errorMessage: String? = null
)

data class CreateOrderUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val orderId: Long? = null,
    val errorMessage: String? = null
)

data class OrderDetailsUiState(
    val isLoading: Boolean = false,
    val order: OrderDto? = null,
    val statusHistory: List<com.bestapp.client.data.api.models.OrderStatusHistoryDto>? = null,
    val errorMessage: String? = null
)

class OrdersViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {

    private val webSocketService = AppContainer.webSocketService

    private val _ordersUiState = MutableStateFlow(OrdersUiState())
    val ordersUiState: StateFlow<OrdersUiState> = _ordersUiState.asStateFlow()

    private val _createOrderUiState = MutableStateFlow(CreateOrderUiState())
    val createOrderUiState: StateFlow<CreateOrderUiState> = _createOrderUiState.asStateFlow()

    private val _orderDetailsUiState = MutableStateFlow(OrderDetailsUiState())
    val orderDetailsUiState: StateFlow<OrderDetailsUiState> = _orderDetailsUiState.asStateFlow()
    
    private val _webSocketNotification = MutableStateFlow<String?>(null)
    val webSocketNotification: StateFlow<String?> = _webSocketNotification.asStateFlow()

    init {
        // Подключаемся к WebSocket и слушаем события
        viewModelScope.launch {
            webSocketService.connect()
        }
        
        // Слушаем события WebSocket
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.OrderStatusChanged -> {
                        _webSocketNotification.value = event.message
                        // Обновляем заказы если статус изменился
                        loadOrders()
                        // Если открыт экран деталей этого заказа, обновляем его
                        if (_orderDetailsUiState.value.order?.id == event.orderId) {
                            loadOrderDetails(event.orderId)
                        }
                    }
                    is WebSocketEvent.Error -> {
                        _webSocketNotification.value = "Ошибка: ${event.message}"
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _ordersUiState.value = OrdersUiState(isLoading = true)
            
            when (val result = repository.getOrders()) {
                is ApiResult.Success -> {
                    _ordersUiState.value = OrdersUiState(orders = result.data)
                }
                is ApiResult.Error -> {
                    _ordersUiState.value = OrdersUiState(errorMessage = result.message)
                }
                ApiResult.Loading -> {
                    _ordersUiState.value = OrdersUiState(isLoading = true)
                }
            }
        }
    }

    fun createOrder(
        deviceType: String,
        deviceCategory: String? = null,
        deviceBrand: String? = null,
        deviceModel: String? = null,
        deviceSerialNumber: String? = null,
        deviceYear: Int? = null,
        warrantyStatus: String? = null,
        problemShortDescription: String? = null,
        problemDescription: String,
        problemWhenStarted: String? = null,
        problemConditions: String? = null,
        problemErrorCodes: String? = null,
        problemAttemptedFixes: String? = null,
        address: String,
        addressStreet: String? = null,
        addressBuilding: String? = null,
        addressApartment: String? = null,
        addressFloor: Int? = null,
        addressEntranceCode: String? = null,
        addressLandmark: String? = null,
        latitude: Double,
        longitude: Double,
        arrivalTime: String? = null,
        desiredRepairDate: String? = null,
        urgency: String? = null,
        clientBudget: Double? = null,
        paymentType: String? = null,
        intercomWorking: Boolean? = true,
        parkingAvailable: Boolean? = true,
        hasPets: Boolean? = false,
        hasSmallChildren: Boolean? = false,
        preferredContactMethod: String? = "call",
        isUrgent: Boolean = false
    ) {
        viewModelScope.launch {
            _createOrderUiState.value = CreateOrderUiState(isLoading = true)
            
            val request = CreateOrderRequest(
                address = address.trim(),
                latitude = latitude,
                longitude = longitude,
                deviceType = deviceType.trim(),
                deviceCategory = deviceCategory?.takeIf { it.isNotBlank() },
                deviceBrand = deviceBrand?.takeIf { it.isNotBlank() },
                deviceModel = deviceModel?.takeIf { it.isNotBlank() },
                deviceSerialNumber = deviceSerialNumber?.takeIf { it.isNotBlank() },
                deviceYear = deviceYear,
                warrantyStatus = warrantyStatus?.takeIf { it.isNotBlank() },
                problemShortDescription = problemShortDescription?.takeIf { it.isNotBlank() },
                problemDescription = problemDescription.trim(),
                problemWhenStarted = problemWhenStarted?.takeIf { it.isNotBlank() },
                problemConditions = problemConditions?.takeIf { it.isNotBlank() },
                problemErrorCodes = problemErrorCodes?.takeIf { it.isNotBlank() },
                problemAttemptedFixes = problemAttemptedFixes?.takeIf { it.isNotBlank() },
                addressStreet = addressStreet?.takeIf { it.isNotBlank() },
                addressBuilding = addressBuilding?.takeIf { it.isNotBlank() },
                addressApartment = addressApartment?.takeIf { it.isNotBlank() },
                addressFloor = addressFloor,
                addressEntranceCode = addressEntranceCode?.takeIf { it.isNotBlank() },
                addressLandmark = addressLandmark?.takeIf { it.isNotBlank() },
                arrivalTime = arrivalTime?.takeIf { it.isNotBlank() },
                desiredRepairDate = desiredRepairDate?.takeIf { it.isNotBlank() },
                urgency = urgency?.takeIf { it.isNotBlank() } ?: if (isUrgent) "urgent" else "planned",
                orderType = if (isUrgent) "urgent" else "regular",
                priority = if (isUrgent) "urgent" else "regular",
                orderSource = "app",
                clientBudget = clientBudget,
                paymentType = paymentType?.takeIf { it.isNotBlank() },
                intercomWorking = intercomWorking,
                parkingAvailable = parkingAvailable,
                hasPets = hasPets,
                hasSmallChildren = hasSmallChildren,
                preferredContactMethod = preferredContactMethod
            )
            
            when (val result = repository.createOrder(request)) {
                is ApiResult.Success -> {
                    _createOrderUiState.value = CreateOrderUiState(
                        isSuccess = true,
                        orderId = result.data.id
                    )
                }
                is ApiResult.Error -> {
                    _createOrderUiState.value = CreateOrderUiState(errorMessage = result.message)
                }
                ApiResult.Loading -> {
                    _createOrderUiState.value = CreateOrderUiState(isLoading = true)
                }
            }
        }
    }

    fun loadOrderDetails(orderId: Long) {
        viewModelScope.launch {
            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(isLoading = true)
            
            // Загружаем заказ и историю статусов параллельно
            val orderResult = repository.getOrderById(orderId)
            val historyResult = repository.getOrderStatusHistory(orderId)
            
            when {
                orderResult is ApiResult.Success -> {
                    val history = if (historyResult is ApiResult.Success) {
                        historyResult.data
                    } else {
                        null
                    }
                    _orderDetailsUiState.value = OrderDetailsUiState(
                        order = orderResult.data,
                        statusHistory = history
                    )
                }
                orderResult is ApiResult.Error -> {
                    _orderDetailsUiState.value = OrderDetailsUiState(errorMessage = orderResult.message)
                }
                else -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            when (val result = repository.cancelOrder(orderId)) {
                is ApiResult.Success -> {
                    loadOrderDetails(orderId)
                }
                is ApiResult.Error -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        errorMessage = result.message
                    )
                }
                ApiResult.Loading -> {}
            }
        }
    }
    
    fun completeOrder(orderId: Long, finalCost: Double? = null, repairDescription: String? = null) {
        viewModelScope.launch {
            when (val result = repository.completeOrder(orderId, finalCost, repairDescription)) {
                is ApiResult.Success -> {
                    loadOrderDetails(orderId)
                    loadOrders() // Обновляем список заказов
                }
                is ApiResult.Error -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        errorMessage = result.message
                    )
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun resetCreateOrderState() {
        _createOrderUiState.value = CreateOrderUiState()
    }
    
    fun clearNotification() {
        _webSocketNotification.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketService.disconnect()
    }
}

