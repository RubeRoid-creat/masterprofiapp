package com.example.bestapp.ui.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiReportTemplate
import com.example.bestapp.api.models.ApiWorkReport
import com.example.bestapp.api.models.PartUsed
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkReportUiState(
    val isLoading: Boolean = false,
    val templates: List<ApiReportTemplate> = emptyList(),
    val selectedTemplate: ApiReportTemplate? = null,
    val workDescription: String = "",
    val partsUsed: List<PartUsed> = emptyList(),
    val workDuration: Int? = null,
    val totalCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val beforePhotos: List<String> = emptyList(),
    val afterPhotos: List<String> = emptyList(),
    val errorMessage: String? = null,
    val isReportCreated: Boolean = false,
    val createdReportId: Long? = null
)

class WorkReportViewModel : ViewModel() {
    private val apiRepository = ApiRepository()
    
    companion object {
        private const val TAG = "WorkReportViewModel"
    }
    
    private val _uiState = MutableStateFlow(WorkReportUiState())
    val uiState: StateFlow<WorkReportUiState> = _uiState.asStateFlow()
    
    var orderId: Long = 0
    
    init {
        loadTemplates()
    }
    
    fun loadTemplates() {
        viewModelScope.launch {
            try {
                val result = apiRepository.getReportTemplates()
                result.onSuccess { templates ->
                    _uiState.value = _uiState.value.copy(templates = templates)
                    Log.d(TAG, "Templates loaded: ${templates.size}")
                }.onFailure { error ->
                    Log.e(TAG, "Error loading templates", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading templates", e)
            }
        }
    }
    
    fun selectTemplate(template: ApiReportTemplate) {
        _uiState.value = _uiState.value.copy(
            selectedTemplate = template,
            workDescription = template.workDescriptionTemplate,
            partsUsed = template.defaultParts ?: emptyList(),
            laborCost = template.defaultLaborCost ?: 0.0
        )
        updateTotalCost()
    }
    
    fun setWorkDescription(description: String) {
        _uiState.value = _uiState.value.copy(workDescription = description)
    }
    
    fun addPart(part: PartUsed) {
        val updatedParts = _uiState.value.partsUsed.toMutableList()
        updatedParts.add(part)
        _uiState.value = _uiState.value.copy(partsUsed = updatedParts)
        updatePartsCost()
        updateTotalCost()
    }
    
    fun removePart(index: Int) {
        val updatedParts = _uiState.value.partsUsed.toMutableList()
        if (index >= 0 && index < updatedParts.size) {
            updatedParts.removeAt(index)
            _uiState.value = _uiState.value.copy(partsUsed = updatedParts)
            updatePartsCost()
            updateTotalCost()
        }
    }
    
    fun setWorkDuration(duration: Int?) {
        _uiState.value = _uiState.value.copy(workDuration = duration)
    }
    
    fun setLaborCost(cost: Double) {
        _uiState.value = _uiState.value.copy(laborCost = cost)
        updateTotalCost()
    }
    
    fun addBeforePhoto(photoPath: String) {
        val updated = _uiState.value.beforePhotos.toMutableList()
        updated.add(photoPath)
        _uiState.value = _uiState.value.copy(beforePhotos = updated)
    }
    
    fun addAfterPhoto(photoPath: String) {
        val updated = _uiState.value.afterPhotos.toMutableList()
        updated.add(photoPath)
        _uiState.value = _uiState.value.copy(afterPhotos = updated)
    }
    
    fun removeBeforePhoto(index: Int) {
        val updated = _uiState.value.beforePhotos.toMutableList()
        if (index >= 0 && index < updated.size) {
            updated.removeAt(index)
            _uiState.value = _uiState.value.copy(beforePhotos = updated)
        }
    }
    
    fun removeAfterPhoto(index: Int) {
        val updated = _uiState.value.afterPhotos.toMutableList()
        if (index >= 0 && index < updated.size) {
            updated.removeAt(index)
            _uiState.value = _uiState.value.copy(afterPhotos = updated)
        }
    }
    
    private fun updatePartsCost() {
        val total = _uiState.value.partsUsed.sumOf { it.cost * it.quantity }
        _uiState.value = _uiState.value.copy(partsCost = total)
    }
    
    private fun updateTotalCost() {
        val total = _uiState.value.partsCost + _uiState.value.laborCost
        _uiState.value = _uiState.value.copy(totalCost = total)
    }
    
    fun createReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = apiRepository.createReport(
                    orderId = orderId,
                    workDescription = _uiState.value.workDescription,
                    totalCost = _uiState.value.totalCost,
                    reportType = "standard",
                    partsUsed = if (_uiState.value.partsUsed.isNotEmpty()) _uiState.value.partsUsed else null,
                    workDuration = _uiState.value.workDuration,
                    partsCost = _uiState.value.partsCost,
                    laborCost = _uiState.value.laborCost,
                    templateId = _uiState.value.selectedTemplate?.id,
                    beforePhotos = _uiState.value.beforePhotos,
                    afterPhotos = _uiState.value.afterPhotos
                )
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isReportCreated = true
                    )
                    Log.d(TAG, "Report created successfully")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Ошибка создания отчета"
                    )
                    Log.e(TAG, "Error creating report", error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка создания отчета"
                )
                Log.e(TAG, "Error creating report", e)
            }
        }
    }
    
    fun reset() {
        _uiState.value = WorkReportUiState()
        loadTemplates()
    }
}

