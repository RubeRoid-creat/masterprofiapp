package com.example.bestapp.ui.settings

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
import com.example.bestapp.R
import com.example.bestapp.data.AutoAcceptSettings
import com.example.bestapp.data.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AutoAcceptSettingsFragment : Fragment() {
    
    private val viewModel: AutoAcceptSettingsViewModel by viewModels {
        AutoAcceptSettingsViewModel.provideFactory(requireContext().applicationContext as android.app.Application)
    }
    private val prefsManager by lazy { PreferencesManager.getInstance(requireContext()) }
    
    private var switchEnabled: SwitchMaterial? = null
    private var inputMinPrice: TextInputEditText? = null
    private var inputMaxDistance: TextInputEditText? = null
    private var switchUrgentOnly: SwitchMaterial? = null
    private var chipGroupDeviceTypes: ChipGroup? = null
    private var btnSave: MaterialButton? = null
    
    // Список доступных типов техники
    private val availableDeviceTypes = listOf(
        "Стиральная машина",
        "Холодильник",
        "Посудомоечная машина",
        "Духовой шкаф",
        "Микроволновая печь",
        "Морозильный ларь",
        "Варочная панель",
        "Ноутбук",
        "Десктоп",
        "Кофемашина",
        "Кондиционер",
        "Водонагреватель"
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_auto_accept_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews(view)
        setupToolbar()
        loadCurrentSettings()
        setupSaveButton()
    }
    
    private fun setupViews(view: View) {
        switchEnabled = view.findViewById(R.id.switch_auto_accept_enabled)
        inputMinPrice = view.findViewById(R.id.input_min_price)
        inputMaxDistance = view.findViewById(R.id.input_max_distance)
        switchUrgentOnly = view.findViewById(R.id.switch_urgent_only)
        chipGroupDeviceTypes = view.findViewById(R.id.chip_group_device_types)
        btnSave = view.findViewById(R.id.btn_save_settings)
        
        // Создаем чипы для типов техники
        setupDeviceTypeChips()
        
        // Обновляем доступность полей при изменении переключателя
        switchEnabled?.setOnCheckedChangeListener { _, isChecked ->
            updateFieldsEnabled(isChecked)
        }
    }
    
    private fun setupToolbar() {
        view?.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)?.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }
    
    private fun setupDeviceTypeChips() {
        chipGroupDeviceTypes?.removeAllViews()
        
        availableDeviceTypes.forEach { deviceType ->
            val chip = Chip(requireContext())
            chip.text = deviceType
            chip.isCheckable = true
            chip.isChecked = false
            chipGroupDeviceTypes?.addView(chip)
        }
    }
    
    private fun loadCurrentSettings() {
        val settings = prefsManager.getAutoAcceptSettings()
        
        switchEnabled?.isChecked = settings.isEnabled
        inputMinPrice?.setText(settings.minPrice?.toInt()?.toString() ?: "")
        inputMaxDistance?.setText(settings.maxDistance?.div(1000)?.toInt()?.toString() ?: "")
        switchUrgentOnly?.isChecked = settings.acceptUrgentOnly
        
        // Восстанавливаем выбранные типы техники
        settings.deviceTypes.forEach { deviceType ->
            chipGroupDeviceTypes?.let { group ->
                val childCount = group.childCount
                for (i in 0 until childCount) {
                    val chip = group.getChildAt(i) as? Chip
                    if (chip?.text == deviceType) {
                        chip.isChecked = true
                        break
                    }
                }
            }
        }
        
        updateFieldsEnabled(settings.isEnabled)
    }
    
    private fun updateFieldsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.5f
        inputMinPrice?.alpha = alpha
        inputMaxDistance?.alpha = alpha
        switchUrgentOnly?.alpha = alpha
        chipGroupDeviceTypes?.alpha = alpha
        
        inputMinPrice?.isEnabled = enabled
        inputMaxDistance?.isEnabled = enabled
        switchUrgentOnly?.isEnabled = enabled
        chipGroupDeviceTypes?.isEnabled = enabled
    }
    
    private fun setupSaveButton() {
        btnSave?.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun saveSettings() {
        val isEnabled = switchEnabled?.isChecked ?: false
        val minPrice = inputMinPrice?.text?.toString()?.toDoubleOrNull()
        val maxDistanceKm = inputMaxDistance?.text?.toString()?.toDoubleOrNull()
        val maxDistance = maxDistanceKm?.times(1000) // Конвертируем км в метры
        val acceptUrgentOnly = switchUrgentOnly?.isChecked ?: false
        
        // Собираем выбранные типы техники
        val selectedDeviceTypes = mutableSetOf<String>()
        chipGroupDeviceTypes?.let { group ->
            val childCount = group.childCount
            for (i in 0 until childCount) {
                val chip = group.getChildAt(i) as? Chip
                if (chip?.isChecked == true) {
                    chip.text?.toString()?.let { selectedDeviceTypes.add(it) }
                }
            }
        }
        
        val settings = AutoAcceptSettings(
            isEnabled = isEnabled,
            minPrice = minPrice,
            maxDistance = maxDistance,
            deviceTypes = selectedDeviceTypes,
            acceptUrgentOnly = acceptUrgentOnly
        )
        
        // Сохраняем настройки
        prefsManager.setAutoAcceptSettings(settings)
        viewModel.updateSettings(settings)
        
        Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        switchEnabled = null
        inputMinPrice = null
        inputMaxDistance = null
        switchUrgentOnly = null
        chipGroupDeviceTypes = null
        btnSave = null
    }
}

