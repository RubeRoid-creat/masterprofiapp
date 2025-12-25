package com.example.bestapp.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.data.Order
import com.example.bestapp.data.RepairStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks

class OrdersFragment : Fragment() {
    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var adapter: OrdersAdapter
    
    private var recyclerView: RecyclerView? = null
    private var searchBar: TextInputEditText? = null
    private var emptyTextView: TextView? = null
    private var chipGroupDeviceType: ChipGroup? = null
    private var chipGroupUrgency: ChipGroup? = null
    private var chipGroupSort: ChipGroup? = null
    private var filterMinPrice: TextInputEditText? = null
    private var filterMaxPrice: TextInputEditText? = null
    private var filterMaxDistance: TextInputEditText? = null
    private var btnShowMap: MaterialButton? = null
    private var btnFilters: MaterialButton? = null
    private var shiftCard: View? = null
    private var shiftStatusText: TextView? = null
    private var shiftStatusHint: TextView? = null
    private var btnAcceptSelected: MaterialButton? = null
    private var btnOptimizeRoute: MaterialButton? = null
    private var btnCancelSelection: MaterialButton? = null
    private var tabLayout: TabLayout? = null
    private var recyclerCompletedOrders: RecyclerView? = null
    private lateinit var completedOrdersAdapter: OrdersAdapter
    private var filtersBottomSheet: com.google.android.material.bottomsheet.BottomSheetDialog? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_orders, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recycler_orders)
        searchBar = view.findViewById(R.id.search_bar)
        emptyTextView = view.findViewById(R.id.text_empty)
        chipGroupDeviceType = null
        chipGroupUrgency = null
        chipGroupSort = null
        filterMinPrice = null
        filterMaxPrice = null
        filterMaxDistance = null
        btnShowMap = view.findViewById(R.id.btn_show_map)
        btnFilters = null
        shiftCard = view.findViewById(R.id.shift_card)
        shiftStatusText = view.findViewById(R.id.shift_status_text)
        shiftStatusHint = view.findViewById(R.id.shift_status_hint)
        tabLayout = view.findViewById(R.id.tab_layout)
        recyclerCompletedOrders = view.findViewById(R.id.recycler_completed_orders)
        
        setupRecyclerView()
        setupCompletedOrdersRecyclerView()
        setupMapButton()
        setupShiftCard()
        setupBatchActions()
        setupTabs()
        observeOrders()
        observeCompletedOrders()
        observeVerificationStatus()
        
        // Инициализируем карточку смены с текущим статусом
        updateShiftCard(viewModel.isShiftActive.value)
        
        // Всегда загружаем заявки при открытии экрана
        android.util.Log.d("OrdersFragment", "onViewCreated: refreshing orders...")
        viewModel.refreshOrders()
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем список заказов при возврате на экран (например, после принятия заказа)
        android.util.Log.d("OrdersFragment", "onResume: refreshing orders...")
        viewModel.refreshOrders()
        
        // Если мастер на смене, автоматическое обновление уже работает через ViewModel
        // Здесь просто убеждаемся, что заявки актуальны
    }
    
    override fun onPause() {
        super.onPause()
        // ViewModel сам управляет polling, не нужно останавливать здесь
        // Автоматическое обновление продолжится, если мастер на смене
    }
    
    private fun setupMapButton() {
        btnShowMap?.setOnClickListener {
            findNavController().navigate(R.id.action_orders_to_orders_map)
        }
    }
    
    private fun setupFiltersButton() {
        // Фильтры убраны
    }
    
    private fun showFiltersBottomSheet() {
        android.util.Log.d("OrdersFragment", "Открываем BottomSheet фильтров")
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_filters, null)
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        bottomSheet.setContentView(bottomSheetView)
        filtersBottomSheet = bottomSheet
        
        // Устанавливаем поведение для полного раскрытия
        val behavior = bottomSheet.behavior
        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = true
        
        // Находим элементы в BottomSheet
        val chipGroupDeviceType = bottomSheetView.findViewById<ChipGroup>(R.id.chip_group_device_type)
        val chipGroupUrgency = bottomSheetView.findViewById<ChipGroup>(R.id.chip_group_urgency)
        val filterMinPriceSheet = bottomSheetView.findViewById<TextInputEditText>(R.id.filter_min_price)
        val filterMaxPriceSheet = bottomSheetView.findViewById<TextInputEditText>(R.id.filter_max_price)
        val filterMaxDistanceSheet = bottomSheetView.findViewById<TextInputEditText>(R.id.filter_max_distance)
        val btnClearFilters = bottomSheetView.findViewById<MaterialButton>(R.id.btn_clear_filters)
        val btnApplyFilters = bottomSheetView.findViewById<MaterialButton>(R.id.btn_apply_filters)
        
        // Восстанавливаем текущие значения фильтров из ViewModel
        val currentFilters = viewModel.getCurrentFilters()
        
        // Восстанавливаем типы устройств
        currentFilters.deviceTypes.forEach { deviceType ->
            val chipId = when (deviceType) {
                "Стиральная машина" -> R.id.chip_washing_machines
                "Посудомоечная машина" -> R.id.chip_dishwashers
                "Духовой шкаф" -> R.id.chip_ovens
                "Холодильник" -> R.id.chip_refrigerators
                "Микроволновая печь" -> R.id.chip_microwaves
                "Морозильный ларь" -> R.id.chip_freezers
                "Варочная панель" -> R.id.chip_cooktops
                "Ноутбук" -> R.id.chip_laptops
                "Десктоп" -> R.id.chip_desktops
                "Кофемашина" -> R.id.chip_coffee_machines
                "Кондиционер" -> R.id.chip_air_conditioners
                "Водонагреватель" -> R.id.chip_water_heaters
                else -> null
            }
            chipId?.let { chipGroupDeviceType?.check(it) }
        }
        
        // Восстанавливаем цену
        currentFilters.minPrice?.let { 
            filterMinPriceSheet?.setText(it.toInt().toString())
        }
        currentFilters.maxPrice?.let { 
            filterMaxPriceSheet?.setText(it.toInt().toString())
        }
        
        // Восстанавливаем расстояние (конвертируем из метров в км)
        currentFilters.maxDistance?.let { 
            filterMaxDistanceSheet?.setText((it / 1000).toString())
        }
        
        // Восстанавливаем срочность
        currentFilters.urgency?.let { urgency ->
            val chipId = when (urgency) {
                "emergency" -> R.id.chip_urgency_emergency
                "urgent" -> R.id.chip_urgency_urgent
                "planned" -> R.id.chip_urgency_planned
                else -> null
            }
            chipId?.let { chipGroupUrgency?.check(it) }
        }
        
        // Настройка фильтров по типу техники
        chipGroupDeviceType?.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedTypes = checkedIds.mapNotNull { id ->
                when (id) {
                    R.id.chip_washing_machines -> "washing_machine"
                    R.id.chip_dishwashers -> "dishwasher"
                    R.id.chip_ovens -> "oven"
                    R.id.chip_refrigerators -> "refrigerator"
                    R.id.chip_microwaves -> "microwave"
                    R.id.chip_freezers -> "freezer"
                    R.id.chip_cooktops -> "cooktop"
                    R.id.chip_laptops -> "laptop"
                    R.id.chip_desktops -> "desktop"
                    R.id.chip_coffee_machines -> "coffee_machine"
                    R.id.chip_air_conditioners -> "air_conditioner"
                    R.id.chip_water_heaters -> "water_heater"
                    else -> null
                }
            }
            // Сохраняем выбранные типы (применятся при нажатии "Применить")
        }
        
        // Настройка фильтров по срочности
        chipGroupUrgency?.setOnCheckedStateChangeListener { group, checkedId ->
            // Сохраняем выбранную срочность
        }
        
        // Кнопка сброса фильтров
        btnClearFilters?.setOnClickListener {
            chipGroupDeviceType?.clearCheck()
            chipGroupUrgency?.clearCheck()
            filterMinPriceSheet?.text?.clear()
            filterMaxPriceSheet?.text?.clear()
            filterMaxDistanceSheet?.text?.clear()
            
            // Очищаем фильтры в ViewModel и сохраняем
            viewModel.setDeviceTypeFilter(emptySet())
            viewModel.setPriceFilter(null, null)
            viewModel.setMaxDistanceFilter(null)
            viewModel.setUrgencyFilter(null)
            
            // Применяем изменения сразу
            bottomSheet.dismiss()
        }
        
        // Кнопка применения фильтров
        btnApplyFilters?.setOnClickListener {
            android.util.Log.d("OrdersFragment", "Кнопка 'Применить' нажата")
            
            // Применяем фильтры
            val minPrice = filterMinPriceSheet?.text?.toString()?.toIntOrNull()
            val maxPrice = filterMaxPriceSheet?.text?.toString()?.toIntOrNull()
            val maxDistance = filterMaxDistanceSheet?.text?.toString()?.toDoubleOrNull()
            
            android.util.Log.d("OrdersFragment", "Фильтры: minPrice=$minPrice, maxPrice=$maxPrice, maxDistance=$maxDistance")
            
            // Применяем фильтры по цене и расстоянию
            viewModel.setPriceFilter(minPrice?.toDouble(), maxPrice?.toDouble())
            maxDistance?.let { 
                val maxDistanceM = it * 1000 // Конвертируем км в метры
                viewModel.setMaxDistanceFilter(maxDistanceM)
            } ?: viewModel.setMaxDistanceFilter(null)
            
            // Получаем выбранные типы техники
            val selectedTypes = chipGroupDeviceType?.checkedChipIds?.mapNotNull { id ->
                when (id) {
                    R.id.chip_washing_machines -> "Стиральная машина"
                    R.id.chip_dishwashers -> "Посудомоечная машина"
                    R.id.chip_ovens -> "Духовой шкаф"
                    R.id.chip_refrigerators -> "Холодильник"
                    R.id.chip_microwaves -> "Микроволновая печь"
                    R.id.chip_freezers -> "Морозильный ларь"
                    R.id.chip_cooktops -> "Варочная панель"
                    R.id.chip_laptops -> "Ноутбук"
                    R.id.chip_desktops -> "Десктоп"
                    R.id.chip_coffee_machines -> "Кофемашина"
                    R.id.chip_air_conditioners -> "Кондиционер"
                    R.id.chip_water_heaters -> "Водонагреватель"
                    else -> null
                }
            }?.toSet() ?: emptySet()
            
            android.util.Log.d("OrdersFragment", "Выбранные типы устройств: $selectedTypes")
            viewModel.setDeviceTypeFilter(selectedTypes)
            
            // Получаем выбранную срочность
            chipGroupUrgency?.checkedChipId?.let { id ->
                val urgency = when (id) {
                    R.id.chip_urgency_emergency -> "emergency"
                    R.id.chip_urgency_urgent -> "urgent"
                    R.id.chip_urgency_planned -> "planned"
                    else -> null
                }
                android.util.Log.d("OrdersFragment", "Выбранная срочность: $urgency")
                urgency?.let { viewModel.setUrgencyFilter(it) }
            } ?: viewModel.setUrgencyFilter(null)
            
            android.util.Log.d("OrdersFragment", "Фильтры применены, закрываем BottomSheet")
            bottomSheet.dismiss()
        }
        
        // Проверяем, что кнопка найдена
        if (btnApplyFilters == null) {
            android.util.Log.e("OrdersFragment", "Кнопка btn_apply_filters не найдена!")
        } else {
            android.util.Log.d("OrdersFragment", "Кнопка btn_apply_filters найдена и настроена")
        }
        
        bottomSheet.show()
    }
    
    private fun setupShiftCard() {
        shiftCard?.setOnClickListener {
            android.util.Log.d("OrdersFragment", "Shift card clicked!")
            val currentStatus = viewModel.isShiftActive.value
            android.util.Log.d("OrdersFragment", "Current shift status: $currentStatus")
            
            // Вызываем toggleShift - он сам обновит UI через StateFlow
            viewModel.toggleShift()
        }
        
        // Убеждаемся, что карточка кликабельна
        shiftCard?.isClickable = true
        shiftCard?.isFocusable = true
        android.util.Log.d("OrdersFragment", "Shift card setup complete, clickable=${shiftCard?.isClickable}")
    }
    
    private fun setupRecyclerView() {
        adapter = OrdersAdapter(
            onOrderClick = { order ->
                // Открываем детали заказа
                val bundle = Bundle().apply {
                    putLong("orderId", order.id)
                    // Передаем assignmentId, если есть
                    order.assignmentId?.let { assignmentId ->
                        putLong("assignmentId", assignmentId)
                        android.util.Log.d("OrdersFragment", "Passing assignmentId=$assignmentId to OrderDetailsFragment")
                    }
                    order.assignmentStatus?.let { status ->
                        putString("assignmentStatus", status)
                        android.util.Log.d("OrdersFragment", "Passing assignmentStatus=$status to OrderDetailsFragment")
                    }
                }
                findNavController().navigate(R.id.action_orders_to_order_details, bundle)
            },
            onOrderSelected = { order, selected ->
                // Используем post для безопасного обновления UI из callback
                view?.post {
                    try {
                        if (selected && !adapter.isSelectionMode) {
                            enterSelectionMode()
                        }
                        // Обновляем адаптер, чтобы показать чекбоксы
                        adapter.notifyDataSetChanged()
                        updateBatchActions()
                    } catch (e: Exception) {
                        android.util.Log.e("OrdersFragment", "Ошибка в onOrderSelected", e)
                    }
                }
            },
            onAcceptOrder = { order ->
                acceptOrder(order)
            },
            onRejectOrder = { order ->
                rejectOrder(order)
            }
        )
        
        recyclerView?.apply {
            this.adapter = this@OrdersFragment.adapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun updateBatchActions() {
        val selectedCount = adapter.getSelectedOrders().size
        btnAcceptSelected?.text = "Принять выбранные ($selectedCount)"
        btnAcceptSelected?.isEnabled = selectedCount > 0
        btnOptimizeRoute?.isEnabled = selectedCount >= 2 // Минимум 2 заказа для маршрута
    }
    
    private fun setupBatchActions() {
        btnAcceptSelected = view?.findViewById(R.id.btn_accept_selected)
        btnOptimizeRoute = view?.findViewById(R.id.btn_optimize_route)
        btnCancelSelection = view?.findViewById(R.id.btn_cancel_selection)
        
        btnAcceptSelected?.setOnClickListener {
            acceptSelectedOrders()
        }
        
        btnOptimizeRoute?.setOnClickListener {
            optimizeRouteForSelected()
        }
        
        btnCancelSelection?.setOnClickListener {
            exitSelectionMode()
        }
    }
    
    private fun exitSelectionMode() {
        adapter.isSelectionMode = false
        adapter.clearSelection()
        view?.findViewById<View>(R.id.batch_actions_container)?.visibility = View.GONE
    }
    
    private fun enterSelectionMode() {
        try {
            adapter.isSelectionMode = true
            view?.findViewById<View>(R.id.batch_actions_container)?.visibility = View.VISIBLE
            updateBatchActions()
        } catch (e: Exception) {
            android.util.Log.e("OrdersFragment", "Ошибка при входе в режим выбора", e)
        }
    }
    
    private fun acceptOrder(order: com.example.bestapp.data.Order) {
        val assignmentId = order.assignmentId
        if (assignmentId == null) {
            android.widget.Toast.makeText(
                context,
                "Не найдено назначение для этой заявки",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Проверяем статус верификации
        if (viewModel.isVerified.value == false) {
            showVerificationDialog("Для принятия заказов необходимо пройти верификацию. Пожалуйста, перейдите в профиль и пройдите верификацию.")
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = viewModel.apiRepository.acceptAssignment(assignmentId)
                result.onSuccess {
                    android.widget.Toast.makeText(
                        context,
                        "Заявка принята",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    // Обновляем список заявок
                    viewModel.refreshOrders()
                }.onFailure { error ->
                    android.widget.Toast.makeText(
                        context,
                        "Ошибка при принятии заявки: ${error.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    android.util.Log.e("OrdersFragment", "Failed to accept order", error)
                }
            } catch (e: Exception) {
                android.util.Log.e("OrdersFragment", "Exception accepting order", e)
                android.widget.Toast.makeText(
                    context,
                    "Ошибка: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun rejectOrder(order: com.example.bestapp.data.Order) {
        val assignmentId = order.assignmentId
        if (assignmentId == null) {
            android.widget.Toast.makeText(
                context,
                "Не найдено назначение для этой заявки",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Показываем диалог для указания причины отклонения
        val inputEditText = android.widget.EditText(requireContext()).apply {
            hint = "Причина отклонения (необязательно)"
            setPadding(32, 16, 32, 16)
        }
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Отклонить заявку")
            .setMessage("Укажите причину отклонения (необязательно)")
            .setView(inputEditText)
            .setPositiveButton("Отклонить") { _, _ ->
                val reason = inputEditText.text?.toString()?.takeIf { it.isNotBlank() }
                    ?: "Мастер отклонил заявку"
                
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = viewModel.apiRepository.rejectAssignment(assignmentId, reason)
                        result.onSuccess {
                            android.widget.Toast.makeText(
                                context,
                                "Заявка отклонена",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            // Обновляем список заявок
                            viewModel.refreshOrders()
                        }.onFailure { error ->
                            android.widget.Toast.makeText(
                                context,
                                "Ошибка при отклонении заявки: ${error.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            android.util.Log.e("OrdersFragment", "Failed to reject order", error)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OrdersFragment", "Exception rejecting order", e)
                        android.widget.Toast.makeText(
                            context,
                            "Ошибка: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun acceptSelectedOrders() {
        // Проверяем статус верификации
        if (viewModel.isVerified.value == false) {
            showVerificationDialog("Для принятия заказов необходимо пройти верификацию. Пожалуйста, перейдите в профиль и пройдите верификацию.")
            return
        }
        
        val selectedOrderIds = adapter.getSelectedOrders()
        if (selectedOrderIds.isEmpty()) {
            android.widget.Toast.makeText(
                context,
                "Выберите заказы для принятия",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Получаем assignment IDs для выбранных заказов
            val assignmentIds = mutableListOf<Long>()
            for (orderId in selectedOrderIds) {
                val assignmentResult = viewModel.apiRepository.getActiveAssignmentForOrder(orderId)
                assignmentResult.onSuccess { assignment ->
                    assignment?.id?.let { assignmentIds.add(it) }
                }
            }
            
            if (assignmentIds.isEmpty()) {
                android.widget.Toast.makeText(
                    context,
                    "Не найдены активные назначения для выбранных заказов",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            
            // Принимаем заказы батчем
            val result = viewModel.apiRepository.acceptAssignmentsBatch(assignmentIds)
            result.onSuccess { response ->
                val acceptedCount = response.accepted.size
                val errorsCount = response.errors.size
                
                android.widget.Toast.makeText(
                    context,
                    "Принято заказов: $acceptedCount${if (errorsCount > 0) ", ошибок: $errorsCount" else ""}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                
                // Выходим из режима выбора и обновляем список
                exitSelectionMode()
                viewModel.refreshOrders()
            }.onFailure { error ->
                val errorMessage = error.message ?: "Ошибка принятия заказов"
                
                // Проверяем, не связана ли ошибка с верификацией
                if (errorMessage.contains("верификац", ignoreCase = true) || 
                    errorMessage.contains("verification", ignoreCase = true)) {
                    showVerificationDialog(errorMessage)
                } else {
                    android.widget.Toast.makeText(
                        context,
                        errorMessage,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun optimizeRouteForSelected() {
        val selectedOrderIds = adapter.getSelectedOrders()
        if (selectedOrderIds.size < 2) {
            android.widget.Toast.makeText(
                context,
                "Выберите минимум 2 заказа для оптимизации маршрута",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Получаем текущее местоположение мастера
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val locationTask = LocationServices
                    .getFusedLocationProviderClient(requireActivity())
                    .lastLocation
                
                val location = try {
                    com.google.android.gms.tasks.Tasks.await(locationTask)
                } catch (e: Exception) {
                    null
                }
                
                val startLat = location?.latitude
                val startLon = location?.longitude
                
                // Оптимизируем маршрут
                val result = viewModel.apiRepository.optimizeRoute(
                    orderIds = selectedOrderIds.toList(),
                    startLatitude = startLat,
                    startLongitude = startLon
                )
                
                result.onSuccess { optimizedRoute ->
                    // Открываем экран с оптимизированным маршрутом
                    val bundle = Bundle().apply {
                        putLongArray("orderIds", selectedOrderIds.toLongArray())
                        putDouble("totalDistance", optimizedRoute.totalDistance)
                        putInt("totalTime", optimizedRoute.totalTime)
                    }
                    findNavController().navigate(R.id.action_orders_to_optimized_route, bundle)
                }.onFailure { error ->
                    android.widget.Toast.makeText(
                        context,
                        "Ошибка оптимизации маршрута: ${error.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("OrdersFragment", "Error optimizing route", e)
                android.widget.Toast.makeText(
                    context,
                    "Ошибка: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupFilters() {
        // Поиск
        searchBar?.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }
    
    private fun updatePriceFilter() {
        val minPrice = filterMinPrice?.text?.toString()?.toDoubleOrNull()
        val maxPrice = filterMaxPrice?.text?.toString()?.toDoubleOrNull()
        viewModel.setPriceFilter(minPrice, maxPrice)
    }
    
    private fun observeOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredOrders.collectLatest { orders ->
                android.util.Log.d("OrdersFragment", "Orders updated: ${orders.size} orders")
                adapter.submitList(orders)
                updateOrdersVisibility()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isShiftActive.collectLatest { isActive ->
                android.util.Log.d("OrdersFragment", "Shift status changed in StateFlow: $isActive")
                updateShiftCard(isActive)
                updateOrdersVisibility()
                // Если смена стала активной, обновляем заявки
                if (isActive) {
                    android.util.Log.d("OrdersFragment", "Shift became active, refreshing orders...")
                    viewModel.refreshOrders()
                }
            }
        }
    }
    
    private fun observeVerificationStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isVerified.collectLatest { isVerified ->
                android.util.Log.d("OrdersFragment", "Verification status changed: $isVerified")
                updateOrdersVisibility()
                
                // Если мастер не верифицирован, показываем сообщение
                if (isVerified == false) {
                    showVerificationMessage()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.verificationMessage.collectLatest { message ->
                if (message != null && viewModel.isVerified.value == false) {
                    showVerificationDialog(message)
                }
            }
        }
    }
    
    private fun showVerificationMessage() {
        // Обновляем emptyTextView для показа сообщения о верификации
        emptyTextView?.text = "Для просмотра и принятия заказов необходимо пройти верификацию.\n\nПожалуйста, перейдите в профиль и загрузите документы для верификации."
        emptyTextView?.visibility = View.VISIBLE
        
        // Скрываем RecyclerView
        recyclerView?.visibility = View.GONE
    }
    
    private fun showVerificationDialog(message: String) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Требуется верификация")
            .setMessage(message)
            .setPositiveButton("Перейти в профиль") { _, _ ->
                // Переход на экран профиля
                findNavController().navigate(R.id.action_orders_to_profile)
            }
            .setNegativeButton("Отмена", null)
            .setCancelable(false)
            .show()
    }
    
    private fun updateShiftCard(isActive: Boolean) {
        if (isActive) {
            shiftStatusText?.text = "✅ Вы на смене"
            shiftStatusHint?.text = "Нажмите на карточку, чтобы завершить смену"
            shiftCard?.visibility = View.VISIBLE
        } else {
            shiftStatusText?.text = "⏰ Вы не на смене"
            shiftStatusHint?.text = "Нажмите на карточку, чтобы начать принимать заказы"
            shiftCard?.visibility = View.VISIBLE
        }
    }
    
    private fun updateOrdersVisibility() {
        val isShiftActive = viewModel.isShiftActive.value
        val orders = viewModel.filteredOrders.value
        val isNotVerified = viewModel.isVerified.value == false
        
        android.util.Log.d("OrdersFragment", "updateOrdersVisibility: isShiftActive=$isShiftActive, ordersCount=${orders.size}, isNotVerified=$isNotVerified")
        
        // Если мастер не верифицирован, показываем сообщение о верификации
        if (isNotVerified) {
            showVerificationMessage()
            recyclerView?.visibility = View.GONE
            return
        }
        
        if (isShiftActive) {
            // При активной смене показываем заявки
            emptyTextView?.text = "Нет доступных заказов"
            emptyTextView?.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
            recyclerView?.visibility = if (orders.isEmpty()) View.GONE else View.VISIBLE
            android.util.Log.d("OrdersFragment", "Shift active: showing ${orders.size} orders")
        } else {
            // При неактивной смене показываем сообщение
            emptyTextView?.text = "Включите смену, чтобы видеть новые заказы"
            emptyTextView?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
            android.util.Log.d("OrdersFragment", "Shift inactive: hiding orders")
        }
    }
    
    
    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
        searchBar = null
        emptyTextView = null
        chipGroupDeviceType = null
        chipGroupUrgency = null
        chipGroupSort = null
        filterMinPrice = null
        filterMaxPrice = null
        filterMaxDistance = null
        btnShowMap = null
        shiftCard = null
        shiftStatusText = null
        shiftStatusHint = null
        tabLayout = null
        recyclerCompletedOrders = null
    }
    
    private fun setupCompletedOrdersRecyclerView() {
        completedOrdersAdapter = OrdersAdapter(
            onOrderClick = { order ->
                // Открываем детали заказа
                val bundle = Bundle().apply {
                    putLong("orderId", order.id)
                }
                findNavController().navigate(R.id.action_orders_to_order_details, bundle)
            },
            onOrderSelected = null, // Для завершенных заказов не нужен режим выбора
            onAcceptOrder = null, // Для завершенных заказов не нужны кнопки принятия
            onRejectOrder = null // Для завершенных заказов не нужны кнопки отклонения
        )
        
        recyclerCompletedOrders?.apply {
            adapter = completedOrdersAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupTabs() {
        tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Новые заказы
                        recyclerView?.visibility = View.VISIBLE
                        recyclerCompletedOrders?.visibility = View.GONE
                    }
                    1 -> {
                        // Завершенные заказы
                        recyclerView?.visibility = View.GONE
                        recyclerCompletedOrders?.visibility = View.VISIBLE
                        viewModel.loadCompletedOrders()
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun observeCompletedOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedOrders.collectLatest { orders ->
                android.util.Log.d("OrdersFragment", "Completed orders updated: ${orders.size} orders")
                completedOrdersAdapter.submitList(orders)
            }
        }
    }
}
