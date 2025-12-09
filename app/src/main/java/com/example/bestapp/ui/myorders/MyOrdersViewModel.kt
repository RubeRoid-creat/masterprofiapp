package com.example.bestapp.ui.myorders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiOrder
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
                Log.d(TAG, "Loading my active orders (in_progress only)")
                
                // Загружаем только активные заказы (in_progress)
                // Завершенные заказы не показываются в "Мои заявки"
                val inProgressResult = apiRepository.getOrders(status = "in_progress")
                val inProgressOrders = inProgressResult.getOrElse { emptyList() }
                
                // Конвертируем в Order
                val orders = inProgressOrders.map { it.toOrder() }
                
                _myOrders.value = orders
                Log.d(TAG, "Loaded ${orders.size} active orders (in_progress only)")
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

// Extension функция для конвертации ApiOrder в Order
private fun ApiOrder.toOrder(): Order {
    // Парсим expiresAt если есть (с учетом UTC)
    val expiresAtDate = this.assignmentExpiresAt?.let { expiresStr ->
        try {
            val formats = listOf(
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            )
            formats.firstNotNullOfOrNull { format ->
                try {
                    format.parse(expiresStr)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Парсим createdAt и updatedAt
    val createdAtDate = this.createdAt?.let { dateStr ->
        try {
            val formats = listOf(
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            )
            formats.firstNotNullOfOrNull { format ->
                try {
                    format.parse(dateStr)
                } catch (e: Exception) {
                    null
                }
            } ?: java.util.Date()
        } catch (e: Exception) {
            java.util.Date()
        }
    } ?: java.util.Date()
    
    val updatedAtDate = this.updatedAt?.let { dateStr ->
        try {
            val formats = listOf(
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            )
            formats.firstNotNullOfOrNull { format ->
                try {
                    format.parse(dateStr)
                } catch (e: Exception) {
                    null
                }
            } ?: java.util.Date()
        } catch (e: Exception) {
            java.util.Date()
        }
    } ?: java.util.Date()
    
    // Конвертируем медиа файлы
    val orderMedia = this.media?.map { apiMedia ->
        com.example.bestapp.data.OrderMedia(
            id = apiMedia.id,
            orderId = apiMedia.orderId,
            mediaType = apiMedia.mediaType,
            fileUrl = apiMedia.fileUrl,
            fileName = apiMedia.fileName,
            fileSize = apiMedia.fileSize,
            mimeType = apiMedia.mimeType,
            description = apiMedia.description,
            thumbnailUrl = apiMedia.thumbnailUrl,
            duration = apiMedia.duration,
            createdAt = apiMedia.createdAt
        )
    }
    
    // Определяем requestStatus
    val requestStatus = when(this.requestStatus) {
        "new" -> com.example.bestapp.data.OrderRequestStatus.NEW
        "repeat" -> com.example.bestapp.data.OrderRequestStatus.REPEAT
        "warranty" -> com.example.bestapp.data.OrderRequestStatus.WARRANTY
        else -> com.example.bestapp.data.OrderRequestStatus.NEW
    }
    
    // Определяем orderType
    val orderType = when {
        this.orderType == "urgent" || this.priority == "urgent" -> com.example.bestapp.data.OrderType.URGENT
        else -> com.example.bestapp.data.OrderType.REGULAR
    }
    
    return Order(
        id = this.id,
        orderNumber = this.orderNumber,
        clientId = this.clientId,
        clientName = this.clientName,
        clientPhone = this.clientPhone,
        clientEmail = this.clientEmail,
        clientAddress = this.address,
        latitude = this.latitude,
        longitude = this.longitude,
        addressStreet = this.addressStreet,
        addressBuilding = this.addressBuilding,
        addressApartment = this.addressApartment,
        addressFloor = this.addressFloor,
        addressEntranceCode = this.addressEntranceCode,
        addressLandmark = this.addressLandmark,
        deviceType = this.deviceType,
        deviceCategory = this.deviceCategory,
        deviceBrand = this.deviceBrand ?: "",
        deviceModel = this.deviceModel ?: "",
        deviceSerialNumber = this.deviceSerialNumber,
        deviceYear = this.deviceYear,
        warrantyStatus = this.warrantyStatus,
        problemShortDescription = this.problemShortDescription,
        problemDescription = this.problemDescription,
        problemWhenStarted = this.problemWhenStarted,
        problemConditions = this.problemConditions,
        problemErrorCodes = this.problemErrorCodes,
        problemAttemptedFixes = this.problemAttemptedFixes,
        problemTags = this.problemTags,
        problemCategory = this.problemCategory,
        problemSeasonality = this.problemSeasonality,
        requestStatus = requestStatus,
        orderType = orderType,
        orderSource = this.orderSource,
        priority = this.priority,
        arrivalTime = this.arrivalTime,
        desiredRepairDate = this.desiredRepairDate,
        status = when(this.repairStatus) {
            "new" -> com.example.bestapp.data.RepairStatus.NEW
            "assigned" -> com.example.bestapp.data.RepairStatus.DIAGNOSTICS
            "in_progress" -> com.example.bestapp.data.RepairStatus.IN_PROGRESS
            "completed" -> com.example.bestapp.data.RepairStatus.COMPLETED
            "cancelled" -> com.example.bestapp.data.RepairStatus.CANCELLED
            else -> com.example.bestapp.data.RepairStatus.NEW
        },
        urgency = this.urgency,
        estimatedCost = this.estimatedCost,
        finalCost = this.finalCost,
        clientBudget = this.clientBudget,
        paymentType = this.paymentType,
        paymentStatus = this.paymentStatus,
        intercomWorking = this.intercomWorking?.let { it == 1 },
        parkingAvailable = this.parkingAvailable?.let { it == 1 },
        hasPets = this.hasPets?.let { it == 1 } ?: false,
        hasSmallChildren = this.hasSmallChildren?.let { it == 1 } ?: false,
        preferredContactMethod = this.preferredContactMethod,
        assignedMasterId = this.assignedMasterId,
        masterName = null,
        preliminaryDiagnosis = this.preliminaryDiagnosis,
        requiredParts = this.requiredParts,
        specialEquipment = this.specialEquipment,
        repairComplexity = this.repairComplexity,
        estimatedRepairTime = this.estimatedRepairTime,
        media = orderMedia,
        mediaCount = this.media?.size,
        distance = this.distance,
        expiresAt = expiresAtDate,
        createdAt = createdAtDate,
        updatedAt = updatedAtDate,
        completedAt = null,
        assignmentDate = this.assignmentDate,
        notes = null,
        assignmentId = this.assignmentId,
        assignmentStatus = this.assignmentStatus
    )
}



