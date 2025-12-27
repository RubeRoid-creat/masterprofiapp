package com.bestapp.client.ui.chat

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.bestapp.client.data.api.models.ChatMessageDto
import com.bestapp.client.data.local.PreferencesManager
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.websocket.WebSocketChatClient
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    orderId: Long,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.provideFactory(orderId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Загрузка сообщений при открытии экрана
    LaunchedEffect(orderId) {
        viewModel.loadMessages()
        viewModel.connectWebSocket()
    }
    
    // Прокрутка вниз при новых сообщениях
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чат") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Список сообщений
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    ChatMessageItem(
                        message = message,
                        isMyMessage = message.senderId == uiState.currentUserId
                    )
                }
            }
            
            // Поле ввода
            ChatInputBar(
                messageText = uiState.messageText,
                onMessageTextChange = { viewModel.updateMessageText(it) },
                onSendClick = { viewModel.sendMessage() },
                onAttachImageClick = { viewModel.requestImagePermission() }
            )
        }
        
        // Индикатор загрузки
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Обработка выбора изображения
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it) }
    }
    
    // Обработка разрешения на чтение изображений
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        }
    }
    
    LaunchedEffect(uiState.shouldRequestImagePermission) {
        if (uiState.shouldRequestImagePermission) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            viewModel.imagePermissionRequested()
        }
    }
    
    // Очистка при закрытии экрана
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectWebSocket()
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageDto,
    isMyMessage: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp),
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
        ) {
            if (!isMyMessage) {
                Text(
                    text = message.senderName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMyMessage) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    when (message.messageType) {
                        "text" -> {
                            Text(
                                text = message.messageText ?: "",
                                fontSize = 14.sp,
                                color = if (isMyMessage)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "image" -> {
                            message.imageUrl?.let { imageUrl ->
                                val fullUrl = if (imageUrl.startsWith("http")) {
                                    imageUrl
                                } else {
                                    "${AppContainer.BASE_URL.replace("/api", "")}$imageUrl"
                                }
                                Image(
                                    painter = rememberAsyncImagePainter(fullUrl),
                                    contentDescription = "Изображение",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatTime(message.createdAt),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Индикатор статуса доставки (только для своих сообщений)
                        if (isMyMessage) {
                            val statusIcon = if (message.readAt != null) {
                                Icons.Default.DoneAll
                            } else {
                                Icons.Default.Done
                            }
                            val statusColor = if (message.readAt != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = if (message.readAt != null) "Прочитано" else "Отправлено",
                                modifier = Modifier.size(12.dp),
                                tint = statusColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachImageClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onAttachImageClick) {
                Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить фото")
            }
            
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Введите сообщение...") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp)
            )
            
            IconButton(
                onClick = onSendClick,
                enabled = messageText.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Отправить",
                    tint = if (messageText.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}

private fun formatTime(createdAt: String): String {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = dateFormat.parse(createdAt)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        date?.let { timeFormat.format(it) } ?: ""
    } catch (e: Exception) {
        ""
    }
}

