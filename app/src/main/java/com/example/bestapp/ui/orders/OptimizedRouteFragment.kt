package com.example.bestapp.ui.orders

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.OptimizedRouteResponse
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.launch
import java.util.Locale

class OptimizedRouteFragment : Fragment() {
    
    private var toolbar: MaterialToolbar? = null
    private var mapView: MapView? = null
    private var tvTotalDistance: TextView? = null
    private var tvTotalTime: TextView? = null
    private var tvOrdersCount: TextView? = null
    private var recyclerRouteOrders: RecyclerView? = null
    private var btnOpenNavigation: MaterialButton? = null
    
    private val apiRepository = ApiRepository()
    private var optimizedRoute: OptimizedRouteResponse? = null
    private lateinit var routeAdapter: RouteOrdersAdapter
    
    companion object {
        private const val TAG = "OptimizedRouteFragment"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_optimized_route, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupToolbar()
        setupRecyclerView()
        setupMap()
        loadRouteData()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        mapView = view.findViewById(R.id.map_view)
        tvTotalDistance = view.findViewById(R.id.tv_total_distance)
        tvTotalTime = view.findViewById(R.id.tv_total_time)
        tvOrdersCount = view.findViewById(R.id.tv_orders_count)
        recyclerRouteOrders = view.findViewById(R.id.recycler_route_orders)
        btnOpenNavigation = view.findViewById(R.id.btn_open_navigation)
        
        btnOpenNavigation?.setOnClickListener {
            openRouteInNavigation()
        }
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupRecyclerView() {
        routeAdapter = RouteOrdersAdapter { orderItem ->
            // Открываем детали заказа
            val bundle = Bundle().apply {
                putLong("orderId", orderItem.order.id)
            }
            findNavController().navigate(R.id.action_optimized_route_to_order_details, bundle)
        }
        
        recyclerRouteOrders?.apply {
            adapter = routeAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupMap() {
        mapView?.map?.let { map ->
            // Настройки карты будут применены после загрузки данных
        }
    }
    
    private fun loadRouteData() {
        val orderIds = arguments?.getLongArray("orderIds")
        val totalDistance = arguments?.getDouble("totalDistance") ?: 0.0
        val totalTime = arguments?.getInt("totalTime") ?: 0
        
        if (orderIds == null || orderIds.isEmpty()) {
            Toast.makeText(context, "Не указаны заказы для маршрута", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        
        // Загружаем полные данные маршрута с сервера
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Получаем текущее местоположение
                val location = com.google.android.gms.location.LocationServices
                    .getFusedLocationProviderClient(requireActivity())
                    .lastLocation
                
                val startLat = try {
                    com.google.android.gms.tasks.Tasks.await(location)?.latitude
                } catch (e: Exception) {
                    null
                }
                
                val startLon = try {
                    com.google.android.gms.tasks.Tasks.await(location)?.longitude
                } catch (e: Exception) {
                    null
                }
                
                val result = apiRepository.optimizeRoute(
                    orderIds = orderIds.toList(),
                    startLatitude = startLat,
                    startLongitude = startLon
                )
                
                result.onSuccess { route ->
                    optimizedRoute = route
                    displayRoute(route)
                }.onFailure { error ->
                    Log.e(TAG, "Error loading route", error)
                    Toast.makeText(
                        context,
                        "Ошибка загрузки маршрута: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    // Показываем базовую информацию из аргументов
                    displayBasicInfo(totalDistance, totalTime, orderIds.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading route", e)
                // Показываем базовую информацию из аргументов
                displayBasicInfo(totalDistance, totalTime, orderIds.size)
            }
        }
    }
    
    private fun displayRoute(route: OptimizedRouteResponse) {
        // Обновляем информацию о маршруте
        tvTotalDistance?.text = formatDistance(route.totalDistance)
        tvTotalTime?.text = "${route.totalTime} мин"
        tvOrdersCount?.text = "${route.orders.size}"
        
        // Обновляем список заказов
        routeAdapter.submitList(route.orders)
        
        // Отображаем маршрут на карте
        displayRouteOnMap(route)
    }
    
    private fun displayBasicInfo(distance: Double, time: Int, count: Int) {
        tvTotalDistance?.text = formatDistance(distance)
        tvTotalTime?.text = "$time мин"
        tvOrdersCount?.text = "$count"
    }
    
    private fun displayRouteOnMap(route: OptimizedRouteResponse) {
        mapView?.map?.let { map ->
            val points = mutableListOf<Point>()
            
            // Добавляем начальную точку, если есть
            route.startLocation?.let { startLoc ->
                if (startLoc.size >= 2) {
                    points.add(Point(startLoc[0], startLoc[1]))
                }
            }
            
            // Добавляем точки заказов
            route.orders.forEach { orderItem ->
                val order = orderItem.order
                points.add(Point(order.latitude, order.longitude))
            }
            
            if (points.isEmpty()) {
                return
            }
            
            // Вычисляем центр всех точек
            val minLat = points.minByOrNull { it.latitude }?.latitude ?: 0.0
            val maxLat = points.maxByOrNull { it.latitude }?.latitude ?: 0.0
            val minLon = points.minByOrNull { it.longitude }?.longitude ?: 0.0
            val maxLon = points.maxByOrNull { it.longitude }?.longitude ?: 0.0
            val centerLat = (minLat + maxLat) / 2
            val centerLon = (minLon + maxLon) / 2
            val centerPoint = Point(centerLat, centerLon)
            
            // Устанавливаем камеру, чтобы показать все точки
            map.move(
                CameraPosition(
                    centerPoint,
                    map.cameraPosition.zoom,
                    0.0f,
                    0.0f
                ),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
            
            // Добавляем маркеры для заказов
            val mapObjects = map.mapObjects
            route.orders.forEachIndexed { index, orderItem ->
                val order = orderItem.order
                val point = Point(order.latitude, order.longitude)
                
                val placemark = mapObjects.addPlacemark(point)
                placemark.userData = order.id
                // Устанавливаем иконку с номером
                val markerIcon = createMarkerBitmapWithNumber(index + 1)
                placemark.setIcon(com.yandex.runtime.image.ImageProvider.fromBitmap(markerIcon))
            }
        }
    }
    
    private fun openRouteInNavigation() {
        val route = optimizedRoute ?: return
        
        if (route.orders.isEmpty()) {
            Toast.makeText(context, "Нет заказов в маршруте", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Открываем первый заказ в навигаторе
        val firstOrder = route.orders.first().order
        openNavigationApp(
            fromLat = route.startLocation?.get(0),
            fromLon = route.startLocation?.get(1),
            toLat = firstOrder.latitude,
            toLon = firstOrder.longitude
        )
    }
    
    private fun openNavigationApp(
        fromLat: Double?,
        fromLon: Double?,
        toLat: Double,
        toLon: Double
    ) {
        val packageManager = requireActivity().packageManager
        val availableApps = mutableListOf<Pair<String, Intent>>()
        
        // Yandex Navigator
        val yandexNavUri = if (fromLat != null && fromLon != null) {
            Uri.parse("yandexnavi://build_route?lat_from=$fromLat&lon_from=$fromLon&lat_to=$toLat&lon_to=$toLon")
        } else {
            Uri.parse("yandexnavi://build_route?lat_to=$toLat&lon_to=$toLon")
        }
        val yandexNavIntent = Intent(Intent.ACTION_VIEW, yandexNavUri).apply {
            setPackage("ru.yandex.yandexnavi")
        }
        if (yandexNavIntent.resolveActivity(packageManager) != null) {
            availableApps.add("Yandex Navigator" to yandexNavIntent)
        }
        
        // Google Maps
        val googleMapsUri = if (fromLat != null && fromLon != null) {
            Uri.parse("google.navigation:q=$toLat,$toLon")
        } else {
            Uri.parse("geo:$toLat,$toLon?q=$toLat,$toLon")
        }
        val googleMapsIntent = Intent(Intent.ACTION_VIEW, googleMapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (googleMapsIntent.resolveActivity(packageManager) != null) {
            availableApps.add("Google Maps" to googleMapsIntent)
        }
        
        // Яндекс.Карты
        val yandexMapsUri = if (fromLat != null && fromLon != null) {
            Uri.parse("yandexmaps://build_route?lat_from=$fromLat&lon_from=$fromLon&lat_to=$toLat&lon_to=$toLon")
        } else {
            Uri.parse("yandexmaps://build_route_on_map?lat_to=$toLat&lon_to=$toLon")
        }
        val yandexMapsIntent = Intent(Intent.ACTION_VIEW, yandexMapsUri).apply {
            setPackage("ru.yandex.yandexmaps")
        }
        if (yandexMapsIntent.resolveActivity(packageManager) != null) {
            availableApps.add("Яндекс.Карты" to yandexMapsIntent)
        }
        
        if (availableApps.isNotEmpty()) {
            if (availableApps.size == 1) {
                startActivity(availableApps[0].second)
            } else {
                val appNames = availableApps.map { it.first }.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Выберите навигатор")
                    .setItems(appNames) { _, which ->
                        startActivity(availableApps[which].second)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        } else {
            // Fallback на веб-версию
            val webUri = if (fromLat != null && fromLon != null) {
                Uri.parse("https://yandex.ru/maps/?rtext=$fromLat,$fromLon~$toLat,$toLon&rtt=auto")
            } else {
                Uri.parse("https://yandex.ru/maps/?pt=$toLon,$toLat&z=16")
            }
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }
    
    private fun formatDistance(meters: Double): String {
        return when {
            meters < 1000 -> "${meters.toInt()} м"
            else -> String.format(Locale.getDefault(), "%.1f км", meters / 1000)
        }
    }
    
    private fun createMarkerBitmapWithNumber(number: Int): android.graphics.Bitmap {
        val size = 100
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FF5252")
            style = android.graphics.Paint.Style.FILL
        }
        
        // Рисуем круг
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
        
        // Рисуем белый обводку
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = android.graphics.Color.WHITE
        paint.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
        
        // Рисуем номер
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 36f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        val textY = size / 2f + (paint.descent() + paint.ascent()) / 2
        canvas.drawText(number.toString(), size / 2f, textY, paint)
        
        return bitmap
    }
    
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView?.onStart()
    }
    
    override fun onStop() {
        mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        toolbar = null
        mapView = null
        tvTotalDistance = null
        tvTotalTime = null
        tvOrdersCount = null
        recyclerRouteOrders = null
        btnOpenNavigation = null
    }
}

