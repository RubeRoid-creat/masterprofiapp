package com.bestapp.client.data.models

/**
 * Каталог бытовой техники для клиентского приложения
 * Основано на базе знаний "МАСТЕРПРОФИ"
 */

data class DeviceCategory(
    val id: String,
    val name: String,
    val icon: String, // Icons8 URL или название
    val devices: List<Device>
)

data class Device(
    val id: String,
    val name: String,
    val icon: String,
    val category: String,
    val commonProblems: List<String>,
    val brands: List<String> = DEFAULT_BRANDS
)

data class DeviceProblem(
    val id: String,
    val deviceType: String,
    val title: String,
    val description: String? = null
)

// Бренды техники
val DEFAULT_BRANDS = listOf(
    "Samsung", "LG", "Bosch", "Indesit", "Ariston",
    "Atlant", "Beko", "Candy", "Electrolux", "Gorenje",
    "Haier", "Hansa", "Hotpoint-Ariston", "Whirlpool",
    "Siemens", "Zanussi", "Midea", "Другой бренд"
)

/**
 * Каталог техники с проблемами из базы знаний
 */
object DeviceCatalog {
    
    val KITCHEN_CATEGORY = DeviceCategory(
        id = "kitchen",
        name = "Кухонная техника",
        icon = "icons8-kitchen",
        devices = listOf(
            Device(
                id = "refrigerator",
                name = "Холодильник",
                icon = "icons8-fridge",
                category = "kitchen",
                commonProblems = listOf(
                    "Не морозит",
                    "Работает без остановки",
                    "Вода в камере/под ящиками",
                    "Сильный конденсат",
                    "Шум",
                    "Шуба в камере (No Frost)",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "dishwasher",
                name = "Посудомоечная машина",
                icon = "icons8-dishwasher",
                category = "kitchen",
                commonProblems = listOf(
                    "Плохо моет",
                    "Остается накипь/порошок",
                    "Течет",
                    "Не сушит",
                    "Не сливает воду",
                    "Код ошибки",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "oven",
                name = "Духовой шкаф",
                icon = "icons8-oven",
                category = "kitchen",
                commonProblems = listOf(
                    "Не нагревается",
                    "Не держит температуру",
                    "Не работает вентилятор",
                    "Не включается",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "electric_stove",
                name = "Электрическая плита",
                icon = "icons8-stove",
                category = "kitchen",
                commonProblems = listOf(
                    "Не включается конфорка",
                    "Не нагревается",
                    "Не держит температуру",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "cooktop",
                name = "Варочная панель (индукционная/электрическая)",
                icon = "icons8-induction",
                category = "kitchen",
                commonProblems = listOf(
                    "Не реагирует на касания",
                    "Греет не все зоны",
                    "Отключается",
                    "Показывает ошибку",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "microwave",
                name = "Микроволновая печь",
                icon = "icons8-microwave",
                category = "kitchen",
                commonProblems = listOf(
                    "Не греет",
                    "Не включается",
                    "Искрит внутри",
                    "Шумит",
                    "Не вращается тарелка",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "coffee_machine",
                name = "Кофемашина (капсульная, рожковая)",
                icon = "icons8-coffee-machine",
                category = "kitchen",
                commonProblems = listOf(
                    "Не выдает кофе",
                    "Течет вода",
                    "Не взбивает молоко",
                    "Показывает ошибку очистки/засора",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "hood",
                name = "Вытяжка",
                icon = "icons8-extractor-fan",
                category = "kitchen",
                commonProblems = listOf(
                    "Не включается",
                    "Слабая тяга",
                    "Шумит",
                    "Не работает подсветка",
                    "Другая проблема..."
                )
            )
        )
    )
    
    val LAUNDRY_CATEGORY = DeviceCategory(
        id = "laundry",
        name = "Стирка и сушка",
        icon = "icons8-washing-machine",
        devices = listOf(
            Device(
                id = "washing_machine",
                name = "Стиральная машина",
                icon = "icons8-washing-machine",
                category = "laundry",
                commonProblems = listOf(
                    "Не включается",
                    "Не греет воду",
                    "Не сливает воду",
                    "Не набирает воду",
                    "Течет",
                    "Сильная вибрация",
                    "Не вращает барабан",
                    "Код ошибки (E01, E10 и т.д.)",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "dryer",
                name = "Сушильная машина",
                icon = "icons8-tumble-dryer",
                category = "laundry",
                commonProblems = listOf(
                    "Не сушит",
                    "Не нагревается",
                    "Не вращает барабан",
                    "Шумит",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "washer_dryer",
                name = "Стирально-сушильная машина",
                icon = "icons8-washer-dryer",
                category = "laundry",
                commonProblems = listOf(
                    "Не стирает",
                    "Не сушит",
                    "Не греет воду",
                    "Не сливает воду",
                    "Другая проблема..."
                )
            )
        )
    )
    
    val CLIMATE_CATEGORY = DeviceCategory(
        id = "climate",
        name = "Климатическая техника",
        icon = "icons8-air-conditioner",
        devices = listOf(
            Device(
                id = "air_conditioner",
                name = "Кондиционер (сплит-система)",
                icon = "icons8-air-conditioner",
                category = "climate",
                commonProblems = listOf(
                    "Не охлаждает",
                    "Не греет",
                    "Течет вода в комнату",
                    "Шумит",
                    "Неприятный запах",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "heater",
                name = "Обогреватель",
                icon = "icons8-heater",
                category = "climate",
                commonProblems = listOf(
                    "Не греет",
                    "Не включается",
                    "Странный запах",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "water_heater",
                name = "Водонагреватель",
                icon = "icons8-water-heater",
                category = "climate",
                commonProblems = listOf(
                    "Не греет воду",
                    "Течет",
                    "Долго греется",
                    "Шумит",
                    "Другая проблема..."
                )
            )
        )
    )
    
    val ELECTRONICS_CATEGORY = DeviceCategory(
        id = "electronics",
        name = "Электроника и компьютеры",
        icon = "icons8-laptop",
        devices = listOf(
            Device(
                id = "laptop",
                name = "Ноутбук",
                icon = "icons8-laptop",
                category = "electronics",
                commonProblems = listOf(
                    "Не включается",
                    "Медленно работает",
                    "Перегревается",
                    "Не работает клавиатура",
                    "Проблемы с экраном",
                    "Не заряжается",
                    "Другая проблема..."
                ),
                brands = listOf(
                    "Apple", "Asus", "Acer", "Dell", "HP", "Lenovo", 
                    "MSI", "Samsung", "Xiaomi", "Huawei", "Другой бренд"
                )
            ),
            Device(
                id = "desktop",
                name = "Стационарный компьютер",
                icon = "icons8-computer",
                category = "electronics",
                commonProblems = listOf(
                    "Не включается",
                    "Медленно работает",
                    "Перегревается",
                    "Шумит",
                    "Синий экран смерти",
                    "Другая проблема..."
                ),
                brands = listOf(
                    "Собранный на заказ", "HP", "Dell", "Lenovo", 
                    "Acer", "Asus", "Другой бренд"
                )
            ),
            Device(
                id = "monitor",
                name = "Монитор",
                icon = "icons8-monitor",
                category = "electronics",
                commonProblems = listOf(
                    "Нет изображения",
                    "Мерцает",
                    "Искажение цветов",
                    "Полосы на экране",
                    "Не включается",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "printer",
                name = "Принтер/МФУ",
                icon = "icons8-printer",
                category = "electronics",
                commonProblems = listOf(
                    "Не печатает",
                    "Плохое качество печати",
                    "Замятие бумаги",
                    "Не видит компьютер",
                    "Другая проблема..."
                ),
                brands = listOf(
                    "HP", "Canon", "Epson", "Brother", "Samsung", 
                    "Xerox", "Kyocera", "Другой бренд"
                )
            )
        )
    )
    
    val OTHER_CATEGORY = DeviceCategory(
        id = "other",
        name = "Другая техника",
        icon = "icons8-toolbox",
        devices = listOf(
            Device(
                id = "freezer",
                name = "Морозильный ларь",
                icon = "icons8-freezer",
                category = "other",
                commonProblems = listOf(
                    "Не морозит",
                    "Работает без остановки",
                    "Шумит",
                    "Другая проблема..."
                )
            ),
            Device(
                id = "tv",
                name = "Телевизор",
                icon = "icons8-tv",
                category = "other",
                commonProblems = listOf(
                    "Не включается",
                    "Нет изображения",
                    "Нет звука",
                    "Полосы на экране",
                    "Не работает пульт",
                    "Другая проблема..."
                ),
                brands = listOf(
                    "Samsung", "LG", "Sony", "Philips", "Xiaomi", 
                    "TCL", "Hisense", "Panasonic", "Другой бренд"
                )
            ),
            Device(
                id = "vacuum",
                name = "Пылесос",
                icon = "icons8-vacuum-cleaner",
                category = "other",
                commonProblems = listOf(
                    "Не включается",
                    "Слабая тяга",
                    "Шумит",
                    "Перегревается",
                    "Другая проблема..."
                )
            )
        )
    )
    
    // Все категории
    val ALL_CATEGORIES = listOf(
        KITCHEN_CATEGORY,
        LAUNDRY_CATEGORY,
        CLIMATE_CATEGORY,
        ELECTRONICS_CATEGORY,
        OTHER_CATEGORY
    )
    
    // Все устройства
    val ALL_DEVICES = ALL_CATEGORIES.flatMap { it.devices }
    
    // Поиск устройства по ID
    fun getDeviceById(deviceId: String): Device? {
        return ALL_DEVICES.find { it.id == deviceId }
    }
    
    // Поиск устройства по названию
    fun getDeviceByName(deviceName: String): Device? {
        return ALL_DEVICES.find { 
            it.name.equals(deviceName, ignoreCase = true)
        }
    }
    
    // Получить проблемы для устройства
    fun getProblemsForDevice(deviceId: String): List<String> {
        return getDeviceById(deviceId)?.commonProblems ?: emptyList()
    }
    
    // Получить бренды для устройства
    fun getBrandsForDevice(deviceId: String): List<String> {
        return getDeviceById(deviceId)?.brands ?: DEFAULT_BRANDS
    }
    
    // Поиск устройств по запросу
    fun searchDevices(query: String): List<Device> {
        if (query.isBlank()) return ALL_DEVICES
        
        val lowerQuery = query.lowercase()
        return ALL_DEVICES.filter { device ->
            device.name.lowercase().contains(lowerQuery) ||
            device.category.lowercase().contains(lowerQuery)
        }
    }
}
