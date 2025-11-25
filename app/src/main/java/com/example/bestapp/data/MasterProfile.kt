package com.example.bestapp.data

// Навык мастера
data class Skill(
    val id: String,
    val name: String,
    val experience: Int // Опыт в годах
)

// Зона работы (район, город)
data class WorkZone(
    val id: String,
    val name: String,
    val type: ZoneType
)

enum class ZoneType(val displayName: String) {
    DISTRICT("Район"),
    CITY("Город"),
    METRO("Метро")
}

// График работы
data class WorkSchedule(
    val monday: DaySchedule = DaySchedule(),
    val tuesday: DaySchedule = DaySchedule(),
    val wednesday: DaySchedule = DaySchedule(),
    val thursday: DaySchedule = DaySchedule(),
    val friday: DaySchedule = DaySchedule(),
    val saturday: DaySchedule = DaySchedule(isWorkday = false),
    val sunday: DaySchedule = DaySchedule(isWorkday = false)
)

data class DaySchedule(
    val isWorkday: Boolean = true,
    val startTime: String = "09:00",
    val endTime: String = "18:00"
)

// Тариф на услугу
data class ServiceRate(
    val id: String,
    val serviceName: String,
    val price: Double,
    val unit: String = "₽", // За единицу (час, услуга и т.д.)
    val duration: Int? = null // Длительность в минутах (опционально)
)

// Полный профиль мастера
data class MasterProfile(
    val userId: Long,
    val skills: List<Skill> = emptyList(),
    val workZones: List<WorkZone> = emptyList(),
    val schedule: WorkSchedule = WorkSchedule(),
    val rates: List<ServiceRate> = emptyList(),
    val description: String? = null,
    val minOrderCost: Double? = null,
    val isCalloutAvailable: Boolean = true, // Выезд на дом
    val isRemoteAvailable: Boolean = false // Удаленная работа
)







