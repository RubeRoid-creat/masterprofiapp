package com.example.bestapp.data

import java.util.Date

// Модель клиента
data class Client(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val createdAt: Date = Date(),
    val notes: String? = null
) {
    fun getDisplayName(): String = name
    
    fun getFormattedPhone(): String {
        // Форматирование телефона +7 (XXX) XXX-XX-XX
        val digits = phone.filter { it.isDigit() }
        return if (digits.length == 11 && digits.startsWith("7")) {
            "+7 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9, 11)}"
        } else {
            phone
        }
    }
}








