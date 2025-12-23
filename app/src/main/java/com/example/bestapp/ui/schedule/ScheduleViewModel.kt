package com.example.bestapp.ui.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiScheduleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val schedule: Map<String, ApiScheduleItem> = emptyMap(), // date -> schedule item
    val selectedDate: String? = null,
    val errorMessage: String? = null
)

class ScheduleViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "ScheduleViewModel"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()
    
    init {
        loadSchedule()
    }
    
    fun loadSchedule(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = apiRepository.getSchedule(startDate, endDate)
                result.onSuccess { scheduleList ->
                    val scheduleMap = scheduleList.associateBy { it.date }
                    _uiState.value = _uiState.value.copy(
                        schedule = scheduleMap,
                        isLoading = false
                    )
                    Log.d(TAG, "Schedule loaded: ${scheduleMap.size} items")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка загрузки расписания",
                        isLoading = false
                    )
                    Log.e(TAG, "Error loading schedule", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Ошибка загрузки расписания",
                    isLoading = false
                )
                Log.e(TAG, "Error loading schedule", e)
            }
        }
    }
    
    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }
    
    fun createOrUpdateSchedule(
        date: String,
        startTime: String? = null,
        endTime: String? = null,
        isAvailable: Boolean = true,
        note: String? = null
    ) {
        viewModelScope.launch {
            try {
                val result = apiRepository.createOrUpdateSchedule(date, startTime, endTime, isAvailable, note)
                result.onSuccess { scheduleItem ->
                    val updatedSchedule = _uiState.value.schedule.toMutableMap()
                    updatedSchedule[date] = scheduleItem
                    _uiState.value = _uiState.value.copy(
                        schedule = updatedSchedule,
                        selectedDate = date
                    )
                    Log.d(TAG, "Schedule updated for date: $date")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка сохранения расписания"
                    )
                    Log.e(TAG, "Error saving schedule", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Ошибка сохранения расписания"
                )
                Log.e(TAG, "Error saving schedule", e)
            }
        }
    }
    
    fun deleteSchedule(date: String) {
        viewModelScope.launch {
            try {
                val result = apiRepository.deleteSchedule(date)
                result.onSuccess {
                    val updatedSchedule = _uiState.value.schedule.toMutableMap()
                    updatedSchedule.remove(date)
                    _uiState.value = _uiState.value.copy(
                        schedule = updatedSchedule,
                        selectedDate = null
                    )
                    Log.d(TAG, "Schedule deleted for date: $date")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка удаления расписания"
                    )
                    Log.e(TAG, "Error deleting schedule", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Ошибка удаления расписания"
                )
                Log.e(TAG, "Error deleting schedule", e)
            }
        }
    }
    
    fun createBatchSchedule(
        startDate: String,
        endDate: String,
        startTime: String? = null,
        endTime: String? = null,
        isAvailable: Boolean = true,
        daysOfWeek: List<Int>? = null
    ) {
        viewModelScope.launch {
            try {
                val result = apiRepository.createBatchSchedule(startDate, endDate, startTime, endTime, isAvailable, daysOfWeek)
                result.onSuccess {
                    loadSchedule(startDate, endDate)
                    Log.d(TAG, "Batch schedule created")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Ошибка создания расписания"
                    )
                    Log.e(TAG, "Error creating batch schedule", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Ошибка создания расписания"
                )
                Log.e(TAG, "Error creating batch schedule", e)
            }
        }
    }
    
    fun refresh() {
        loadSchedule()
    }
    
    fun getScheduleForDate(date: String): ApiScheduleItem? {
        return _uiState.value.schedule[date]
    }
}


