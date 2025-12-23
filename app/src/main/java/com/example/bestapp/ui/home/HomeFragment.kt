package com.example.bestapp.ui.home

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {
    
    private val viewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(requireContext().applicationContext as Application) as T
            }
        }
    }
    private lateinit var newsAdapter: NewsAdapter
    
    private var recyclerNews: RecyclerView? = null
    private var weeklyChart: LineChart? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }
    
    private fun setupViews() {
        // Настройка RecyclerView для новостей
        recyclerNews = view?.findViewById(R.id.recycler_news)
        newsAdapter = NewsAdapter { news ->
            Toast.makeText(context, news.title, Toast.LENGTH_SHORT).show()
        }
        
        recyclerNews?.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        // Настройка графика
        weeklyChart = view?.findViewById(R.id.weekly_chart)
        setupWeeklyChart()
    }
    
    private fun setupWeeklyChart() {
        weeklyChart?.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            setDragEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            
            // Настройка осей
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.WHITE
                textSize = 10f
                labelCount = 7
                setLabelCount(7, true)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#33FFFFFF")
                axisMinimum = 0f
                textColor = Color.WHITE
                textSize = 10f
                setDrawAxisLine(false)
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
            
            // Прозрачный фон
            setBackgroundColor(Color.TRANSPARENT)
        }
    }
    
    private fun updateWeeklyChart(weeklyRevenue: List<Double>) {
        if (weeklyRevenue.isEmpty()) return
        
        val entries = weeklyRevenue.mapIndexed { index, revenue ->
            Entry(index.toFloat(), revenue.toFloat())
        }
        
        val dataSet = LineDataSet(entries, "").apply {
            color = Color.WHITE
            lineWidth = 2f
            setCircleColor(Color.WHITE)
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(true)
            fillColor = Color.parseColor("#33FFFFFF")
            fillAlpha = 255
        }
        
        val lineData = LineData(dataSet)
        weeklyChart?.data = lineData
        
        // Форматирование подписей на оси X (дни недели)
        val dayLabels = getDayLabels()
        weeklyChart?.xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < dayLabels.size) {
                    dayLabels[index]
                } else {
                    ""
                }
            }
        }
        
        weeklyChart?.invalidate()
    }
    
    private fun getDayLabels(): List<String> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("E", Locale.getDefault())
        val labels = mutableListOf<String>()
        
        for (i in 6 downTo 0) {
            calendar.time = java.util.Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayName = dateFormat.format(calendar.time)
            labels.add(dayName.substring(0, 1).uppercase()) // Первая буква дня недели
        }
        
        return labels
    }
    
    private fun observeData() {
        // Наблюдаем за статистикой
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statistics.collect { stats ->
                updateStatistics(stats)
            }
        }
        
        // Наблюдаем за данными "Сегодня"
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayStats.collect { todayStats ->
                updateTodayCard(todayStats)
            }
        }
        
        // Наблюдаем за доходами за неделю
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weeklyRevenue.collect { weeklyRevenue ->
                updateWeeklyChart(weeklyRevenue)
            }
        }
        
        // Наблюдаем за новостями
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.news.collect { newsList ->
                newsAdapter.submitList(newsList)
            }
        }
    }
    
    private fun updateTodayCard(todayStats: com.example.bestapp.ui.home.TodayStats) {
        view?.apply {
            // Доход
            findViewById<TextView>(R.id.today_revenue)?.text = 
                String.format(java.util.Locale.getDefault(), "%.0f₽", todayStats.todayRevenue)
            
            // Заказы
            findViewById<TextView>(R.id.today_orders)?.text = todayStats.todayOrders.toString()
            
            // Рейтинг
            findViewById<TextView>(R.id.today_rating)?.text = 
                String.format(java.util.Locale.getDefault(), "%.1f", todayStats.rating)
            
            // Количество отзывов
            findViewById<TextView>(R.id.today_reviews)?.text = "(${todayStats.reviewsCount})"
            
            // Статус смены (только отображение, без возможности переключения)
            val statusText = findViewById<TextView>(R.id.today_status)
            val statusIndicator = findViewById<View>(R.id.status_indicator)
            
            if (todayStats.isShiftActive) {
                statusText?.text = "Примите заявку"
                statusIndicator?.setBackgroundResource(R.drawable.circle_green)
            } else {
                statusText?.text = "Не на смене"
                statusIndicator?.setBackgroundResource(R.drawable.circle_red)
            }
            
            // Настройка автоприема
            setupAutoAccept()
        }
    }
    
    private fun setupAutoAccept() {
        val switchAutoAccept = view?.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_auto_accept)
        val btnSettings = view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_auto_accept_settings)
        val statusText = view?.findViewById<TextView>(R.id.auto_accept_status)
        
        // Загружаем текущие настройки
        val prefsManager = com.example.bestapp.data.PreferencesManager.getInstance(requireContext())
        val settings = prefsManager.getAutoAcceptSettings()
        
        switchAutoAccept?.isChecked = settings.isEnabled
        updateAutoAcceptStatus(settings, statusText)
        
        switchAutoAccept?.setOnCheckedChangeListener { _, isChecked ->
            val newSettings = settings.copy(isEnabled = isChecked)
            prefsManager.setAutoAcceptSettings(newSettings)
            updateAutoAcceptStatus(newSettings, statusText)
            viewModel.updateAutoAcceptSettings(newSettings)
        }
        
        btnSettings?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_auto_accept_settings)
        }
        
        // Кнопка зон работы
        view?.findViewById<View>(R.id.btn_work_zones)?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_work_zones)
        }
        
        // Обновляем статус зон
        updateZonesStatus()
    }
    
    private fun updateZonesStatus() {
        val prefsManager = com.example.bestapp.data.PreferencesManager.getInstance(requireContext())
        val zones = prefsManager.getWorkZones()
        val activeZones = zones.count { zone -> zone.isActive }
        
        val statusText = view?.findViewById<TextView>(R.id.zones_status)
        statusText?.text = if (zones.isEmpty()) {
            "Настроить зоны"
        } else {
            "$activeZones из ${zones.size} активны"
        }
    }
    
    private fun updateAutoAcceptStatus(settings: com.example.bestapp.data.AutoAcceptSettings, statusText: TextView?) {
        if (!settings.isEnabled) {
            statusText?.text = "Выключен"
            return
        }
        
        val conditions = mutableListOf<String>()
        settings.minPrice?.let { conditions.add("от ${it.toInt()}₽") }
        settings.maxDistance?.let { conditions.add("до ${(it / 1000).toInt()}км") }
        if (settings.acceptUrgentOnly) {
            conditions.add("только срочные")
        }
        
        statusText?.text = if (conditions.isEmpty()) {
            "Все заказы"
        } else {
            conditions.joinToString(", ")
        }
    }
    
    private fun updateStatistics(stats: com.example.bestapp.data.Statistics) {
        // Активные заказы
        view?.findViewById<View>(R.id.stat_active_orders)?.apply {
            findViewById<TextView>(R.id.stat_value)?.text = stats.activeOrdersCount.toString()
            findViewById<TextView>(R.id.stat_label)?.text = getString(R.string.home_active_orders)
        }
        
        // Новые заказы
        view?.findViewById<View>(R.id.stat_new_orders)?.apply {
            findViewById<TextView>(R.id.stat_value)?.text = stats.newOrdersCount.toString()
            findViewById<TextView>(R.id.stat_label)?.text = getString(R.string.home_new_orders)
        }
        
        // Клиенты
        view?.findViewById<View>(R.id.stat_clients)?.apply {
            findViewById<TextView>(R.id.stat_value)?.text = stats.clientsCount.toString()
            findViewById<TextView>(R.id.stat_label)?.text = getString(R.string.home_clients)
        }
        
        // Доход за месяц
        view?.findViewById<View>(R.id.stat_revenue)?.apply {
            findViewById<TextView>(R.id.stat_value)?.text = 
                String.format(Locale.getDefault(), "%.0f₽", stats.monthlyRevenue)
            findViewById<TextView>(R.id.stat_label)?.text = getString(R.string.home_monthly_revenue)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        recyclerNews = null
        weeklyChart = null
    }
}
