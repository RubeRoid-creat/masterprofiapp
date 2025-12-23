package com.bestapp.client.data.websocket

import android.util.Log
import com.bestapp.client.data.api.models.ChatMessageDto
import com.bestapp.client.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketChatClient(
    private val prefsManager: PreferencesManager,
    private val baseUrl: String = "ws://212.74.227.208:3000/ws" // Продакшн-сервер
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    private val _messages = MutableStateFlow<List<ChatMessageDto>>(emptyList())
    val messages: StateFlow<List<ChatMessageDto>> = _messages.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private var currentOrderId: Long? = null
    private var pendingJoinOrderId: Long? = null
    
    suspend fun connect() {
        // Если уже подключен и авторизован, ничего не делаем
        if (webSocket != null && _isConnected.value && _isAuthenticated.value) {
            Log.d("WebSocketChat", "Уже подключен и авторизован")
            return
        }
        
        // Если подключен, но не авторизован, пытаемся авторизоваться
        if (webSocket != null && _isConnected.value && !_isAuthenticated.value) {
            Log.d("WebSocketChat", "Подключен, но не авторизован. Повторная авторизация...")
            authenticate()
            return
        }
        
        val token = prefsManager.authToken.first()
        if (token == null) {
            Log.e("WebSocketChat", "Токен не найден")
            return
        }
        
        val request = Request.Builder()
            .url(baseUrl)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketChat", "Соединение установлено")
                _isConnected.value = true
                _isAuthenticated.value = false
                // Отправляем токен для аутентификации
                CoroutineScope(Dispatchers.IO).launch {
                    authenticate()
                }
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
                _isAuthenticated.value = false
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketChat", "Соединение закрыто: $code - $reason")
                _isConnected.value = false
                _isAuthenticated.value = false
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketChat", "Ошибка соединения", t)
                _isConnected.value = false
                _isAuthenticated.value = false
            }
        })
        Log.d("WebSocketChat", "Попытка подключения к $baseUrl")
    }
    
    private suspend fun authenticate() {
        val token = prefsManager.authToken.first()
        if (token != null) {
            val authMessage = JSONObject().apply {
                put("type", "auth")
                put("token", token)
            }
            webSocket?.send(authMessage.toString())
            Log.d("WebSocketChat", "Отправлен токен для аутентификации")
        }
    }
    
    suspend fun joinOrderChat(orderId: Long) {
        currentOrderId = orderId
        
        if (!_isConnected.value) {
            // Сохраняем orderId для присоединения после подключения
            pendingJoinOrderId = orderId
            connect()
            return
        }
        
        if (!_isAuthenticated.value) {
            // Сохраняем orderId для присоединения после авторизации
            pendingJoinOrderId = orderId
            Log.d("WebSocketChat", "Ожидание авторизации перед присоединением к чату заказа $orderId")
            return
        }
        
        // Отправляем запрос на присоединение к чату
        val joinMessage = JSONObject().apply {
            put("type", "join_order_chat")
            put("orderId", orderId)
        }
        webSocket?.send(joinMessage.toString())
        Log.d("WebSocketChat", "Присоединился к чату заказа $orderId")
        pendingJoinOrderId = null
    }
    
    fun sendMessage(orderId: Long, message: String) {
        if (!_isConnected.value || !_isAuthenticated.value) {
            Log.e("WebSocketChat", "Не подключен или не авторизован в WebSocket")
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
                    _isAuthenticated.value = true
                    // Присоединяемся к чату, если был отложенный запрос
                    pendingJoinOrderId?.let { orderId ->
                        CoroutineScope(Dispatchers.IO).launch {
                            joinOrderChat(orderId)
                        }
                    } ?: currentOrderId?.let { orderId ->
                        CoroutineScope(Dispatchers.IO).launch {
                            joinOrderChat(orderId)
                        }
                    }
                }
                "auth_error" -> {
                    val errorMsg = json.optString("message", "Ошибка аутентификации")
                    Log.e("WebSocketChat", "Ошибка аутентификации: $errorMsg")
                    _isAuthenticated.value = false
                    // Закрываем соединение при ошибке авторизации
                    webSocket?.close(1008, "Ошибка аутентификации")
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
    
    private fun parseChatMessage(json: JSONObject): ChatMessageDto? {
        return try {
            ChatMessageDto(
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
    
    fun setMessages(messages: List<ChatMessageDto>) {
        _messages.value = messages
    }
    
    fun addMessage(message: ChatMessageDto) {
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
        pendingJoinOrderId = null
        _isConnected.value = false
        _isAuthenticated.value = false
    }
}

