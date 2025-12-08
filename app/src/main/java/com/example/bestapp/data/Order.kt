package com.example.bestapp.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Модель заказа на ремонт
data class Order(
    val id: Long = 0,
    val clientId: Long,
    val clientName: String, // ФИО клиента
    val clientPhone: String, // Номер телефона клиента
    val clientAddress: String, // Полный адрес клиента
    val latitude: Double? = null, // Широта адреса клиента
    val longitude: Double? = null, // Долгота адреса клиента
    val deviceType: String, // Тип устройства (телефон, ноутбук, планшет и т.д.)
    val deviceBrand: String, // Бренд (Samsung, Apple, Xiaomi и т.д.)
    val deviceModel: String, // Модель
    val problemDescription: String, // Описание проблемы
    val requestStatus: OrderRequestStatus = OrderRequestStatus.NEW, // Статус заявки (новый, повторный, гарантия)
    val orderType: OrderType = OrderType.REGULAR, // Тип заказа (срочный, обычный)
    val arrivalTime: String? = null, // Время приезда мастера
    val status: RepairStatus = RepairStatus.NEW, // Статус работы
    val estimatedCost: Double? = null, // Примерная стоимость
    val finalCost: Double? = null, // Финальная стоимость
    val distance: Double? = null, // Расстояние до заказа в метрах (только для мастеров)
    val urgency: String? = null, // Срочность: emergency, urgent, planned
    val expiresAt: Date? = null, // Время истечения назначения (для таймера обратного отсчета)
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val completedAt: Date? = null,
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
}

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
