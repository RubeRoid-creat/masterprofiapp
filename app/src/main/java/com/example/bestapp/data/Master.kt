package com.example.bestapp.data

import java.util.Date

// Модель мастера
data class Master(
    val id: Long = 0,
    val userId: Long, // Связь с User
    val name: String,
    val phone: String,
    val email: String? = null,
    val specialization: List<String>, // Типы техники, которые ремонтирует
    val rating: Double = 0.0,
    val completedOrders: Int = 0,
    val status: MasterStatus = MasterStatus.AVAILABLE,
    val latitude: Double? = null, // Текущее местоположение
    val longitude: Double? = null,
    val isOnShift: Boolean = false, // На смене или нет
    val createdAt: Date = Date()
)

// Статус мастера
enum class MasterStatus(val displayName: String) {
    AVAILABLE("Свободен"),
    BUSY("Занят"),
    IN_WORK("В работе"),
    OFFLINE("Не на смене")
}

// Модель назначения заказа мастеру
data class OrderAssignment(
    val id: Long = 0,
    val orderId: Long,
    val masterId: Long,
    val status: AssignmentStatus = AssignmentStatus.PENDING,
    val notifiedAt: Date = Date(),
    val respondedAt: Date? = null,
    val expiresAt: Date, // Время истечения ожидания ответа
    val rejectionReason: String? = null
)

// Статус назначения заказа
enum class AssignmentStatus(val displayName: String) {
    PENDING("Ожидает ответа"),
    ACCEPTED("Принят"),
    REJECTED("Отклонён"),
    EXPIRED("Истекло время")
}







