package com.bestapp.client.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bestapp.client.MainActivity
import com.bestapp.client.data.api.models.FcmTokenRequest
import com.bestapp.client.data.local.PreferencesManager
import com.bestapp.client.di.AppContainer
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Сервис для обработки входящих push-уведомлений от Firebase Cloud Messaging
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    private val TAG = "FCMService"
    private val CHANNEL_ID = "bestapp_notifications"
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    /**
     * Создание канала уведомлений (требуется для Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Уведомления BestApp",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о статусе заказов"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Обработка нового FCM токена
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "🔄 Новый FCM токен получен: ${token.take(30)}...")
        
        // Пытаемся зарегистрировать токен сразу, если пользователь авторизован
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefsManager = PreferencesManager(applicationContext)
                val authToken = prefsManager.authToken.first()
                
                if (!authToken.isNullOrEmpty()) {
                    Log.d(TAG, "✅ Пользователь авторизован, регистрируем токен...")
                    
                    val deviceId = Settings.Secure.getString(
                        applicationContext.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ) ?: "unknown"
                    
                    val request = FcmTokenRequest(
                        token = token,
                        deviceType = "android",
                        deviceId = deviceId
                    )
                    
                    val response = AppContainer.apiService.registerFcmToken(request)
                    if (response.isSuccessful) {
                        Log.d(TAG, "✅ FCM токен зарегистрирован из onNewToken")
                    } else {
                        Log.e(TAG, "❌ Ошибка регистрации токена из onNewToken: ${response.code()}, ${response.message()}")
                    }
                } else {
                    Log.d(TAG, "ℹ️ Пользователь не авторизован, токен будет зарегистрирован при входе")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка регистрации токена из onNewToken", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Обработка входящего сообщения
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Получено сообщение от: ${remoteMessage.from}")
        
        // Проверяем наличие данных
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Данные сообщения: ${remoteMessage.data}")
            
            val type = remoteMessage.data["type"]
            val orderId = remoteMessage.data["orderId"]
            val status = remoteMessage.data["status"]
            val masterName = remoteMessage.data["masterName"] ?: ""
            
            // Показываем уведомление
            remoteMessage.notification?.let {
                showNotification(
                    title = it.title ?: "BestApp",
                    body = it.body ?: "",
                    orderId = orderId?.toLongOrNull(),
                    type = type
                )
            } ?: run {
                // Если нет notification payload, создаем уведомление из data
                val title = when (status) {
                    "completed" -> "Заказ завершен"
                    "in_progress" -> "Мастер принял заказ"
                    "cancelled" -> "Заказ отменен"
                    else -> "Статус заказа изменен"
                }
                
                val body = remoteMessage.data["message"] ?: "Ваш заказ обновлен"
                
                showNotification(title, body, orderId?.toLongOrNull(), type)
            }
        }
        
        // Также обрабатываем notification payload (если есть)
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification Title: ${it.title}")
            Log.d(TAG, "Notification Body: ${it.body}")
        }
    }
    
    /**
     * Показать уведомление
     */
    private fun showNotification(
        title: String,
        body: String,
        orderId: Long?,
        type: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (orderId != null) {
                putExtra("orderId", orderId)
                putExtra("openOrderDetails", true)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Временная иконка
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(orderId?.toInt() ?: System.currentTimeMillis().toInt(), notification)
    }
}

