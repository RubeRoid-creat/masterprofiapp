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
import com.example.bestapp.network.WebSocketManager
import com.example.bestapp.network.ConnectionState
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
    
    // WebSocket менеджер для real-time обновлений
    private val webSocketManager = WebSocketManager(scope = viewModelScope)
    
    companion object {
        private const val TAG = "OrdersViewModel"
        private const val FALLBACK_POLLING_INTERVAL_MS = 60_000L // Fallback polling каждую минуту
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
     * Наблюдает за статусом смены и управляет WebSocket подключением
     */
    private fun observeShiftStatusAndStartPolling() {
        viewModelScope.launch {
            _isShiftActive.collect { isActive ->
                if (isActive) {
                    // Мастер на смене - подключаемся к WebSocket
                    connectWebSocket()
                } else {
                    // Мастер не на смене - отключаемся от WebSocket
                    disconnectWebSocket()
                }
            }
        }
        
        // Наблюдаем за WebSocket событиями
        observeWebSocketEvents()
    }
    
    /**
     * Подключение к WebSocket для real-time обновлений
     */
    private fun connectWebSocket() {
        val token = RetrofitClient.getAuthToken()
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ Нет токена для WebSocket подключения")
            startFallbackPolling() // Запускаем fallback polling
            return
        }
        
        Log.d(TAG, "🔌 Подключение к WebSocket для real-time обновлений")
        webSocketManager.connect(token)
    }
    
    /**
     * Отключение от WebSocket
     */
    private fun disconnectWebSocket() {
        Log.d(TAG, "🔌 Отключение от WebSocket")
        webSocketManager.disconnect()
        stopFallbackPolling()
    }
    
    /**
     * Наблюдение за WebSocket событиями
     */
    private fun observeWebSocketEvents() {
        // Новые назначения
        viewModelScope.launch {
            webSocketManager.newAssignment.collect { event ->
                event?.let {
                    Log.d(TAG, "🆕 WebSocket: Получена новая заявка #${it.id}")
                    // Обновляем список заявок через API для получения полных данных
                    loadNewOrders()
                    webSocketManager.clearNewAssignment()
                }
            }
        }
        
        // Истекшие назначения
        viewModelScope.launch {
            webSocketManager.expiredAssignment.collect { assignmentId ->
                assignmentId?.let {
                    Log.d(TAG, "⏰ WebSocket: Заявка #$it истекла")
                    // Удаляем из списка новых заявок
                    removeExpiredAssignment(it)
                    webSocketManager.clearExpiredAssignment()
                }
            }
        }
        
        // Обновления статуса заказа
        viewModelScope.launch {
            webSocketManager.orderStatusUpdate.collect { update ->
                update?.let {
                    Log.d(TAG, "📝 WebSocket: Обновление статуса заказа #${it.orderId}: ${it.newStatus}")
                    // Обновляем список заказов
                    loadNewOrders()
                    loadCompletedOrders()
                    webSocketManager.clearOrderStatusUpdate()
                }
            }
        }
        
        // Состояние подключения - запускаем fallback polling при ошибке
        viewModelScope.launch {
            webSocketManager.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        Log.d(TAG, "✅ WebSocket подключен")
                        stopFallbackPolling() // Останавливаем fallback
                    }
                    is ConnectionState.Error -> {
                        Log.e(TAG, "❌ Ошибка WebSocket: ${state.message}")
                        // Запускаем fallback polling при ошибке WebSocket
                        if (_isShiftActive.value) {
                            startFallbackPolling()
                        }
                    }
                    is ConnectionState.Disconnected -> {
                        Log.w(TAG, "⚠️ WebSocket отключен")
                        // Запускаем fallback polling при отключении
                        if (_isShiftActive.value) {
                            startFallbackPolling()
                        }
                    }
                    is ConnectionState.Connecting -> {
                        Log.d(TAG, "🔄 Подключение к WebSocket...")
                    }
                }
            }
        }
    }
    
    /**
     * Удаление истекшего назначения из списка
     */
    private fun removeExpiredAssignment(assignmentId: Int) {
        val currentOrders = _newOrders.value.toMutableList()
        val orderToRemove = currentOrders.find { it.assignmentId?.toInt() == assignmentId }
        if (orderToRemove != null) {
            currentOrders.remove(orderToRemove)
            _newOrders.value = currentOrders
            Log.d(TAG, "🗑️ Истекшая заявка #$assignmentId удалена из списка")
        }
    }
    
    /**
     * Fallback polling на случай проблем с WebSocket (каждую минуту)
     */
    private fun startFallbackPolling() {
        if (pollingJob?.isActive == true) {
            Log.d(TAG, "Fallback polling уже запущен")
            return
        }
        
        Log.d(TAG, "⚠️ Запуск fallback polling (каждые 60 сек)")
        pollingJob = viewModelScope.launch {
            while (isActive && _isShiftActive.value) {
                delay(FALLBACK_POLLING_INTERVAL_MS)
                if (_isShiftActive.value && !webSocketManager.isConnected()) {
                    Log.d(TAG, "🔄 Fallback polling: обновление заявок")
                    loadNewOrders()
                }
            }
        }
    }
    
    /**
     * Остановка fallback polling
     */
    private fun stopFallbackPolling() {
        if (pollingJob?.isActive == true) {
            pollingJob?.cancel()
            pollingJob = null
            Log.d(TAG, "⏹️ Fallback polling остановлен")
        }
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
                Log.d(TAG, "✅ Загружено ${assignments.size} назначений с API")
                
                if (assignments.isEmpty()) {
                    Log.w(TAG, "⚠️ API вернул пустой список назначений!")
                    Log.w(TAG, "   Проверьте:")
                    Log.w(TAG, "   1. Мастер авторизован? (токен есть?)")
                    Log.w(TAG, "   2. Есть ли активные назначения в БД?")
                    Log.w(TAG, "   3. Мастер на смене?")
                } else {
                    Log.d(TAG, "📋 Детали назначений:")
                    // Логируем все назначения для отладки
                    assignments.forEach { assignment ->
                        Log.d(TAG, "   Назначение: id=${assignment.id}, orderId=${assignment.orderId}, status=${assignment.status}, expiresAt=${assignment.expiresAt}")
                    }
                }
                
                // Фильтруем только pending назначения
                val pendingAssignments = assignments.filter { it.status == "pending" }
                Log.d(TAG, "📋 Pending назначений: ${pendingAssignments.size} из ${assignments.size}")
                
                // Фильтруем истекшие назначения
                val now = System.currentTimeMillis()
                val activeAssignments = pendingAssignments.filter { assignment ->
                    val expiresAt = assignment.expiresAt?.let { expiresStr ->
                        try {
                            // Пробуем разные форматы даты с учетом UTC
                            val formats = listOf(
                                // ISO 8601 с миллисекундами и UTC (Z)
                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                },
                                // ISO 8601 без миллисекунд и UTC (Z)
                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                },
                                // Стандартный формат БД без часового пояса
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            )
                            
                            formats.firstNotNullOfOrNull { format ->
                                try {
                                    val parsed = format.parse(expiresStr)
                                    if (parsed != null) {
                                        // Если формат был с 'Z' (UTC), время уже в UTC, иначе считаем локальным
                                        val time = parsed.time
                                        Log.d(TAG, "   Парсинг expiresAt: '$expiresStr' -> ${java.util.Date(time).toString()}, timestamp=$time")
                                        time
                                    } else null
                                } catch (e: Exception) {
                                    Log.w(TAG, "   Ошибка парсинга '$expiresStr' с форматом: ${e.message}")
                                    null
                                }
                            } ?: run {
                                Log.e(TAG, "   ❌ Не удалось распарсить expiresAt: '$expiresStr'")
                                null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "   ❌ Исключение при парсинге expiresAt '$expiresStr': ${e.message}")
                            null
                        }
                    }
                    
                    if (expiresAt == null) {
                        // Если expiresAt не указан, считаем назначение активным
                        Log.w(TAG, "⚠️ Назначение #${assignment.id} без expiresAt - считаем активным")
                        return@filter true
                    }
                    
                    val isActive = expiresAt > now
                    if (!isActive) {
                        val expiredMinutesAgo = (now - expiresAt) / (1000 * 60)
                        Log.d(TAG, "⏰ Назначение #${assignment.id} истекло ${expiredMinutesAgo} минут назад: ${assignment.expiresAt}")
                    } else {
                        val minutesLeft = (expiresAt - now) / (1000 * 60)
                        Log.d(TAG, "✅ Назначение #${assignment.id} активно, осталось ${minutesLeft} минут")
                    }
                    isActive
                }
                
                Log.d(TAG, "✅ Активных назначений: ${activeAssignments.size} из ${pendingAssignments.size} pending")
                
                // Конвертируем assignments в orders для отображения (только активные, не истекшие)
                val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())
                
                val apiOrders = activeAssignments
                    .map { assignment ->
                        // assignedAt может быть null, если API вернул неполные данные, используем текущее время как fallback
                        val assignedAt = assignment.assignedAt?.takeIf { it.isNotBlank() } ?: currentTime
                        
                        // Парсим problemTags если это JSON строка
                        val problemTagsList = assignment.problemTags?.let { tagsStr ->
                            try {
                                if (tagsStr.startsWith("[") || tagsStr.startsWith("{")) {
                                    // JSON массив или объект
                                    val gson = com.google.gson.Gson()
                                    val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                                    gson.fromJson<List<String>>(tagsStr, listType) ?: emptyList()
                                } else {
                                    // Простая строка, разделенная запятыми
                                    tagsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Ошибка парсинга problemTags: ${e.message}")
                                emptyList()
                            }
                        } ?: emptyList()
                        
                        ApiOrder(
                            id = assignment.orderId,
                            clientId = assignment.clientId ?: 0,
                            clientName = assignment.clientName ?: "Клиент",
                            clientPhone = assignment.clientPhone ?: "",
                            clientEmail = assignment.clientEmail,
                            address = assignment.address ?: "",
                            latitude = assignment.latitude ?: 0.0,
                            longitude = assignment.longitude ?: 0.0,
                            deviceType = assignment.deviceType ?: "",
                            deviceBrand = assignment.deviceBrand,
                            deviceModel = assignment.deviceModel,
                            deviceCategory = assignment.deviceCategory,
                            deviceSerialNumber = assignment.deviceSerialNumber,
                            deviceYear = assignment.deviceYear,
                            warrantyStatus = assignment.warrantyStatus,
                            problemDescription = assignment.problemDescription ?: "",
                            problemShortDescription = assignment.problemShortDescription,
                            problemWhenStarted = assignment.problemWhenStarted,
                            problemConditions = assignment.problemConditions,
                            problemErrorCodes = assignment.problemErrorCodes,
                            problemAttemptedFixes = assignment.problemAttemptedFixes,
                            problemTags = if (problemTagsList.isNotEmpty()) problemTagsList else null,
                            problemCategory = assignment.problemCategory,
                            problemSeasonality = assignment.problemSeasonality,
                            addressStreet = assignment.addressStreet,
                            addressBuilding = assignment.addressBuilding,
                            addressApartment = assignment.addressApartment,
                            addressFloor = assignment.addressFloor,
                            addressEntranceCode = assignment.addressEntranceCode,
                            addressLandmark = assignment.addressLandmark,
                            repairStatus = assignment.repairStatus ?: "new",
                            requestStatus = assignment.requestStatus ?: "new",
                            paymentStatus = null, // ApiAssignment не содержит paymentStatus
                            estimatedCost = assignment.estimatedCost,
                            finalCost = assignment.finalCost,
                            clientBudget = assignment.clientBudget,
                            paymentType = assignment.paymentType,
                            orderNumber = assignment.orderNumber,
                            createdAt = assignment.createdAt ?: assignedAt,
                            updatedAt = assignment.updatedAt ?: assignedAt,
                            assignedMasterId = assignment.assignedMasterId ?: assignment.masterId,
                            distance = null, // Будет рассчитано если нужно
                            urgency = assignment.urgency ?: assignment.orderType,
                            priority = assignment.priority,
                            orderSource = assignment.orderSource,
                            orderType = assignment.orderType ?: "regular",
                            arrivalTime = assignment.arrivalTime,
                            desiredRepairDate = assignment.desiredRepairDate,
                            intercomWorking = assignment.intercomWorking,
                            parkingAvailable = assignment.parkingAvailable,
                            hasPets = assignment.hasPets,
                            hasSmallChildren = assignment.hasSmallChildren,
                            preferredContactMethod = assignment.preferredContactMethod,
                            masterGenderPreference = assignment.masterGenderPreference,
                            masterMinExperience = assignment.masterMinExperience,
                            preferredMasterId = assignment.preferredMasterId,
                            preliminaryDiagnosis = assignment.preliminaryDiagnosis,
                            requiredParts = assignment.requiredParts,
                            specialEquipment = assignment.specialEquipment,
                            repairComplexity = assignment.repairComplexity,
                            estimatedRepairTime = assignment.estimatedRepairTime,
                            assignmentDate = assignment.assignmentDate ?: assignedAt,
                            media = null, // Медиа загружаются отдельно
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
                
                // Выводим детальную информацию о каждом заказе
                convertedOrders.forEach { order ->
                    Log.d(TAG, "   Заказ #${order.id}: deviceType=${order.deviceType}, expiresAt=${order.expiresAt}, assignmentId=${order.assignmentId}")
                }
                
                _newOrders.value = convertedOrders
                Log.d(TAG, "✅ Установлено ${_newOrders.value.size} заказов в _newOrders")
                
                // Фильтры уже применены на backend, но можем применить локальные фильтры (поиск, тип устройства)
                applyLocalFilters()
                Log.d(TAG, "✅ Применены локальные фильтры:")
                Log.d(TAG, "   - _newOrders.value.size = ${_newOrders.value.size}")
                Log.d(TAG, "   - _filteredOrders.value.size = ${_filteredOrders.value.size}")
                Log.d(TAG, "   - _selectedDeviceTypes.value = ${_selectedDeviceTypes.value}")
                Log.d(TAG, "   - _searchQuery.value = '${_searchQuery.value}'")
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
            masterName = null, // Можно добавить если API возвращает
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
            completedAt = null, // Можно парсить если API возвращает
            assignmentDate = this.assignmentDate,
            notes = null, // Внутренние заметки мастера
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
        val initialCount = filtered.size
        
        Log.d(TAG, "🔍 applyLocalFilters: начальное количество заказов = $initialCount")
        
        // Фильтр по типу устройства (локальный, так как может быть несколько типов)
        if (_selectedDeviceTypes.value.isNotEmpty()) {
            val beforeDeviceFilter = filtered.size
            filtered = filtered.filter { order ->
                val matches = _selectedDeviceTypes.value.contains(order.deviceType)
                if (!matches) {
                    Log.v(TAG, "   ❌ Заказ #${order.id} отфильтрован: deviceType=${order.deviceType} не в списке ${_selectedDeviceTypes.value}")
                }
                matches
            }
            Log.d(TAG, "   После фильтра по типу устройства: $beforeDeviceFilter -> ${filtered.size}")
        }
        
        // Поиск (локальный)
        if (_searchQuery.value.isNotEmpty()) {
            val beforeSearchFilter = filtered.size
            val query = _searchQuery.value.lowercase()
            filtered = filtered.filter { order ->
                val matches = order.clientName.lowercase().contains(query) ||
                order.deviceBrand.lowercase().contains(query) ||
                order.deviceModel.lowercase().contains(query) ||
                order.problemDescription.lowercase().contains(query) ||
                order.clientAddress.lowercase().contains(query) ||
                order.clientPhone.contains(query) ||
                order.id.toString().contains(query)
                if (!matches) {
                    Log.v(TAG, "   ❌ Заказ #${order.id} отфильтрован поиском: '$query'")
                }
                matches
            }
            Log.d(TAG, "   После фильтра поиска: $beforeSearchFilter -> ${filtered.size}")
        }
        
        _filteredOrders.value = filtered
        Log.d(TAG, "✅ applyLocalFilters завершен: $initialCount -> ${filtered.size} заказов")
        
        if (filtered.isEmpty() && initialCount > 0) {
            Log.w(TAG, "⚠️ ВНИМАНИЕ: Все $initialCount заказов отфильтрованы!")
        }
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


