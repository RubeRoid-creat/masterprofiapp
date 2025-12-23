package com.example.bestapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

// Репозиторий для управления данными (временно в памяти)
object DataRepository {
    private var nextOrderId = 1L
    private var nextClientId = 1L
    private var nextNewsId = 1L
    private var nextMasterId = 1L
    private var nextAssignmentId = 1L
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()
    
    private val _news = MutableStateFlow<List<News>>(emptyList())
    val news: StateFlow<List<News>> = _news.asStateFlow()
    
    private val _masters = MutableStateFlow<List<Master>>(emptyList())
    val masters: StateFlow<List<Master>> = _masters.asStateFlow()
    
    private val _orderAssignments = MutableStateFlow<List<OrderAssignment>>(emptyList())
    val orderAssignments: StateFlow<List<OrderAssignment>> = _orderAssignments.asStateFlow()
    
    init {
        // Добавляем только новости, без тестовых заказов
        initNewsData()
        initMastersData()
    }
    
    private fun initTestData() {
        // Тестовые клиенты
        val testClients = listOf(
            Client(
                id = nextClientId++,
                name = "Иванов Иван",
                phone = "79991234567",
                email = "ivanov@example.com"
            ),
            Client(
                id = nextClientId++,
                name = "Петрова Мария",
                phone = "79997654321",
                email = "petrova@example.com"
            ),
            Client(
                id = nextClientId++,
                name = "Сидоров Петр",
                phone = "79995556677"
            )
        )
        _clients.value = testClients
        
        // Тестовые заказы (адреса в Твери)
        val testOrders = listOf(
            // Принятый в работу
            Order(
                id = nextOrderId++,
                clientId = testClients[0].id,
                clientName = testClients[0].name,
                clientPhone = testClients[0].phone,
                clientAddress = "Тверь, ул. Советская, д. 34, кв. 15",
                latitude = 56.859611,
                longitude = 35.911896,
                deviceType = "Стиральная машина",
                deviceBrand = "Samsung",
                deviceModel = "WW80R42",
                problemDescription = "Не сливает воду",
                requestStatus = OrderRequestStatus.NEW,
                orderType = OrderType.URGENT,
                arrivalTime = "14:00 - 16:00",
                status = RepairStatus.IN_PROGRESS,
                estimatedCost = 5000.0
            ),
            // Новые заявки (лента)
            Order(
                id = nextOrderId++,
                clientId = testClients[1].id,
                clientName = testClients[1].name,
                clientPhone = testClients[1].phone,
                clientAddress = "Тверь, пр-т Калинина, д. 1, кв. 45",
                latitude = 56.858506,
                longitude = 35.900775,
                deviceType = "Холодильник",
                deviceBrand = "Bosch",
                deviceModel = "KGN39VI21R",
                problemDescription = "Не морозит морозильная камера",
                requestStatus = OrderRequestStatus.NEW,
                orderType = OrderType.REGULAR,
                arrivalTime = "10:00 - 12:00",
                status = RepairStatus.NEW,
                estimatedCost = 12000.0
            ),
            Order(
                id = nextOrderId++,
                clientId = testClients[0].id,
                clientName = testClients[0].name,
                clientPhone = testClients[0].phone,
                clientAddress = "Тверь, ул. Трёхсвятская, д. 28, кв. 8",
                latitude = 56.857422,
                longitude = 35.917034,
                deviceType = "Посудомоечная машина",
                deviceBrand = "Siemens",
                deviceModel = "SN636X03ME",
                problemDescription = "Не набирает воду",
                requestStatus = OrderRequestStatus.NEW,
                orderType = OrderType.URGENT,
                arrivalTime = "15:00 - 17:00",
                status = RepairStatus.NEW,
                estimatedCost = 3500.0
            ),
            Order(
                id = nextOrderId++,
                clientId = testClients[2].id,
                clientName = testClients[2].name,
                clientPhone = testClients[2].phone,
                clientAddress = "Тверь, наб. Афанасия Никитина, д. 24, кв. 12",
                latitude = 56.864273,
                longitude = 35.907448,
                deviceType = "Кондиционер",
                deviceBrand = "Daikin",
                deviceModel = "FTXS35K",
                problemDescription = "Не включается компрессор",
                requestStatus = OrderRequestStatus.WARRANTY,
                orderType = OrderType.REGULAR,
                arrivalTime = "12:00 - 14:00",
                status = RepairStatus.NEW,
                estimatedCost = 8000.0
            ),
            Order(
                id = nextOrderId++,
                clientId = testClients[1].id,
                clientName = testClients[1].name,
                clientPhone = testClients[1].phone,
                clientAddress = "Тверь, ул. Желябова, д. 7, кв. 22",
                latitude = 56.863120,
                longitude = 35.920450,
                deviceType = "Десктоп",
                deviceBrand = "ASUS",
                deviceModel = "Custom Build",
                problemDescription = "Не загружается Windows",
                requestStatus = OrderRequestStatus.REPEAT,
                orderType = OrderType.URGENT,
                arrivalTime = "09:00 - 11:00",
                status = RepairStatus.NEW,
                estimatedCost = 2000.0
            ),
            Order(
                id = nextOrderId++,
                clientId = testClients[0].id,
                clientName = testClients[0].name,
                clientPhone = testClients[0].phone,
                clientAddress = "Тверь, ул. Вольного Новгорода, д. 1, кв. 5",
                latitude = 56.858278,
                longitude = 35.924444,
                deviceType = "Кофемашина",
                deviceBrand = "Delonghi",
                deviceModel = "ECAM 350.55",
                problemDescription = "Не молет кофе",
                requestStatus = OrderRequestStatus.NEW,
                orderType = OrderType.REGULAR,
                arrivalTime = "16:00 - 18:00",
                status = RepairStatus.NEW,
                estimatedCost = 1500.0
            ),
            Order(
                id = nextOrderId++,
                clientId = testClients[2].id,
                clientName = testClients[2].name,
                clientPhone = testClients[2].phone,
                clientAddress = "Тверь, Комсомольская пл., д. 6, кв. 33",
                latitude = 56.857667,
                longitude = 35.904611,
                deviceType = "Духовой шкаф",
                deviceBrand = "Electrolux",
                deviceModel = "EOB8851AAX",
                problemDescription = "Не нагревается",
                requestStatus = OrderRequestStatus.NEW,
                orderType = OrderType.REGULAR,
                arrivalTime = "13:00 - 15:00",
                status = RepairStatus.NEW,
                estimatedCost = 4500.0
            )
        )
        _orders.value = testOrders
    }
    
