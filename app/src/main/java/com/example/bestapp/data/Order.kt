package com.example.bestapp.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Модель заказа на ремонт
data class Order(
    val id: Long = 0,
    val orderNumber: String? = null, // Номер заказа
    val clientId: Long,
    val clientName: String, // ФИО клиента
    val clientPhone: String, // Номер телефона клиента
    val clientEmail: String? = null, // Email клиента
    val clientAddress: String, // Полный адрес клиента
    val latitude: Double? = null, // Широта адреса клиента
    val longitude: Double? = null, // Долгота адреса клиента
    
    // Детализированный адрес
    val addressStreet: String? = null,
    val addressBuilding: String? = null,
    val addressApartment: String? = null,
    val addressFloor: Int? = null,
    val addressEntranceCode: String? = null,
    val addressLandmark: String? = null,
    
    // Информация об устройстве
    val deviceType: String, // Тип устройства (телефон, ноутбук, планшет и т.д.)
    val deviceCategory: String? = null, // Категория: "large", "small", "builtin"
    val deviceBrand: String, // Бренд (Samsung, Apple, Xiaomi и т.д.)
    val deviceModel: String, // Модель
    val deviceSerialNumber: String? = null, // Серийный номер
    val deviceYear: Int? = null, // Год выпуска
    val warrantyStatus: String? = null, // Статус гарантии: "warranty", "post_warranty", "no_warranty"
    
    // Описание проблемы
    val problemShortDescription: String? = null, // Краткое описание
    val problemDescription: String, // Полное описание проблемы
    val problemWhenStarted: String? = null, // Когда началась проблема
    val problemConditions: String? = null, // Условия возникновения
    val problemErrorCodes: String? = null, // Коды ошибок
    val problemAttemptedFixes: String? = null, // Попытки самостоятельного ремонта
    val problemTags: List<String>? = null, // Теги проблемы
    val problemCategory: String? = null, // Категория: "electrical", "mechanical", "electronic", "software"
    val problemSeasonality: String? = null, // Сезонность: "seasonal", "permanent"
    
    // Статусы и типы
    val requestStatus: OrderRequestStatus = OrderRequestStatus.NEW, // Статус заявки (новый, повторный, гарантия)
    val orderType: OrderType = OrderType.REGULAR, // Тип заказа (срочный, обычный)
    val orderSource: String? = null, // Источник: "app", "website", "phone", "admin"
    val priority: String? = null, // Приоритет: "low", "regular", "high", "urgent"
    val arrivalTime: String? = null, // Время приезда мастера
    val desiredRepairDate: String? = null, // Желаемая дата ремонта
    val status: RepairStatus = RepairStatus.NEW, // Статус работы
    val urgency: String? = null, // Срочность: emergency, urgent, planned
    
    // Финансы
    val estimatedCost: Double? = null, // Примерная стоимость
    val finalCost: Double? = null, // Финальная стоимость
    val clientBudget: Double? = null, // Бюджет клиента
    val paymentType: String? = null, // Тип оплаты: "cash", "card", "online", "yoomoney", "qiwi", "installment"
    val paymentStatus: String? = null, // Статус оплаты
    
    // Дополнительная информация
    val intercomWorking: Boolean? = true, // Работает ли домофон
    val parkingAvailable: Boolean? = true, // Есть ли парковка
    val hasPets: Boolean? = false, // Есть ли домашние животные
    val hasSmallChildren: Boolean? = false, // Есть ли маленькие дети
    val preferredContactMethod: String? = "call", // Предпочтительный способ связи: "call", "sms", "whatsapp", "telegram"
    
    // Информация о мастере
    val assignedMasterId: Long? = null, // ID назначенного мастера
    val masterName: String? = null, // Имя мастера
    
    // Диагностика и ремонт
    val preliminaryDiagnosis: String? = null, // Предварительный диагноз
    val requiredParts: String? = null, // Необходимые запчасти
    val specialEquipment: String? = null, // Специальное оборудование
    val repairComplexity: String? = null, // Сложность: "simple", "medium", "complex", "very_complex"
    val estimatedRepairTime: Int? = null, // Оценка времени ремонта в минутах
    
    // Медиа файлы
    val media: List<OrderMedia>? = null, // Фото/видео/документы
    val mediaCount: Int? = null, // Количество медиа файлов
    
    // Временные метки
    val distance: Double? = null, // Расстояние до заказа в метрах (только для мастеров)
    val expiresAt: Date? = null, // Время истечения назначения (для таймера обратного отсчета)
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val completedAt: Date? = null,
    val assignmentDate: String? = null, // Дата назначения
    
    // Внутренние заметки
    val notes: String? = null, // Внутренние заметки мастера
    
    // Информация о назначении (если мастер видит этот заказ через assignment)
    val assignmentId: Long? = null,
    val assignmentStatus: String? = null
) {
    fun getFormattedCreatedDate(): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(createdAt)
    }
    
    fun getDeviceFullName(): String {
        return "$deviceBrand $deviceModel"
    }
    
    fun getFormattedCost(): String {
        val cost = finalCost ?: estimatedCost
        return if (cost != null) {
            String.format(Locale.getDefault(), "%.0f ₽", cost)
        } else {
            "Не определена"
        }
    }
    
    fun isActive(): Boolean {
        return status != RepairStatus.COMPLETED && status != RepairStatus.CANCELLED
    }
    
    /**
     * Получить полный адрес с деталями
     */
    fun getFullAddress(): String {
        val parts = mutableListOf<String>()
        addressStreet?.let { parts.add(it) }
        addressBuilding?.let { parts.add("д. $it") }
        addressApartment?.let { parts.add("кв. $it") }
        addressFloor?.let { parts.add("эт. $it") }
        addressEntranceCode?.let { parts.add("код: $it") }
        addressLandmark?.let { parts.add("($it)") }
        
        return if (parts.isNotEmpty()) {
            "$clientAddress, ${parts.joinToString(", ")}"
        } else {
            clientAddress
        }
    }
    
    /**
     * Получить полное описание проблемы
     */
    fun getFullProblemDescription(): String {
        val parts = mutableListOf<String>()
        problemShortDescription?.let { parts.add(it) }
        parts.add(problemDescription)
        problemWhenStarted?.let { parts.add("Началось: $it") }
        problemConditions?.let { parts.add("Условия: $it") }
        problemErrorCodes?.let { parts.add("Коды ошибок: $it") }
        problemAttemptedFixes?.let { parts.add("Попытки ремонта: $it") }
        return parts.joinToString("\n\n")
    }
}

// Модель медиа файла заказа
data class OrderMedia(
    val id: Long,
    val orderId: Long,
    val mediaType: String, // "photo", "video", "document", "audio"
    val fileUrl: String?,
    val fileName: String?,
    val fileSize: Long?,
    val mimeType: String?,
    val description: String?,
    val thumbnailUrl: String?,
    val duration: Int?, // Для видео/аудио в секундах
    val createdAt: String?
)

// Статус заявки
enum class OrderRequestStatus(val displayName: String) {
    NEW("Новый"),
    REPEAT("Повторный"),
    WARRANTY("Гарантия")
}

// Тип заказа
enum class OrderType(val displayName: String) {
    URGENT("Срочный"),
    REGULAR("Обычный")
}

// Статус ремонта
enum class RepairStatus(val displayName: String) {
    NEW("Новый"),
    DIAGNOSTICS("Диагностика"),
    WAITING_PARTS("Ожидание запчастей"),
    IN_PROGRESS("В работе"),
    READY("Готов"),
    COMPLETED("Завершен"),
    CANCELLED("Отменен")
}
