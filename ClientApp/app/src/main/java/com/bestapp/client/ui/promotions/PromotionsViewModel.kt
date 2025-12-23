package com.bestapp.client.ui.promotions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class PromotionsUiState(
    val isLoading: Boolean = false,
    val promotions: List<PromotionItem> = emptyList(),
    val errorMessage: String? = null
)

class PromotionsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PromotionsUiState())
    val uiState: StateFlow<PromotionsUiState> = _uiState.asStateFlow()

    fun loadPromotions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Реализовать загрузку акций из API
            // Временные тестовые данные
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 30)
            
            val mockPromotions = listOf(
                PromotionItem(
                    id = 1,
                    title = "Скидка 20% на первый заказ",
                    description = "Новые клиенты получают скидку 20% на первый заказ. Акция действует для всех видов услуг.",
                    discount = 20,
                    validUntil = calendar.timeInMillis
                ),
                PromotionItem(
                    id = 2,
                    title = "Бесплатная диагностика",
                    description = "При заказе ремонта диагностика выполняется бесплатно. Экономия до 2000 рублей!",
                    discount = null,
                    validUntil = calendar.timeInMillis
                ),
                PromotionItem(
                    id = 3,
                    title = "Двойные баллы лояльности",
                    description = "В декабре получайте двойные баллы за каждый заказ. Копите баллы и получайте скидки!",
                    discount = null,
                    validUntil = calendar.timeInMillis
                )
            )
            
            _uiState.value = PromotionsUiState(promotions = mockPromotions)
        }
    }
}







