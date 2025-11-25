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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bestapp.client.R
import com.bestapp.client.data.api.models.ServiceCategoryDto
import com.bestapp.client.data.api.models.ServiceTemplateDto
import com.bestapp.client.ui.map.MapAddressPicker
import com.bestapp.client.ui.orders.MediaItem
import com.bestapp.client.ui.orders.MediaType
import com.bestapp.client.ui.orders.MediaUploader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.bestapp.client.utils.MediaUtils
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody
import com.bestapp.client.ui.orders.rememberLocationPermissionLauncher
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.bestapp.client.data.api.models.MasterDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    navController: NavController,
    viewModel: OrdersViewModel = viewModel(),
    servicesViewModel: ServicesViewModel = viewModel()
) {
    // Выбранная категория и шаблон
    var selectedCategory by remember { mutableStateOf<ServiceCategoryDto?>(null) }
    var selectedTemplate by remember { mutableStateOf<ServiceTemplateDto?>(null) }
    
    val servicesUiState by servicesViewModel.uiState.collectAsState()
    
    // Информация о технике
    var deviceType by remember { mutableStateOf("") }
    var deviceCategory by remember { mutableStateOf("") }
    var deviceBrand by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var deviceSerialNumber by remember { mutableStateOf("") }
    var deviceYear by remember { mutableStateOf("") }
    var warrantyStatus by remember { mutableStateOf("") }
    
    // Описание проблемы
    var problemShortDescription by remember { mutableStateOf("") }
    var problemDescription by remember { mutableStateOf("") }
    var problemWhenStarted by remember { mutableStateOf("") }
    var problemConditions by remember { mutableStateOf("") }
    var problemErrorCodes by remember { mutableStateOf("") }
    var problemAttemptedFixes by remember { mutableStateOf("") }
    
    // Адрес
    var address by remember { mutableStateOf("") }
    var addressStreet by remember { mutableStateOf("") }
    var addressBuilding by remember { mutableStateOf("") }
    var addressApartment by remember { mutableStateOf("") }
    var addressFloor by remember { mutableStateOf("") }
    var addressEntranceCode by remember { mutableStateOf("") }
    var addressLandmark by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(56.859611) }
    var longitude by remember { mutableStateOf(35.911896) }
    
    // Ближайшие мастера
    var nearbyMasters by remember { mutableStateOf<List<com.bestapp.client.data.api.models.MasterDto>>(emptyList()) }
    var isLoadingMasters by remember { mutableStateOf(false) }
    
    // Временные параметры
    var arrivalTime by remember { mutableStateOf("") }
    var desiredRepairDate by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("planned") }
    
    // Приоритет
    var isUrgent by remember { mutableStateOf(false) }
    
    // Финансовые параметры
    var clientBudget by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("") }
    
    // Дополнительная информация
    var intercomWorking by remember { mutableStateOf(true) }
    var parkingAvailable by remember { mutableStateOf(true) }
    var hasPets by remember { mutableStateOf(false) }
    var hasSmallChildren by remember { mutableStateOf(false) }
    var preferredContactMethod by remember { mutableStateOf("call") }
    
    var showMapDialog by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf("main") } // main, device, problem, address, additional
    
    // Медиафайлы
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Launcher для разрешений на геолокацию
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
                    
                    // Загружаем ближайших мастеров
                    isLoadingMasters = true
                    try {
                        val response = AppContainer.apiService.getMasters(
                            status = "available",
                            isOnShift = true,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            radius = 20000.0,
                            limit = 10
                        )
                        if (response.isSuccessful && response.body() != null) {
                            nearbyMasters = response.body()!!
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoadingMasters = false
                    }
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
    
    // Launchers для камеры и галереи
    // Используем GetContent для более широкой совместимости
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                mediaItems = mediaItems + MediaItem(
                    uri = uri,
                    type = MediaType.PHOTO,
                    name = uri.toString()
                )
            }
        }
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            mediaItems = mediaItems + MediaItem(
                uri = it,
                type = MediaType.VIDEO,
                name = it.toString()
            )
        }
    }
    
    // Состояния для выпадающих списков
    var deviceTypeExpanded by remember { mutableStateOf(false) }
    var deviceCategoryExpanded by remember { mutableStateOf(false) }
    var deviceBrandExpanded by remember { mutableStateOf(false) }
    var arrivalTimeExpanded by remember { mutableStateOf(false) }
    var urgencyExpanded by remember { mutableStateOf(false) }
    var warrantyStatusExpanded by remember { mutableStateOf(false) }
    var contactMethodExpanded by remember { mutableStateOf(false) }
    var isOtherBrand by remember { mutableStateOf(false) }
    
    val uiState by viewModel.createOrderUiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && mediaItems.isNotEmpty()) {
            // Загружаем медиафайлы после создания заказа
            val orderId = uiState.orderId ?: return@LaunchedEffect
            isUploadingMedia = true
            
            scope.launch {
                try {
                    val parts = mediaItems.mapNotNull { item ->
                        MediaUtils.createMultipartPart(context, item.uri)
                    }
                    
                    if (parts.isNotEmpty()) {
                        val description = problemShortDescription.ifBlank { null }
                        val descriptionBody = description?.toRequestBody(null) ?: "".toRequestBody(null)
                        
                        val response = AppContainer.mediaApiService.uploadMedia(
                            orderId = orderId,
                            files = parts,
                            description = if (description != null) descriptionBody else null
                        )
                        
                        if (response.isSuccessful) {
                            // Медиа загружены успешно
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isUploadingMedia = false
                    viewModel.resetCreateOrderState()
                    navController.navigateUp()
                }
            }
        } else if (uiState.isSuccess) {
            viewModel.resetCreateOrderState()
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_order)) },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            Text(
                text = "Создание заявки на ремонт",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            
            // Выбор категории и шаблона услуги
            CategorySelector(
                categories = servicesUiState.categories,
                templates = servicesUiState.templates,
                onCategorySelected = { category ->
                    selectedCategory = category
                    selectedTemplate = null
                    // Автозаполнение типа техники из категории
                    category?.name?.takeIf { it.isNotBlank() }?.let {
                        deviceType = it
                    }
                    // Загружаем шаблоны для выбранной категории
                    if (category != null) {
                        servicesViewModel.loadTemplates(category.id)
                    }
                },
                onTemplateSelected = { template ->
                    selectedTemplate = template
                    // Автозаполнение полей из шаблона
                    template?.let { t ->
                        t.deviceType?.let { deviceType = it }
                        t.name.takeIf { it.isNotBlank() }?.let {
                            problemShortDescription = it
                        }
                        t.description?.let {
                            if (problemDescription.isBlank()) {
                                problemDescription = it
                            }
                        }
                        t.fixedPrice?.let {
                            if (clientBudget.isBlank()) {
                                clientBudget = it.toInt().toString()
                            }
                        }
                    }
                },
                selectedCategory = selectedCategory,
                selectedTemplate = selectedTemplate,
                isLoading = servicesUiState.isLoading
            )
            
            // Загрузка фото/видео
            MediaUploader(
                mediaItems = mediaItems,
                onAddPhoto = {
                    // GetMultipleContents принимает MIME тип
                    photoPickerLauncher.launch("image/*")
                },
                onAddVideo = {
                    // GetContent принимает MIME тип
                    videoPickerLauncher.launch("video/*")
                },
                onRemove = { index ->
                    mediaItems = mediaItems.toMutableList().apply { removeAt(index) }
                },
                isLoading = isUploadingMedia,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // ОСНОВНАЯ ИНФОРМАЦИЯ О ТЕХНИКЕ
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { expandedSection = if (expandedSection == "device") "" else "device" }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📱 Информация о технике",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                    
                    if (expandedSection == "device") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Тип техники
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
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = deviceTypeExpanded, onDismissRequest = { deviceTypeExpanded = false }) {
                                OrderFormData.deviceTypes.forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = {
                                        deviceType = type
                                        deviceTypeExpanded = false
                                    })
                                }
                            }
                        }
                        
                        // Категория техники
                        ExposedDropdownMenuBox(
                            expanded = deviceCategoryExpanded,
                            onExpandedChange = { deviceCategoryExpanded = !deviceCategoryExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = deviceCategory,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Категория") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = deviceCategoryExpanded, onDismissRequest = { deviceCategoryExpanded = false }) {
                                listOf("Крупная", "Мелкая", "Встраиваемая").forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat) }, onClick = {
                                        deviceCategory = when(cat) {
                                            "Крупная" -> "large"
                                            "Мелкая" -> "small"
                                            "Встраиваемая" -> "builtin"
                                            else -> ""
                                        }
                                        deviceCategoryExpanded = false
                                    })
                                }
                            }
                        }
                        
                        // Бренд
                        ExposedDropdownMenuBox(
                            expanded = deviceBrandExpanded,
                            onExpandedChange = { deviceBrandExpanded = !deviceBrandExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = deviceBrand,
                                onValueChange = { if (!isOtherBrand) deviceBrand = it },
                                readOnly = !isOtherBrand,
                                label = { Text("Бренд") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = deviceBrandExpanded, onDismissRequest = { deviceBrandExpanded = false }) {
                                OrderFormData.deviceBrands.forEach { brand ->
                                    DropdownMenuItem(text = { Text(brand) }, onClick = {
                                        if (brand == "Другое") {
                                            isOtherBrand = true
                                            deviceBrand = ""
                                        } else {
                                            isOtherBrand = false
                                            deviceBrand = brand
                                        }
                                        deviceBrandExpanded = false
                                    })
                                }
                            }
                        }
                        
                        // Модель
                        OutlinedTextField(
                            value = deviceModel,
                            onValueChange = { deviceModel = it },
                            label = { Text("Модель") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Серийный номер
                        OutlinedTextField(
                            value = deviceSerialNumber,
                            onValueChange = { deviceSerialNumber = it },
                            label = { Text("Серийный номер") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Год выпуска/покупки
                        OutlinedTextField(
                            value = deviceYear,
                            onValueChange = { deviceYear = it },
                            label = { Text("Год выпуска/покупки") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Гарантийный статус
                        ExposedDropdownMenuBox(
                            expanded = warrantyStatusExpanded,
                            onExpandedChange = { warrantyStatusExpanded = !warrantyStatusExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = when(warrantyStatus) {
                                    "warranty" -> "На гарантии"
                                    "post_warranty" -> "Постгарантийный"
                                    else -> ""
                                },
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Гарантийный статус") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = warrantyStatusExpanded, onDismissRequest = { warrantyStatusExpanded = false }) {
                                DropdownMenuItem(text = { Text("На гарантии") }, onClick = {
                                    warrantyStatus = "warranty"
                                    warrantyStatusExpanded = false
                                })
                                DropdownMenuItem(text = { Text("Постгарантийный") }, onClick = {
                                    warrantyStatus = "post_warranty"
                                    warrantyStatusExpanded = false
                                })
                            }
                        }
                    }
                }
            }

            // ОПИСАНИЕ ПРОБЛЕМЫ
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { expandedSection = if (expandedSection == "problem") "" else "problem" }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚨 Описание проблемы",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    
                    if (expandedSection == "problem") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Краткое описание
                        OutlinedTextField(
                            value = problemShortDescription,
                            onValueChange = { problemShortDescription = it },
                            label = { Text("Краткое описание (1-2 предложения)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Подробное описание
                        OutlinedTextField(
                            value = problemDescription,
                            onValueChange = { problemDescription = it },
                            label = { Text("Подробное описание проблемы *") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Когда началась
                        OutlinedTextField(
                            value = problemWhenStarted,
                            onValueChange = { problemWhenStarted = it },
                            label = { Text("Когда началась проблема") },
                            placeholder = { Text("Например: Вчера вечером") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // При каких условиях
                        OutlinedTextField(
                            value = problemConditions,
                            onValueChange = { problemConditions = it },
                            label = { Text("При каких условиях проявляется") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Коды ошибок
                        OutlinedTextField(
                            value = problemErrorCodes,
                            onValueChange = { problemErrorCodes = it },
                            label = { Text("Коды ошибок на дисплее") },
                            placeholder = { Text("Например: E2, F5") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Что уже пробовали
                        OutlinedTextField(
                            value = problemAttemptedFixes,
                            onValueChange = { problemAttemptedFixes = it },
                            label = { Text("Что уже пробовали сделать") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // АДРЕС
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { expandedSection = if (expandedSection == "address") "" else "address" }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 Адрес",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    
                    if (expandedSection == "address") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Адрес на карте
                        OutlinedTextField(
                            value = address,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Адрес *") },
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
                            Button(
                                onClick = {
                                    requestLocationPermission()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocationOn, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Мое местоположение")
                            }
                            
                            OutlinedButton(
                                onClick = { showMapDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Map, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("На карте")
                            }
                        }
                        
                        // Показываем информацию о ближайших мастерах
                        if (nearbyMasters.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "📍 Ближайшие мастера (${nearbyMasters.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    nearbyMasters.take(3).forEach { master ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = master.name ?: "Мастер",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "⭐ ${String.format("%.1f", master.rating)} • ${master.completedOrders} заказов",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                master.distanceFormatted?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                master.arrivalTimeFormatted?.let {
                                                    Text(
                                                        text = "~$it",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        if (master != nearbyMasters.take(3).last()) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        } else if (isLoadingMasters) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Поиск ближайших мастеров...")
                            }
                        }
                        
                        // Детали адреса
                        OutlinedTextField(
                            value = addressStreet,
                            onValueChange = { addressStreet = it },
                            label = { Text("Улица") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = addressBuilding,
                                onValueChange = { addressBuilding = it },
                                label = { Text("Дом") },
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            OutlinedTextField(
                                value = addressApartment,
                                onValueChange = { addressApartment = it },
                                label = { Text("Квартира") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = addressFloor,
                                onValueChange = { addressFloor = it },
                                label = { Text("Этаж") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            OutlinedTextField(
                                value = addressEntranceCode,
                                onValueChange = { addressEntranceCode = it },
                                label = { Text("Код домофона") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        OutlinedTextField(
                            value = addressLandmark,
                            onValueChange = { addressLandmark = it },
                            label = { Text("Ориентир") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ВРЕМЕННЫЕ ПАРАМЕТРЫ И ПРИОРИТЕТ
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🕒 Временные параметры",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Время прибытия
                    ExposedDropdownMenuBox(
                        expanded = arrivalTimeExpanded,
                        onExpandedChange = { arrivalTimeExpanded = !arrivalTimeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = arrivalTime,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Желаемое время прибытия") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = arrivalTimeExpanded, onDismissRequest = { arrivalTimeExpanded = false }) {
                            OrderFormData.arrivalTimeSlots.forEach { timeSlot ->
                                DropdownMenuItem(text = { Text(timeSlot) }, onClick = {
                                    arrivalTime = if (timeSlot == "В любое время") "" else timeSlot
                                    arrivalTimeExpanded = false
                                })
                            }
                        }
                    }
                    
                    // Желаемая дата
                    OutlinedTextField(
                        value = desiredRepairDate,
                        onValueChange = { desiredRepairDate = it },
                        label = { Text("Желаемая дата ремонта") },
                        placeholder = { Text("ДД.ММ.ГГГГ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Срочность
                    ExposedDropdownMenuBox(
                        expanded = urgencyExpanded,
                        onExpandedChange = { urgencyExpanded = !urgencyExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = when(urgency) {
                                "emergency" -> "Экстренный (сегодня)"
                                "urgent" -> "Срочный (завтра)"
                                "planned" -> "Плановый (в течение недели)"
                                else -> ""
                            },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Срочность") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = urgencyExpanded, onDismissRequest = { urgencyExpanded = false }) {
                            DropdownMenuItem(text = { Text("Экстренный (сегодня)") }, onClick = {
                                urgency = "emergency"
                                isUrgent = true
                                urgencyExpanded = false
                            })
                            DropdownMenuItem(text = { Text("Срочный (завтра)") }, onClick = {
                                urgency = "urgent"
                                isUrgent = true
                                urgencyExpanded = false
                            })
                            DropdownMenuItem(text = { Text("Плановый (в течение недели)") }, onClick = {
                                urgency = "planned"
                                isUrgent = false
                                urgencyExpanded = false
                            })
                        }
                    }
                    
                    // Срочный заказ (переключатель)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Срочный заказ", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = isUrgent, onCheckedChange = { 
                            isUrgent = it
                            if (it && urgency != "emergency" && urgency != "urgent") {
                                urgency = "urgent"
                            }
                        })
                    }
                }
            }

            // ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { expandedSection = if (expandedSection == "additional") "" else "additional" }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ℹ️ Дополнительная информация",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    
                    if (expandedSection == "additional") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Финансовые параметры
                        Text(
                            "Способ оплаты",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        PaymentMethodSelector(
                            selectedPaymentType = paymentType,
                            onPaymentTypeSelected = { paymentType = it }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = clientBudget,
                            onValueChange = { clientBudget = it },
                            label = { Text("Предварительный бюджет (руб.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { 
                                Text(
                                    "Укажите примерную сумму, которую вы готовы потратить",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Особенности доступа
                        Text("Особенности доступа", style = MaterialTheme.typography.titleMedium)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Домофон работает")
                            Switch(checked = intercomWorking, onCheckedChange = { intercomWorking = it })
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Парковка для мастера")
                            Switch(checked = parkingAvailable, onCheckedChange = { parkingAvailable = it })
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Особенности помещения
                        Text("Особенности помещения", style = MaterialTheme.typography.titleMedium)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Домашние животные")
                            Switch(checked = hasPets, onCheckedChange = { hasPets = it })
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Маленькие дети")
                            Switch(checked = hasSmallChildren, onCheckedChange = { hasSmallChildren = it })
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Предпочтительный способ связи
                        ExposedDropdownMenuBox(
                            expanded = contactMethodExpanded,
                            onExpandedChange = { contactMethodExpanded = !contactMethodExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = when(preferredContactMethod) {
                                    "call" -> "Звонок"
                                    "sms" -> "SMS"
                                    "chat" -> "Чат"
                                    else -> ""
                                },
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Предпочтительный способ связи") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = contactMethodExpanded, onDismissRequest = { contactMethodExpanded = false }) {
                                DropdownMenuItem(text = { Text("Звонок") }, onClick = {
                                    preferredContactMethod = "call"
                                    contactMethodExpanded = false
                                })
                                DropdownMenuItem(text = { Text("SMS") }, onClick = {
                                    preferredContactMethod = "sms"
                                    contactMethodExpanded = false
                                })
                                DropdownMenuItem(text = { Text("Чат") }, onClick = {
                                    preferredContactMethod = "chat"
                                    contactMethodExpanded = false
                                })
                            }
                        }
                    }
                }
            }

            // Ошибка
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Карта
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
                                    Icon(Icons.Default.ArrowBack, "Закрыть")
                                }
                            }
                            MapAddressPicker(
                                initialLatitude = latitude,
                                initialLongitude = longitude,
                                onLocationSelected = { location ->
                                    latitude = location.latitude
                                    longitude = location.longitude
                                    address = location.address ?: "Адрес не определен"
                                    
                                    // Загружаем ближайших мастеров при выборе адреса
                                    scope.launch {
                                        isLoadingMasters = true
                                        try {
                                            val response = AppContainer.apiService.getMasters(
                                                status = "available",
                                                isOnShift = true,
                                                latitude = location.latitude,
                                                longitude = location.longitude,
                                                radius = 20000.0, // 20 км
                                                limit = 10
                                            )
                                            if (response.isSuccessful && response.body() != null) {
                                                nearbyMasters = response.body()!!
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isLoadingMasters = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                showNearbyMasters = true,
                                nearbyMasters = nearbyMasters
                            )
                        }
                    }
                }
            }
            
            // Кнопка создания заказа
            Button(
                onClick = {
                    viewModel.createOrder(
                        deviceType = deviceType,
                        deviceCategory = deviceCategory.ifBlank { null },
                        deviceBrand = deviceBrand.ifBlank { null },
                        deviceModel = deviceModel.ifBlank { null },
                        deviceSerialNumber = deviceSerialNumber.ifBlank { null },
                        deviceYear = deviceYear.toIntOrNull(),
                        warrantyStatus = warrantyStatus.ifBlank { null },
                        problemShortDescription = problemShortDescription.ifBlank { null },
                        problemDescription = problemDescription,
                        problemWhenStarted = problemWhenStarted.ifBlank { null },
                        problemConditions = problemConditions.ifBlank { null },
                        problemErrorCodes = problemErrorCodes.ifBlank { null },
                        problemAttemptedFixes = problemAttemptedFixes.ifBlank { null },
                        address = address,
                        addressStreet = addressStreet.ifBlank { null },
                        addressBuilding = addressBuilding.ifBlank { null },
                        addressApartment = addressApartment.ifBlank { null },
                        addressFloor = addressFloor.toIntOrNull(),
                        addressEntranceCode = addressEntranceCode.ifBlank { null },
                        addressLandmark = addressLandmark.ifBlank { null },
                        latitude = latitude,
                        longitude = longitude,
                        arrivalTime = arrivalTime.ifBlank { null },
                        desiredRepairDate = desiredRepairDate.ifBlank { null },
                        urgency = urgency,
                        clientBudget = clientBudget.toDoubleOrNull(),
                        paymentType = paymentType.ifBlank { null },
                        intercomWorking = intercomWorking,
                        parkingAvailable = parkingAvailable,
                        hasPets = hasPets,
                        hasSmallChildren = hasSmallChildren,
                        preferredContactMethod = preferredContactMethod,
                        isUrgent = isUrgent
                    )
                },
                enabled = !uiState.isLoading && 
                          deviceType.isNotBlank() && 
                          problemDescription.isNotBlank() && 
                          address.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Создать заявку", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSelector(
    selectedPaymentType: String,
    onPaymentTypeSelected: (String) -> Unit
) {
    val paymentMethods = listOf(
        PaymentMethod("cash", "Наличные", "Оплата наличными мастеру", Icons.Default.Money),
        PaymentMethod("card", "Карта", "Оплата банковской картой", Icons.Default.CreditCard),
        PaymentMethod("online", "Онлайн", "Оплата через интернет", Icons.Default.Payment),
        PaymentMethod("yoomoney", "ЮMoney", "Оплата через ЮMoney", Icons.Default.AccountBalanceWallet),
        PaymentMethod("qiwi", "QIWI", "Оплата через QIWI", Icons.Default.AccountBalanceWallet),
        PaymentMethod("installment", "Рассрочка", "Оплата в рассрочку", Icons.Default.CalendarToday)
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        paymentMethods.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { method ->
                    PaymentMethodCard(
                        method = method,
                        isSelected = selectedPaymentType == method.value,
                        onClick = { onPaymentTypeSelected(method.value) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Если нечетное количество, добавляем пустое место
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = method.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = method.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (method.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = method.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2
                )
            }
        }
    }
}

data class PaymentMethod(
    val value: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
