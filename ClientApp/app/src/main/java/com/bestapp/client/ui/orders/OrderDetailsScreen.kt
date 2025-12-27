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
import com.bestapp.client.ui.orders.RepairDescriptionParser
import com.bestapp.client.ui.orders.RepairWorksAndPartsCard
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "📱 Информация о технике",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                InfoRow(icon = Icons.Default.Build, label = "Тип техники", value = order.deviceType)
                                if (order.deviceBrand != null) {
                                    InfoRow(icon = Icons.Default.Info, label = "Бренд", value = order.deviceBrand)
                                }
                            }
                        }

                        // Описание проблемы
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🚨 Описание проблемы",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Парсим описание проблемы для извлечения работ и запчастей
                                val parsedDescription = remember(order.problemDescription) {
                                    RepairDescriptionParser.parseDescription(order.problemDescription)
                                }
                                
                                // Отображаем базовое описание проблемы (без структурированных данных)
                                val baseDescription = if (parsedDescription.originalDescription != null) {
                                    parsedDescription.originalDescription
                                } else if (parsedDescription.additionalComments != null) {
                                    // Если есть только дополнительные комментарии, показываем их
                                    parsedDescription.additionalComments
                                } else {
                                    // Если есть структурированные данные, показываем только краткое описание
                                    order.problemDescription
                                        .lines()
                                        .takeWhile { 
                                            !it.contains("Выполненные работы:", ignoreCase = true) &&
                                            !it.contains("Использованные запчасти:", ignoreCase = true) &&
                                            !it.contains("Предполагаемые работы:", ignoreCase = true) &&
                                            !it.contains("Предполагаемые запчасти:", ignoreCase = true) &&
                                            !it.contains("Дополнительно:", ignoreCase = true)
                                        }
                                        .joinToString("\n")
                                        .trim()
                                }
                                
                                if (baseDescription.isNotBlank()) {
                                    Text(
                                        text = baseDescription,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                // Отображаем структурированные работы и запчасти
                                if (parsedDescription.completedWorks.isNotEmpty() || 
                                    parsedDescription.usedParts.isNotEmpty() ||
                                    parsedDescription.estimatedWorks.isNotEmpty() ||
                                    parsedDescription.estimatedParts.isNotEmpty()) {
                                    RepairWorksAndPartsCard(
                                        completedWorks = parsedDescription.completedWorks,
                                        usedParts = parsedDescription.usedParts,
                                        estimatedWorks = parsedDescription.estimatedWorks,
                                        estimatedParts = parsedDescription.estimatedParts,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                // Дополнительные комментарии (если есть и они не были показаны выше)
                                if (parsedDescription.additionalComments != null && parsedDescription.originalDescription == null) {
                                    Text(
                                        text = "Дополнительно:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = parsedDescription.additionalComments,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Адрес
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "📍 Адрес",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                InfoRow(icon = Icons.Default.LocationOn, label = "Адрес", value = order.address)
                            }
                        }


                        // Желаемая дата и время
                        if (order.desiredRepairDate != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "🕒 Желаемая дата и время",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    InfoRow(icon = Icons.Default.DateRange, label = "Дата и время", value = order.desiredRepairDate)
                                }
                            }
                        }


                        // Стоимость
                        if (order.clientBudget != null || order.finalCost != null) {
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
                                    Text(
                                        text = "💰 Стоимость",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (order.clientBudget != null) {
                                        InfoRow(
                                            icon = Icons.Default.Info,
                                            label = "Предварительная стоимость",
                                            value = "${order.clientBudget} ₽"
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
