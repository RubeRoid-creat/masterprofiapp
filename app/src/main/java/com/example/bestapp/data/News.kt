package com.example.bestapp.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Модель новости
data class News(
    val id: Long = 0,
    val title: String, // Заголовок
    val summary: String, // Краткое описание
    val content: String, // Полный текст
    val category: NewsCategory = NewsCategory.TIPS, // Категория
    val imageUrl: String? = null, // URL изображения
    val publishedAt: Date = Date(),
    val source: String? = null // Источник
) {
    fun getFormattedDate(): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale("ru"))
        return formatter.format(publishedAt)
    }
}

// Категории новостей
enum class NewsCategory(val displayName: String) {
    TIPS("Советы"),
    INDUSTRY("Новости индустрии"),
    GUIDES("Руководства"),
    TOOLS("Инструменты"),
    TRENDS("Тренды")
}







