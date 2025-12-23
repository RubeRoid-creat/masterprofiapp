package com.example.bestapp.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class StatisticsFragment : Fragment() {
    private val viewModel: StatisticsViewModel by viewModels()
    
    private var toolbar: MaterialToolbar? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null
    private var totalOrdersText: TextView? = null
    private var totalIncomeText: TextView? = null
    private var chipGroupPeriod: ChipGroup? = null
    private var chartIncome: LineChart? = null
    private var chartOrders: BarChart? = null
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"))
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupToolbar()
        setupPeriodFilter()
        observeUiState()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        progressBar = view.findViewById(R.id.progress_loading)
        errorText = view.findViewById(R.id.text_error)
        totalOrdersText = view.findViewById(R.id.text_total_orders)
        totalIncomeText = view.findViewById(R.id.text_total_income)
        chipGroupPeriod = view.findViewById(R.id.chip_group_period)
        chartIncome = view.findViewById(R.id.chart_income)
        chartOrders = view.findViewById(R.id.chart_orders)
        
        setupCharts()
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupPeriodFilter() {
        chipGroupPeriod?.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            when (checkedId) {
                R.id.chip_day -> viewModel.setPeriod("day")
                R.id.chip_week -> viewModel.setPeriod("week")
                R.id.chip_month -> viewModel.setPeriod("month")
                R.id.chip_all -> viewModel.setPeriod("all")
            }
        }
    }
    
    private fun setupCharts() {
        // Настройка графика доходов
        chartIncome?.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
        
        // Настройка графика заказов
        chartOrders?.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                progressBar?.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                state.errorMessage?.let { error ->
                    errorText?.text = error
                    errorText?.visibility = View.VISIBLE
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                } ?: run {
                    errorText?.visibility = View.GONE
                }
                
                state.stats?.let { stats ->
                    updateStatistics(stats)
                }
            }
        }
    }
    
    private fun updateStatistics(stats: Map<String, Any>) {
        val statsData = stats["stats"] as? Map<*, *> ?: return
        
        // Обновляем общие показатели
        val totalOrders = (statsData["totalOrders"] as? Number)?.toInt() ?: 0
        val totalIncome = (statsData["totalIncome"] as? Number)?.toDouble() ?: 0.0
        
        totalOrdersText?.text = totalOrders.toString()
        totalIncomeText?.text = currencyFormat.format(totalIncome)
        
        // Обновляем график доходов
        @Suppress("UNCHECKED_CAST")
        val incomeChartData = statsData["incomeChartData"] as? List<Map<*, *>>
        incomeChartData?.let { data ->
            updateIncomeChart(data)
        }
        
        // Обновляем график заказов
        @Suppress("UNCHECKED_CAST")
        val ordersChartData = statsData["ordersChartData"] as? List<Map<*, *>>
        ordersChartData?.let { data ->
            updateOrdersChart(data)
        }
    }
    
    private fun updateIncomeChart(data: List<Map<*, *>>) {
        val entries = data.mapIndexed { index, item ->
            val value = (item["value"] as? Number)?.toFloat() ?: 0f
            Entry(index.toFloat(), value)
        }
        
        val dataSet = LineDataSet(entries, "Доход").apply {
            color = Color.parseColor("#4CAF50")
            valueTextColor = Color.BLACK
            lineWidth = 2f
            setCircleColor(Color.parseColor("#4CAF50"))
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
        }
        
        val lineData = LineData(dataSet)
        chartIncome?.data = lineData
        
        // Настройка подписей оси X
        val labels = data.map { (it["label"] as? String) ?: "" }
        chartIncome?.xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) {
                    labels[index]
                } else {
                    ""
                }
            }
        }
        
        chartIncome?.invalidate()
    }
    
    private fun updateOrdersChart(data: List<Map<*, *>>) {
        val entries = data.mapIndexed { index, item ->
            val value = (item["value"] as? Number)?.toFloat() ?: 0f
            BarEntry(index.toFloat(), value)
        }
        
        val dataSet = BarDataSet(entries, "Заказы").apply {
            color = Color.parseColor("#2196F3")
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }
        
        val barData = BarData(dataSet)
        barData.barWidth = 0.5f
        chartOrders?.data = barData
        
        // Настройка подписей оси X
        val labels = data.map { (it["label"] as? String) ?: "" }
        chartOrders?.xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) {
                    labels[index]
                } else {
                    ""
                }
            }
        }
        
        chartOrders?.invalidate()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

