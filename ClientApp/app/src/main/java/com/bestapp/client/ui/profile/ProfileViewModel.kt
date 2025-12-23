package com.bestapp.client.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val name = repository.getCurrentUserName() ?: ""
            val email = repository.getCurrentUserEmail() ?: ""
            val phone = repository.getCurrentUserPhone() ?: ""
            
            _uiState.value = ProfileUiState(
                userName = name,
                userEmail = email,
                userPhone = phone
            )
        }
    }

    fun updateProfile(name: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Реализовать API для обновления профиля
            // when (val result = repository.updateProfile(name, phone)) {
            //     is ApiResult.Success -> {
            //         _uiState.value = _uiState.value.copy(
            //             userName = name,
            //             userPhone = phone,
            //             isLoading = false
            //         )
            //     }
            //     is ApiResult.Error -> {
            //         _uiState.value = _uiState.value.copy(
            //             isLoading = false,
            //             errorMessage = result.message
            //         )
            //     }
            //     else -> {}
            // }
            
            // Временное сохранение локально
            _uiState.value = _uiState.value.copy(
                userName = name,
                userPhone = phone,
                isLoading = false
            )
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLogoutComplete()
        }
    }
}







