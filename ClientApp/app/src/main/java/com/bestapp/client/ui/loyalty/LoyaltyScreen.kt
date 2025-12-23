package com.bestapp.client.ui.loyalty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyScreen(
    navController: NavController,
    viewModel: LoyaltyViewModel = viewModel(factory = LoyaltyViewModel.provideFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadLoyaltyData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Программа лояльности") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is LoyaltyUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoyaltyUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ошибка: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadLoyaltyData() }) {
                        Text("Повторить")
                    }
                }
            }
            is LoyaltyUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Карточка с балансом
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ваш баланс",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${state.balance} баллов",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "≈ ${String.format("%.2f", state.balance * state.config.rublesPerPoint)} ₽",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // Информация о программе
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Как накопить баллы",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Divider()
                            InfoRow(
                                label = "За каждый заказ",
                                value = "+${state.info.config.pointsPerOrder} баллов"
                            )
                            InfoRow(
                                label = "За каждый отзыв",
                                value = "+${state.info.config.pointsPerReview} баллов"
                            )
                            InfoRow(
                                label = "За приглашение друга",
                                value = "+${state.info.config.pointsPerReferral} баллов"
                            )
                            Divider()
                            Text(
                                text = "Использование",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            InfoRow(
                                label = "1 балл =",
                                value = "${state.info.config.rublesPerPoint} ₽"
                            )
                            InfoRow(
                                label = "Минимум для использования",
                                value = "${state.info.config.minPointsToUse} баллов"
                            )
                        }
                    }
                    
                    // История
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "История",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Divider()
                            
                            if (state.history.points.isEmpty() && state.history.transactions.isEmpty()) {
                                Text(
                                    text = "История пуста",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                // Начисления
                                state.history.points.take(10).forEach { point ->
                                    HistoryItem(
                                        title = point.description ?: getSourceTypeName(point.sourceType),
                                        subtitle = formatDate(point.createdAt),
                                        value = "+${point.points}",
                                        isPositive = true
                                    )
                                    if (point != state.history.points.last()) {
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                                
                                // Использования
                                if (state.history.transactions.isNotEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    state.history.transactions.take(10).forEach { transaction ->
                                        HistoryItem(
                                            title = transaction.description ?: "Использование баллов",
                                            subtitle = formatDate(transaction.createdAt),
                                            value = "-${transaction.pointsUsed}",
                                            isPositive = false
                                        )
                                        if (transaction != state.history.transactions.last()) {
                                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Ошибка использования баллов
                    state.errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearError() }) {
                                    Text("Закрыть")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HistoryItem(
    title: String,
    subtitle: String,
    value: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

private fun getSourceTypeName(sourceType: String): String {
    return when (sourceType) {
        "order" -> "Баллы за заказ"
        "review" -> "Баллы за отзыв"
        "referral" -> "Баллы за приглашение"
        "bonus" -> "Бонусные баллы"
        else -> "Начисление баллов"
    }
}

private fun formatDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

