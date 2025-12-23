package com.bestapp.client.ui.addresses

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Экран управления адресами клиента
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressesScreen(
    navController: NavController,
    viewModel: AddressesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<ClientAddress?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadAddresses()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои адреса") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить адрес")
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
                    ErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.loadAddresses() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.addresses.isEmpty() -> {
                    EmptyAddressesState(
                        onAddAddress = { showAddDialog = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.addresses) { address ->
                            AddressCard(
                                address = address,
                                onEdit = { editingAddress = address },
                                onDelete = { viewModel.deleteAddress(address.id) },
                                onSetDefault = { viewModel.setDefaultAddress(address.id) }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
    
    // Диалог добавления адреса
    if (showAddDialog) {
        AddEditAddressDialog(
            address = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { label, fullAddress, lat, lon ->
                viewModel.addAddress(label, fullAddress, lat, lon)
                showAddDialog = false
            }
        )
    }
    
    // Диалог редактирования адреса
    if (editingAddress != null) {
        AddEditAddressDialog(
            address = editingAddress,
            onDismiss = { editingAddress = null },
            onConfirm = { label, fullAddress, lat, lon ->
                viewModel.updateAddress(editingAddress!!.id, label, fullAddress, lat, lon)
                editingAddress = null
            }
        )
    }
}

@Composable
fun AddressCard(
    address: ClientAddress,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isDefault) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (address.isDefault) Icons.Default.Home else Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (address.isDefault) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = address.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (address.isDefault) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "По умолчанию",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = address.fullAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!address.isDefault) {
                            DropdownMenuItem(
                                text = { Text("Сделать основным") },
                                onClick = {
                                    onSetDefault()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                onEdit()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                showDeleteDialog = true
                                showMenu = false
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                ) 
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить адрес?") },
            text = { Text("Вы действительно хотите удалить адрес \"${address.label}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressDialog(
    address: ClientAddress?,
    onDismiss: () -> Unit,
    onConfirm: (label: String, fullAddress: String, latitude: Double, longitude: Double) -> Unit
) {
    var label by remember { mutableStateOf(address?.label ?: "") }
    var fullAddress by remember { mutableStateOf(address?.fullAddress ?: "") }
    var showMapPicker by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf(address?.latitude ?: 56.859611) }
    var longitude by remember { mutableStateOf(address?.longitude ?: 35.911896) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (address == null) "Новый адрес" else "Редактировать адрес") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Название *") },
                    placeholder = { Text("Дом, Работа, Дача...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
                )
                
                OutlinedTextField(
                    value = fullAddress,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Адрес *") },
                    placeholder = { Text("Выберите на карте") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMapPicker = true },
                    minLines = 2,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showMapPicker = true }) {
                            Icon(Icons.Default.Map, contentDescription = "Выбрать на карте")
                        }
                    }
                )
                
                TextButton(
                    onClick = { showMapPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбрать на карте")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank() && fullAddress.isNotBlank()) {
                        onConfirm(label, fullAddress, latitude, longitude)
                    }
                },
                enabled = label.isNotBlank() && fullAddress.isNotBlank()
            ) {
                Text(if (address == null) "Добавить" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
    
    // TODO: Добавить MapAddressPicker в диалог
}

@Composable
fun EmptyAddressesState(
    onAddAddress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Нет сохраненных адресов",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Добавьте адреса для быстрого оформления заказов",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAddAddress,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить адрес")
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

// Data classes
data class ClientAddress(
    val id: Long,
    val label: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean
)

// ViewModel
class AddressesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AddressesUiState())
    val uiState: StateFlow<AddressesUiState> = _uiState.asStateFlow()
    
    fun loadAddresses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // TODO: Загрузка с сервера
            kotlinx.coroutines.delay(1000)
            
            val mockAddresses = listOf(
                ClientAddress(
                    id = 1,
                    label = "Дом",
                    fullAddress = "г. Тверь, ул. Коминтерна, д. 123, кв. 45",
                    latitude = 56.859611,
                    longitude = 35.911896,
                    isDefault = true
                ),
                ClientAddress(
                    id = 2,
                    label = "Работа",
                    fullAddress = "г. Тверь, пр-т Ленина, д. 45, оф. 301",
                    latitude = 56.858611,
                    longitude = 35.921896,
                    isDefault = false
                )
            )
            
            _uiState.value = _uiState.value.copy(
                addresses = mockAddresses,
                isLoading = false
            )
        }
    }
    
    fun addAddress(label: String, fullAddress: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            // TODO: Отправка на сервер
            loadAddresses()
        }
    }
    
    fun updateAddress(id: Long, label: String, fullAddress: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            // TODO: Обновление на сервере
            loadAddresses()
        }
    }
    
    fun deleteAddress(id: Long) {
        viewModelScope.launch {
            // TODO: Удаление на сервере
            loadAddresses()
        }
    }
    
    fun setDefaultAddress(id: Long) {
        viewModelScope.launch {
            // TODO: Установка основного адреса на сервере
            loadAddresses()
        }
    }
}

data class AddressesUiState(
    val addresses: List<ClientAddress> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
