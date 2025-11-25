package com.example.bestapp.data

/**
 * Настройки автоматического приема заказов
 */
data class AutoAcceptSettings(
    val isEnabled: Boolean = false,
    val minPrice: Double? = null, // Минимальная цена заказа
    val maxDistance: Double? = null, // Максимальное расстояние в метрах
    val deviceTypes: Set<String> = emptySet(), // Типы техники (пустое = все типы)
    val workHoursStart: Int? = null, // Начало рабочего времени (0-23)
    val workHoursEnd: Int? = null, // Конец рабочего времени (0-23)
    val acceptUrgentOnly: Boolean = false, // Принимать только срочные заказы
    val minRating: Double? = null // Минимальный рейтинг клиента (опционально)
) {
    /**
     * Проверяет, подходит ли заказ под настройки автоприема
     */
    fun matchesOrder(order: Order, distance: Double? = null): Boolean {
        if (!isEnabled) return false
        
        // Проверка цены
        minPrice?.let {
            val orderPrice = order.estimatedCost ?: 0.0
            if (orderPrice < it) return false
        }
        
        // Проверка расстояния
        maxDistance?.let {
            val orderDistance = distance ?: order.distance ?: return false
            if (orderDistance > it) return false
        }
        
        // Проверка типа техники
        if (deviceTypes.isNotEmpty() && !deviceTypes.contains(order.deviceType)) {
            return false
        }
        
        // Проверка срочности
        if (acceptUrgentOnly) {
            if (order.orderType != OrderType.URGENT && 
                order.urgency != "emergency" && 
                order.urgency != "urgent") {
                return false
            }
        }
        
        // Проверка рабочего времени
        if (workHoursStart != null && workHoursEnd != null) {
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (workHoursStart <= workHoursEnd) {
                // Обычный диапазон (например, 9-18)
                if (currentHour < workHoursStart || currentHour >= workHoursEnd) {
                    return false
                }
            } else {
                // Переход через полночь (например, 22-6)
                if (currentHour < workHoursStart && currentHour >= workHoursEnd) {
                    return false
                }
            }
        }
        
        return true
    }
}

