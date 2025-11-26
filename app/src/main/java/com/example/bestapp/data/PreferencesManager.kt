package com.example.bestapp.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Менеджер для сохранения настроек и состояния приложения
 */
class PreferencesManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "bestapp_prefs"
        private const val KEY_IS_SHIFT_ACTIVE = "is_shift_active"
        private const val KEY_MASTER_ID = "master_id"
        private const val KEY_SHIFT_START_TIME = "shift_start_time"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
        private const val KEY_SELECTED_CITY = "selected_city"
        
        // Настройки автоприема
        private const val KEY_AUTO_ACCEPT_ENABLED = "auto_accept_enabled"
        private const val KEY_AUTO_ACCEPT_MIN_PRICE = "auto_accept_min_price"
        private const val KEY_AUTO_ACCEPT_MAX_DISTANCE = "auto_accept_max_distance"
        private const val KEY_AUTO_ACCEPT_DEVICE_TYPES = "auto_accept_device_types"
        private const val KEY_AUTO_ACCEPT_WORK_HOURS_START = "auto_accept_work_hours_start"
        private const val KEY_AUTO_ACCEPT_WORK_HOURS_END = "auto_accept_work_hours_end"
        private const val KEY_AUTO_ACCEPT_URGENT_ONLY = "auto_accept_urgent_only"
        
        // Зоны работы
        private const val KEY_WORK_ZONES = "work_zones"
        
        // Фильтры заказов
        private const val KEY_FILTER_DEVICE_TYPES = "filter_device_types"
        private const val KEY_FILTER_MIN_PRICE = "filter_min_price"
        private const val KEY_FILTER_MAX_PRICE = "filter_max_price"
        private const val KEY_FILTER_MAX_DISTANCE = "filter_max_distance"
        private const val KEY_FILTER_URGENCY = "filter_urgency"
        private const val KEY_FILTER_SORT_BY = "filter_sort_by"
        
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    /**
     * Сохраняет статус смены
     */
    fun setShiftActive(isActive: Boolean) {
        prefs.edit().putBoolean(KEY_IS_SHIFT_ACTIVE, isActive).apply()
        if (isActive) {
            // Сохраняем время начала смены
            prefs.edit().putLong(KEY_SHIFT_START_TIME, System.currentTimeMillis()).apply()
        } else {
            // Удаляем время начала при завершении смены
            prefs.edit().remove(KEY_SHIFT_START_TIME).apply()
        }
    }
    
    /**
     * Получает статус смены
     */
    fun isShiftActive(): Boolean {
        return prefs.getBoolean(KEY_IS_SHIFT_ACTIVE, false)
    }
    
    /**
     * Получает время начала смены
     */
    fun getShiftStartTime(): Long? {
        val time = prefs.getLong(KEY_SHIFT_START_TIME, -1)
        return if (time > 0) time else null
    }
    
    /**
     * Сохраняет ID мастера
     */
    fun setMasterId(masterId: Long?) {
        if (masterId != null) {
            prefs.edit().putLong(KEY_MASTER_ID, masterId).apply()
        } else {
            prefs.edit().remove(KEY_MASTER_ID).apply()
        }
    }
    
    /**
     * Получает ID мастера
     */
    fun getMasterId(): Long? {
        val id = prefs.getLong(KEY_MASTER_ID, -1)
        return if (id > 0) id else null
    }
    
    /**
     * Сохраняет токен авторизации
     */
    fun setAuthToken(token: String?) {
        if (token != null) {
            prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        } else {
            prefs.edit().remove(KEY_AUTH_TOKEN).apply()
        }
    }
    
    /**
     * Получает токен авторизации
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    /**
     * Онбординг показан
     */
    fun isOnboardingShown(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
    }
    
    fun setOnboardingShown(shown: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, shown).apply()
    }
    
    /**
     * Выбранный город
     */
    fun setSelectedCity(city: String) {
        prefs.edit().putString(KEY_SELECTED_CITY, city).apply()
    }
    
    fun getSelectedCity(): String? {
        return prefs.getString(KEY_SELECTED_CITY, null)
    }
    
    /**
     * Сохраняет настройки автоприема
     */
    fun setAutoAcceptSettings(settings: AutoAcceptSettings) {
        prefs.edit().apply {
            putBoolean(KEY_AUTO_ACCEPT_ENABLED, settings.isEnabled)
            if (settings.minPrice != null) {
                putFloat(KEY_AUTO_ACCEPT_MIN_PRICE, settings.minPrice.toFloat())
            } else {
                remove(KEY_AUTO_ACCEPT_MIN_PRICE)
            }
            if (settings.maxDistance != null) {
                putFloat(KEY_AUTO_ACCEPT_MAX_DISTANCE, settings.maxDistance.toFloat())
            } else {
                remove(KEY_AUTO_ACCEPT_MAX_DISTANCE)
            }
            if (settings.deviceTypes.isNotEmpty()) {
                putStringSet(KEY_AUTO_ACCEPT_DEVICE_TYPES, settings.deviceTypes)
            } else {
                remove(KEY_AUTO_ACCEPT_DEVICE_TYPES)
            }
            putBoolean(KEY_AUTO_ACCEPT_URGENT_ONLY, settings.acceptUrgentOnly)
        }.apply()
    }
    
    /**
     * Получает настройки автоприема
     */
    fun getAutoAcceptSettings(): AutoAcceptSettings {
        return AutoAcceptSettings(
            isEnabled = prefs.getBoolean(KEY_AUTO_ACCEPT_ENABLED, false),
            minPrice = if (prefs.contains(KEY_AUTO_ACCEPT_MIN_PRICE)) {
                prefs.getFloat(KEY_AUTO_ACCEPT_MIN_PRICE, 0f).toDouble()
            } else null,
            maxDistance = if (prefs.contains(KEY_AUTO_ACCEPT_MAX_DISTANCE)) {
                prefs.getFloat(KEY_AUTO_ACCEPT_MAX_DISTANCE, 0f).toDouble()
            } else null,
            deviceTypes = prefs.getStringSet(KEY_AUTO_ACCEPT_DEVICE_TYPES, emptySet()) ?: emptySet(),
            acceptUrgentOnly = prefs.getBoolean(KEY_AUTO_ACCEPT_URGENT_ONLY, false)
        )
    }
    
    /**
     * Сохраняет зоны работы
     */
    fun setWorkZones(zones: List<com.example.bestapp.data.MapWorkZone>) {
        val zonesJson = zones.map { zone ->
            """
            {
                "id": ${zone.id},
                "name": "${zone.name}",
                "points": ${zone.points.map { "[${it.first}, ${it.second}]" }},
                "isActive": ${zone.isActive}
            }
            """.trimIndent()
        }.joinToString(",", "[", "]")
        
        prefs.edit().putString(KEY_WORK_ZONES, zonesJson).apply()
    }
    
    /**
     * Получает зоны работы
     */
    fun getWorkZones(): List<com.example.bestapp.data.MapWorkZone> {
        val zonesJson = prefs.getString(KEY_WORK_ZONES, null) ?: return emptyList()
        
        // Упрощенный парсинг (в реальности лучше использовать Gson)
        try {
            val zones = mutableListOf<com.example.bestapp.data.MapWorkZone>()
            // Простой парсинг JSON массива
            val zoneMatches = Regex("""\{[^}]+\}""").findAll(zonesJson)
            zoneMatches.forEach { match ->
                val zoneStr = match.value
                val nameMatch = Regex(""""name":\s*"([^"]+)"""").find(zoneStr)
                val pointsMatch = Regex(""""points":\s*\[(.*?)\]""").find(zoneStr)
                val isActiveMatch = Regex(""""isActive":\s*(true|false)""").find(zoneStr)
                
                nameMatch?.let { name ->
                    pointsMatch?.let { points ->
                        val pointsList = points.groupValues[1].split("],[").mapNotNull { pointStr ->
                            val coords = pointStr.replace("[", "").replace("]", "").split(",")
                            if (coords.size >= 2) {
                                Pair(coords[0].trim().toDoubleOrNull() ?: return@mapNotNull null,
                                     coords[1].trim().toDoubleOrNull() ?: return@mapNotNull null)
                            } else null
                        }
                        
                        if (pointsList.isNotEmpty()) {
                            zones.add(com.example.bestapp.data.MapWorkZone(
                                id = zones.size.toLong(),
                                name = name.groupValues[1],
                                points = pointsList,
                                isActive = isActiveMatch?.groupValues[1]?.toBoolean() ?: true
                            ))
                        }
                    }
                }
            }
            return zones
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Сохраняет фильтры заказов
     */
    fun saveOrderFilters(
        deviceTypes: Set<String>,
        minPrice: Double?,
        maxPrice: Double?,
        maxDistance: Double?,
        urgency: String?,
        sortBy: String?
    ) {
        prefs.edit().apply {
            if (deviceTypes.isNotEmpty()) {
                putStringSet(KEY_FILTER_DEVICE_TYPES, deviceTypes)
            } else {
                remove(KEY_FILTER_DEVICE_TYPES)
            }
            
            if (minPrice != null) {
                putFloat(KEY_FILTER_MIN_PRICE, minPrice.toFloat())
            } else {
                remove(KEY_FILTER_MIN_PRICE)
            }
            
            if (maxPrice != null) {
                putFloat(KEY_FILTER_MAX_PRICE, maxPrice.toFloat())
            } else {
                remove(KEY_FILTER_MAX_PRICE)
            }
            
            if (maxDistance != null) {
                putFloat(KEY_FILTER_MAX_DISTANCE, maxDistance.toFloat())
            } else {
                remove(KEY_FILTER_MAX_DISTANCE)
            }
            
            if (urgency != null) {
                putString(KEY_FILTER_URGENCY, urgency)
            } else {
                remove(KEY_FILTER_URGENCY)
            }
            
            if (sortBy != null) {
                putString(KEY_FILTER_SORT_BY, sortBy)
            } else {
                remove(KEY_FILTER_SORT_BY)
            }
        }.apply()
    }
    
    /**
     * Получает сохраненные фильтры заказов
     */
    fun getOrderFilters(): OrderFilters {
        return OrderFilters(
            deviceTypes = prefs.getStringSet(KEY_FILTER_DEVICE_TYPES, emptySet()) ?: emptySet(),
            minPrice = if (prefs.contains(KEY_FILTER_MIN_PRICE)) {
                prefs.getFloat(KEY_FILTER_MIN_PRICE, 0f).toDouble()
            } else null,
            maxPrice = if (prefs.contains(KEY_FILTER_MAX_PRICE)) {
                prefs.getFloat(KEY_FILTER_MAX_PRICE, 0f).toDouble()
            } else null,
            maxDistance = if (prefs.contains(KEY_FILTER_MAX_DISTANCE)) {
                prefs.getFloat(KEY_FILTER_MAX_DISTANCE, 0f).toDouble()
            } else null,
            urgency = prefs.getString(KEY_FILTER_URGENCY, null),
            sortBy = prefs.getString(KEY_FILTER_SORT_BY, null)
        )
    }
    
    /**
     * Сохраняет boolean значение
     */
    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    /**
     * Получает boolean значение
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    /**
     * Очищает все настройки
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}

/**
 * Класс для хранения фильтров заказов
 */
data class OrderFilters(
    val deviceTypes: Set<String> = emptySet(),
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val maxDistance: Double? = null,
    val urgency: String? = null,
    val sortBy: String? = null
)



