package com.bestapp.client.ui.masters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.api.models.MasterDto
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.repository.ApiResult
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MasterProfileUiState(
    val isLoading: Boolean = false,
    val master: MasterDto? = null,
    val reviews: com.bestapp.client.data.api.models.ReviewsResponse? = null,
    val errorMessage: String? = null
)

class MasterProfileViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MasterProfileUiState())
    val uiState: StateFlow<MasterProfileUiState> = _uiState.asStateFlow()
    
    fun loadMasterProfile(masterId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Загружаем профиль мастера и отзывы параллельно
            val masterResult = repository.getMasterById(masterId)
            val reviewsResult = repository.getMasterReviews(masterId, limit = 10)
            
            when {
                masterResult is ApiResult.Success -> {
                    val reviews = if (reviewsResult is ApiResult.Success) {
                        reviewsResult.data
                    } else {
                        null
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        master = masterResult.data,
                        reviews = reviews
                    )
                }
                masterResult is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = masterResult.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}

