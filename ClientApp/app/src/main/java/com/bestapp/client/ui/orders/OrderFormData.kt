package com.bestapp.client.ui.orders

/**
 * Данные для выпадающих списков в форме создания заказа
 */
object OrderFormData {
    
    // Типы техники (согласно требованиям)
    val deviceTypes = listOf(
        "Стиральная машина",
        "Посудомоечная машина",
        "Духовой шкаф",
        "Холодильник",
        "Микроволновая печь",
        "Морозильный ларь",
        "Варочная панель",
        "Ноутбук",
        "Десктоп",
        "Кофемашина",
        "Кондиционер",
        "Водонагреватель"
    )
    
    // Популярные бренды
    val deviceBrands = listOf(
        "Samsung",
        "LG",
        "Bosch",
        "Siemens",
        "Electrolux",
        "Indesit",
        "Beko",
        "Ariston",
        "Hotpoint-Ariston",
        "Whirlpool",
        "Candy",
        "Haier",
        "Panasonic",
        "Sharp",
        "Daewoo",
        "Атлант",
        "Стинол",
        "Другое"
    )
    
    // Временные интервалы для прибытия
    val arrivalTimeSlots = listOf(
        "09:00 - 11:00",
        "11:00 - 13:00",
        "13:00 - 15:00",
        "15:00 - 17:00",
        "17:00 - 19:00",
        "19:00 - 21:00",
        "В любое время"
    )
}







