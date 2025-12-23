package com.bestapp.client.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bestapp.client.ui.home.StatusChip
import com.bestapp.client.ui.common.Icons8Icon
import com.bestapp.client.ui.common.Icons8
import com.bestapp.client.ui.navigation.Screen
import androidx.compose.material.icons.filled.Chat
import androidx.compose.foundation.clickable
import com.bestapp.client.ui.reviews.ReviewDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bestapp.client.ui.reviews.ReviewViewModel
import com.bestapp.client.ui.orders.OrderStatusTracker
import com.bestapp.client.ui.orders.CompleteOrderDialog
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    navController: NavController,
    orderId: Long,
    viewModel: OrdersViewModel = viewModel()
) {
    val uiState by viewModel.orderDetailsUiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val reviewViewModel: ReviewViewModel = viewModel()
    
    // Проверяем, можно ли оставить отзыв
    var canReview by remember { mutableStateOf(false) }
    var existingReview by remember { mutableStateOf<com.bestapp.client.data.api.models.ReviewDto?>(null) }
    
    // Проверяем наличие отчета для подписания
    var reportId by remember { mutableStateOf<Long?>(null) }
    var reportStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var showReorderError by remember { mutableStateOf<String?>(null) }
    var isReordering by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.order?.id) {
        uiState.order?.let { order ->
            val isCompleted = order.repairStatus == "completed" || order.requestStatus == "completed"
            canReview = isCompleted && order.masterId != null
            
            if (canReview) {
                // Проверяем, есть ли уже отзыв
                when (val result = com.bestapp.client.di.AppContainer.apiRepository.getOrderReview(order.id)) {
                    is com.bestapp.client.data.repository.ApiResult.Success -> {
                        existingReview = result.data
                    }
                    else -> {}
                }
            }
            
            // Проверяем наличие отчета для подписания
            // Показываем кнопку если заказ назначен мастеру и в работе или завершен
            if (order.masterId != null) {
                val isInProgress = order.repairStatus == "in_progress" || 
                                   order.repairStatus == "completed" ||
                                   order.requestStatus == "in_progress" ||
                                   order.requestStatus == "completed" ||
                                   order.requestStatus == "accepted" ||
                                   order.repairStatus == "assigned"
                
                if (isInProgress) {
                    scope.launch {
                        when (val result = AppContainer.apiRepository.getReports(orderId = order.id, status = null)) {
                            is com.bestapp.client.data.repository.ApiResult.Success -> {
                                val reports = result.data
                                android.util.Log.d("OrderDetails", "Загружено отчетов: ${reports.size}")
                                reports.forEach { report ->
                                    android.util.Log.d("OrderDetails", "Отчет ID: ${report.id}, статус: ${report.status}, подпись: ${report.clientSignature != null}")
                                }
                                
                                // Ищем отчет, который можно подписать
                                // Статусы: 'draft', 'pending_signature', 'pending' (любой не подписанный)
                                val pendingReport = reports.firstOrNull { report ->
                                    val status = report.status?.lowercase() ?: ""
                                    val canSign = status == "pending_signature" || 
                                                 status == "pending" || 
                                                 status == "draft" ||
                                                 (status != "signed" && status != "completed" && report.clientSignature == null)
                                    android.util.Log.d("OrderDetails", "Проверка отчета ${report.id}: статус=$status, canSign=$canSign")
                                    canSign
                                }
                                
                                if (pendingReport != null) {
                                    android.util.Log.d("OrderDetails", "Найден отчет для подписания: ID=${pendingReport.id}, статус=${pendingReport.status}")
                                    reportId = pendingReport.id
                                    reportStatus = pendingReport.status
                                } else {
                                    android.util.Log.d("OrderDetails", "Отчет для подписания не найден")
                                }
                            }
                            is com.bestapp.client.data.repository.ApiResult.Error -> {
                                android.util.Log.e("OrderDetails", "Ошибка загрузки отчетов: ${result.message}")
                            }
                            else -> {
                                android.util.Log.d("OrderDetails", "Неизвестный результат загрузки отчетов")
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetails(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val orderTitle = uiState.order?.orderNumber ?: "Заказ #$orderId"
                    Text(
                        text = orderTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        // Диалог отзыва
        if (showReviewDialog && uiState.order != null) {
            ReviewDialog(
                orderId = uiState.order!!.id,
                orderNumber = uiState.order!!.orderNumber,
                onDismiss = { 
                    showReviewDialog = false
                    reviewViewModel.resetState()
                },
                onReviewSubmitted = {
                    // Перезагружаем детали заказа
                    viewModel.loadOrderDetails(orderId)
                },
                viewModel = reviewViewModel
            )
        }
        
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
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadOrderDetails(orderId) }) {
                            Text("Повторить")
                        }
                    }
                }
                uiState.order != null -> {
                    val order = uiState.order!!
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Трекер статуса заказа
                        OrderStatusTracker(
                            order = order,
                            statusHistory = uiState.statusHistory,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Статус и приоритет
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Статус заявки",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusChip(status = order.requestStatus ?: order.repairStatus)
                                }
                                
                                if (order.priority != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Приоритет", style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = when(order.priority) {
                                                "emergency" -> "Экстренный"
                                                "urgent" -> "Срочный"
                                                "regular" -> "Обычный"
                                                "planned" -> "Плановый"
                                                else -> order.priority ?: ""
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Информация о технике
                        ExpandableCard(
                            title = "📱 Информация о технике",
                            expanded = expandedSection == "device",
                            onToggle = { expandedSection = if (expandedSection == "device") null else "device" }
                        ) {
                            InfoRow(icon = Icons.Default.Build, label = "Тип", value = order.deviceType)
                            
                            if (order.deviceCategory != null) {
                                InfoRow(
                                    icon = Icons.Default.Info,
                                    label = "Категория",
                                    value = when(order.deviceCategory) {
                                        "large" -> "Крупная"
                                        "small" -> "Мелкая"
                                        "builtin" -> "Встраиваемая"
                                        else -> order.deviceCategory
                                    }
                                )
                            }
                            
                            if (order.deviceBrand != null) {
                                InfoRow(icon = Icons.Default.Info, label = "Бренд", value = order.deviceBrand)
                            }
                            
                            if (order.deviceModel != null) {
                                InfoRow(icon = Icons.Default.Info, label = "Модель", value = order.deviceModel)
                            }
                            
                            if (order.deviceSerialNumber != null) {
                                InfoRow(icon = Icons.Default.Info, label = "Серийный номер", value = order.deviceSerialNumber)
                            }
                            
                            if (order.deviceYear != null) {
                                InfoRow(icon = Icons.Default.DateRange, label = "Год выпуска/покупки", value = order.deviceYear.toString())
                            }
                            
                            if (order.warrantyStatus != null) {
                                InfoRow(
                                    icon = Icons.Default.CheckCircle,
                                    label = "Гарантийный статус",
                                    value = if (order.warrantyStatus == "warranty") "На гарантии" else "Постгарантийный"
                                )
                            }
                        }

                        // Описание проблемы
                        ExpandableCard(
                            title = "🚨 Описание проблемы",
                            expanded = expandedSection == "problem",
                            onToggle = { expandedSection = if (expandedSection == "problem") null else "problem" }
                        ) {
                            if (order.problemShortDescription != null) {
                                Text(
                                    text = order.problemShortDescription,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            Text(
                                text = order.problemDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (order.problemWhenStarted != null) {
                                InfoRow(icon = Icons.Default.Schedule, label = "Когда началась", value = order.problemWhenStarted)
                            }
                            
                            if (order.problemConditions != null) {
                                InfoRow(icon = Icons.Default.Info, label = "Условия проявления", value = order.problemConditions)
                            }
                            
                            if (order.problemErrorCodes != null) {
                                InfoRow(
                                    icon = Icons.Default.Warning,
                                    label = "Коды ошибок",
                                    value = order.problemErrorCodes,
                                    valueColor = MaterialTheme.colorScheme.error
                                )
                            }
                            
                            if (order.problemAttemptedFixes != null) {
                                InfoRow(icon = Icons.Default.Build, label = "Что уже пробовали", value = order.problemAttemptedFixes)
                            }
                            
                            if (order.problemCategory != null) {
                                InfoRow(
                                    icon = Icons.Default.Info,
                                    label = "Категория проблемы",
                                    value = when(order.problemCategory) {
                                        "electrical" -> "Электрика"
                                        "mechanical" -> "Механика"
                                        "electronic" -> "Электроника"
                                        "software" -> "Программное обеспечение"
                                        else -> order.problemCategory
                                    }
                                )
                            }
                            
                            if (order.problemTags != null && order.problemTags.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        order.problemTags.forEach { tag ->
                                            AssistChip(
                                                onClick = { },
                                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Адрес (детализированный)
                        ExpandableCard(
                            title = "📍 Адрес",
                            expanded = expandedSection == "address",
                            onToggle = { expandedSection = if (expandedSection == "address") null else "address" }
                        ) {
                            InfoRow(icon = Icons.Default.LocationOn, label = "Адрес", value = order.address)
                            
                            if (order.addressStreet != null) {
                                InfoRow(icon = Icons.Default.LocationOn, label = "Улица", value = order.addressStreet)
                            }
                            
                            if (order.addressBuilding != null || order.addressApartment != null) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    if (order.addressBuilding != null) {
                                        InfoRow(
                                            icon = Icons.Default.Home,
                                            label = "Дом",
                                            value = order.addressBuilding,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                    }
                                    if (order.addressApartment != null) {
                                        InfoRow(
                                            icon = Icons.Default.Home,
                                            label = "Квартира",
                                            value = order.addressApartment,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            
                            if (order.addressFloor != null || order.addressEntranceCode != null) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    if (order.addressFloor != null) {
                                        InfoRow(
                                            icon = Icons.Default.Info,
                                            label = "Этаж",
                                            value = order.addressFloor.toString(),
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                    }
                                    if (order.addressEntranceCode != null) {
                                        InfoRow(
                                            icon = Icons.Default.Info,
                                            label = "Код домофона",
                                            value = order.addressEntranceCode,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            
                            if (order.addressLandmark != null) {
                                InfoRow(icon = Icons.Default.LocationOn, label = "Ориентир", value = order.addressLandmark)
                            }
                            
                            // Координаты (для мастера)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Координаты: ${String.format("%.6f", order.latitude)}, ${String.format("%.6f", order.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Клиент
                        ExpandableCard(
                            title = "👤 Информация о клиенте",
                            expanded = expandedSection == "client",
                            onToggle = { expandedSection = if (expandedSection == "client") null else "client" }
                        ) {
                            InfoRow(icon = Icons.Default.Person, label = "Имя", value = order.clientName)
                            InfoRow(icon = Icons.Default.Phone, label = "Телефон", value = order.clientPhone)
                            if (order.clientEmail != null) {
                                InfoRow(icon = Icons.Default.Info, label = "Email", value = order.clientEmail)
                            }
                            
                            if (order.preferredContactMethod != null) {
                                InfoRow(
                                    icon = Icons.Default.Phone,
                                    label = "Предпочтительный способ связи",
                                    value = when(order.preferredContactMethod) {
                                        "call" -> "Звонок"
                                        "sms" -> "SMS"
                                        "chat" -> "Чат"
                                        else -> order.preferredContactMethod
                                    }
                                )
                            }
                        }

                        // Временные параметры
                        ExpandableCard(
                            title = "🕒 Временные параметры",
                            expanded = expandedSection == "time",
                            onToggle = { expandedSection = if (expandedSection == "time") null else "time" }
                        ) {
                            if (order.arrivalTime != null) {
                                InfoRow(icon = Icons.Default.Schedule, label = "Желаемое время прибытия", value = order.arrivalTime)
                            }
                            
                            if (order.desiredRepairDate != null) {
                                InfoRow(icon = Icons.Default.DateRange, label = "Желаемая дата ремонта", value = order.desiredRepairDate)
                            }
                            
                            if (order.urgency != null) {
                                InfoRow(
                                    icon = Icons.Default.Info,
                                    label = "Срочность",
                                    value = when(order.urgency) {
                                        "emergency" -> "Экстренный (сегодня)"
                                        "urgent" -> "Срочный (завтра)"
                                        "planned" -> "Плановый (в течение недели)"
                                        else -> order.urgency
                                    }
                                )
                            }
                        }

                        // Дополнительная информация
                        ExpandableCard(
                            title = "ℹ️ Дополнительная информация",
                            expanded = expandedSection == "additional",
                            onToggle = { expandedSection = if (expandedSection == "additional") null else "additional" }
                        ) {
                            // Особенности доступа
                            Text("Особенности доступа", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Домофон работает", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if ((order.intercomWorking ?: 1) == 1) "Да" else "Нет",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Парковка для мастера", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if ((order.parkingAvailable ?: 1) == 1) "Да" else "Нет",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Особенности помещения
                            Text("Особенности помещения", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Домашние животные", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if ((order.hasPets ?: 0) == 1) "Да" else "Нет",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Маленькие дети", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if ((order.hasSmallChildren ?: 0) == 1) "Да" else "Нет",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Финансовые параметры
                        if (order.clientBudget != null || order.paymentType != null || order.estimatedCost != null || order.finalCost != null) {
                            ExpandableCard(
                                title = "💰 Финансовые параметры",
                                expanded = expandedSection == "finance",
                                onToggle = { expandedSection = if (expandedSection == "finance") null else "finance" }
                            ) {
                                if (order.clientBudget != null) {
                                    InfoRow(
                                        icon = Icons.Default.Info,
                                        label = "Предварительный бюджет клиента",
                                        value = "${order.clientBudget} ₽"
                                    )
                                }
                                
                                if (order.paymentType != null) {
                                    InfoRow(
                                        icon = Icons.Default.Info,
                                        label = "Тип оплаты",
                                        value = when(order.paymentType) {
                                            "cash" -> "Наличные"
                                            "card" -> "Карта"
                                            "online" -> "Онлайн"
                                            "installment" -> "Рассрочка"
                                            else -> order.paymentType
                                        }
                                    )
                                }
                                
                                if (order.estimatedCost != null) {
                                    InfoRow(
                                        icon = Icons.Default.Info,
                                        label = "Предварительная стоимость",
                                        value = "${order.estimatedCost} ₽",
                                        valueColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                if (order.finalCost != null) {
                                    InfoRow(
                                        icon = Icons.Default.CheckCircle,
                                        label = "Итоговая стоимость",
                                        value = "${order.finalCost} ₽",
                                        valueColor = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Информация о мастере
                        if (order.masterName != null && order.masterId != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate(Screen.MasterProfile.createRoute(order.masterId!!))
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔧 Мастер",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Открыть профиль",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    InfoRow(icon = Icons.Default.Person, label = "Имя", value = order.masterName)
                                    if (order.assignmentDate != null) {
                                        InfoRow(icon = Icons.Default.DateRange, label = "Дата назначения", value = order.assignmentDate)
                                    }
                                }
                            }
                        }

                        // Служебная информация (для мастера)
                        if (order.preliminaryDiagnosis != null || order.repairComplexity != null || order.estimatedRepairTime != null) {
                            ExpandableCard(
                                title = "🔧 Служебная информация",
                                expanded = expandedSection == "service",
                                onToggle = { expandedSection = if (expandedSection == "service") null else "service" }
                            ) {
                                if (order.preliminaryDiagnosis != null) {
                                    InfoRow(
                                        icon = Icons.Default.Info,
                                        label = "Предварительный диагноз",
                                        value = order.preliminaryDiagnosis
                                    )
                                }
                                
                                if (order.repairComplexity != null) {
                                    InfoRow(
                                        icon = Icons.Default.Build,
                                        label = "Сложность ремонта",
                                        value = when(order.repairComplexity) {
                                            "simple" -> "Простой"
                                            "medium" -> "Средний"
                                            "complex" -> "Сложный"
                                            else -> order.repairComplexity
                                        }
                                    )
                                }
                                
                                if (order.estimatedRepairTime != null) {
                                    InfoRow(
                                        icon = Icons.Default.Schedule,
                                        label = "Расчетное время работы",
                                        value = "${order.estimatedRepairTime} мин."
                                    )
                                }
                                
                                if (order.requiredParts != null) {
                                    val parts = try {
                                        // Если это JSON массив строк
                                        order.requiredParts
                                    } catch (e: Exception) {
                                        order.requiredParts
                                    }
                                    InfoRow(
                                        icon = Icons.Default.Info,
                                        label = "Необходимые запчасти",
                                        value = parts ?: "Не указано"
                                    )
                                }
                                
                                if (order.specialEquipment != null) {
                                    InfoRow(
                                        icon = Icons.Default.Build,
                                        label = "Специальное оборудование",
                                        value = order.specialEquipment
                                    )
                                }
                            }
                        }

                        // Медиафайлы
                        if (order.media != null && order.media.isNotEmpty()) {
                            ExpandableCard(
                                title = "📎 Медиафайлы (${order.media.size})",
                                expanded = expandedSection == "media",
                                onToggle = { expandedSection = if (expandedSection == "media") null else "media" }
                            ) {
                                order.media.forEach { media ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icons8Icon(
                                            iconId = when(media.mediaType) {
                                                "photo" -> Icons8.PHOTO
                                                "video" -> Icons8.VIDEO
                                                "document" -> Icons8.DOCUMENT
                                                "audio" -> Icons8.ATTACH
                                                else -> Icons8.ATTACH
                                            },
                                            contentDescription = null,
                                            size = 24,
                                            modifier = Modifier
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = media.fileName ?: media.mediaType,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (media.description != null) {
                                                Text(
                                                    text = media.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (media != order.media.last()) {
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                        
                        // Кнопка завершения заказа (для мастера)
                        // Показываем кнопку если заказ назначен мастеру и в работе
                        val canComplete = order.masterId != null && 
                                (order.repairStatus == "in_progress" || order.requestStatus == "in_progress")
                        
                        // Временная отладка
                        android.util.Log.d("OrderDetails", "masterId: ${order.masterId}, repairStatus: ${order.repairStatus}, requestStatus: ${order.requestStatus}, canComplete: $canComplete")
                        
                        if (canComplete) {
                            var showCompleteDialog by remember { mutableStateOf(false) }
                            
                            Button(
                                onClick = { showCompleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Завершить заказ")
                            }
                            
                            if (showCompleteDialog) {
                                CompleteOrderDialog(
                                    order = order,
                                    onDismiss = { showCompleteDialog = false },
                                    onComplete = { finalCost, repairDescription ->
                                        viewModel.completeOrder(order.id, finalCost, repairDescription)
                                        showCompleteDialog = false
                                    }
                                )
                            }
                        }

                        // Кнопка чата (если заказ назначен мастеру)
                        if (order.masterId != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { navController.navigate(Screen.Chat.createRoute(order.id)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Чат с мастером")
                            }
                        }
                        
                        // Кнопка отмены (для клиента)
                        if ((order.repairStatus == "new" || order.repairStatus == "in_progress") && order.masterName == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Отменить заказ")
                            }
                        }
                        
                        // Кнопка подписания отчета (для клиента)
                        // Показываем кнопку если заказ назначен мастеру и в работе
                        // Упрощенная логика: показываем всегда, если есть мастер
                        if (order.masterId != null) {
                            val isInWork = order.repairStatus == "in_progress" || 
                                          order.repairStatus == "completed" ||
                                          order.repairStatus == "assigned" ||
                                          order.requestStatus == "in_progress" ||
                                          order.requestStatus == "completed" ||
                                          order.requestStatus == "accepted"
                            
                            if (isInWork) {
                                // Если есть reportId, переходим к подписанию
                                // Если нет, все равно показываем кнопку (пользователь увидит, что отчета нет)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { 
                                        if (reportId != null) {
                                            android.util.Log.d("OrderDetails", "Переход к подписанию отчета: reportId=$reportId, orderId=$orderId")
                                            navController.navigate(
                                                Screen.ReportSign.createRoute(reportId!!, orderId)
                                            )
                                        } else {
                                            android.util.Log.d("OrderDetails", "Отчет не найден, но показываем кнопку для проверки")
                                            // Попробуем загрузить отчеты еще раз
                                            scope.launch {
                                                when (val result = AppContainer.apiRepository.getReports(orderId = order.id, status = null)) {
                                                    is com.bestapp.client.data.repository.ApiResult.Success -> {
                                                        val reports = result.data
                                                        val pendingReport = reports.firstOrNull { report ->
                                                            val status = report.status?.lowercase() ?: ""
                                                            status == "pending_signature" || 
                                                            status == "pending" || 
                                                            status == "draft" ||
                                                            (status != "signed" && status != "completed" && report.clientSignature == null)
                                                        }
                                                        if (pendingReport != null) {
                                                            navController.navigate(
                                                                Screen.ReportSign.createRoute(pendingReport.id, orderId)
                                                            )
                                                        }
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    enabled = true
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (reportId != null) "Подписать отчет" else "Проверить отчет")
                                }
                            }
                        }
                        
                        // Кнопка "Заказать снова" (для завершенных заказов)
                        val isCompleted = order.repairStatus == "completed" || order.requestStatus == "completed"
                        if (isCompleted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isReordering = true
                                        showReorderError = null
                                        when (val result = AppContainer.apiRepository.reorderOrder(orderId)) {
                                            is com.bestapp.client.data.repository.ApiResult.Success -> {
                                                val newOrder = result.data.order
                                                isReordering = false
                                                navController.navigate(Screen.OrderDetails.createRoute(newOrder.id)) {
                                                    popUpTo(Screen.Orders.route)
                                                }
                                            }
                                            is com.bestapp.client.data.repository.ApiResult.Error -> {
                                                isReordering = false
                                                showReorderError = result.message ?: "Ошибка создания повторного заказа"
                                                android.util.Log.e("OrderDetails", "Ошибка reorder: ${result.message}")
                                            }
                                            else -> {
                                                isReordering = false
                                                showReorderError = "Неизвестная ошибка"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isReordering
                            ) {
                                if (isReordering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Создание заказа...")
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Заказать снова")
                                }
                            }
                            
                            // Показываем ошибку, если есть
                            showReorderError?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { showReorderError = null }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Закрыть",
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Кнопка оставить отзыв (для завершенных заказов)
                        if (canReview) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val review = existingReview
                            if (review == null) {
                                Button(
                                    onClick = { showReviewDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Оставить отзыв")
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Ваш отзыв",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            repeat(review.rating) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        review.comment?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Отмена заказа") },
            text = { Text("Вы уверены, что хотите отменить заказ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelOrder(orderId)
                        showCancelDialog = false
                    }
                ) {
                    Text("Да, отменить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Нет")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icons8Icon(
                    iconId = if (expanded) Icons8.EXPAND_UP else Icons8.EXPAND_DOWN,
                    contentDescription = null,
                    size = 24
                )
            }
            
            if (expanded) {
                Divider()
                content()
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Divider()
            content()
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (valueColor != androidx.compose.ui.graphics.Color.Unspecified) valueColor else androidx.compose.ui.graphics.Color.Unspecified,
                fontWeight = fontWeight
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
