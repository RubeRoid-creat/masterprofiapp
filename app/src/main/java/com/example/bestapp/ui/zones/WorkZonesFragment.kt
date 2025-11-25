package com.example.bestapp.ui.zones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.data.PreferencesManager
import com.example.bestapp.data.MapWorkZone
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
    private var inputZoneName: TextInputEditText? = null
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
        inputZoneName = view.findViewById(R.id.input_zone_name)
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
        fabAddZone?.setOnClickListener {
            startDrawingZone()
        }
        
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
        currentZonePoints.clear()
        drawingModePanel?.visibility = View.VISIBLE
        inputZoneName?.setText("")
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
        val zoneName = inputZoneName?.text?.toString()?.trim() ?: ""
        
        if (zoneName.isEmpty()) {
            Toast.makeText(context, "Введите название зоны", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (currentZonePoints.size < 3) {
            Toast.makeText(context, "Добавьте минимум 3 точки", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Конвертируем Point в Pair<Double, Double>
        val points = currentZonePoints.map { Pair(it.latitude, it.longitude) }
        
        val newZone = MapWorkZone(
            id = 0L, // ViewModel сгенерирует уникальный ID
            name = zoneName,
            points = points,
            isActive = true
        )
        
        viewModel.addZone(newZone)
        
        // Очищаем текущий полигон
        currentPolygon?.let {
            val map = mapView?.map ?: return@let
            val mapObjects = map.mapObjects ?: return@let
            mapObjects.remove(it)
            currentPolygon = null
        }
        
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
        inputZoneName = null
        btnFinishZone = null
        btnCancelZone = null
        btnSaveZones = null
        recyclerZones = null
        zonesAdapter = null
    }
}

