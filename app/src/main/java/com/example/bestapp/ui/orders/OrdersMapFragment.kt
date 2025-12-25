package com.example.bestapp.ui.orders

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.data.Order
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.android.gms.tasks.Tasks

class OrdersMapFragment : Fragment() {
    
    private val viewModel: OrdersViewModel by viewModels()
    private val apiRepository = ApiRepository()
    
    private var mapView: MapView? = null
    private var orderInfoCard: MaterialCardView? = null
    private var selectedOrderCard: MaterialCardView? = null
    
    // Новые элементы UI
    private var btnBack: FloatingActionButton? = null
    private var tvPickupAddress: TextView? = null
    private var tvDestinationAddress: TextView? = null
    private var tvOrderDevice: TextView? = null
    private var btnAcceptOrder: MaterialButton? = null
    private var btnEditPickup: MaterialButton? = null
    private var btnAddDestination: ImageButton? = null
    private var btnEditOrder: ImageButton? = null
    
    // Старые элементы (для совместимости)
    private var selectedOrderId: TextView? = null
    private var selectedOrderDevice: TextView? = null
    private var selectedOrderAddress: TextView? = null
    private var selectedOrderCost: TextView? = null
    private var btnViewOrder: MaterialButton? = null
    
    private var selectedOrder: Order? = null
    private var isSingleOrderView = false
    private var currentPlacemark: PlacemarkMapObject? = null
    private var masterLocationPlacemark: PlacemarkMapObject? = null
    private var routePolyline: PolylineMapObject? = null
    private val orderPlacemarks = mutableListOf<PlacemarkMapObject>()
    
    // Геолокация
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var masterLatitude: Double = 56.859611 // Тверь по умолчанию
    private var masterLongitude: Double = 35.911896
    
    // Маршрут (упрощенный - простая линия между точками)
    
    
    // Регистрация для запроса разрешений на геолокацию
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_orders_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        initViews(view)
        setupButtons()
        
        // Проверяем разрешения на геолокацию
        checkLocationPermission()
        
        // Проверяем, переданы ли координаты конкретного заказа
        val latitude = arguments?.getDouble("latitude")
        val longitude = arguments?.getDouble("longitude")
        val orderId = arguments?.getLong("orderId")
        
        if (latitude != null && longitude != null) {
            // Открыть карту с конкретным заказом
            isSingleOrderView = true
            setupMapWithOrder(latitude, longitude, orderId)
        } else {
            // Открыть карту со всеми заказами
            isSingleOrderView = false
            setupMap()
            observeOrders()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем метки при возврате к фрагменту, если это карта со всеми заказами
        if (!isSingleOrderView) {
            // Обновляем заказы в ViewModel, чтобы метки обновились
            viewModel.refreshOrders()
        }
    }
    
    private fun initViews(view: View) {
        mapView = view.findViewById(R.id.map_view)
        
        // Новые элементы UI
        orderInfoCard = view.findViewById(R.id.order_info_card)
        btnBack = view.findViewById(R.id.btn_back)
        tvPickupAddress = view.findViewById(R.id.tv_pickup_address)
        tvDestinationAddress = view.findViewById(R.id.tv_destination_address)
        tvOrderDevice = view.findViewById(R.id.tv_order_device)
        btnAcceptOrder = view.findViewById(R.id.btn_accept_order)
        btnEditPickup = view.findViewById(R.id.btn_edit_pickup)
        btnAddDestination = view.findViewById(R.id.btn_add_destination)
        btnEditOrder = view.findViewById(R.id.btn_edit_order)
        
        // Старые элементы (для совместимости)
        selectedOrderCard = view.findViewById(R.id.selected_order_card)
        selectedOrderId = view.findViewById(R.id.selected_order_id)
        selectedOrderDevice = view.findViewById(R.id.selected_order_device)
        selectedOrderAddress = view.findViewById(R.id.selected_order_address)
        selectedOrderCost = view.findViewById(R.id.selected_order_cost)
        btnViewOrder = view.findViewById(R.id.btn_view_order)
        
        btnViewOrder?.setOnClickListener {
            selectedOrder?.let { order ->
                val bundle = Bundle().apply {
                    putLong("orderId", order.id)
                }
                findNavController().navigate(R.id.action_orders_map_to_order_details, bundle)
            }
        }
    }
    
