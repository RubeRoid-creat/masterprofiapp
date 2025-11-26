package com.example.bestapp.ui.zones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.data.MapWorkZone
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PolygonMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.ui_view.ViewProvider
import kotlinx.coroutines.launch

class WorkZonesFragment : Fragment() {
    
    private val viewModel: WorkZonesViewModel by viewModels {
        WorkZonesViewModel.provideFactory(requireContext().applicationContext as android.app.Application)
    }
    
    private var mapView: MapView? = null
    private var fabAddZone: FloatingActionButton? = null
    private var zonesPanel: MaterialCardView? = null
    private var drawingModePanel: MaterialCardView? = null
    private var btnAddZone: MaterialButton? = null
    private var inputCity: TextInputEditText? = null
    private var inputDistrict: TextInputEditText? = null
    private var inputRadius: TextInputEditText? = null
    private var btnFinishZone: MaterialButton? = null
    private var btnCancelZone: MaterialButton? = null
    private var btnSaveZones: MaterialButton? = null
    private var recyclerZones: RecyclerView? = null
    
    private var zonesAdapter: WorkZonesAdapter? = null
    private var isDrawingMode = false
    private var currentZonePoints = mutableListOf<Point>()
    private var currentPolygon: PolygonMapObject? = null
    private val zonePolygons = mutableListOf<PolygonMapObject>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_work_zones, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews(view)
        setupToolbar()
        setupMap()
        setupRecyclerView()
        setupButtons()
        observeZones()
    }
    
    private fun setupViews(view: View) {
        mapView = view.findViewById(R.id.map_view_zones)
        fabAddZone = view.findViewById(R.id.fab_add_zone)
        zonesPanel = view.findViewById(R.id.zones_panel)
        drawingModePanel = view.findViewById(R.id.drawing_mode_panel)
        btnAddZone = view.findViewById(R.id.btn_add_zone)
        inputCity = view.findViewById(R.id.input_city)
        inputDistrict = view.findViewById(R.id.input_district)
        inputRadius = view.findViewById(R.id.input_radius)
        btnFinishZone = view.findViewById(R.id.btn_finish_zone)
        btnCancelZone = view.findViewById(R.id.btn_cancel_zone)
        btnSaveZones = view.findViewById(R.id.btn_save_zones)
        recyclerZones = view.findViewById(R.id.recycler_zones)
    }
    
    private fun setupToolbar() {
        view?.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)?.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }
    
    private fun setupMap() {
        mapView?.map?.let { map ->
            // Устанавливаем начальную позицию (Тверь)
            val cameraPosition = CameraPosition(
                Point(56.859611, 35.911896),
                12.0f, 0.0f, 0.0f
            )
            map.move(cameraPosition, Animation(Animation.Type.SMOOTH, 0f), null)
            
            // Обработчик кликов на карте для рисования зон
            map.addInputListener(object : com.yandex.mapkit.map.InputListener {
                override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                    if (isDrawingMode) {
                        addPointToZone(point)
                    }
                }
                
                override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
                    // Не используется
                }
            })
        }
    }
    
    private fun setupRecyclerView() {
        zonesAdapter = WorkZonesAdapter(
            onZoneToggle = { zone, isActive ->
                viewModel.toggleZone(zone.id, isActive)
            },
            onZoneDelete = { zone ->
                viewModel.deleteZone(zone.id)
            }
        )
        
        recyclerZones?.apply {
            adapter = zonesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupButtons() {
        fabAddZone?.setOnClickListener { startDrawingZone() }
        btnAddZone?.setOnClickListener { startDrawingZone() }
        
        btnFinishZone?.setOnClickListener {
            finishDrawingZone()
        }
        
        btnCancelZone?.setOnClickListener {
            cancelDrawingZone()
        }
        
        btnSaveZones?.setOnClickListener {
            viewModel.saveZones()
            Toast.makeText(context, "Зоны сохранены", Toast.LENGTH_SHORT).show()
        }

        // Выбор города
        inputCity?.setOnClickListener { showCityPicker() }
        inputCity?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showCityPicker()
        }

        // Выбор района
        inputDistrict?.setOnClickListener { showDistrictPicker() }
        inputDistrict?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDistrictPicker()
        }

        // Выбор радиуса
        inputRadius?.setOnClickListener { showRadiusPicker() }
        inputRadius?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showRadiusPicker()
        }
    }
    
    private fun observeZones() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.zones.collect { zones: List<MapWorkZone> ->
                zonesAdapter?.submitList(zones)
                updateZonesOnMap(zones)
            }
        }
    }
    
    private fun startDrawingZone() {
        isDrawingMode = true
        drawingModePanel?.visibility = View.VISIBLE
        inputCity?.setText("")
        inputDistrict?.setText("")
        inputRadius?.setText("")
        fabAddZone?.visibility = View.GONE
    }
    
    private fun addPointToZone(point: Point) {
        currentZonePoints.add(point)
        updateCurrentPolygon()
    }
    
    private fun updateCurrentPolygon() {
        if (currentZonePoints.size < 3) return
        
        val map = mapView?.map ?: return
        val mapObjects = map.mapObjects ?: return
        
        // Удаляем старый полигон
        currentPolygon?.let { mapObjects.remove(it) }
        
        // Создаем новый полигон
        val outerRing = com.yandex.mapkit.geometry.LinearRing(currentZonePoints)
        val polygon = com.yandex.mapkit.geometry.Polygon(outerRing, emptyList())
        
        currentPolygon = mapObjects.addPolygon(polygon).apply {
            fillColor = 0x3300FF00.toInt() // Полупрозрачный зеленый
            strokeColor = 0xFF00FF00.toInt() // Зеленая обводка
            strokeWidth = 3.0f
        }
    }
    
    private fun finishDrawingZone() {
        val city = inputCity?.text?.toString()?.trim().orEmpty()
        val district = inputDistrict?.text?.toString()?.trim().orEmpty()
        val radiusText = inputRadius?.text?.toString()?.trim().orEmpty()

        if (city.isEmpty()) {
            Toast.makeText(context, "Выберите город", Toast.LENGTH_SHORT).show()
            return
        }

        val parsedRadius = radiusText
            .replace("км", "")
            .trim()
            .toDoubleOrNull()

        // Если радиус не указан — считаем, что мастер работает по городу, условно берём 10 км
        val radiusKmForPolygon = if (parsedRadius != null && parsedRadius > 0.0) {
            parsedRadius
        } else {
            10.0
        }

        val center = getZoneCenter(city, district)
        val points = generateCirclePoints(center.first, center.second, radiusKmForPolygon)

        val zoneName = buildString {
            append(city)
            if (district.isNotEmpty()) {
                append(" • ")
                append(district)
            }
            if (parsedRadius != null && parsedRadius > 0.0) {
                append(" +")
                append(parsedRadius.toInt())
                append("км")
            }
        }

        val newZone = MapWorkZone(
            id = 0L,
            name = zoneName,
            points = points,
            isActive = true
        )

        viewModel.addZone(newZone)
        cancelDrawingZone()
    }
    
    private fun cancelDrawingZone() {
        isDrawingMode = false
        currentZonePoints.clear()
        drawingModePanel?.visibility = View.GONE
        fabAddZone?.visibility = View.VISIBLE
        
        currentPolygon?.let {
            val map = mapView?.map ?: return@let
            val mapObjects = map.mapObjects ?: return@let
            mapObjects.remove(it)
            currentPolygon = null
        }
    }

    private fun showCityPicker() {
        val cities = arrayOf("Тверь", "Москва", "Санкт-Петербург")
        val edit = inputCity ?: return

        val current = edit.text?.toString()
        var checkedIndex = cities.indexOfFirst { it == current }
        if (checkedIndex < 0) checkedIndex = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Город")
            .setSingleChoiceItems(cities, checkedIndex) { dialog, which ->
                edit.setText(cities[which])
                val center = getCityCenter(cities[which])
                // Центрируем карту на выбранный город
                mapView?.map?.move(
                    CameraPosition(Point(center.first, center.second), 11.5f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 0.5f),
                    null
                )
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDistrictPicker() {
        val city = inputCity?.text?.toString()?.trim().orEmpty()
        val edit = inputDistrict ?: return

        if (city.isEmpty()) {
            Toast.makeText(context, "Сначала выберите город", Toast.LENGTH_SHORT).show()
            showCityPicker()
            return
        }

        val districts = getDistrictsForCity(city)
        if (districts.isEmpty()) {
            Toast.makeText(context, "Для этого города районы не настроены", Toast.LENGTH_SHORT).show()
            return
        }

        val items = arrayOf("Без района") + districts

        val current = edit.text?.toString()
        var checkedIndex = items.indexOfFirst { it == current }
        if (checkedIndex < 0) checkedIndex = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Район")
            .setSingleChoiceItems(items, checkedIndex) { dialog, which ->
                if (which == 0) {
                    edit.setText("")
                } else {
                    edit.setText(items[which])
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showRadiusPicker() {
        val radii = arrayOf("10 км", "20 км", "30 км", "50 км")
        val edit = inputRadius ?: return

        val current = edit.text?.toString()
        var checkedIndex = radii.indexOfFirst { it == current }
        if (checkedIndex < 0) checkedIndex = 1 // по умолчанию 20 км

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Радиус от города")
            .setSingleChoiceItems(radii, checkedIndex) { dialog, which ->
                edit.setText(radii[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun getCityCenter(city: String): Pair<Double, Double> {
        return when (city) {
            "Москва" -> Pair(55.7558, 37.6176)
            "Санкт-Петербург" -> Pair(59.9311, 30.3609)
            else -> Pair(56.859611, 35.911896) // Тверь по умолчанию
        }
    }

    /**
     * Центр зоны: если выбран район — возвращаем центр района, иначе центр города.
     * Координаты приблизительные, нужны для визуальной зоны на карте.
     */
    private fun getZoneCenter(city: String, district: String): Pair<Double, Double> {
        if (district.isBlank()) return getCityCenter(city)

        return when (city) {
            "Москва" -> when (district) {
                "ЦАО" -> Pair(55.7558, 37.6176)
                "САО" -> Pair(55.8530, 37.4760)
                "СВАО" -> Pair(55.8660, 37.6500)
                "ВАО" -> Pair(55.7800, 37.8000)
                "ЮВАО" -> Pair(55.7100, 37.7800)
                "ЮАО" -> Pair(55.6500, 37.6200)
                "ЮЗАО" -> Pair(55.6500, 37.5200)
                "ЗАО" -> Pair(55.7200, 37.4300)
                "СЗАО" -> Pair(55.8300, 37.4300)
                "ТиНАО" -> Pair(55.4300, 37.2000)
                else -> getCityCenter(city)
            }
            "Санкт-Петербург" -> when (district) {
                "Адмиралтейский" -> Pair(59.9180, 30.3050)
                "Василеостровский" -> Pair(59.9420, 30.2580)
                "Выборгский" -> Pair(60.0200, 30.3300)
                "Калининский" -> Pair(59.9800, 30.4100)
                "Кировский" -> Pair(59.8600, 30.2600)
                "Московский" -> Pair(59.8600, 30.3200)
                "Невский" -> Pair(59.8800, 30.4800)
                "Петроградский" -> Pair(59.9660, 30.3000)
                "Приморский" -> Pair(60.0000, 30.2600)
                "Фрунзенский" -> Pair(59.8700, 30.3800)
                "Центральный" -> Pair(59.9320, 30.3550)
                else -> getCityCenter(city)
            }
            // Тверь
            else -> when (district) {
                "Центральный" -> Pair(56.8578, 35.9219)
                "Московский" -> Pair(56.8450, 35.9000)
                "Пролетарский" -> Pair(56.8400, 35.9500)
                "Заволжский" -> Pair(56.8800, 35.9300)
                else -> getCityCenter(city)
            }
        }
    }

    private fun getDistrictsForCity(city: String): Array<String> {
        return when (city) {
            "Москва" -> arrayOf(
                "ЦАО",
                "САО",
                "СВАО",
                "ВАО",
                "ЮВАО",
                "ЮАО",
                "ЮЗАО",
                "ЗАО",
                "СЗАО",
                "ТиНАО"
            )
            "Санкт-Петербург" -> arrayOf(
                "Адмиралтейский",
                "Василеостровский",
                "Выборгский",
                "Калининский",
                "Кировский",
                "Московский",
                "Невский",
                "Петроградский",
                "Приморский",
                "Фрунзенский",
                "Центральный"
            )
            else -> arrayOf(
                "Центральный",
                "Московский",
                "Пролетарский",
                "Заволжский"
            )
        }
    }

    /**
     * Генерирует точки круга вокруг центра (lat, lon) с радиусом radiusKm.
     */
    private fun generateCirclePoints(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        steps: Int = 36
    ): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()
        val radiusRadLat = radiusKm / 111.0
        val latRad = Math.toRadians(centerLat)

        for (i in 0 until steps) {
            val angle = 2.0 * Math.PI * i / steps
            val dLat = radiusRadLat * Math.sin(angle)
            val dLon = radiusRadLat * Math.cos(angle) / Math.cos(latRad)
            val lat = centerLat + dLat
            val lon = centerLon + dLon
            result.add(Pair(lat, lon))
        }

        return result
    }
    
    private fun updateZonesOnMap(zones: List<MapWorkZone>) {
        val map = mapView?.map ?: return
        val mapObjects = map.mapObjects ?: return
        
        // Удаляем старые полигоны
        zonePolygons.forEach { mapObjects.remove(it) }
        zonePolygons.clear()
        
        // Добавляем новые полигоны для активных зон
        zones.filter { it.isActive }.forEach { zone ->
            val points = zone.points.map { Point(it.first, it.second) }
            if (points.size >= 3) {
                val outerRing = com.yandex.mapkit.geometry.LinearRing(points)
                val polygon = com.yandex.mapkit.geometry.Polygon(outerRing, emptyList())
                
                val polygonObject = mapObjects.addPolygon(polygon).apply {
                    fillColor = 0x332196F3.toInt() // Полупрозрачный синий
                    strokeColor = 0xFF2196F3.toInt() // Синяя обводка
                    strokeWidth = 3.0f
                }
                
                zonePolygons.add(polygonObject)
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }
    
    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        mapView = null
        fabAddZone = null
        zonesPanel = null
        drawingModePanel = null
        btnAddZone = null
        inputCity = null
        inputDistrict = null
        inputRadius = null
        btnFinishZone = null
        btnCancelZone = null
        btnSaveZones = null
        recyclerZones = null
        zonesAdapter = null
    }
}

