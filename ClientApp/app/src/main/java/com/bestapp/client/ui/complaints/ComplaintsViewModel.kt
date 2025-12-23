package com.bestapp.client.ui.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана жалоб
 */
class ComplaintsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(ComplaintsUiState())
    val uiState: StateFlow<ComplaintsUiState> = _uiState.asStateFlow()

    init {
        loadComplaints()
    }

    fun loadComplaints() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // TODO: Интеграция с API
                // val complaints = apiRepository.getComplaints()
                
                // Временные тестовые данные
                val testComplaints = listOf(
                    Complaint(
                        id = 1,
                        orderId = 1234,
                        category = "Качество работы",
                        description = "Мастер не полностью устранил проблему с холодильником",
                        status = "in_review",
                        createdAt = "20.12.2025"
                    ),
                    Complaint(
                        id = 2,
                        orderId = 1230,
                        category = "Опоздание мастера",
                        description = "Мастер опоздал на 2 часа без предупреждения",
                        status = "resolved",
                        createdAt = "15.12.2025",
                        response = "Приносим извинения за неудобства. Мастер получил выговор."
                    )
                )

                _uiState.value = _uiState.value.copy(
                    complaints = testComplaints,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Неизвестная ошибка"
                )
            }
        }
    }

    fun createComplaint(orderId: String, category: String, description: String) {
        viewModelScope.launch {
            try {
                // TODO: Интеграция с API
                // apiRepository.createComplaint(orderId, category, description)
                
                // Обновить список после создания
                loadComplaints()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка создания жалобы: ${e.message}"
                )
            }
        }
    }
}

/**
 * UI состояние экрана жалоб
 */
data class ComplaintsUiState(
    val complaints: List<Complaint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
