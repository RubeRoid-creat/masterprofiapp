package com.example.bestapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.data.DataRepository
import com.example.bestapp.data.News
import com.example.bestapp.data.Statistics
import com.example.bestapp.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TodayStats(
    val todayRevenue: Double = 0.0,
    val todayOrders: Int = 0,
    val rating: Double = 4.8,
    val reviewsCount: Int = 135,
    val isShiftActive: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataRepository
    private val prefsManager = PreferencesManager.getInstance(application)
    
    private val _statistics = MutableStateFlow(Statistics(0, 0, 0, 0.0))
    val statistics: StateFlow<Statistics> = _statistics.asStateFlow()
    
    private val _todayStats = MutableStateFlow(TodayStats())
    val todayStats: StateFlow<TodayStats> = _todayStats.asStateFlow()
    
    private val _weeklyRevenue = MutableStateFlow<List<Double>>(emptyList())
    val weeklyRevenue: StateFlow<List<Double>> = _weeklyRevenue.asStateFlow()
    
    private val _news = MutableStateFlow<List<News>>(emptyList())
    val news: StateFlow<List<News>> = _news.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Загружаем статистику
            _statistics.value = repository.getStatistics()
            
            // Загружаем данные "Сегодня"
            loadTodayStats()
            
            // Загружаем доходы за неделю
            _weeklyRevenue.value = repository.getWeeklyRevenue()
            
            // Загружаем новости
            repository.news.collect { newsList ->
                _news.value = newsList
            }
        }
    }
    
    private fun loadTodayStats() {
        viewModelScope.launch {
            // Получаем статус смены
            val isShiftActive = prefsManager.isShiftActive()
            
            // TODO: Загрузить реальные данные из API
            // Пока используем демо-данные
            val todayRevenue = calculateTodayRevenue()
            val todayOrders = calculateTodayOrders()
            
            _todayStats.value = TodayStats(
                todayRevenue = todayRevenue,
                todayOrders = todayOrders,
                rating = 4.8,
                reviewsCount = 135,
                isShiftActive = isShiftActive
            )
        }
    }
    
    private suspend fun calculateTodayRevenue(): Double {
        // TODO: Реальная логика расчета дохода за сегодня
        // Пока возвращаем демо-значение
        return 5200.0
    }
    
    private suspend fun calculateTodayOrders(): Int {
        // TODO: Реальная логика подсчета заказов за сегодня
        // Пока возвращаем демо-значение
        return 8
    }
    
    fun toggleShift() {
        viewModelScope.launch {
            val currentStatus = prefsManager.isShiftActive()
            prefsManager.setShiftActive(!currentStatus)
            _todayStats.value = _todayStats.value.copy(isShiftActive = !currentStatus)
        }
    }
    
    fun updateAutoAcceptSettings(settings: com.example.bestapp.data.AutoAcceptSettings) {
        // Сохранение уже выполнено в Fragment, здесь можно добавить дополнительную логику
        // Например, уведомление других компонентов об изменении настроек
    }
}
