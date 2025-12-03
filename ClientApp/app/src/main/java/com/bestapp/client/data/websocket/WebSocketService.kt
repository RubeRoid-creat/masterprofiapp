package com.bestapp.client.data.websocket

import android.util.Log
import com.bestapp.client.data.local.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class WebSocketEvent {
    data class OrderStatusChanged(
        val orderId: Long,
        val status: String,
        val message: String,
        val masterName: String? = null
    ) : WebSocketEvent()
    
    data class Error(val message: String) : WebSocketEvent()
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
}

class WebSocketService(
    private val prefsManager: PreferencesManager,
    private val baseUrl: String = "ws://212.74.227.208:3000/ws" // Продакшн-сервер
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    private val _events = MutableStateFlow<WebSocketEvent?>(null)
    val events: StateFlow<WebSocketEvent?> = _events.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "Соединение установлено")
            _isConnected.value = true
            _events.value = WebSocketEvent.Connected
            
            // Отправляем токен для аутентификации
            runBlocking {
                authenticate()
            }
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "Получено сообщение: $text")
            handleMessage(text)
        }
        
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d("WebSocket", "Получено сообщение (bytes)")
            handleMessage(bytes.utf8())
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "Закрытие соединения: $code - $reason")
            _isConnected.value = false
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "Соединение закрыто: $code - $reason")
            _isConnected.value = false
            _events.value = WebSocketEvent.Disconnected
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocket", "Ошибка соединения", t)
            _isConnected.value = false
            _events.value = WebSocketEvent.Error(t.message ?: "Ошибка соединения")
        }
    }
    
    suspend fun connect() {
        if (webSocket != null && _isConnected.value) {
            Log.d("WebSocket", "Уже подключен")
            return
        }
        
        try {
            val token = prefsManager.authToken.first()
            if (token == null) {
                Log.e("WebSocket", "Токен не найден")
                _events.value = WebSocketEvent.Error("Токен авторизации не найден")
                return
            }
            
            val request = Request.Builder()
                .url(baseUrl)
                .build()
            
            webSocket = client.newWebSocket(request, listener)
            Log.d("WebSocket", "Попытка подключения к $baseUrl")
        } catch (e: Exception) {
            Log.e("WebSocket", "Ошибка подключения", e)
            _events.value = WebSocketEvent.Error(e.message ?: "Ошибка подключения")
        }
    }
    
    private suspend fun authenticate() {
        val token = prefsManager.authToken.first()
        if (token != null) {
            val authMessage = JSONObject().apply {
                put("type", "auth")
                put("token", token)
            }
            webSocket?.send(authMessage.toString())
            Log.d("WebSocket", "Отправлен токен для аутентификации")
        }
    }
    
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.getString("type")
            
            when (type) {
                "auth_success" -> {
                    Log.d("WebSocket", "Аутентификация успешна")
                }
                "auth_error" -> {
                    val message = json.optString("message", "Ошибка аутентификации")
                    _events.value = WebSocketEvent.Error(message)
                }
                "order_status_changed" -> {
                    val orderId = json.getLong("orderId")
                    val status = json.getString("status")
                    val message = json.optString("message", "Статус заказа изменен")
                    val masterName = json.optString("masterName", null)
                    
                    _events.value = WebSocketEvent.OrderStatusChanged(
                        orderId = orderId,
                        status = status,
                        message = message,
                        masterName = masterName
                    )
                }
                "pong" -> {
                    // Ответ на ping, ничего не делаем
                }
                else -> {
                    Log.d("WebSocket", "Неизвестный тип сообщения: $type")
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Ошибка обработки сообщения", e)
            _events.value = WebSocketEvent.Error("Ошибка обработки сообщения: ${e.message}")
        }
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Закрытие соединения")
        webSocket = null
        _isConnected.value = false
    }
    
    fun sendPing() {
        val ping = JSONObject().apply {
            put("type", "ping")
        }
        webSocket?.send(ping.toString())
    }
}

