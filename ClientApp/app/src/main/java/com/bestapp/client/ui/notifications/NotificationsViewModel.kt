package com.bestapp.client.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val errorMessage: String? = null
)

class NotificationsViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Загружаем заказы для создания уведомлений на основе их статусов
            // В реальном приложении здесь должен быть отдельный API для уведомлений
            when (val result = repository.getOrders()) {
                is com.bestapp.client.data.repository.ApiResult.Success -> {
                    val notifications = buildList<NotificationItem> {
                        result.data.forEach { order ->
                            // Создаем уведомление о статусе заказа
                            if (order.repairStatus == "assigned" && order.masterName != null) {
                                add(
                                    NotificationItem(
                                        id = order.id * 1000 + 1, // Временный ID
                                        title = "Мастер назначен",
                                        message = "Вашему заказу #${order.id} назначен мастер ${order.masterName}",
                                        type = NotificationType.MASTER_ASSIGNED,
                                        timestamp = System.currentTimeMillis(),
                                        isRead = false
                                    )
                                )
                            }
                            if (order.repairStatus == "in_progress") {
                                add(
                                    NotificationItem(
                                        id = order.id * 1000 + 2,
                                        title = "Заказ в работе",
                                        message = "Мастер приступил к работе над заказом #${order.id}",
                                        type = NotificationType.ORDER_STATUS,
                                        timestamp = System.currentTimeMillis(),
                                        isRead = false
                                    )
                                )
                            }
                            if (order.repairStatus == "completed") {
                                add(
                                    NotificationItem(
                                        id = order.id * 1000 + 3,
                                        title = "Заказ завершен",
                                        message = "Ваш заказ #${order.id} успешно завершен",
                                        type = NotificationType.ORDER_STATUS,
                                        timestamp = System.currentTimeMillis(),
                                        isRead = false
                                    )
                                )
                            }
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications.sortedByDescending { it.timestamp },
                        isLoading = false
                    )
                }
                is com.bestapp.client.data.repository.ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            // Локальная отметка как прочитанное
            val updated = _uiState.value.notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            _uiState.value = _uiState.value.copy(notifications = updated)
            // TODO: Отправить на сервер через API когда будет готов endpoint
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val updated = _uiState.value.notifications.map { it.copy(isRead = true) }
            _uiState.value = _uiState.value.copy(notifications = updated)
            // TODO: Отправить на сервер через API когда будет готов endpoint
        }
    }
}







