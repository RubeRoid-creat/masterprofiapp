package com.bestapp.client.ui.orders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestapp.client.data.api.models.OrderDto
import com.bestapp.client.data.api.models.OrderStatusHistoryDto
import com.bestapp.client.ui.home.StatusChip
import java.text.SimpleDateFormat
import java.util.*

enum class OrderStage(
    val status: String,
    val title: String,
    val icon: ImageVector
) {
    NEW("new", "Новая заявка", Icons.Default.AddCircle),
    ACCEPTED("accepted", "Принята мастером", Icons.Default.CheckCircle),
    IN_PROGRESS("in_progress", "В работе", Icons.Default.Build),
    COMPLETED("completed", "Завершена", Icons.Default.Done),
    CANCELLED("cancelled", "Отменена", Icons.Default.Cancel);
    
    @Composable
    fun getColor(): Color {
        return when (this) {
            NEW -> MaterialTheme.colorScheme.primary
            ACCEPTED -> MaterialTheme.colorScheme.tertiary
            IN_PROGRESS -> MaterialTheme.colorScheme.secondary
            COMPLETED -> Color(0xFF4CAF50)
            CANCELLED -> MaterialTheme.colorScheme.error
        }
    }
    
    companion object {
        fun fromStatus(status: String): OrderStage {
            return values().find { it.status == status } ?: NEW
        }
    }
}

@Composable
fun OrderStatusTracker(
    order: OrderDto,
    statusHistory: List<OrderStatusHistoryDto>? = null,
    modifier: Modifier = Modifier
) {
    val currentStatus = when {
        order.repairStatus == "cancelled" || order.requestStatus == "cancelled" -> OrderStage.CANCELLED
        order.repairStatus == "completed" || order.requestStatus == "completed" -> OrderStage.COMPLETED
        order.repairStatus == "in_progress" || order.requestStatus == "in_progress" -> OrderStage.IN_PROGRESS
        order.repairStatus == "assigned" || order.requestStatus == "accepted" -> OrderStage.ACCEPTED
        else -> OrderStage.NEW
    }
    
    val stages = listOf(
        OrderStage.NEW,
        OrderStage.ACCEPTED,
        OrderStage.IN_PROGRESS,
        OrderStage.COMPLETED
    )
    
    val currentStageIndex = stages.indexOf(currentStatus).coerceAtLeast(0)
    val progress = if (currentStatus == OrderStage.CANCELLED) {
        0f
    } else {
        (currentStageIndex + 1) / stages.size.toFloat()
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Статус заказа",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = currentStatus.status)
            }
            
            // Прогресс-бар
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = currentStatus.getColor(),
                trackColor = MaterialTheme.colorScheme.surface
            )
            
            // Этапы
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stages.forEachIndexed { index, stage ->
                    val isCompleted = index <= currentStageIndex
                    val isCurrent = index == currentStageIndex && currentStatus != OrderStage.CANCELLED
                    val isCancelled = currentStatus == OrderStage.CANCELLED
                    
                    OrderStageItem(
                        stage = stage,
                        isCompleted = isCompleted && !isCancelled,
                        isCurrent = isCurrent,
                        isCancelled = isCancelled,
                        historyItem = statusHistory?.find { 
                            it.newStatus == stage.status 
                        }
                    )
                }
            }
            
            // История изменений (если есть)
            if (!statusHistory.isNullOrEmpty()) {
                Divider()
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "История изменений",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    statusHistory.takeLast(5).forEach { historyItem ->
                        StatusHistoryItem(historyItem = historyItem)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStageItem(
    stage: OrderStage,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isCancelled: Boolean,
    historyItem: OrderStatusHistoryDto? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка этапа
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
                    val stageColor = stage.getColor()
            val iconColor = when {
                isCancelled -> MaterialTheme.colorScheme.error
                isCompleted -> stageColor
                isCurrent -> stageColor
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            val backgroundColor = when {
                isCancelled -> MaterialTheme.colorScheme.errorContainer
                isCompleted -> stageColor.copy(alpha = 0.1f)
                isCurrent -> stageColor.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = backgroundColor,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = stage.icon,
                        contentDescription = stage.title,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(8.dp),
                        tint = iconColor
                    )
                }
            }
        }
        
        // Информация об этапе
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stage.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCancelled -> MaterialTheme.colorScheme.error
                    isCurrent -> stage.getColor()
                    isCompleted -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            historyItem?.let {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it.createdAt)
                } catch (e: Exception) {
                    null
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    it.changedByName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    date?.let {
                        Text(
                            text = dateFormat.format(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Индикатор выполнения
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Выполнено",
                tint = stage.getColor(),
                modifier = Modifier.size(20.dp)
            )
        } else if (isCurrent) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = stage.getColor(),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun StatusHistoryItem(
    historyItem: OrderStatusHistoryDto,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${historyItem.oldStatus ?: "—"} → ${historyItem.newStatus}",
                style = MaterialTheme.typography.bodySmall
            )
            historyItem.changedByName?.let {
                Text(
                    text = "Изменено: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        val date = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(historyItem.createdAt)
        } catch (e: Exception) {
            null
        }
        
        date?.let {
            Text(
                text = dateFormat.format(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

