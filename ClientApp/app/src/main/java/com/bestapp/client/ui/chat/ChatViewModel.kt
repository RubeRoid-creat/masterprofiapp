package com.bestapp.client.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.api.models.ChatMessageDto
import com.bestapp.client.data.local.PreferencesManager
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.websocket.WebSocketChatClient
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

data class ChatUiState(
    val messages: List<ChatMessageDto> = emptyList(),
    val messageText: String = "",
    val isLoading: Boolean = false,
    val currentUserId: Long? = null,
    val shouldRequestImagePermission: Boolean = false
)

class ChatViewModel(
    private val orderId: Long
) : ViewModel() {
    
    private val apiRepository = AppContainer.apiRepository
    private val prefsManager = AppContainer.preferencesManager
    private var webSocketClient: WebSocketChatClient? = null
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentUserId()
    }
    
    private fun loadCurrentUserId() {
        viewModelScope.launch {
            val userId = prefsManager.userId.first()
            _uiState.value = _uiState.value.copy(currentUserId = userId)
        }
    }
    
    fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = apiRepository.getChatMessages(orderId)
                when (result) {
                    is com.bestapp.client.data.repository.ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            messages = result.data,
                            isLoading = false
                        )
                        webSocketClient?.setMessages(result.data)
                    }
                    is com.bestapp.client.data.repository.ApiResult.Error -> {
                        Log.e("ChatViewModel", "Ошибка загрузки сообщений: ${result.message}")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Ошибка загрузки сообщений", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun connectWebSocket() {
        viewModelScope.launch {
            try {
                webSocketClient = WebSocketChatClient(prefsManager, AppContainer.WS_BASE_URL)
                
                // Подписываемся на обновления сообщений
                launch {
                    webSocketClient?.messages?.collect { messages ->
                        _uiState.value = _uiState.value.copy(messages = messages)
                    }
                }
                
                webSocketClient?.connect()
                webSocketClient?.joinOrderChat(orderId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Ошибка подключения WebSocket", e)
            }
        }
    }
    
    fun disconnectWebSocket() {
        webSocketClient?.disconnect()
        webSocketClient = null
    }
    
    fun updateMessageText(text: String) {
        _uiState.value = _uiState.value.copy(messageText = text)
    }
    
    fun sendMessage() {
        val messageText = _uiState.value.messageText.trim()
        if (messageText.isEmpty()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(messageText = "")
            
            // Пытаемся отправить через WebSocket
            if (webSocketClient?.isConnected?.value == true && 
                webSocketClient?.isAuthenticated?.value == true) {
                webSocketClient?.sendMessage(orderId, messageText)
            } else {
                // Fallback на REST API
                try {
                    val result = apiRepository.sendChatMessage(orderId, messageText)
                    when (result) {
                        is com.bestapp.client.data.repository.ApiResult.Success -> {
                            webSocketClient?.addMessage(result.data)
                            val currentList = _uiState.value.messages.toMutableList()
                            currentList.add(result.data)
                            _uiState.value = _uiState.value.copy(messages = currentList)
                        }
                        is com.bestapp.client.data.repository.ApiResult.Error -> {
                            Log.e("ChatViewModel", "Ошибка отправки сообщения: ${result.message}")
                            _uiState.value = _uiState.value.copy(messageText = messageText)
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(messageText = messageText)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Ошибка отправки сообщения", e)
                    _uiState.value = _uiState.value.copy(messageText = messageText)
                }
            }
        }
    }
    
    fun requestImagePermission() {
        _uiState.value = _uiState.value.copy(shouldRequestImagePermission = true)
    }
    
    fun imagePermissionRequested() {
        _uiState.value = _uiState.value.copy(shouldRequestImagePermission = false)
    }
    
    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Получаем контекст из AppContainer
                val context: Context = AppContainer.appContext
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File.createTempFile("chat_image_", ".jpg", context.cacheDir)
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                
                val result = apiRepository.sendChatImage(orderId, imagePart)
                when (result) {
                    is com.bestapp.client.data.repository.ApiResult.Success -> {
                        webSocketClient?.addMessage(result.data)
                        val currentList = _uiState.value.messages.toMutableList()
                        currentList.add(result.data)
                        _uiState.value = _uiState.value.copy(
                            messages = currentList,
                            isLoading = false
                        )
                    }
                    is com.bestapp.client.data.repository.ApiResult.Error -> {
                        Log.e("ChatViewModel", "Ошибка загрузки изображения: ${result.message}")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Ошибка загрузки изображения", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    companion object {
        fun provideFactory(orderId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(orderId) as T
                }
            }
        }
    }
}

