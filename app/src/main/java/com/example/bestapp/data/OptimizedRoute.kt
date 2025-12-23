package com.example.bestapp.data

/**
 * Оптимизированный маршрут для нескольких заказов
 */
data class OptimizedRoute(
    val orders: List<RouteOrder>, // Заказы в оптимальном порядке
    val totalDistance: Double, // Общее расстояние в метрах
    val totalTime: Int, // Общее время в минутах
    val startLocation: Pair<Double, Double>? = null // Начальная точка (lat, lon)
) {
    /**
     * Заказ в маршруте с информацией о расстоянии и времени
     */
    data class RouteOrder(
        val order: Order,
        val distanceFromPrevious: Double, // Расстояние от предыдущего заказа в метрах
        val timeFromPrevious: Int, // Время от предыдущего заказа в минутах
        val cumulativeDistance: Double, // Накопленное расстояние от начала
        val cumulativeTime: Int // Накопленное время от начала
    )
}







