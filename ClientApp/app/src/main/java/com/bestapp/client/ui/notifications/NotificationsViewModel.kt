package com.bestapp.client.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val errorMessage: String? = null
)

class NotificationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Реализовать загрузку уведомлений из API
            // Временные тестовые данные
            val mockNotifications = listOf(
                NotificationItem(
                    id = 1,
                    title = "Статус заказа изменен",
                    message = "Ваш заказ #123 переведен в статус 'В работе'",
                    type = NotificationType.ORDER_STATUS,
                    timestamp = System.currentTimeMillis() - 3600000,
                    isRead = false
                ),
                NotificationItem(
                    id = 2,
                    title = "Мастер назначен",
                    message = "Вашему заказу #123 назначен мастер Иван Петров",
                    type = NotificationType.MASTER_ASSIGNED,
                    timestamp = System.currentTimeMillis() - 7200000,
                    isRead = true
                )
            )
            
            _uiState.value = NotificationsUiState(notifications = mockNotifications)
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            // TODO: Реализовать API для отметки уведомления как прочитанного
            val updated = _uiState.value.notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            _uiState.value = _uiState.value.copy(notifications = updated)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            // TODO: Реализовать API для отметки всех уведомлений как прочитанных
            val updated = _uiState.value.notifications.map { it.copy(isRead = true) }
            _uiState.value = _uiState.value.copy(notifications = updated)
        }
    }
}







