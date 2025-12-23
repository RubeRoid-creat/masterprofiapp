package com.bestapp.client.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.bestapp.client.ui.promotions.PromotionItem
import java.util.Calendar

data class HomeUiState(
    val isLoading: Boolean = false,
    val news: List<NewsItem> = emptyList(),
    val promotions: List<PromotionItem> = emptyList(),
    val errorMessage: String? = null,
    val userName: String = ""
)

class HomeViewModel(
    private val repository: ApiRepository = AppContainer.apiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        loadNewsAndPromotions()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val name = repository.getCurrentUserName() ?: "Пользователь"
            _uiState.value = _uiState.value.copy(userName = name)
        }
    }

    fun loadNewsAndPromotions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Реализовать загрузку новостей и акций из API
            // Временные тестовые данные
            val mockNews = listOf(
                NewsItem(
                    id = 1,
                    title = "Новая услуга: Ремонт кондиционеров",
                    description = "Теперь мы предлагаем полный спектр услуг по ремонту и обслуживанию кондиционеров всех марок.",
                    timestamp = System.currentTimeMillis() - 86400000,
                    category = "Услуги"
                ),
                NewsItem(
                    id = 2,
                    title = "Расширение зоны обслуживания",
                    description = "Мы расширили зону обслуживания! Теперь наши мастера работают во всех районах города.",
                    timestamp = System.currentTimeMillis() - 172800000,
                    category = "Новости"
                ),
                NewsItem(
                    id = 3,
                    title = "Выходные дни - скидка 15%",
                    description = "При заказе ремонта в выходные дни вы получаете скидку 15% на все виды работ.",
                    timestamp = System.currentTimeMillis() - 259200000,
                    category = "Акции"
                )
            )
            
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 30)
            
            val mockPromotions = listOf(
                PromotionItem(
                    id = 1,
                    title = "Скидка 20% на первый заказ",
                    description = "Новые клиенты получают скидку 20% на первый заказ.",
                    discount = 20,
                    validUntil = calendar.timeInMillis
                ),
                PromotionItem(
                    id = 2,
                    title = "Бесплатная диагностика",
                    description = "При заказе ремонта диагностика выполняется бесплатно.",
                    discount = null,
                    validUntil = calendar.timeInMillis
                )
            )
            
            _uiState.value = HomeUiState(
                news = mockNews,
                promotions = mockPromotions,
                userName = _uiState.value.userName
            )
        }
    }
}

