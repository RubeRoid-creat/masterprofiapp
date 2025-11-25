package com.example.bestapp.data

/**
 * Зона работы мастера (полигон на карте)
 * Переименовано в MapWorkZone, чтобы избежать конфликта с WorkZone в MasterProfile
 */
data class MapWorkZone(
    val id: Long = 0,
    val name: String, // Название зоны (например, "Центр", "Северный район")
    val points: List<Pair<Double, Double>>, // Координаты точек полигона (lat, lon)
    val isActive: Boolean = true // Активна ли зона
) {
    /**
     * Проверяет, находится ли точка внутри зоны (алгоритм Ray Casting)
     */
    fun containsPoint(latitude: Double, longitude: Double): Boolean {
        if (points.size < 3) return false
        
        var inside = false
        var j = points.size - 1
        
        for (i in points.indices) {
            val xi = points[i].first
            val yi = points[i].second
            val xj = points[j].first
            val yj = points[j].second
            
            val intersect = ((yi > latitude) != (yj > latitude)) &&
                    (longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi)
            
            if (intersect) inside = !inside
            j = i
        }
        
        return inside
    }
    
    /**
     * Конвертирует в GeoJSON формат для сохранения
     */
    fun toGeoJson(): String {
        val coordinates = points.map { listOf(it.second, it.first) } // GeoJSON: [lon, lat]
        // Замыкаем полигон (первая точка = последняя)
        val closedCoordinates = coordinates + coordinates.first()
        
        return """
        {
            "type": "Polygon",
            "coordinates": [$closedCoordinates]
        }
        """.trimIndent()
    }
    
    companion object {
        /**
         * Создает WorkZone из GeoJSON
         */
        fun fromGeoJson(name: String, geoJson: String): MapWorkZone? {
            // Упрощенный парсинг GeoJSON (в реальности лучше использовать библиотеку)
            try {
                // Извлекаем coordinates из JSON
                val coordsMatch = Regex("""\[\[(.*?)\]\]""").find(geoJson)
                coordsMatch?.let {
                    val coordsStr = it.groupValues[1]
                    val points = coordsStr.split("],[").map { coordPair ->
                        val parts = coordPair.replace("[", "").replace("]", "").split(",")
                        if (parts.size >= 2) {
                            Pair(parts[1].trim().toDouble(), parts[0].trim().toDouble()) // [lon, lat] -> (lat, lon)
                        } else null
                    }.filterNotNull()
                    
                    if (points.isNotEmpty()) {
                        return MapWorkZone(name = name, points = points)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}

