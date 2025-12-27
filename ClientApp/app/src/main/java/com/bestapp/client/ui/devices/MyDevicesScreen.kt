package com.bestapp.client.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.api.models.ClientDeviceDto
import com.bestapp.client.di.AppContainer
import com.bestapp.client.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран "Моя техника" - управление техникой клиента
 * - История ремонтов каждого устройства
 * - Напоминания о техническом обслуживании
 * - Быстрый заказ ремонта для сохраненной техники
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_API_USAGE")
@Composable
fun MyDevicesScreen(
    navController: NavController,
    viewModel: MyDevicesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Моя техника") },
                actions = {
                    IconButton(onClick = { showAddDeviceDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDeviceDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Добавить технику") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.loadDevices() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.devices.isEmpty() -> {
                    EmptyDevicesState(
                        onAddDevice = { showAddDeviceDialog = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Статистика
                        item {
                            DevicesStatisticsCard(
                                totalDevices = uiState.devices.size,
                                needsMaintenance = uiState.devices.count { it.needsMaintenance }
                            )
                        }
                        
                        // Уведомления о ТО
                        val devicesNeedingMaintenance = uiState.devices.filter { it.needsMaintenance }
                        if (devicesNeedingMaintenance.isNotEmpty()) {
                            item {
                                MaintenanceReminderCard(
                                    devices = devicesNeedingMaintenance,
                                    onScheduleMaintenance = { device ->
                                        navController.navigate(Screen.CreateOrder.route)
                                    }
                                )
                            }
                        }
                        
                        // Список техники
                        items(uiState.devices) { device ->
                            DeviceCard(
                                device = device,
                                onClick = {
                                    navController.navigate("device_details/${device.id}")
                                },
                                onOrderRepair = {
                                    navController.navigate(Screen.CreateOrder.route)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Диалог добавления техники
    if (showAddDeviceDialog) {
        AddDeviceDialog(
            onDismiss = { showAddDeviceDialog = false },
            onConfirm = { deviceType, brand, model ->
                viewModel.addDevice(deviceType, brand, model)
                showAddDeviceDialog = false
            }
        )
    }
}

@Composable
fun DevicesStatisticsCard(
    totalDevices: Int,
    needsMaintenance: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Devices,
                label = "Всего техники",
                value = totalDevices.toString()
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            StatItem(
                icon = Icons.Default.Notifications,
                label = "Требует ТО",
                value = needsMaintenance.toString(),
                isAlert = needsMaintenance > 0
            )
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isAlert: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MaintenanceReminderCard(
    devices: List<ClientDevice>,
    onScheduleMaintenance: (ClientDevice) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.NotificationImportant,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Напоминание о ТО",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            devices.forEach { device ->
                Text(
                    text = "• ${device.name} (${device.brand})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { if (devices.isNotEmpty()) onScheduleMaintenance(devices.first()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Запланировать ТО")
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: ClientDevice,
    onClick: () -> Unit,
    onOrderRepair: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка
                Icon(
                    Icons.Default.Devices,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (device.model.isNullOrBlank()) device.brand else "${device.brand} ${device.model}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (device.lastServiceDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Последний ремонт: ${formatDate(device.lastServiceDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (device.needsMaintenance) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Требуется ТО",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Статистика
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DeviceInfoChip(
                    icon = Icons.Default.Build,
                    text = "${device.repairCount} ремонтов"
                )
                
                if (device.warrantyUntil != null && device.warrantyUntil.after(Date())) {
                    DeviceInfoChip(
                        icon = Icons.Default.VerifiedUser,
                        text = "На гарантии"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Подробнее")
                }
                
                Button(
                    onClick = onOrderRepair,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Build, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Заказать ремонт")
                }
            }
        }
    }
}

@Composable
fun DeviceInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun EmptyDevicesState(
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Devices,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Пока нет сохраненной техники",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Добавьте вашу технику для отслеживания истории ремонтов",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAddDevice,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить технику")
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (deviceType: String, brand: String, model: String?) -> Unit
) {
    var deviceType by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var deviceTypeExpanded by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }
    
    val deviceTypes = listOf(
        "Холодильник", "Стиральная машина", "Посудомоечная машина",
        "Духовой шкаф", "Варочная панель", "Микроволновая печь",
        "Кондиционер", "Кофемашина", "Телевизор"
    )
    
    val brands = listOf(
        "Samsung", "LG", "Bosch", "Indesit", "Ariston",
        "Atlant", "Beko", "Electrolux", "Другой"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить технику") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Тип техники
                ExposedDropdownMenuBox(
                    expanded = deviceTypeExpanded,
                    onExpandedChange = { deviceTypeExpanded = !deviceTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = deviceType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип техники *") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = deviceTypeExpanded,
                        onDismissRequest = { deviceTypeExpanded = false }
                    ) {
                        deviceTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    deviceType = type
                                    deviceTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Бренд
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded }
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Бренд *") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false }
                    ) {
                        brands.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b) },
                                onClick = {
                                    brand = b
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Модель (опционально)
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Модель (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (deviceType.isNotBlank() && brand.isNotBlank()) {
                        onConfirm(deviceType, brand, model.ifBlank { null })
                    }
                },
                enabled = deviceType.isNotBlank() && brand.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun formatDate(date: Date): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(date)
}

// Data classes
data class ClientDevice(
    val id: Long,
    val name: String,
    val brand: String,
    val model: String?,
    val icon: String,
    val repairCount: Int,
    val lastServiceDate: Date?,
    val warrantyUntil: Date?,
    val needsMaintenance: Boolean
) {
    // Полное название устройства
    val fullName: String
        get() = if (model.isNullOrBlank()) {
            "$name $brand".trim()
        } else {
            "$name $brand $model".trim()
        }
}

// ViewModel
class MyDevicesViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyDevicesUiState())
    val uiState: StateFlow<MyDevicesUiState> = _uiState
    
    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.getClientDevices()) {
                is com.bestapp.client.data.repository.ApiResult.Success -> {
                    val devices = result.data.map { dto ->
                        ClientDevice(
                            id = dto.lastOrderId, // Используем ID последнего заказа как идентификатор
                            name = dto.deviceType,
                            brand = dto.deviceBrand ?: "",
                            model = dto.deviceModel,
                            icon = getDeviceIcon(dto.deviceType),
                            repairCount = dto.orderCount,
                            lastServiceDate = try {
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dto.lastOrderDate)
                            } catch (e: Exception) {
                                null
                            },
                            warrantyUntil = null, // Не приходит с API
                            needsMaintenance = isMaintenanceNeeded(dto.lastOrderDate, dto.orderCount)
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        devices = devices,
                        isLoading = false
                    )
                }
                is com.bestapp.client.data.repository.ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
    
    fun addDevice(deviceType: String, brand: String, model: String?) {
        viewModelScope.launch {
            // Устройства автоматически добавляются при создании заказа
            // Поэтому просто перезагружаем список
            loadDevices()
        }
    }
    
    private fun getDeviceIcon(deviceType: String): String {
        return when {
            deviceType.contains("холодильник", ignoreCase = true) -> "icons8-fridge"
            deviceType.contains("стирал", ignoreCase = true) -> "icons8-washing-machine"
            deviceType.contains("посудомо", ignoreCase = true) -> "icons8-dishwasher"
            deviceType.contains("микроволнов", ignoreCase = true) -> "icons8-microwave"
            deviceType.contains("духов", ignoreCase = true) -> "icons8-oven"
            else -> "icons8-devices"
        }
    }
    
    private fun isMaintenanceNeeded(lastOrderDate: String, orderCount: Int): Boolean {
        return try {
            val lastDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(lastOrderDate)
            val now = Date()
            val daysSinceLastService = ((now.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
            // Если последний ремонт был более 6 месяцев назад и было более 1 ремонта
            daysSinceLastService > 180 && orderCount > 1
        } catch (e: Exception) {
            false
        }
    }
}

data class MyDevicesUiState(
    val devices: List<ClientDevice> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
