package com.bestapp.client.ui.map

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.runtime.image.ImageProvider

data class SelectedLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

@Composable
fun MapAddressPicker(
    initialLatitude: Double = 56.859611, // Тверь по умолчанию
    initialLongitude: Double = 35.911896,
    onLocationSelected: (SelectedLocation) -> Unit,
    modifier: Modifier = Modifier,
    autoDetectLocation: Boolean = true,
    showNearbyMasters: Boolean = false, // Показывать ближайших мастеров на карте
    nearbyMasters: List<com.bestapp.client.data.api.models.MasterDto> = emptyList() // Список ближайших мастеров
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedPoint by remember { mutableStateOf<Point?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var placemark by remember { mutableStateOf<com.yandex.mapkit.map.PlacemarkMapObject?>(null) }
    var currentLocation by remember { mutableStateOf<Point?>(null) }
    var masterPlacemarks by remember { mutableStateOf<List<com.yandex.mapkit.map.PlacemarkMapObject>>(emptyList()) }
    
    // Автоопределение текущего местоположения
    LaunchedEffect(autoDetectLocation) {
        if (autoDetectLocation) {
            val location = com.bestapp.client.utils.LocationUtils.getCurrentLocation(context)
            location?.let {
                val point = Point(it.latitude, it.longitude)
                currentLocation = point
                selectedPoint = point
                
                // Получаем адрес
                val address = com.bestapp.client.utils.LocationUtils.getAddressFromCoordinates(
                    context,
                    it.latitude,
                    it.longitude
                )
                selectedAddress = address
                
                onLocationSelected(
                    SelectedLocation(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        address = address
                    )
                )
            }
        }
    }
    
    val searchManager = remember {
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    }
    
    // Обработка lifecycle для MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapView?.onStart()
                    MapKitFactory.getInstance().onStart()
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView?.onStop()
                    MapKitFactory.getInstance().onStop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView = this
                    val startPoint = currentLocation ?: Point(initialLatitude, initialLongitude)
                    map.move(
                        CameraPosition(
                            startPoint,
                            14.0f,
                            0.0f,
                            0.0f
                        ),
                        Animation(Animation.Type.SMOOTH, 0f),
                        null
                    )
                    
                    // Добавляем метку в центре карты
                    placemark = map.mapObjects.addPlacemark(
                        startPoint,
                        ImageProvider.fromResource(ctx, android.R.drawable.ic_menu_mylocation)
                    )
                    
                    // Добавляем метки ближайших мастеров, если нужно
                    if (showNearbyMasters && nearbyMasters.isNotEmpty()) {
                        nearbyMasters.forEach { master ->
                            master.latitude?.let { lat ->
                                master.longitude?.let { lon ->
                                    val masterPoint = Point(lat, lon)
                                    val masterPlacemark = map.mapObjects.addPlacemark(
                                        masterPoint,
                                        ImageProvider.fromResource(ctx, android.R.drawable.ic_menu_myplaces)
                                    )
                                    // Можно добавить подпись с именем и временем прибытия
                                    masterPlacemark.setText(
                                        "${master.name ?: "Мастер"}\n${master.arrivalTimeFormatted ?: ""}"
                                    )
                                }
                            }
                        }
                    }
                    
                    // Обработчик нажатий на карту
                    map.addInputListener(object : InputListener {
                        override fun onMapTap(map: Map, point: Point) {
                            // Обновляем метку
                            placemark?.geometry = point
                            selectedPoint = point
                            selectedAddress = null
                            isLoadingAddress = true
                            
                            // Получаем адрес по координатам
                            performReverseGeocoding(
                                context = ctx,
                                searchManager = searchManager,
                                point = point,
                                onAddressReceived = { address ->
                                    selectedAddress = address
                                    isLoadingAddress = false
                                    onLocationSelected(
                                        SelectedLocation(
                                            latitude = point.latitude,
                                            longitude = point.longitude,
                                            address = address
                                        )
                                    )
                                },
                                onError = {
                                    isLoadingAddress = false
                                    onLocationSelected(
                                        SelectedLocation(
                                            latitude = point.latitude,
                                            longitude = point.longitude,
                                            address = null
                                        )
                                    )
                                }
                            )
                        }
                        
                        override fun onMapLongTap(map: Map, point: Point) {}
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                // Обновление при изменении начальных координат
                if (selectedPoint == null) {
                    mapView.map.move(
                        CameraPosition(
                            Point(initialLatitude, initialLongitude),
                            14.0f,
                            0.0f,
                            0.0f
                        ),
                        Animation(Animation.Type.SMOOTH, 0f),
                        null
                    )
                }
                
                // Обновляем метки мастеров при изменении списка
                if (showNearbyMasters) {
                    // Удаляем старые метки (кроме основной метки адреса)
                    masterPlacemarks.forEach { mapView.map.mapObjects.remove(it) }
                    masterPlacemarks = emptyList()
                    
                    // Добавляем новые метки
                    if (nearbyMasters.isNotEmpty()) {
                        val newPlacemarks = nearbyMasters.mapNotNull { master ->
                            master.latitude?.let { lat ->
                                master.longitude?.let { lon ->
                                    val masterPoint = Point(lat, lon)
                                    val masterPlacemark = mapView.map.mapObjects.addPlacemark(
                                        masterPoint,
                                        ImageProvider.fromResource(mapView.context, android.R.drawable.ic_menu_myplaces)
                                    )
                                    // Добавляем подпись с именем и временем прибытия
                                    val label = "${master.name ?: "Мастер"}${master.arrivalTimeFormatted?.let { "\n$it" } ?: ""}"
                                    masterPlacemark.setText(label)
                                    masterPlacemark
                                }
                            }
                        }
                        masterPlacemarks = newPlacemarks
                    }
                }
            }
        )
        
        // Индикатор в центре карты
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Выбранная точка",
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        // Индикатор загрузки адреса
        if (isLoadingAddress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
        
        // Отображение выбранного адреса
        selectedAddress?.let { address ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = address,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
    
}

private fun performReverseGeocoding(
    context: Context,
    searchManager: SearchManager,
    point: Point,
    onAddressReceived: (String) -> Unit,
    onError: () -> Unit
) {
    val searchOptions = SearchOptions()
    val session = searchManager.submit(
        point,
        null, // zoom level
        searchOptions,
        object : Session.SearchListener {
            override fun onSearchResponse(response: Response) {
                val firstResult = response.collection.children.firstOrNull()
                if (firstResult != null && firstResult.obj != null) {
                    val addressText = firstResult.obj?.name ?: "Адрес не найден"
                    onAddressReceived(addressText)
                } else {
                    onError()
                }
            }
            
            override fun onSearchError(error: com.yandex.runtime.Error) {
                onError()
            }
        }
    )
}

