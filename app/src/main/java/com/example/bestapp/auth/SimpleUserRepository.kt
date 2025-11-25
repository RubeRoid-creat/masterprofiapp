package com.example.bestapp.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.bestapp.database.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SimpleUserRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("users_db", Context.MODE_PRIVATE)
    private val authManager = AuthManager(context)
    
    suspend fun register(fullName: String, email: String, phone: String, passwordHash: String, specialization: String): Long = withContext(Dispatchers.IO) {
        val userId = System.currentTimeMillis()
        prefs.edit().apply {
            putString("user_${userId}_fullName", fullName)
            putString("user_${userId}_email", email)
            putString("user_${userId}_phone", phone)
            putString("user_${userId}_passwordHash", passwordHash)
            putString("user_${userId}_specialization", specialization)
            putString("email_to_id_$email", userId.toString())
            apply()
        }
        userId
    }
    
    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        val userIdStr = prefs.getString("email_to_id_$email", null) ?: return@withContext null
        val userId = userIdStr.toLongOrNull() ?: return@withContext null
        
        val fullName = prefs.getString("user_${userId}_fullName", null) ?: return@withContext null
        val phone = prefs.getString("user_${userId}_phone", "") ?: ""
        val passwordHash = prefs.getString("user_${userId}_passwordHash", "") ?: ""
        val specialization = prefs.getString("user_${userId}_specialization", "") ?: ""
        
        User(userId, fullName, email, phone, passwordHash, specialization)
    }
    
    suspend fun isEmailExists(email: String): Boolean = withContext(Dispatchers.IO) {
        prefs.contains("email_to_id_$email")
    }
}