    // Методы для работы с заказами
    fun addOrder(order: Order) {
        val newOrder = order.copy(id = nextOrderId++)
        _orders.value = _orders.value + newOrder
    }
    
    fun updateOrder(order: Order) {
        _orders.value = _orders.value.map { 
            if (it.id == order.id) order else it 
        }
    }
    
    fun deleteOrder(orderId: Long) {
        _orders.value = _orders.value.filter { it.id != orderId }
    }
    
    fun getOrderById(orderId: Long): Order? {
        return _orders.value.find { it.id == orderId }
    }
    
    fun getOrdersByClient(clientId: Long): List<Order> {
        return _orders.value.filter { it.clientId == clientId }
    }
    
    fun getActiveOrders(): List<Order> {
        return _orders.value.filter { it.isActive() }
    }
    
    // Методы для работы с клиентами
    fun addClient(client: Client) {
        val newClient = client.copy(id = nextClientId++)
        _clients.value = _clients.value + newClient
    }
    
    fun updateClient(client: Client) {
        _clients.value = _clients.value.map { 
            if (it.id == client.id) client else it 
        }
    }
    
    fun deleteClient(clientId: Long) {
        _clients.value = _clients.value.filter { it.id != clientId }
    }
    
    fun getClientById(clientId: Long): Client? {
        return _clients.value.find { it.id == clientId }
    }
    
    // Статистика
    fun getStatistics(): Statistics {
        val allOrders = _orders.value
        val activeOrders = allOrders.filter { it.isActive() }
        val completedThisMonth = allOrders.filter { 
            it.status == RepairStatus.COMPLETED &&
            isCurrentMonth(it.completedAt)
        }
        val totalRevenue = completedThisMonth.sumOf { it.finalCost ?: 0.0 }
        
        return Statistics(
            activeOrdersCount = activeOrders.size,
            newOrdersCount = allOrders.count { it.status == RepairStatus.NEW },
            clientsCount = _clients.value.size,
            monthlyRevenue = totalRevenue
        )
    }
    
