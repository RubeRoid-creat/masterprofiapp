package com.bestapp.client.ui.complaints

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

/**
 * Экран жалоб и обращений
 * Позволяет клиенту подавать жалобы на мастеров или качество обслуживания
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ComplaintsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Жалобы и обращения") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Создать жалобу")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    ErrorContent(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadComplaints() }
                    )
                }
                uiState.complaints.isEmpty() -> {
                    EmptyStateContent()
                }
                else -> {
                    ComplaintsList(
                        complaints = uiState.complaints,
                        onComplaintClick = { /* Navigate to details */ }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateComplaintDialog(
            onDismiss = { showCreateDialog = false },
            onSubmit = { orderId, category, description ->
                viewModel.createComplaint(orderId, category, description)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ComplaintsList(
    complaints: List<Complaint>,
    onComplaintClick: (Complaint) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(complaints) { complaint ->
            ComplaintCard(
                complaint = complaint,
                onClick = { onComplaintClick(complaint) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComplaintCard(
    complaint: Complaint,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = complaint.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ComplaintStatusChip(status = complaint.status)
            }

            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Заказ #${complaint.orderId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = complaint.createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ComplaintStatusChip(status: String) {
    val (color, text) = when (status) {
        "new" -> Pair(MaterialTheme.colorScheme.primary, "Новая")
        "in_review" -> Pair(MaterialTheme.colorScheme.tertiary, "На рассмотрении")
        "resolved" -> Pair(MaterialTheme.colorScheme.secondary, "Решена")
        "rejected" -> Pair(MaterialTheme.colorScheme.error, "Отклонена")
        else -> Pair(MaterialTheme.colorScheme.outline, status)
    }

    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

@Composable
private fun EmptyStateContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "У вас нет жалоб",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Если возникнут проблемы, вы можете создать обращение",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ошибка загрузки",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Повторить")
        }
    }
}

@Composable
private fun CreateComplaintDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var orderId by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Качество работы") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Качество работы",
        "Опоздание мастера",
        "Неправомерные действия",
        "Завышенная цена",
        "Другое"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать жалобу") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = orderId,
                    onValueChange = { orderId = it },
                    label = { Text("Номер заказа") },
                    placeholder = { Text("Например: 1234") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    category = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание проблемы") },
                    placeholder = { Text("Подробно опишите ситуацию") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(orderId, category, description) },
                enabled = orderId.isNotBlank() && description.isNotBlank()
            ) {
                Text("Отправить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// Data class для жалобы
data class Complaint(
    val id: Int,
    val orderId: Int,
    val category: String,
    val description: String,
    val status: String,
    val createdAt: String,
    val response: String? = null
)