    private fun setupButtons() {
        btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }
        
        btnAcceptOrder?.setOnClickListener {
            acceptOrder()
        }
        
        btnEditPickup?.setOnClickListener {
            // Можно добавить диалог для выбора адреса отправления
            Toast.makeText(context, "Функция в разработке", Toast.LENGTH_SHORT).show()
        }
        
        btnAddDestination?.setOnClickListener {
            // Можно добавить диалог для добавления дополнительного адреса
            Toast.makeText(context, "Функция в разработке", Toast.LENGTH_SHORT).show()
        }
        
        btnEditOrder?.setOnClickListener {
            selectedOrder?.let { order ->
                val bundle = Bundle().apply {
                    putLong("orderId", order.id)
                }
                findNavController().navigate(R.id.action_orders_map_to_order_details, bundle)
            }
        }
    }
    
    private fun checkLocationPermission() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            getCurrentLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        lifecycleScope.launch {
            try {
                val task = fusedLocationClient.lastLocation
                val location: Location? = try {
                    Tasks.await(task)
                } catch (e: Exception) {
                    null
                }
                location?.let {
                    masterLatitude = it.latitude
                    masterLongitude = it.longitude
                    updateMasterLocationOnMap()
                    // Если выбран заказ, строим маршрут
                    selectedOrder?.let { buildRoute() }
                } ?: run {
                    // Если не удалось получить местоположение, используем координаты по умолчанию
                    updateMasterLocationOnMap()
                }
            } catch (e: Exception) {
                // Используем координаты по умолчанию
                updateMasterLocationOnMap()
            }
        }
    }
    
    private fun updateMasterLocationOnMap() {
        val map = mapView?.map ?: return
        val masterPoint = Point(masterLatitude, masterLongitude)
        
        // Удаляем старую метку мастера
        masterLocationPlacemark?.let {
            map.mapObjects.remove(it)
        }
        
        // Создаем новую метку мастера (зеленая точка)
        val markerIcon = createMasterLocationBitmap()
        masterLocationPlacemark = map.mapObjects.addPlacemark()
        masterLocationPlacemark?.geometry = masterPoint
        masterLocationPlacemark?.setIcon(ImageProvider.fromBitmap(markerIcon))
        
        // Обновляем адрес в UI
        tvPickupAddress?.text = "Текущее местоположение"
    }
    
    private fun createMasterLocationBitmap(): Bitmap {
        val size = 100
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Рисуем зеленый круг
        paint.color = Color.parseColor("#4CAF50")
        canvas.drawCircle(size / 2f, size / 2f, 40f, paint)
        
        // Рисуем белый круг внутри
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 30f, paint)
        
        // Рисуем зеленую точку в центре
        paint.color = Color.parseColor("#4CAF50")
        canvas.drawCircle(size / 2f, size / 2f, 15f, paint)
        
        return bitmap
    }
    
    
    private fun acceptOrder() {
        selectedOrder?.let { order ->
            lifecycleScope.launch {
                // Сначала получаем активное назначение для заказа
                val assignmentResult = apiRepository.getActiveAssignmentForOrder(order.id)
                assignmentResult.onSuccess { assignment ->
                    if (assignment != null) {
                        // Принимаем назначение
                        val acceptResult = apiRepository.acceptAssignment(assignment.id)
                        acceptResult.onSuccess {
                            Toast.makeText(context, "Заказ принят", Toast.LENGTH_SHORT).show()
                            // Переходим к деталям заказа
                            val bundle = Bundle().apply {
                                putLong("orderId", order.id)
                            }
                            findNavController().navigate(R.id.action_orders_map_to_order_details, bundle)
                        }.onFailure { error ->
                            Toast.makeText(
                                context,
                                "Ошибка: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Нет активного назначения для этого заказа",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        "Ошибка: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun setupMap() {
        mapView?.map?.let { map ->
            // Устанавливаем начальную позицию камеры (центр Твери)
            map.move(
                CameraPosition(
                    Point(56.859611, 35.911896),
                    12.0f, // zoom
                    0.0f, // azimuth
                    0.0f  // tilt
                ),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }
        // Скрываем панель заказа при открытии карты со всеми заказами
        orderInfoCard?.visibility = View.GONE
    }
    
    private fun setupMapWithOrder(latitude: Double, longitude: Double, orderId: Long?) {
        val map = mapView?.map ?: return
        
        // Очищаем старые метки
        map.mapObjects.clear()
        currentPlacemark = null
        
        // Если передан id заказа, загружаем актуальные данные заказа ПЕРЕД созданием метки
        orderId?.let { id ->
            viewLifecycleOwner.lifecycleScope.launch {
                // Обновляем заказы в ViewModel
                viewModel.refreshOrders()
                
                // Ждем первого обновления заказов и находим нужный заказ
                val orders = viewModel.filteredOrders.first { it.isNotEmpty() }
                val order = orders.find { it.id == id }
                
                if (order != null) {
                    // Используем координаты из актуального заказа
                    val actualLat = order.latitude ?: latitude
                    val actualLon = order.longitude ?: longitude
                    val actualPoint = Point(actualLat, actualLon)
                    
                    // Центрируем карту на актуальных координатах заказа
                    map.move(
                        CameraPosition(actualPoint, 15.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 1f),
                        null
                    )
                    
                    // Создаем метку с правильными координатами
                    val markerIcon = createMarkerBitmapForDevice(order.deviceType)
                    currentPlacemark = map.mapObjects.addPlacemark()
                    currentPlacemark?.geometry = actualPoint
                    currentPlacemark?.setIcon(ImageProvider.fromBitmap(markerIcon))
                    
                    showOrderInfo(order)
                } else {
                    // Если заказ не найден, используем переданные координаты
                    val orderPoint = Point(latitude, longitude)
                    map.move(
                        CameraPosition(orderPoint, 15.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 1f),
                        null
                    )
                    val markerIcon = createMarkerBitmapForDevice("")
                    currentPlacemark = map.mapObjects.addPlacemark()
                    currentPlacemark?.geometry = orderPoint
                    currentPlacemark?.setIcon(ImageProvider.fromBitmap(markerIcon))
                }
            }
        } ?: run {
            // Если orderId не передан, используем переданные координаты
            val orderPoint = Point(latitude, longitude)
            map.move(
                CameraPosition(orderPoint, 15.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
            val markerIcon = createMarkerBitmapForDevice("")
            currentPlacemark = map.mapObjects.addPlacemark()
            currentPlacemark?.geometry = orderPoint
            currentPlacemark?.setIcon(ImageProvider.fromBitmap(markerIcon))
        }
    }
    
    private fun observeOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredOrders.collect { orders ->
                addOrderMarkers(orders)
            }
        }
    }
    
    private fun addOrderMarkers(orders: List<Order>) {
        val map = mapView?.map ?: return
        
        // Удаляем старые метки заказов
        orderPlacemarks.forEach { placemark ->
            map.mapObjects.remove(placemark)
        }
        orderPlacemarks.clear()
        
        // Добавляем метки для каждого заказа с координатами
        orders.forEach { order ->
            if (order.latitude != null && order.longitude != null) {
                val point = Point(order.latitude, order.longitude)
                
                // Вычисляем расстояние до заказа (если есть координаты мастера)
                val distance = calculateDistance(
                    masterLatitude, masterLongitude,
                    order.latitude, order.longitude
                )
                
                // Создаем метку с цветовой индикацией по срочности и цене
                val markerIcon = createMarkerBitmapForOrder(order, distance)
                val placemark = map.mapObjects.addPlacemark()
                placemark.geometry = point
                placemark.setIcon(ImageProvider.fromBitmap(markerIcon))
                
                // Сохраняем заказ в userData для быстрого доступа
                placemark.userData = order
                
                // Сохраняем метку в список
                orderPlacemarks.add(placemark)
                
                // Добавляем обработчик нажатия на метку
                placemark.addTapListener { _, _ ->
                    showOrderInfo(order, distance)
                    // Перемещаем камеру к выбранному заказу
                    map.move(
                        CameraPosition(point, 15.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 0.5f),
                        null
                    )
                    true
                }
            }
        }
    }
    
    /**
     * Вычисляет расстояние между двумя точками в метрах (формула гаверсинуса)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Радиус Земли в метрах
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Форматирует расстояние для отображения
     */
    private fun formatDistance(meters: Double): String {
        return when {
            meters < 1000 -> String.format("%.0f м", meters)
            else -> String.format("%.1f км", meters / 1000)
        }
    }
    
    /**
     * Вычисляет примерное время в пути (в минутах)
     */
    private fun calculateEstimatedTime(distanceMeters: Double): Int {
        // Средняя скорость в городе ~40 км/ч = 11.1 м/с
        // Добавляем 30% на светофоры и пробки
        val averageSpeed = 11.1 * 0.7 // ~7.8 м/с
        val timeSeconds = distanceMeters / averageSpeed
        return (timeSeconds / 60).toInt() + 1 // +1 минута на парковку
    }
    
    /**
     * Создает маркер для заказа с цветовой индикацией по срочности и цене
     * Красный - срочные/экстренные
     * Синий - обычные
     * Зеленый - высокооплачиваемые (>10000₽)
     */
    private fun createMarkerBitmapForOrder(order: Order, distance: Double): Bitmap {
        val size = 100
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Определяем цвет маркера по срочности и цене (как в Яндекс Про)
        val markerColor = when {
            // Красный - срочные/экстренные заказы
            order.orderType == com.example.bestapp.data.OrderType.URGENT ||
            order.urgency == "emergency" ||
            order.urgency == "urgent" -> Color.parseColor("#FF5252") // Красный
            
            // Зеленый - высокооплачиваемые заказы (>10000₽)
            (order.estimatedCost ?: 0.0) > 10000 -> Color.parseColor("#4CAF50") // Зеленый
            
            // Синий - обычные заказы
            else -> Color.parseColor("#2196F3") // Синий
        }
        
        // Выбираем эмодзи в зависимости от типа устройства
        val emoji = when (order.deviceType) {
            "Стиральная машина" -> "🧺"
            "Холодильник" -> "❄️"
            "Посудомоечная машина" -> "🍽️"
            "Духовой шкаф" -> "🔥"
            "Микроволновая печь" -> "📻"
            "Морозильный ларь" -> "🧊"
            "Варочная панель" -> "🔥"
            "Ноутбук" -> "💻"
            "Десктоп" -> "🖥️"
            "Кофемашина" -> "☕"
            "Кондиционер" -> "❄️"
            "Водонагреватель" -> "🔥"
            else -> "📍"
        }
        
        // Рисуем внешний круг (цвет по срочности/цене)
        paint.color = markerColor
        canvas.drawCircle(size / 2f, size / 2f, 40f, paint)
        
        // Рисуем обводку для срочных заказов
        if (order.orderType == com.example.bestapp.data.OrderType.URGENT || 
            order.urgency == "emergency" || 
            order.urgency == "urgent") {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.WHITE
            canvas.drawCircle(size / 2f, size / 2f, 40f, paint)
            paint.style = Paint.Style.FILL
        }
        
        // Рисуем внутренний круг (белый)
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 30f, paint)
        
        // Рисуем эмодзи в центре
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val textOffset = textHeight / 2 - fontMetrics.bottom
        
        canvas.drawText(emoji, size / 2f, size / 2f + textOffset, textPaint)
        
        return bitmap
    }
    
    /**
     * Старый метод для совместимости
     */
    private fun createMarkerBitmapForDevice(deviceType: String): Bitmap {
        // Создаем временный заказ для использования нового метода
        val tempOrder = Order(
            id = 0,
            clientId = 0,
            clientName = "",
            clientPhone = "",
            clientAddress = "",
            deviceType = deviceType,
            deviceBrand = "",
            deviceModel = "",
            problemDescription = ""
        )
        return createMarkerBitmapForOrder(tempOrder, 0.0)
    }
    
    private fun createMarkerBitmap(): Bitmap {
        return createMarkerBitmapForDevice("")
    }
    
    private fun showOrderInfo(order: Order, distance: Double = 0.0) {
        selectedOrder = order
        
        // Обновляем новую панель
        tvOrderDevice?.text = order.getDeviceFullName()
        
        tvDestinationAddress?.text = order.clientAddress
        
        orderInfoCard?.visibility = View.VISIBLE
        
        // Скрываем старую карточку
        selectedOrderCard?.visibility = View.GONE
        
        // Строим маршрут
        buildRoute()
        
        // Старые элементы (для совместимости)
        selectedOrderId?.text = "#${order.id}"
        selectedOrderDevice?.text = order.getDeviceFullName()
        selectedOrderAddress?.text = order.clientAddress
        selectedOrderCost?.text = order.getFormattedCost()
    }
    
    private fun buildRoute() {
        selectedOrder?.let { order ->
            if (order.latitude == null || order.longitude == null) {
                return
            }
            
            val destinationPoint = Point(order.latitude, order.longitude)
            val startPoint = Point(masterLatitude, masterLongitude)
            
            // Создаем простую линию между точками (упрощенный маршрут)
            val map = mapView?.map ?: return
            
            // Удаляем старую линию маршрута
            routePolyline?.let {
                map.mapObjects.remove(it)
            }
            
            // Создаем простую прямую линию между точками
            val polyline = com.yandex.mapkit.geometry.Polyline(listOf(startPoint, destinationPoint))
            routePolyline = map.mapObjects.addPolyline(polyline)
            routePolyline?.setStrokeColor(Color.parseColor("#2196F3"))
            routePolyline?.strokeWidth = 5f
            
            // Масштабируем карту, чтобы показать обе точки
            val centerLat = (startPoint.latitude + destinationPoint.latitude) / 2
            val centerLon = (startPoint.longitude + destinationPoint.longitude) / 2
            val centerPoint = Point(centerLat, centerLon)
            
            // Вычисляем подходящий zoom для показа обеих точек
            val latDiff = kotlin.math.abs(startPoint.latitude - destinationPoint.latitude)
            val lonDiff = kotlin.math.abs(startPoint.longitude - destinationPoint.longitude)
            val maxDiff = kotlin.math.max(latDiff, lonDiff)
            val zoom = when {
                maxDiff > 0.1 -> 10f
                maxDiff > 0.05 -> 12f
                maxDiff > 0.01 -> 14f
                else -> 15f
            }
            
            map.move(
                CameraPosition(
                    centerPoint,
                    zoom,
                    0f,
                    0f
                ),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }
    }
    
    
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }
    
    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        orderPlacemarks.clear()
        mapView = null
        orderInfoCard = null
        selectedOrderCard = null
        btnBack = null
        tvPickupAddress = null
        tvDestinationAddress = null
        tvOrderDevice = null
        btnAcceptOrder = null
        btnEditPickup = null
        btnAddDestination = null
        btnEditOrder = null
        selectedOrderId = null
        selectedOrderDevice = null
        selectedOrderAddress = null
        selectedOrderCost = null
        btnViewOrder = null
        selectedOrder = null
        currentPlacemark = null
        masterLocationPlacemark = null
        routePolyline = null
    }
}