    private fun isCurrentMonth(date: Date?): Boolean {
        if (date == null) return false
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        
        calendar.time = date
        val dateMonth = calendar.get(java.util.Calendar.MONTH)
        val dateYear = calendar.get(java.util.Calendar.YEAR)
        
        return currentMonth == dateMonth && currentYear == dateYear
    }
    
    private fun initNewsData() {
        val testNews = listOf(
            News(
                id = nextNewsId++,
                title = "5 признаков того, что смартфон нуждается в ремонте",
                summary = "Узнайте основные сигналы, указывающие на необходимость профессионального ремонта вашего устройства",
                content = "Быстрая разрядка батареи, перегрев устройства, медленная работа, проблемы с сенсором и странные звуки - все это может указывать на серьезные проблемы.",
                category = NewsCategory.TIPS,
                publishedAt = Date(System.currentTimeMillis() - 86400000) // 1 день назад
            ),
            News(
                id = nextNewsId++,
                title = "Новые стандарты USB-C в 2025 году",
                summary = "Европейский союз вводит обязательное использование USB-C для всех мобильных устройств",
                content = "С 2025 года все новые смартфоны, планшеты и ноутбуки должны поддерживать стандарт зарядки USB-C.",
                category = NewsCategory.INDUSTRY,
                publishedAt = Date(System.currentTimeMillis() - 172800000) // 2 дня назад
            ),
            News(
                id = nextNewsId++,
                title = "Как правильно чистить ноутбук от пыли",
                summary = "Пошаговое руководство по безопасной очистке вашего ноутбука",
                content = "Регулярная чистка ноутбука от пыли продлевает срок его службы и предотвращает перегрев. Узнайте, как это сделать правильно.",
                category = NewsCategory.GUIDES,
                publishedAt = Date(System.currentTimeMillis() - 259200000) // 3 дня назад
            ),
            News(
                id = nextNewsId++,
                title = "Топ-5 инструментов для ремонта техники",
                summary = "Необходимый набор для профессионального ремонта электроники",
                content = "Качественная отвертка, пинцет, мультиметр, термофен и увеличительное стекло - базовый набор каждого мастера.",
                category = NewsCategory.TOOLS,
                publishedAt = Date(System.currentTimeMillis() - 345600000) // 4 дня назад
            ),
            News(
                id = nextNewsId++,
                title = "Рост спроса на ремонт складных смартфонов",
                summary = "Складные устройства требуют специализированного подхода к ремонту",
                content = "С ростом популярности складных смартфонов увеличивается потребность в специалистах по их ремонту.",
                category = NewsCategory.TRENDS,
                publishedAt = Date(System.currentTimeMillis() - 432000000) // 5 дней назад
            ),
            News(
                id = nextNewsId++,
                title = "Защита от влаги: миф или реальность?",
                summary = "Разбираемся в стандартах водозащиты современных устройств",
                content = "IP67, IP68 - что означают эти обозначения и стоит ли доверять рекламе производителей.",
                category = NewsCategory.TIPS,
                publishedAt = Date(System.currentTimeMillis() - 518400000) // 6 дней назад
            )
        )
        _news.value = testNews
    }
    
    private fun initMastersData() {
        val testMasters = listOf(
            Master(
                id = nextMasterId++,
                userId = 1,
                name = "Алексей Смирнов",
                phone = "79161234567",
                email = "smirnov@example.com",
                specialization = listOf("Стиральная машина", "Посудомоечная машина", "Холодильник"),
                rating = 4.8,
                completedOrders = 145,
                status = MasterStatus.AVAILABLE,
                latitude = 56.859611,
                longitude = 35.911896,
                isOnShift = true
            ),
            Master(
                id = nextMasterId++,
                userId = 2,
                name = "Дмитрий Кузнецов",
                phone = "79167654321",
                email = "kuznetsov@example.com",
                specialization = listOf("Кондиционер", "Водонагреватель", "Духовой шкаф"),
                rating = 4.9,
                completedOrders = 203,
                status = MasterStatus.AVAILABLE,
                latitude = 56.858506,
                longitude = 35.900775,
                isOnShift = true
            ),
            Master(
                id = nextMasterId++,
                userId = 3,
                name = "Сергей Попов",
                phone = "79165556677",
                email = "popov@example.com",
                specialization = listOf("Ноутбук", "Десктоп", "Кофемашина"),
                rating = 4.7,
                completedOrders = 98,
                status = MasterStatus.AVAILABLE,
                latitude = 56.857422,
                longitude = 35.917034,
                isOnShift = true
            )
        )
        _masters.value = testMasters
    }
    
