package com.bestapp.client.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bestapp.client.data.api.models.OrderDto
import com.bestapp.client.ui.home.OrderCard
import com.bestapp.client.ui.navigation.Screen

/**
 * Улучшенный экран заказов с фильтрами и группировкой
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavController,
    viewModel: OrdersViewModel = viewModel()
) {
    val uiState by viewModel.ordersUiState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<OrderFilter>(OrderFilter.ALL) }
    var selectedSort by remember { mutableStateOf<OrderSort>(OrderSort.DATE_DESC) }
    var showFilters by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Фильтрация и сортировка заказов
    val filteredAndSortedOrders = remember(uiState.orders, searchQuery, selectedFilter, selectedSort) {
        val orders = uiState.orders
        
        // Поиск
        val searchFiltered = if (searchQuery.isNotBlank()) {
            orders.filter { order ->
                order.deviceType.contains(searchQuery, ignoreCase = true) ||
                order.address.contains(searchQuery, ignoreCase = true) ||
                order.id.toString().contains(searchQuery) ||
                order.masterName?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            orders
        }
        
        // Фильтр по статусу
        val statusFiltered = when (selectedFilter) {
            OrderFilter.ALL -> searchFiltered
            OrderFilter.ACTIVE -> searchFiltered.filter { 
                it.repairStatus in listOf("new", "assigned", "in_progress") 
            }
            OrderFilter.COMPLETED -> searchFiltered.filter { it.repairStatus == "completed" }
            OrderFilter.CANCELLED -> searchFiltered.filter { it.repairStatus == "cancelled" }
        }
        
        // Сортировка
        statusFiltered.sortedWith(
            when (selectedSort) {
                OrderSort.DATE_DESC -> compareByDescending<OrderDto> { it.id }
                OrderSort.DATE_ASC -> compareBy { it.id }
                OrderSort.STATUS -> compareBy { it.repairStatus }
                OrderSort.DEVICE_TYPE -> compareBy { it.deviceType }
            }
        )
    }
    
    // Группировка по статусу
    val groupedOrders = remember(filteredAndSortedOrders) {
        filteredAndSortedOrders.groupBy { order ->
            when (order.repairStatus) {
                "new" -> "Новые"
                "assigned" -> "Назначены"
                "in_progress" -> "В работе"
                "completed" -> "Завершены"
                "cancelled" -> "Отменены"
                else -> "Другие"
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Мои заказы") },
                    actions = {
                        // Кнопка фильтра
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Badge(
                                containerColor = if (selectedFilter != OrderFilter.ALL) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                            }
                        }
                        
                        // Сортировка
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Сортировка")
                        }
                        
                        // Обновить
                        IconButton(onClick = { 
                            scope.launch {
                                viewModel.loadOrders()
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                        }
                    }
                )
                
                // Поиск
                if (uiState.orders.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Поиск по заказам...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }
                
                // Чипы фильтров
                if (showFilters) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrderFilter.values().forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                leadingIcon = if (selectedFilter == filter) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
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
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
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
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadOrders() }) {
                            Text("Повторить")
                        }
                    }
                }
                uiState.orders.isEmpty() -> {
                    EmptyOrdersState(navController)
                }
                filteredAndSortedOrders.isEmpty() -> {
                    NoResultsState(onReset = { 
                        searchQuery = ""
                        selectedFilter = OrderFilter.ALL
                    })
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Группировка по статусу
                        groupedOrders.forEach { (status, orders) ->
                            item {
                                OrderGroupHeader(
                                    status = status,
                                    count = orders.size
                                )
                            }
                            
                            items(orders) { order ->
                                OrderCard(
                                    order = order,
                                    onClick = {
                                        navController.navigate(Screen.OrderDetails.createRoute(order.id))
                                    }
                                )
                            }
                        }
                        
                        // Статистика
                        item {
                            OrdersStatistics(orders = uiState.orders)
                        }
                    }
                }
            }
        }
        
        // Диалог сортировки
        if (showSortDialog) {
            SortOrdersDialog(
                selectedSort = selectedSort,
                onSortSelected = { selectedSort = it; showSortDialog = false },
                onDismiss = { showSortDialog = false }
            )
        }
    }
}

@Composable
fun OrderGroupHeader(status: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun EmptyOrdersState(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Assignment,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "У вас пока нет заказов",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Создайте первый заказ на ремонт техники",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { navController.navigate(Screen.CreateOrder.route) },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Создать заказ")
        }
    }
}

@Composable
fun NoResultsState(onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Ничего не найдено",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Попробуйте изменить фильтры или поисковый запрос",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onReset) {
            Text("Сбросить фильтры")
        }
    }
}

@Composable
fun OrdersStatistics(orders: List<OrderDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Статистика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val stats = mapOf(
                "Всего заказов" to orders.size,
                "Активных" to orders.count { it.repairStatus in listOf("new", "assigned", "in_progress") },
                "Завершено" to orders.count { it.repairStatus == "completed" },
                "Отменено" to orders.count { it.repairStatus == "cancelled" }
            )
            
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

enum class OrderFilter(val label: String) {
    ALL("Все"),
    ACTIVE("Активные"),
    COMPLETED("Завершенные"),
    CANCELLED("Отмененные")
}

enum class OrderSort(val label: String) {
    DATE_DESC("Дата (новые сначала)"),
    DATE_ASC("Дата (старые сначала)"),
    STATUS("По статусу"),
    DEVICE_TYPE("По типу техники")
}

@Composable
fun SortOrdersDialog(
    selectedSort: OrderSort,
    onSortSelected: (OrderSort) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сортировка заказов") },
        text = {
            Column {
                OrderSort.values().forEach { sort ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(sort) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSort == sort,
                            onClick = { onSortSelected(sort) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = sort.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

