package com.example.bestapp.ui.orders

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.RetrofitClient
import com.example.bestapp.api.models.ApiOrder
import com.example.bestapp.data.DataRepository
import com.example.bestapp.data.Order
import com.example.bestapp.data.PreferencesManager
import com.example.bestapp.data.RepairStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrdersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataRepository
    val apiRepository = ApiRepository() // Публичный для доступа из Fragment
    private val prefsManager = PreferencesManager.getInstance(application)
    
    companion object {
        private const val TAG = "OrdersViewModel"
    }
    
    private val _newOrders = MutableStateFlow<List<Order>>(emptyList())
    val newOrders: StateFlow<List<Order>> = _newOrders.asStateFlow()
    
    private val _filteredOrders = MutableStateFlow<List<Order>>(emptyList())
    val filteredOrders: StateFlow<List<Order>> = _filteredOrders.asStateFlow()
    
    private val _completedOrders = MutableStateFlow<List<Order>>(emptyList())
    val completedOrders: StateFlow<List<Order>> = _completedOrders.asStateFlow()
    
    private val _rejectedOrders = MutableStateFlow<List<com.example.bestapp.api.models.ApiRejectedAssignment>>(emptyList())
    val rejectedOrders: StateFlow<List<com.example.bestapp.api.models.ApiRejectedAssignment>> = _rejectedOrders.asStateFlow()
    
    // Состояние смены - загружаем из SharedPreferences
    private val _isShiftActive = MutableStateFlow(prefsManager.isShiftActive())
    val isShiftActive: StateFlow<Boolean> = _isShiftActive.asStateFlow()
    
    // Настройки автоприема
    private val _autoAcceptSettings = MutableStateFlow(prefsManager.getAutoAcceptSettings())
    val autoAcceptSettings: StateFlow<com.example.bestapp.data.AutoAcceptSettings> = _autoAcceptSettings.asStateFlow()
    
    // Фильтры
    private val _selectedDeviceTypes = MutableStateFlow<Set<String>>(emptySet())
    private val _minPrice = MutableStateFlow<Double?>(null)
    private val _maxPrice = MutableStateFlow<Double?>(null)
    private val _urgency = MutableStateFlow<String?>(null) // emergency, urgent, planned
    private val _maxDistance = MutableStateFlow<Double?>(null) // в метрах
    private val _sortBy = MutableStateFlow<String?>(null) // distance, price, urgency, created_at
    private val _searchQuery = MutableStateFlow("")
    
    init {
        // Инициализируем RetrofitClient для загрузки сохраненного токена
        RetrofitClient.initialize(application)
        
        // Загружаем сохраненные фильтры
        val savedFilters = prefsManager.getOrderFilters()
        Log.d(TAG, "Загрузка сохраненных фильтров: deviceTypes=${savedFilters.deviceTypes}, minPrice=${savedFilters.minPrice}, maxPrice=${savedFilters.maxPrice}, maxDistance=${savedFilters.maxDistance}, urgency=${savedFilters.urgency}")
        _selectedDeviceTypes.value = savedFilters.deviceTypes
        _minPrice.value = savedFilters.minPrice
        _maxPrice.value = savedFilters.maxPrice
        _maxDistance.value = savedFilters.maxDistance
        _urgency.value = savedFilters.urgency
        _sortBy.value = savedFilters.sortBy
        
        loadNewOrders()
    }
    
    private fun loadNewOrders() {
        viewModelScope.launch {
            Log.d(TAG, "Loading new orders...")
            Log.d(TAG, "Current shift status: ${_isShiftActive.value}")
            
            // Получаем координаты мастера (если есть)
            val masterLocation = getMasterLocation()
            
            // Бэкенд автоматически фильтрует заказы для мастера (только новые)
            val result = apiRepository.getOrders(
                status = null,
                deviceType = null,
                orderType = null,
                urgency = _urgency.value,
                maxDistance = _maxDistance.value,
                minPrice = _minPrice.value,
                maxPrice = _maxPrice.value,
                sortBy = _sortBy.value,
                masterLatitude = masterLocation?.first,
                masterLongitude = masterLocation?.second
            )
            
            result.onSuccess { apiOrders ->
                Log.d(TAG, "Loaded ${apiOrders.size} orders from API")
                
                // Проверяем автоприем для новых заказов
                if (_isShiftActive.value && _autoAcceptSettings.value.isEnabled) {
                    checkAutoAccept(apiOrders)
                }
                
                if (apiOrders.isNotEmpty()) {
                    val firstOrder = apiOrders.first()
                    Log.d(TAG, "First order: id=${firstOrder.id}, repairStatus=${firstOrder.repairStatus}, distance=${firstOrder.distance}")
                }
                
                // Конвертируем ApiOrder в Order
                val convertedOrders = apiOrders.map { it.toOrder() }
                Log.d(TAG, "Converted ${convertedOrders.size} orders")
                _newOrders.value = convertedOrders
                
                // Асинхронно загружаем expiresAt для каждого заказа из активных назначений
                convertedOrders.forEach { order ->
                    viewModelScope.launch {
                        val assignmentResult = apiRepository.getActiveAssignmentForOrder(order.id)
                        assignmentResult.onSuccess { assignment ->
                            if (assignment != null && assignment.status == "pending") {
                                val expiresAt = try {
                                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                                    dateFormat.parse(assignment.expiresAt)
                                } catch (e: Exception) {
                                    try {
                                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                        dateFormat.parse(assignment.expiresAt)
                                    } catch (e2: Exception) {
                                        null
                                    }
                                }
                                
                                if (expiresAt != null) {
                                    // Обновляем заказ в списке с expiresAt
                                    val updatedOrders = _newOrders.value.toMutableList()
                                    val orderIndex = updatedOrders.indexOfFirst { it.id == order.id }
                                    if (orderIndex >= 0) {
                                        val existingOrder = updatedOrders[orderIndex]
                                        updatedOrders[orderIndex] = existingOrder.copy(expiresAt = expiresAt)
                                        _newOrders.value = updatedOrders
                                        applyLocalFilters()
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Фильтры уже применены на backend, но можем применить локальные фильтры (поиск, тип устройства)
                applyLocalFilters()
                Log.d(TAG, "Applied filters, filtered orders count: ${_filteredOrders.value.size}")
            }.onFailure { error ->
                Log.e(TAG, "Failed to load orders from API: ${error.message}", error)
                error.printStackTrace()
                _newOrders.value = emptyList()
                applyLocalFilters()
            }
        }
    }
    
    private fun getMasterLocation(): Pair<Double, Double>? {
        // TODO: Получить координаты мастера из настроек или из последнего обновления смены
        // Пока возвращаем null - координаты будут получены из БД на backend
        return null
    }
    
    // Конвертер ApiOrder -> Order
    private fun ApiOrder.toOrder(): Order {
        return Order(
            id = this.id,
            clientId = this.clientId,
            clientName = this.clientName,
            clientPhone = this.clientPhone,
            clientAddress = this.address,
            latitude = this.latitude,
            longitude = this.longitude,
            deviceType = this.deviceType,
            deviceBrand = this.deviceBrand ?: "",
            deviceModel = this.deviceModel ?: "",
            problemDescription = this.problemDescription,
            status = when(this.repairStatus) {
                "new" -> RepairStatus.NEW
                "in_progress" -> RepairStatus.IN_PROGRESS
                "completed" -> RepairStatus.COMPLETED
                "cancelled" -> RepairStatus.CANCELLED
                else -> RepairStatus.NEW
            },
            estimatedCost = this.estimatedCost,
            distance = this.distance,
            urgency = this.urgency,
            expiresAt = null, // Будет обновлено асинхронно из назначения
            createdAt = java.util.Date() // Можно парсить из createdAt если нужно
        )
    }
    
    fun setDeviceTypeFilter(types: Set<String>) {
        _selectedDeviceTypes.value = types
        saveFilters()
        applyFilters()
    }
    
    fun setPriceFilter(minPrice: Double?, maxPrice: Double?) {
        _minPrice.value = minPrice
        _maxPrice.value = maxPrice
        saveFilters()
        loadNewOrders() // Перезагружаем заказы с новым фильтром
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }
    
    fun setUrgencyFilter(urgency: String?) {
        _urgency.value = urgency
        saveFilters()
        loadNewOrders() // Перезагружаем заказы с новым фильтром
    }
    
    fun setMaxDistanceFilter(maxDistance: Double?) {
        _maxDistance.value = maxDistance
        saveFilters()
        loadNewOrders()
    }
    
    fun setSortBy(sortBy: String?) {
        _sortBy.value = sortBy
        saveFilters()
        loadNewOrders()
    }
    
    /**
     * Сохраняет текущие фильтры в SharedPreferences
     */
    private fun saveFilters() {
        val filters = com.example.bestapp.data.OrderFilters(
            deviceTypes = _selectedDeviceTypes.value,
            minPrice = _minPrice.value,
            maxPrice = _maxPrice.value,
            maxDistance = _maxDistance.value,
            urgency = _urgency.value,
            sortBy = _sortBy.value
        )
        Log.d(TAG, "Сохранение фильтров: deviceTypes=${filters.deviceTypes}, minPrice=${filters.minPrice}, maxPrice=${filters.maxPrice}, maxDistance=${filters.maxDistance}, urgency=${filters.urgency}")
        prefsManager.saveOrderFilters(
            deviceTypes = filters.deviceTypes,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            maxDistance = filters.maxDistance,
            urgency = filters.urgency,
            sortBy = filters.sortBy
        )
    }
    
    /**
     * Получает текущие значения фильтров
     */
    fun getCurrentFilters(): com.example.bestapp.data.OrderFilters {
        return com.example.bestapp.data.OrderFilters(
            deviceTypes = _selectedDeviceTypes.value,
            minPrice = _minPrice.value,
            maxPrice = _maxPrice.value,
            maxDistance = _maxDistance.value,
            urgency = _urgency.value,
            sortBy = _sortBy.value
        )
    }
    
    // Локальные фильтры (применяются после получения данных с backend)
    private fun applyLocalFilters() {
        var filtered = _newOrders.value
        
        // Фильтр по типу устройства (локальный, так как может быть несколько типов)
        if (_selectedDeviceTypes.value.isNotEmpty()) {
            filtered = filtered.filter { order ->
                _selectedDeviceTypes.value.contains(order.deviceType)
            }
        }
        
        // Поиск (локальный)
        if (_searchQuery.value.isNotEmpty()) {
            val query = _searchQuery.value.lowercase()
            filtered = filtered.filter { order ->
                order.clientName.lowercase().contains(query) ||
                order.deviceBrand.lowercase().contains(query) ||
                order.deviceModel.lowercase().contains(query) ||
                order.problemDescription.lowercase().contains(query) ||
                order.clientAddress.lowercase().contains(query) ||
                order.clientPhone.contains(query) ||
                order.id.toString().contains(query)
            }
        }
        
        _filteredOrders.value = filtered
    }
    
    // Устаревший метод - оставлен для совместимости
    private fun applyFilters() {
        applyLocalFilters()
    }
    
    fun refreshOrders() {
        loadNewOrders()
    }
    
    fun loadCompletedOrders() {
        viewModelScope.launch {
            Log.d(TAG, "Loading completed orders...")
            
            val result = apiRepository.getOrders(
                status = "completed",
                deviceType = null,
                orderType = null,
                urgency = null,
                maxDistance = null,
                minPrice = null,
                maxPrice = null,
                sortBy = "created_at",
                masterLatitude = null,
                masterLongitude = null
            )
            
            result.onSuccess { apiOrders ->
                Log.d(TAG, "Loaded ${apiOrders.size} completed orders from API")
                
                // Конвертируем ApiOrder в Order
                val convertedOrders = apiOrders.map { it.toOrder() }
                _completedOrders.value = convertedOrders
            }.onFailure { error ->
                Log.e(TAG, "Failed to load completed orders from API: ${error.message}", error)
                _completedOrders.value = emptyList()
            }
        }
    }
    
    fun startShift(latitude: Double = 56.859611, longitude: Double = 35.911896) {
        viewModelScope.launch {
            Log.d(TAG, "Starting shift...")
            val result = apiRepository.startShift(latitude, longitude)
            result.onSuccess {
                Log.d(TAG, "Shift started successfully")
                _isShiftActive.value = true
                prefsManager.setShiftActive(true) // Сохраняем в SharedPreferences
                Log.d(TAG, "Refreshing orders after shift start...")
                refreshOrders() // Обновляем заказы после начала смены
            }.onFailure { error ->
                Log.e(TAG, "Failed to start shift", error)
                // Все равно обновляем UI для тестирования
                _isShiftActive.value = true
                prefsManager.setShiftActive(true) // Сохраняем в SharedPreferences
                Log.d(TAG, "Refreshing orders despite shift start failure...")
                refreshOrders() // Пробуем загрузить заявки даже при ошибке
            }
        }
    }
    
    fun endShift() {
        viewModelScope.launch {
            val result = apiRepository.endShift()
            result.onSuccess {
                Log.d(TAG, "Shift ended successfully")
                _isShiftActive.value = false
                prefsManager.setShiftActive(false) // Сохраняем в SharedPreferences
            }.onFailure { error ->
                Log.e(TAG, "Failed to end shift", error)
                // Все равно обновляем UI
                _isShiftActive.value = false
                prefsManager.setShiftActive(false) // Сохраняем в SharedPreferences
            }
        }
    }
    
    fun toggleShift() {
        if (_isShiftActive.value) {
            endShift()
        } else {
            startShift()
        }
    }
    
    fun loadRejectedOrders() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Загрузка истории отклонений...")
                val result = apiRepository.getRejectedAssignments()
                result.onSuccess { rejectedAssignments ->
                    Log.d(TAG, "Загружено отклоненных заказов: ${rejectedAssignments.size}")
                    _rejectedOrders.value = rejectedAssignments
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки истории отклонений: ${error.message}", error)
                    _rejectedOrders.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при загрузке истории отклонений", e)
                _rejectedOrders.value = emptyList()
            }
        }
    }
    
    /**
     * Проверяет заказы на соответствие настройкам автоприема и автоматически принимает подходящие
     */
    private suspend fun checkAutoAccept(apiOrders: List<com.example.bestapp.api.models.ApiOrder>) {
        val settings = _autoAcceptSettings.value
        if (!settings.isEnabled || !_isShiftActive.value) return
        
        val masterLocation = getMasterLocation()
        
        for (apiOrder in apiOrders) {
            // Пропускаем заказы, которые уже приняты или не новые
            if (apiOrder.repairStatus != "new") continue
            
            // Конвертируем в Order для проверки
            val order = com.example.bestapp.data.Order(
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
                orderType = when (apiOrder.orderType) {
                    "urgent" -> com.example.bestapp.data.OrderType.URGENT
                    else -> com.example.bestapp.data.OrderType.REGULAR
                },
                estimatedCost = apiOrder.estimatedCost,
                urgency = apiOrder.urgency,
                distance = apiOrder.distance
            )
            
            // Вычисляем расстояние, если не указано
            val distance = if (order.distance != null) {
                order.distance
            } else if (masterLocation != null && order.latitude != null && order.longitude != null) {
                calculateDistance(
                    masterLocation.first, masterLocation.second,
                    order.latitude, order.longitude
                )
            } else {
                null
            }
            
            // Проверяем соответствие настройкам
            if (settings.matchesOrder(order, distance)) {
                Log.d(TAG, "Автоприем заказа #${order.id}")
                
                // Получаем активное назначение для заказа
                val assignmentResult = apiRepository.getActiveAssignmentForOrder(order.id)
                assignmentResult.onSuccess { assignment ->
                    assignment?.let {
                        // Автоматически принимаем назначение
                        val acceptResult = apiRepository.acceptAssignment(it.id)
                        acceptResult.onSuccess {
                            Log.d(TAG, "Заказ #${order.id} автоматически принят")
                            // Можно добавить уведомление
                        }.onFailure { error ->
                            Log.e(TAG, "Ошибка автоприема заказа #${order.id}: ${error.message}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Вычисляет расстояние между двумя точками в метрах
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Радиус Земли в метрах
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Обновляет настройки автоприема
     */
    fun updateAutoAcceptSettings(settings: com.example.bestapp.data.AutoAcceptSettings) {
        prefsManager.setAutoAcceptSettings(settings)
        _autoAcceptSettings.value = settings
    }
}


