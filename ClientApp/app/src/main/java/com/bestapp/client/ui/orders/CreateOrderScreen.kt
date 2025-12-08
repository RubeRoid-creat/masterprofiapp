package com.bestapp.client.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bestapp.client.ui.map.MapAddressPicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    navController: NavController,
    viewModel: OrdersViewModel = viewModel()
) {
    // Основные поля
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(56.859611) }
    var longitude by remember { mutableStateOf(35.911896) }
    var deviceType by remember { mutableStateOf("") }
    var deviceBrand by remember { mutableStateOf("") }
    var problemDescription by remember { mutableStateOf("") }
    var desiredDate by remember { mutableStateOf("") }
    var desiredTime by remember { mutableStateOf("") }
    
    // UI состояния
    var showMapDialog by remember { mutableStateOf(false) }
    var deviceTypeExpanded by remember { mutableStateOf(false) }
    var deviceBrandExpanded by remember { mutableStateOf(false) }
    var isOtherBrand by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.createOrderUiState.collectAsState()
    
    // Date and Time pickers
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Списки для выбора
    val deviceTypes = listOf(
        "Стиральная машина",
        "Холодильник",
        "Посудомоечная машина",
        "Духовой шкаф",
        "Микроволновая печь",
        "Морозильный ларь",
        "Варочная панель",
        "Ноутбук",
        "Компьютер",
        "Кофемашина",
        "Кондиционер",
        "Водонагреватель"
    )
    
    val deviceBrands = listOf(
        "Samsung", "LG", "Bosch", "Indesit", "Ariston", 
        "Atlant", "Beko", "Candy", "Electrolux", "Gorenje",
        "Haier", "Hansa", "Hotpoint-Ariston", "Whirlpool",
        "Siemens", "Zanussi", "Другое"
    )
    
    // Launcher для разрешений
    val requestLocationPermission = rememberLocationPermissionLauncher(
        onPermissionGranted = {
            scope.launch {
                val location = com.bestapp.client.utils.LocationUtils.getCurrentLocation(context)
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    val addr = com.bestapp.client.utils.LocationUtils.getAddressFromCoordinates(
                        context,
                        location.latitude,
                        location.longitude
                    )
                    address = addr ?: "Адрес не определен"
                    snackbarHostState.showSnackbar("Местоположение определено")
                } else {
                    snackbarHostState.showSnackbar("Не удалось определить местоположение")
                }
            }
        },
        onPermissionDenied = {
            scope.launch {
                snackbarHostState.showSnackbar("Необходимо разрешение на геолокацию")
            }
        }
    )
    
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetCreateOrderState()
            navController.navigateUp()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создать заявку") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Оставьте заявку на ремонт",
                style = MaterialTheme.typography.headlineSmall
            )
            
            // Адрес
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "📍 Адрес",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = address,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Адрес *") },
                        placeholder = { Text("Выберите адрес на карте") },
                        trailingIcon = {
                            IconButton(onClick = { showMapDialog = true }) {
                                Icon(Icons.Default.Map, "Выбрать на карте")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showMapDialog = true }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { requestLocationPermission() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Моё местоположение")
                        }
                        
                        OutlinedButton(
                            onClick = { showMapDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("На карте")
                        }
                    }
                }
            }
            
            // Тип техники
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "📱 Тип техники",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = deviceTypeExpanded,
                        onExpandedChange = { deviceTypeExpanded = !deviceTypeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = deviceType,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Тип техники *") },
                            placeholder = { Text("Выберите тип техники") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
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
                }
            }
            
            // Бренд техники
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🏭 Бренд техники",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = deviceBrandExpanded,
                        onExpandedChange = { deviceBrandExpanded = !deviceBrandExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = deviceBrand,
                            onValueChange = { if (isOtherBrand) deviceBrand = it },
                            readOnly = !isOtherBrand,
                            label = { Text("Бренд *") },
                            placeholder = { Text("Выберите бренд") },
                            trailingIcon = { if (!isOtherBrand) Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        if (!isOtherBrand) {
                            ExposedDropdownMenu(
                                expanded = deviceBrandExpanded,
                                onDismissRequest = { deviceBrandExpanded = false }
                            ) {
                                deviceBrands.forEach { brand ->
                                    DropdownMenuItem(
                                        text = { Text(brand) },
                                        onClick = {
                                            if (brand == "Другое") {
                                                isOtherBrand = true
                                                deviceBrand = ""
                                            } else {
                                                deviceBrand = brand
                                            }
                                            deviceBrandExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Проблема
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🔧 Описание проблемы",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = problemDescription,
                        onValueChange = { problemDescription = it },
                        label = { Text("Опишите проблему *") },
                        placeholder = { Text("Например: не включается, не греет воду, шумит при отжиме...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Дата и время
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🕒 Дата и время",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = desiredDate,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Дата") },
                            placeholder = { Text("Выберите дату") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, "Выбрать дату")
                                }
                            },
                            modifier = Modifier.weight(1f).clickable { showDatePicker = true }
                        )
                        
                        OutlinedTextField(
                            value = desiredTime,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Время") },
                            placeholder = { Text("Выберите время") },
                            trailingIcon = {
                                IconButton(onClick = { showTimePicker = true }) {
                                    Icon(Icons.Default.AccessTime, "Выбрать время")
                                }
                            },
                            modifier = Modifier.weight(1f).clickable { showTimePicker = true }
                        )
                    }
                    
                    Text(
                        text = "Вы можете указать желаемую дату и время приезда мастера",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Ошибка
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Кнопка создания
            Button(
                onClick = {
                    viewModel.createOrder(
                        deviceType = deviceType,
                        deviceBrand = deviceBrand.ifBlank { null },
                        problemDescription = problemDescription,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        desiredRepairDate = if (desiredDate.isNotBlank()) 
                            "$desiredDate ${desiredTime.ifBlank { "00:00" }}" 
                        else null,
                        urgency = "planned"
                    )
                },
                enabled = !uiState.isLoading && 
                          address.isNotBlank() && 
                          deviceType.isNotBlank() && 
                          deviceBrand.isNotBlank() && 
                          problemDescription.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (uiState.isLoading) "Создание..." else "Создать заявку",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        calendar.timeInMillis = it
                        desiredDate = dateFormatter.format(calendar.time)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    desiredTime = timeFormatter.format(calendar.time)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Отмена")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
    
    // Map Dialog
    if (showMapDialog) {
        Dialog(onDismissRequest = { showMapDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
                shape = MaterialTheme.shapes.large
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Выберите адрес на карте", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { showMapDialog = false }) {
                            Icon(Icons.Default.Close, "Закрыть")
                        }
                    }
                    MapAddressPicker(
                        initialLatitude = latitude,
                        initialLongitude = longitude,
                        onLocationSelected = { location ->
                            latitude = location.latitude
                            longitude = location.longitude
                            address = location.address ?: "Адрес не определен"
                            showMapDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        showNearbyMasters = false
                    )
                }
            }
        }
    }
}
