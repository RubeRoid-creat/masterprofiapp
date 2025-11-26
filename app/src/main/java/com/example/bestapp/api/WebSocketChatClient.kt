package com.example.bestapp.api

import android.util.Log
import com.example.bestapp.data.PreferencesManager
import com.example.bestapp.api.models.ApiChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WebSocketChatClient(
    private val prefsManager: PreferencesManager,
    private val baseUrl: String = "ws://212.74.227.208:3000/ws" // Продакшн‑сервер
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    private val _messages = MutableStateFlow<List<ApiChatMessage>>(emptyList())
    val messages: StateFlow<List<ApiChatMessage>> = _messages.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private var currentOrderId: Long? = null
    
    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocketChat", "Соединение установлено")
            _isConnected.value = true
            authenticate()
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocketChat", "Получено сообщение: $text")
            handleMessage(text)
        }
        
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            handleMessage(bytes.utf8())
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocketChat", "Закрытие соединения: $code - $reason")
            _isConnected.value = false
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocketChat", "Соединение закрыто: $code - $reason")
            _isConnected.value = false
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocketChat", "Ошибка соединения", t)
            _isConnected.value = false
        }
    }
    
    fun connect() {
        if (webSocket != null && _isConnected.value) {
            Log.d("WebSocketChat", "Уже подключен")
            return
        }
        
        val token = prefsManager.getAuthToken()
        if (token == null) {
            Log.e("WebSocketChat", "Токен не найден")
            return
        }
        
        val request = Request.Builder()
            .url(baseUrl)
            .build()
        
        webSocket = client.newWebSocket(request, listener)
        Log.d("WebSocketChat", "Попытка подключения к $baseUrl")
    }
    
    private fun authenticate() {
        val token = prefsManager.getAuthToken()
        if (token != null) {
            val authMessage = JSONObject().apply {
                put("type", "auth")
                put("token", token)
            }
            webSocket?.send(authMessage.toString())
            Log.d("WebSocketChat", "Отправлен токен для аутентификации")
        }
    }
    
    fun joinOrderChat(orderId: Long) {
        currentOrderId = orderId
        if (!_isConnected.value) {
            connect()
            // Ждем подключения перед отправкой join
            return
        }
        
        val joinMessage = JSONObject().apply {
            put("type", "join_order_chat")
            put("orderId", orderId)
        }
        webSocket?.send(joinMessage.toString())
        Log.d("WebSocketChat", "Присоединился к чату заказа $orderId")
    }
    
    fun sendMessage(orderId: Long, message: String) {
        if (!_isConnected.value) {
            Log.e("WebSocketChat", "Не подключен к WebSocket")
            return
        }
        
        val chatMessage = JSONObject().apply {
            put("type", "chat_message")
            put("orderId", orderId)
            put("message", message)
            put("messageType", "text")
        }
        webSocket?.send(chatMessage.toString())
        Log.d("WebSocketChat", "Отправлено сообщение в чат заказа $orderId")
    }
    
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            
            when (type) {
                "auth_success" -> {
                    Log.d("WebSocketChat", "Аутентификация успешна")
                    // Автоматически присоединяемся к чату, если был выбран заказ
                    currentOrderId?.let { joinOrderChat(it) }
                }
                "joined_order_chat" -> {
                    Log.d("WebSocketChat", "Присоединился к чату заказа")
                }
                "chat_message" -> {
                    val message = parseChatMessage(json)
                    if (message != null) {
                        val currentList = _messages.value.toMutableList()
                        currentList.add(message)
                        _messages.value = currentList
                    }
                }
                "error" -> {
                    val errorMsg = json.optString("message", "Неизвестная ошибка")
                    Log.e("WebSocketChat", "Ошибка: $errorMsg")
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocketChat", "Ошибка обработки сообщения", e)
        }
    }
    
    private fun parseChatMessage(json: JSONObject): ApiChatMessage? {
        return try {
            ApiChatMessage(
                id = json.getLong("messageId"),
                orderId = json.getLong("orderId"),
                senderId = json.getLong("senderId"),
                senderName = json.getString("senderName"),
                senderRole = json.optString("senderRole", "user"),
                messageType = json.getString("messageType"),
                messageText = json.optString("message"),
                imageUrl = json.optString("imageUrl"),
                imageThumbnailUrl = json.optString("imageThumbnailUrl"),
                readAt = null,
                createdAt = json.getString("createdAt")
            )
        } catch (e: Exception) {
            Log.e("WebSocketChat", "Ошибка парсинга сообщения", e)
            null
        }
    }
    
    fun setMessages(messages: List<ApiChatMessage>) {
        _messages.value = messages
    }
    
    fun addMessage(message: ApiChatMessage) {
        val currentList = _messages.value.toMutableList()
        currentList.add(message)
        _messages.value = currentList
    }
    
    fun disconnect() {
        currentOrderId?.let { orderId ->
            val leaveMessage = JSONObject().apply {
                put("type", "leave_order_chat")
                put("orderId", orderId)
            }
            webSocket?.send(leaveMessage.toString())
        }
        webSocket?.close(1000, "Закрытие соединения")
        webSocket = null
        currentOrderId = null
        _isConnected.value = false
    }
}



