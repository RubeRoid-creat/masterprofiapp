package com.bestapp.client.ui.services

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ServicesScreenUiState(
    val isLoading: Boolean = false,
    val categories: List<ServiceCategory> = emptyList(),
    val errorMessage: String? = null
)

class ServicesScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ServicesScreenUiState())
    val uiState: StateFlow<ServicesScreenUiState> = _uiState.asStateFlow()

    fun loadServices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Реализовать загрузку категорий услуг из API
            // Временные тестовые данные
            val mockCategories = listOf(
                ServiceCategory(
                    id = 1,
                    name = "Стиральные машины",
                    description = "Ремонт и обслуживание стиральных машин",
                    icon = Icons.Default.LocalLaundryService,
                    priceRange = "1500"
                ),
                ServiceCategory(
                    id = 2,
                    name = "Холодильники",
                    description = "Ремонт холодильников и морозильных камер",
                    icon = Icons.Default.Kitchen,
                    priceRange = "2000"
                ),
                ServiceCategory(
                    id = 3,
                    name = "Посудомоечные машины",
                    description = "Ремонт и установка посудомоечных машин",
                    icon = Icons.Default.Kitchen,
                    priceRange = "1800"
                ),
                ServiceCategory(
                    id = 4,
                    name = "Духовые шкафы",
                    description = "Ремонт духовых шкафов и варочных панелей",
                    icon = Icons.Default.Microwave,
                    priceRange = "1700"
                ),
                ServiceCategory(
                    id = 5,
                    name = "Кондиционеры",
                    description = "Установка и ремонт кондиционеров",
                    icon = Icons.Default.AcUnit,
                    priceRange = "2500"
                ),
                ServiceCategory(
                    id = 6,
                    name = "Водонагреватели",
                    description = "Установка и ремонт водонагревателей",
                    icon = Icons.Default.WaterDrop,
                    priceRange = "2200"
                )
            )
            
            _uiState.value = ServicesScreenUiState(categories = mockCategories)
        }
    }
}

