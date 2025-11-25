package com.bestapp.client.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.api.models.ServiceCategoryDto
import com.bestapp.client.data.api.models.ServiceTemplateDto
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.repository.ApiResult
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ServicesUiState(
    val isLoading: Boolean = false,
    val categories: List<ServiceCategoryDto> = emptyList(),
    val templates: List<ServiceTemplateDto> = emptyList(),
    val popularTemplates: List<ServiceTemplateDto> = emptyList(),
    val errorMessage: String? = null
)

class ServicesViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ServicesUiState())
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
        loadTemplates()
        loadPopularTemplates()
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        categories = result.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.message,
                        isLoading = false
                    )
                }
                ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    fun loadTemplates(categoryId: Long? = null, deviceType: String? = null) {
        viewModelScope.launch {
            when (val result = repository.getTemplates(categoryId, deviceType, null)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(templates = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }
    
    fun loadPopularTemplates() {
        viewModelScope.launch {
            when (val result = repository.getTemplates(null, null, true)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(popularTemplates = result.data)
                }
                is ApiResult.Error -> {}
                ApiResult.Loading -> {}
            }
        }
    }
    
    fun loadCategoryWithSubcategories(categoryId: Long) {
        viewModelScope.launch {
            when (val result = repository.getCategoryById(categoryId)) {
                is ApiResult.Success -> {
                    val category = result.data
                    // Обновляем список категорий, добавляя подкатегории
                    _uiState.value = _uiState.value.copy(
                        categories = _uiState.value.categories.map { cat ->
                            if (cat.id == categoryId) category else cat
                        }
                    )
                }
                is ApiResult.Error -> {}
                ApiResult.Loading -> {}
            }
        }
    }
}





