package com.example.bestapp.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.RetrofitClient
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
    val rating: Double = 0.0,
    val reviewsCount: Int = 0,
    val isShiftActive: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataRepository
    private val apiRepository = ApiRepository()
    private val prefsManager = PreferencesManager.getInstance(application)
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _statistics = MutableStateFlow(Statistics(0, 0, 0, 0.0))
    val statistics: StateFlow<Statistics> = _statistics.asStateFlow()
    
    private val _todayStats = MutableStateFlow(
        TodayStats(isShiftActive = prefsManager.isShiftActive())
    )
    val todayStats: StateFlow<TodayStats> = _todayStats.asStateFlow()
    
    private val _weeklyRevenue = MutableStateFlow<List<Double>>(emptyList())
    val weeklyRevenue: StateFlow<List<Double>> = _weeklyRevenue.asStateFlow()
    
    private val _news = MutableStateFlow<List<News>>(emptyList())
    val news: StateFlow<List<News>> = _news.asStateFlow()
    
    init {
        // Инициализируем RetrofitClient
        RetrofitClient.initialize(application)
        Log.d(TAG, "HomeViewModel init: isShiftActive=${prefsManager.isShiftActive()}")
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Загружаем статистику мастера через API
            loadMasterStats()
            
            // Загружаем новости через API
            loadNewsFromApi()
        }
    }
    
    fun refreshNews() {
        viewModelScope.launch {
            loadNewsFromApi()
        }
    }
    
    private suspend fun loadNewsFromApi() {
        try {
            Log.d(TAG, "🔄 Загрузка новостей из API...")
            val result = apiRepository.getNews()
            result.onSuccess { apiNewsList ->
                val newsList = apiNewsList.map { apiNews ->
                    com.example.bestapp.data.News(
                        id = apiNews.id,
                        title = apiNews.title,
                        summary = apiNews.summary ?: "",
                        content = apiNews.content,
                        category = when (apiNews.category?.lowercase()) {
                            "tips" -> com.example.bestapp.data.NewsCategory.TIPS
                            "industry" -> com.example.bestapp.data.NewsCategory.INDUSTRY
                            "guides" -> com.example.bestapp.data.NewsCategory.GUIDES
                            "tools" -> com.example.bestapp.data.NewsCategory.TOOLS
                            "trends" -> com.example.bestapp.data.NewsCategory.TRENDS
                            else -> com.example.bestapp.data.NewsCategory.TIPS
                        },
                        publishedAt = try {
                            // Парсим ISO дату с сервера
                            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }.parse(apiNews.publishedAt) ?: java.util.Date()
                        } catch (e: Exception) {
                            try {
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).parse(apiNews.publishedAt) ?: java.util.Date()
                            } catch (e2: Exception) {
                                java.util.Date()
                            }
                        }
                    )
                }
                _news.value = newsList
                Log.d(TAG, "✅ Loaded ${newsList.size} news from API")
            }.onFailure { error ->
                Log.e(TAG, "❌ Failed to load news from API: ${error.message}")
                // Если API не сработало, пытаемся загрузить из репозитория (моки)
                repository.news.collect { newsList ->
                    _news.value = newsList
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading news from API", e)
        }
    }
    
    private suspend fun loadMasterStats() {
        try {
            val result = apiRepository.getMasterStats()
            result.onSuccess { response ->
                val masterData = response["master"] as? Map<*, *>
                val statsData = response["stats"] as? Map<*, *>
                
                // Обновляем статистику
                statsData?.let { stats ->
                    val activeOrders = (stats["inProgressOrders"] as? Number)?.toInt() ?: 0
                    val newOrders = (stats["newOrders"] as? Number)?.toInt() ?: 0
                    val clientsCount = (stats["clientsCount"] as? Number)?.toInt() ?: 0
                    val monthlyRevenue = (stats["monthlyRevenue"] as? Number)?.toDouble() ?: 0.0
                    
                    _statistics.value = Statistics(
                        activeOrdersCount = activeOrders,
                        newOrdersCount = newOrders,
                        clientsCount = clientsCount,
                        monthlyRevenue = monthlyRevenue
                    )
                    
                    // Обновляем данные "Сегодня"
                    val todayRevenue = (stats["todayRevenue"] as? Number)?.toDouble() ?: 0.0
                    val todayOrders = (stats["todayOrders"] as? Number)?.toInt() ?: 0
                    val rating = (stats["averageRating"] as? Number)?.toDouble() ?: 0.0
                    val reviewsCount = (stats["reviewsCount"] as? Number)?.toInt() ?: 0
                    // Используем локальный статус смены из prefsManager, а не с сервера
                    // чтобы не перезаписывать статус, который был изменен локально
                    val isShiftActive = prefsManager.isShiftActive()
                    Log.d(TAG, "loadMasterStats: Updating stats, isShiftActive=$isShiftActive (from prefsManager)")
                    
                    // Обновляем только если статус действительно изменился, чтобы не перезаписывать оптимистичное обновление
                    _todayStats.value = _todayStats.value.copy(
                        todayRevenue = todayRevenue,
                        todayOrders = todayOrders,
                        rating = rating,
                        reviewsCount = reviewsCount,
                        isShiftActive = isShiftActive
                    )
                    
                    // Обновляем доходы за неделю
                    val weeklyRevenueData = stats["weeklyRevenue"] as? List<*>
                    if (weeklyRevenueData != null) {
                        _weeklyRevenue.value = weeklyRevenueData.mapNotNull { 
                            (it as? Number)?.toDouble() ?: 0.0 
                        }
                    }
                    
                    Log.d(TAG, "✅ Master stats loaded: todayRevenue=$todayRevenue, todayOrders=$todayOrders, rating=$rating")
                }
            }.onFailure { error ->
                Log.e(TAG, "❌ Failed to load master stats: ${error.message}")
                // В случае ошибки используем демо-данные
                val isShiftActive = prefsManager.isShiftActive()
                _todayStats.value = TodayStats(
                    todayRevenue = 0.0,
                    todayOrders = 0,
                    rating = 0.0,
                    reviewsCount = 0,
                    isShiftActive = isShiftActive
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading master stats", e)
        }
    }
    
    fun toggleShift() {
        viewModelScope.launch {
            val currentStatus = prefsManager.isShiftActive()
            val newStatus = !currentStatus
            
            Log.d(TAG, "toggleShift: currentStatus=$currentStatus, newStatus=$newStatus")
            
            // Оптимистичное обновление UI сразу
            _todayStats.value = _todayStats.value.copy(isShiftActive = newStatus)
            prefsManager.setShiftActive(newStatus)
            
            Log.d(TAG, "toggleShift: Updated _todayStats.isShiftActive=${_todayStats.value.isShiftActive}, prefsManager.isShiftActive=${prefsManager.isShiftActive()}")
            
            try {
                if (newStatus) {
                    // Начинаем смену (используем дефолтные координаты, можно улучшить позже)
                    Log.d(TAG, "toggleShift: Calling startShift API...")
                    val result = apiRepository.startShift(0.0, 0.0)
                    result.onSuccess {
                        Log.d(TAG, "✅ Shift started successfully, status=${prefsManager.isShiftActive()}")
                        // Не перезагружаем данные сразу, чтобы не перезаписать статус
                        // Статус уже обновлен оптимистично и сохранен в prefsManager
                    }.onFailure { error ->
                        // Откатываем изменения при ошибке
                        Log.e(TAG, "❌ Failed to start shift: ${error.message}, rolling back...")
                        _todayStats.value = _todayStats.value.copy(isShiftActive = currentStatus)
                        prefsManager.setShiftActive(currentStatus)
                    }
                } else {
                    // Заканчиваем смену
                    Log.d(TAG, "toggleShift: Calling endShift API...")
                    val result = apiRepository.endShift()
                    result.onSuccess {
                        Log.d(TAG, "✅ Shift ended successfully, status=${prefsManager.isShiftActive()}")
                        // Не перезагружаем данные сразу, чтобы не перезаписать статус
                        // Статус уже обновлен оптимистично и сохранен в prefsManager
                    }.onFailure { error ->
                        // Откатываем изменения при ошибке
                        Log.e(TAG, "❌ Failed to end shift: ${error.message}, rolling back...")
                        _todayStats.value = _todayStats.value.copy(isShiftActive = currentStatus)
                        prefsManager.setShiftActive(currentStatus)
                    }
                }
            } catch (e: Exception) {
                // Откатываем изменения при ошибке
                Log.e(TAG, "Error toggling shift", e)
                _todayStats.value = _todayStats.value.copy(isShiftActive = currentStatus)
                prefsManager.setShiftActive(currentStatus)
            }
        }
    }
}
