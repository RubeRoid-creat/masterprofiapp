package com.example.bestapp.data

/**
 * Настройки автоматического приема заказов
 */
data class AutoAcceptSettings(
    val isEnabled: Boolean = false,
    val minPrice: Double? = null, // Минимальная цена заказа
    val maxDistance: Double? = null, // Максимальное расстояние в метрах
    val deviceTypes: Set<String> = emptySet(), // Типы техники (пустое = все типы)
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
        
        return true
    }
}


