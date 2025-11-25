package com.bestapp.client.services

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.bestapp.client.data.api.ApiService
import com.bestapp.client.data.api.models.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Сервис для работы с Firebase Cloud Messaging
 */
class FcmService(
    private val apiService: ApiService,
    private val context: Context? = null
) {
    private val TAG = "FcmService"
    
    /**
     * Получить FCM токен и зарегистрировать его на сервере
     */
    fun registerToken(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начинаем получение FCM токена...")
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM токен получен: ${token.take(20)}...")
                
                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "FCM токен пустой!")
                    return@launch
                }
                
                // Регистрируем токен на сервере
                val deviceId = context?.let {
                    Settings.Secure.getString(it.contentResolver, Settings.Secure.ANDROID_ID)
                } ?: "unknown"
                
                Log.d(TAG, "Отправляем токен на сервер...")
                val request = FcmTokenRequest(
                    token = token,
                    deviceType = "android",
                    deviceId = deviceId
                )
                
                val response = apiService.registerFcmToken(request)
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ FCM токен успешно зарегистрирован на сервере")
                } else {
                    Log.e(TAG, "❌ Ошибка регистрации FCM токена: ${response.code()}, ${response.message()}")
                    try {
                        response.errorBody()?.let { errorBody ->
                            Log.e(TAG, "Тело ошибки: ${errorBody.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Не удалось прочитать тело ошибки", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка получения/регистрации FCM токена", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Удалить FCM токен с сервера
     */
    fun unregisterToken(scope: CoroutineScope, token: String? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()
                
                val request = FcmTokenRequest(
                    token = fcmToken,
                    deviceType = "android"
                )
                
                val response = apiService.unregisterFcmToken(request)
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM токен успешно удален с сервера")
                } else {
                    Log.e(TAG, "Ошибка удаления FCM токена: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления FCM токена", e)
            }
        }
    }
}

