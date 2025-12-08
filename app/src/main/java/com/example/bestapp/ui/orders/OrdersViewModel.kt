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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    
    // Статус верификации мастера
    private val _isVerified = MutableStateFlow<Boolean?>(null) // null = еще не проверено
    val isVerified: StateFlow<Boolean?> = _isVerified.asStateFlow()
    
    // Сообщение о необходимости верификации
    private val _verificationMessage = MutableStateFlow<String?>(null)
    val verificationMessage: StateFlow<String?> = _verificationMessage.asStateFlow()
    
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
    
    // Job для автоматического обновления заявок
    private var pollingJob: Job? = null
    
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
        
        // Проверяем статус верификации при инициализации
        checkVerificationStatus()
        loadNewOrders()
        
        // Запускаем автоматическое обновление заявок, если мастер на смене
        observeShiftStatusAndStartPolling()
    }
    
    /**
     * Наблюдает за статусом смены и запускает/останавливает автоматическое обновление заявок
     */
    private fun observeShiftStatusAndStartPolling() {
        viewModelScope.launch {
            _isShiftActive.collect { isActive ->
                if (isActive) {
                    // Мастер на смене - запускаем периодическое обновление
                    startPollingAssignments()
                } else {
                    // Мастер не на смене - останавливаем обновление
                    stopPollingAssignments()
                }
            }
        }
    }
    
    /**
     * Запускает периодическое обновление заявок (каждые 30 секунд)
     */
    private fun startPollingAssignments() {
        // Останавливаем предыдущий polling, если он есть
        stopPollingAssignments()
        
        pollingJob = viewModelScope.launch {
            while (isActive && _isShiftActive.value) {
                delay(30000) // 30 секунд
                if (_isShiftActive.value) {
                    Log.d(TAG, "🔄 Автоматическое обновление заявок (polling)")
                    loadNewOrders()
                }
            }
        }
        Log.d(TAG, "✅ Автоматическое обновление заявок запущено (каждые 30 сек)")
    }
    
    /**
     * Останавливает периодическое обновление заявок
     */
    private fun stopPollingAssignments() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "⏹️ Автоматическое обновление заявок остановлено")
    }
    
    /**
     * Проверяет статус верификации мастера
     */
    private fun checkVerificationStatus() {
        viewModelScope.launch {
            try {
                val statsResult = apiRepository.getMasterStats()
                statsResult.onSuccess { response ->
                    val masterData = response["master"] as? Map<*, *>
                    val verificationStatus = masterData?.get("verificationStatus")?.toString()?.lowercase()
                    val isVerified = verificationStatus == "verified"
                    _isVerified.value = isVerified
                    
                    if (!isVerified) {
                        _verificationMessage.value = "Для просмотра и принятия заказов необходимо пройти верификацию. Пожалуйста, перейдите в профиль и пройдите верификацию."
                    } else {
                        _verificationMessage.value = null
                    }
                    
                    Log.d(TAG, "Verification status checked: $verificationStatus, isVerified: $isVerified")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to check verification status: ${error.message}", error)
                    // При ошибке загрузки профиля не блокируем заявки:
                    // считаем статус неизвестным (null), чтобы не скрывать заказы
                    _isVerified.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception checking verification status", e)
                // При исключении также не блокируем заявки
                _isVerified.value = null
            }
        }
    }
    
    private fun loadNewOrders() {
        viewModelScope.launch {
            Log.d(TAG, "Loading new orders...")
            Log.d(TAG, "Current shift status: ${_isShiftActive.value}")
            
            // Получаем координаты мастера (если есть)
            val masterLocation = getMasterLocation()
            
            // Загружаем назначения мастера (assignments) вместо заказов
            // Согласно правилам, мастер должен видеть НАЗНАЧЕНИЯ со статусом "pending"
            val assignmentsResult = apiRepository.getMyAssignments()
            
            assignmentsResult.onSuccess { assignments ->
                Log.d(TAG, "Loaded ${assignments.size} assignments from API")
                
                // Конвертируем assignments в orders для отображения
                val apiOrders = assignments
                    .filter { it.status == "pending" } // Только ожидающие принятия
                    .map { assignment ->
                        ApiOrder(
                            id = assignment.orderId,
                            clientId = 0, // Не нужно для назначения
                            clientName = assignment.clientName ?: "Клиент",
                            clientPhone = assignment.clientPhone ?: "",
                            address = assignment.address ?: "",
                            latitude = assignment.latitude ?: 0.0,
                            longitude = assignment.longitude ?: 0.0,
                            deviceType = assignment.deviceType ?: "",
                            deviceBrand = assignment.deviceBrand,
                            deviceModel = assignment.deviceModel,
                            problemDescription = assignment.problemDescription ?: "",
                            repairStatus = "new", // Всегда новый для pending assignments
                            requestStatus = "new",
                            paymentStatus = null,
                            estimatedCost = assignment.estimatedCost,
                            orderNumber = null,
                            createdAt = assignment.assignedAt,
                            assignedMasterId = assignment.masterId,
                            distance = null, // Будет рассчитано если нужно
                            urgency = assignment.orderType,
                            arrivalTime = assignment.arrivalTime,
                            // Остальные поля не нужны для назначения
                            deviceCategory = null,
                            deviceSerialNumber = null,
                            deviceYear = null,
                            warrantyStatus = null,
                            problemShortDescription = null,
                            problemWhenStarted = null,
                            problemConditions = null,
                            problemErrorCodes = null,
                            problemAttemptedFixes = null,
                            addressStreet = null,
                            addressBuilding = null,
                            addressApartment = null,
                            addressFloor = null,
                            addressEntranceCode = null,
                            addressLandmark = null,
                            desiredRepairDate = null,
                            priority = null,
                            orderSource = null,
                            orderType = assignment.orderType ?: "regular",
                            clientBudget = null,
                            paymentType = null,
                            intercomWorking = null,
                            parkingAvailable = null,
                            hasPets = null,
                            hasSmallChildren = null,
                            preferredContactMethod = null,
                            masterGenderPreference = null,
                            masterMinExperience = null,
                            preferredMasterId = null,
                            problemTags = null,
                            problemCategory = null,
                            problemSeasonality = null,
                            preliminaryDiagnosis = null,
                            requiredParts = null,
                            specialEquipment = null,
                            repairComplexity = null,
                            estimatedRepairTime = null,
                            assignmentDate = assignment.assignedAt,
                            updatedAt = assignment.assignedAt,
                            finalCost = null,
                            clientEmail = null,
                            media = null,
                            // Важно! Сохраняем assignmentId и expiresAt
                            assignmentId = assignment.id,
                            assignmentExpiresAt = assignment.expiresAt,
                            assignmentStatus = assignment.status
                        )
                    }
                    
                Log.d(TAG, "Converted ${apiOrders.size} assignments to orders for display")
                
                // Верификация больше не блокирует просмотр заявок (исправлено на сервере)
                // Мастера могут видеть заявки даже без верификации, но не могут их принимать
                
                // Проверяем автоприем для новых заказов
                if (_isShiftActive.value && _autoAcceptSettings.value.isEnabled && _isVerified.value == true) {
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
                
                // Верификация больше не блокирует загрузку заявок
                // Показываем ошибку, но не очищаем список полностью
                val errorMessage = error.message ?: ""
                if (errorMessage.contains("верификац", ignoreCase = true) || 
                    errorMessage.contains("verification", ignoreCase = true)) {
                    // Просто записываем, что верификация требуется для принятия
                    _verificationMessage.value = "Для принятия заказов требуется верификация. Заявки доступны для просмотра."
                    Log.w(TAG, "Верификация требуется, но заявки доступны для просмотра")
                }
                
                // Не очищаем заявки полностью - оставляем предыдущий список, если он есть
                // Это позволяет видеть заявки даже при временных ошибках сети
                if (_newOrders.value.isEmpty()) {
                    _newOrders.value = emptyList()
                }
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
        // Парсим expiresAt если есть
        val expiresAtDate = this.assignmentExpiresAt?.let {
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                dateFormat.parse(it)
            } catch (e: Exception) {
                try {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    dateFormat.parse(it)
                } catch (e2: Exception) {
                    null
                }
            }
        }
        
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
            expiresAt = expiresAtDate,
            createdAt = java.util.Date(), // Можно парсить из createdAt если нужно
            // Сохраняем информацию о назначении
            assignmentId = this.assignmentId,
            assignmentStatus = this.assignmentStatus
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
            Log.d(TAG, "Starting shift... current status=${_isShiftActive.value}")
            val result = apiRepository.startShift(latitude, longitude)
            result.onSuccess {
                Log.d(TAG, "Shift started successfully on server")
                // Статус уже обновлен оптимистично, не перезаписываем
                Log.d(TAG, "Current _isShiftActive=${_isShiftActive.value}, prefsManager=${prefsManager.isShiftActive()}")
                Log.d(TAG, "Refreshing orders after shift start...")
                refreshOrders() // Обновляем заказы после начала смены
            }.onFailure { error ->
                Log.e(TAG, "Failed to start shift: ${error.message}", error)
                // Откатываем изменения при ошибке
                Log.d(TAG, "Rolling back shift status to false")
                _isShiftActive.value = false
                prefsManager.setShiftActive(false)
                Log.d(TAG, "After rollback: _isShiftActive=${_isShiftActive.value}, prefsManager=${prefsManager.isShiftActive()}")
            }
        }
    }
    
    fun endShift() {
        viewModelScope.launch {
            Log.d(TAG, "Ending shift... current status=${_isShiftActive.value}")
            val result = apiRepository.endShift()
            result.onSuccess {
                Log.d(TAG, "Shift ended successfully on server")
                // Статус уже обновлен оптимистично, не перезаписываем
                Log.d(TAG, "Current _isShiftActive=${_isShiftActive.value}, prefsManager=${prefsManager.isShiftActive()}")
            }.onFailure { error ->
                Log.e(TAG, "Failed to end shift: ${error.message}", error)
                // Откатываем изменения при ошибке
                Log.d(TAG, "Rolling back shift status to true")
                _isShiftActive.value = true
                prefsManager.setShiftActive(true)
                Log.d(TAG, "After rollback: _isShiftActive=${_isShiftActive.value}, prefsManager=${prefsManager.isShiftActive()}")
            }
        }
    }
    
    fun toggleShift() {
        val currentStatus = _isShiftActive.value
        val newStatus = !currentStatus
        
        Log.d(TAG, "toggleShift: currentStatus=$currentStatus, newStatus=$newStatus")
        
        // Оптимистичное обновление UI сразу
        _isShiftActive.value = newStatus
        prefsManager.setShiftActive(newStatus)
        
        Log.d(TAG, "toggleShift: Updated _isShiftActive=${_isShiftActive.value}, prefsManager.isShiftActive=${prefsManager.isShiftActive()}")
        
        if (newStatus) {
            startShift()
        } else {
            endShift()
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


