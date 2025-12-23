package com.example.bestapp.ui.zones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bestapp.data.PreferencesManager
import com.example.bestapp.data.MapWorkZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkZonesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PreferencesManager.getInstance(application)
    
    private val _zones = MutableStateFlow(prefsManager.getWorkZones())
    val zones: StateFlow<List<MapWorkZone>> = _zones.asStateFlow()
    
    fun addZone(zone: MapWorkZone) {
        viewModelScope.launch {
            val currentZones = _zones.value.toMutableList()
            // Генерируем уникальный ID, если нужно
            val zoneWithId = if (zone.id == 0L) {
                val maxId = currentZones.maxOfOrNull { zoneItem: MapWorkZone -> zoneItem.id } ?: 0L
                zone.copy(id = maxId + 1)
            } else {
                zone
            }
            currentZones.add(zoneWithId)
            _zones.value = currentZones
            saveZones()
        }
    }
    
    fun toggleZone(zoneId: Long, isActive: Boolean) {
        viewModelScope.launch {
            val currentZones = _zones.value.map { zone: MapWorkZone ->
                if (zone.id == zoneId) zone.copy(isActive = isActive) else zone
            }
            _zones.value = currentZones
            saveZones()
        }
    }
    
    fun deleteZone(zoneId: Long) {
        viewModelScope.launch {
            val currentZones = _zones.value.filter { zone: MapWorkZone -> zone.id != zoneId }
            _zones.value = currentZones
            saveZones()
        }
    }
    
    fun saveZones() {
        viewModelScope.launch {
            prefsManager.setWorkZones(_zones.value)
        }
    }
    
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return WorkZonesViewModel(application) as T
                }
            }
        }
    }
}

