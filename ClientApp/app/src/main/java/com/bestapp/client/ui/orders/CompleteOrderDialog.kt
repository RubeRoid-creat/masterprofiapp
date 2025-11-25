package com.bestapp.client.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bestapp.client.data.api.models.OrderDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteOrderDialog(
    order: OrderDto,
    onDismiss: () -> Unit,
    onComplete: (finalCost: Double?, repairDescription: String?) -> Unit
) {
    var finalCostText by remember { mutableStateOf("") }
    var repairDescription by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Завершить заказ",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        order.orderNumber?.let {
                            Text(
                                text = "Заказ $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
                
                Divider()
                
                // Форма
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Итоговая стоимость
                    OutlinedTextField(
                        value = finalCostText,
                        onValueChange = { 
                            finalCostText = it
                            showError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Итоговая стоимость (руб.)") },
                        placeholder = { Text("Оставьте пустым, если не изменилась") },
                        singleLine = true,
                        supportingText = {
                            if (order.estimatedCost != null) {
                                Text(
                                    text = "Ориентировочная стоимость: ${order.estimatedCost} руб.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    
                    // Описание выполненной работы
                    OutlinedTextField(
                        value = repairDescription,
                        onValueChange = { 
                            repairDescription = it
                            showError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        label = { Text("Описание выполненной работы") },
                        placeholder = { Text("Что было сделано...") },
                        maxLines = 5
                    )
                }
                
                // Ошибка
                showError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val finalCost = finalCostText.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
                            
                            if (finalCost != null && finalCost < 0) {
                                showError = "Стоимость не может быть отрицательной"
                                return@Button
                            }
                            
                            onComplete(
                                finalCost,
                                repairDescription.takeIf { it.isNotBlank() }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Завершить")
                    }
                }
            }
        }
    }
}





