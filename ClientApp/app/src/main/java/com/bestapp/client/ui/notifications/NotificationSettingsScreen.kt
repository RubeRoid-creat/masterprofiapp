package com.bestapp.client.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Экран настроек уведомлений
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: NotificationSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки уведомлений") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Общие уведомления
                SettingsSection(
                    title = "Общие уведомления",
                    description = "Управление push-уведомлениями приложения"
                ) {
                    NotificationSettingItem(
                        icon = Icons.Default.Notifications,
                        title = "Push-уведомления",
                        description = "Получать уведомления на устройство",
                        checked = uiState.pushEnabled,
                        onCheckedChange = { viewModel.updatePushEnabled(it) }
                    )
                    
                    if (uiState.pushEnabled) {
                        NotificationSettingItem(
                            icon = Icons.Default.Vibration,
                            title = "Вибрация",
                            description = "Вибрировать при получении уведомления",
                            checked = uiState.vibrationEnabled,
                            onCheckedChange = { viewModel.updateVibrationEnabled(it) }
                        )
                        
                        NotificationSettingItem(
                            icon = Icons.Default.VolumeUp,
                            title = "Звук",
                            description = "Воспроизводить звук уведомления",
                            checked = uiState.soundEnabled,
                            onCheckedChange = { viewModel.updateSoundEnabled(it) }
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Уведомления о заказах
                SettingsSection(
                    title = "Уведомления о заказах",
                    description = "Получайте важные обновления о ваших заказах"
                ) {
                    NotificationSettingItem(
                        icon = Icons.Default.Assignment,
                        title = "Назначение мастера",
                        description = "Когда мастер принял ваш заказ",
                        checked = uiState.masterAssignedEnabled,
                        onCheckedChange = { viewModel.updateMasterAssignedEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                    
                    NotificationSettingItem(
                        icon = Icons.Default.DirectionsCar,
                        title = "Мастер в пути",
                        description = "Когда мастер едет к вам",
                        checked = uiState.masterEnRouteEnabled,
                        onCheckedChange = { viewModel.updateMasterEnRouteEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                    
                    NotificationSettingItem(
                        icon = Icons.Default.Build,
                        title = "Начало работы",
                        description = "Когда мастер начал выполнять ремонт",
                        checked = uiState.workStartedEnabled,
                        onCheckedChange = { viewModel.updateWorkStartedEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                    
                    NotificationSettingItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Завершение заказа",
                        description = "Когда ремонт завершен",
                        checked = uiState.orderCompletedEnabled,
                        onCheckedChange = { viewModel.updateOrderCompletedEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                    
                    NotificationSettingItem(
                        icon = Icons.Default.Cancel,
                        title = "Отмена заказа",
                        description = "Когда заказ отменен",
                        checked = uiState.orderCancelledEnabled,
                        onCheckedChange = { viewModel.updateOrderCancelledEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Маркетинговые уведомления
                SettingsSection(
                    title = "Маркетинг и новости",
                    description = "Акции, скидки и специальные предложения"
                ) {
                    NotificationSettingItem(
                        icon = Icons.Default.LocalOffer,
                        title = "Акции и скидки",
                        description = "Получать информацию о специальных предложениях",
                        checked = uiState.promotionsEnabled,
                        onCheckedChange = { viewModel.updatePromotionsEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                    
                    NotificationSettingItem(
                        icon = Icons.Default.Newspaper,
                        title = "Новости",
                        description = "Получать новости о сервисе",
                        checked = uiState.newsEnabled,
                        onCheckedChange = { viewModel.updateNewsEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Напоминания
                SettingsSection(
                    title = "Напоминания",
                    description = "Автоматические напоминания о важных событиях"
                ) {
                    NotificationSettingItem(
                        icon = Icons.Default.Schedule,
                        title = "Техническое обслуживание",
                        description = "Напоминать о плановом ТО техники",
                        checked = uiState.maintenanceRemindersEnabled,
                        onCheckedChange = { viewModel.updateMaintenanceRemindersEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                    
                    NotificationSettingItem(
                        icon = Icons.Default.Star,
                        title = "Оставить отзыв",
                        description = "Напоминать оставить отзыв после ремонта",
                        checked = uiState.reviewRemindersEnabled,
                        onCheckedChange = { viewModel.updateReviewRemindersEnabled(it) },
                        enabled = uiState.pushEnabled
                    )
                }
                
                // Информация о разрешениях
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Разрешения системы",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Если уведомления не приходят, проверьте разрешения в настройках устройства",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        content()
    }
}

@Composable
fun NotificationSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

// ViewModel
class NotificationSettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()
    
    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Загрузка настроек с сервера
            kotlinx.coroutines.delay(500)
            
            _uiState.value = _uiState.value.copy(
                pushEnabled = true,
                vibrationEnabled = true,
                soundEnabled = true,
                masterAssignedEnabled = true,
                masterEnRouteEnabled = true,
                workStartedEnabled = true,
                orderCompletedEnabled = true,
                orderCancelledEnabled = true,
                promotionsEnabled = true,
                newsEnabled = false,
                maintenanceRemindersEnabled = true,
                reviewRemindersEnabled = true,
                isLoading = false
            )
        }
    }
    
    fun updatePushEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(pushEnabled = enabled)
        saveSettings()
    }
    
    fun updateVibrationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
        saveSettings()
    }
    
    fun updateSoundEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
        saveSettings()
    }
    
    fun updateMasterAssignedEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(masterAssignedEnabled = enabled)
        saveSettings()
    }
    
    fun updateMasterEnRouteEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(masterEnRouteEnabled = enabled)
        saveSettings()
    }
    
    fun updateWorkStartedEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(workStartedEnabled = enabled)
        saveSettings()
    }
    
    fun updateOrderCompletedEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(orderCompletedEnabled = enabled)
        saveSettings()
    }
    
    fun updateOrderCancelledEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(orderCancelledEnabled = enabled)
        saveSettings()
    }
    
    fun updatePromotionsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(promotionsEnabled = enabled)
        saveSettings()
    }
    
    fun updateNewsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(newsEnabled = enabled)
        saveSettings()
    }
    
    fun updateMaintenanceRemindersEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(maintenanceRemindersEnabled = enabled)
        saveSettings()
    }
    
    fun updateReviewRemindersEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(reviewRemindersEnabled = enabled)
        saveSettings()
    }
    
    private fun saveSettings() {
        viewModelScope.launch {
            // TODO: Сохранение настроек на сервере
        }
    }
}

data class NotificationSettingsUiState(
    val pushEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val masterAssignedEnabled: Boolean = true,
    val masterEnRouteEnabled: Boolean = true,
    val workStartedEnabled: Boolean = true,
    val orderCompletedEnabled: Boolean = true,
    val orderCancelledEnabled: Boolean = true,
    val promotionsEnabled: Boolean = true,
    val newsEnabled: Boolean = false,
    val maintenanceRemindersEnabled: Boolean = true,
    val reviewRemindersEnabled: Boolean = true,
    val isLoading: Boolean = false
)