    // Методы для работы с мастерами
    fun getAvailableMasters(deviceType: String): List<Master> {
        return _masters.value.filter { master ->
            master.isOnShift &&
            master.status == MasterStatus.AVAILABLE &&
            master.specialization.contains(deviceType)
        }.sortedByDescending { it.rating }
    }
    
    fun updateMasterStatus(masterId: Long, status: MasterStatus) {
        _masters.value = _masters.value.map { 
            if (it.id == masterId) it.copy(status = status) else it 
        }
    }
    
    fun getMasterById(masterId: Long): Master? {
        return _masters.value.find { it.id == masterId }
    }
    
    // Методы для работы с назначениями заказов
    fun createOrderAssignment(orderId: Long, masterId: Long, expirationMinutes: Int = 5): OrderAssignment {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MINUTE, expirationMinutes)
        
        val assignment = OrderAssignment(
            id = nextAssignmentId++,
            orderId = orderId,
            masterId = masterId,
            status = AssignmentStatus.PENDING,
            expiresAt = calendar.time
        )
        
        _orderAssignments.value = _orderAssignments.value + assignment
        return assignment
    }
    
    fun updateAssignmentStatus(assignmentId: Long, status: AssignmentStatus) {
        _orderAssignments.value = _orderAssignments.value.map { 
            if (it.id == assignmentId) {
                it.copy(
                    status = status,
                    respondedAt = if (status != AssignmentStatus.PENDING) Date() else null
                )
            } else it 
        }
    }
    
    fun getActiveAssignmentForOrder(orderId: Long): OrderAssignment? {
        return _orderAssignments.value.find { 
            it.orderId == orderId && it.status == AssignmentStatus.PENDING 
        }
    }
    
    fun getPendingAssignmentsForMaster(masterId: Long): List<OrderAssignment> {
        return _orderAssignments.value.filter { 
            it.masterId == masterId && it.status == AssignmentStatus.PENDING 
        }
    }
    
    fun getAssignmentsByOrder(orderId: Long): List<OrderAssignment> {
        return _orderAssignments.value.filter { it.orderId == orderId }
    }
    
    /**
     * Получает доходы за последние 7 дней
     * @return Список из 7 значений (от самого старого к самому новому)
     */
    fun getWeeklyRevenue(): List<Double> {
        val calendar = java.util.Calendar.getInstance()
        val weeklyRevenue = mutableListOf<Double>()
        
        // Получаем доходы за последние 7 дней
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            
            val dayStart = calendar.time
            
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.time
            
            // Считаем доход за этот день
            val dayRevenue = _orders.value
                .filter { order ->
                    val completedAt = order.completedAt
                    completedAt != null && completedAt >= dayStart && completedAt < dayEnd &&
                    order.status == RepairStatus.COMPLETED
                }
                .sumOf { it.finalCost ?: 0.0 }
            
            weeklyRevenue.add(dayRevenue)
        }
        
        // Если данных нет, возвращаем демо-данные
        if (weeklyRevenue.all { it == 0.0 }) {
            return listOf(3200.0, 4500.0, 2800.0, 5100.0, 3800.0, 4200.0, 5200.0)
        }
        
        return weeklyRevenue
    }
}

data class Statistics(
    val activeOrdersCount: Int,
    val newOrdersCount: Int,
    val clientsCount: Int,
    val monthlyRevenue: Double
)

enum class OrderFilter {
    ALL, ACTIVE, NEW, IN_PROGRESS, READY, COMPLETED
}

// Фильтры для ленты новых заказов
data class OrderFeedFilter(
    val deviceTypes: Set<String> = emptySet(), // Пустой = все типы
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val searchQuery: String = ""
)


