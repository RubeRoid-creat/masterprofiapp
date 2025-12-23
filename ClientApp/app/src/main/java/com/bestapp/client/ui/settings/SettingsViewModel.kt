package com.bestapp.client.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val pushNotificationsEnabled: Boolean = true,
    val emailNotificationsEnabled: Boolean = true
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // TODO: Загрузить настройки из локального хранилища
            // Пока используем значения по умолчанию
        }
    }

    fun setPushNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(pushNotificationsEnabled = enabled)
            // TODO: Сохранить настройку
        }
    }

    fun setEmailNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(emailNotificationsEnabled = enabled)
            // TODO: Сохранить настройку
        }
    }
}







