package com.example.bestapp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bestapp.data.AutoAcceptSettings
import com.example.bestapp.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutoAcceptSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PreferencesManager.getInstance(application)
    
    private val _settings = MutableStateFlow(prefsManager.getAutoAcceptSettings())
    val settings: StateFlow<AutoAcceptSettings> = _settings.asStateFlow()
    
    fun updateSettings(newSettings: AutoAcceptSettings) {
        viewModelScope.launch {
            prefsManager.setAutoAcceptSettings(newSettings)
            _settings.value = newSettings
        }
    }
    
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return AutoAcceptSettingsViewModel(application) as T
                }
            }
        }
    }
}
