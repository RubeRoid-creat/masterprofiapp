package com.example.bestapp.ui.client

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.bestapp.R
import com.example.bestapp.ui.auth.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class CreateOrderFragment : Fragment() {
    
    private val viewModel: CreateOrderViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    private var deviceTypeDropdown: AutoCompleteTextView? = null
    private var deviceBrandInput: TextInputEditText? = null
    private var deviceModelInput: TextInputEditText? = null
    private var problemInput: TextInputEditText? = null
    private var addressInput: TextInputEditText? = null
    private var arrivalTimeInput: TextInputEditText? = null
    private var urgentSwitch: SwitchMaterial? = null
    private var createButton: MaterialButton? = null
    private var testOrderButton: MaterialButton? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_order, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupDeviceTypeDropdown()
        setupObservers()
        setupButtons()
    }
    
    private fun initViews(view: View) {
        deviceTypeDropdown = view.findViewById(R.id.device_type_dropdown)
        deviceBrandInput = view.findViewById(R.id.device_brand_input)
        deviceModelInput = view.findViewById(R.id.device_model_input)
        problemInput = view.findViewById(R.id.problem_input)
        addressInput = view.findViewById(R.id.address_input)
        arrivalTimeInput = view.findViewById(R.id.arrival_time_input)
        urgentSwitch = view.findViewById(R.id.urgent_switch)
        createButton = view.findViewById(R.id.create_order_button)
        testOrderButton = view.findViewById(R.id.test_order_button)
    }
    
    private fun setupDeviceTypeDropdown() {
        val deviceTypes = arrayOf(
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
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, deviceTypes)
        deviceTypeDropdown?.setAdapter(adapter)
        
        deviceTypeDropdown?.setOnItemClickListener { _, _, position, _ ->
            viewModel.setDeviceType(deviceTypes[position])
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                createButton?.isEnabled = !isLoading
                testOrderButton?.isEnabled = !isLoading
                createButton?.text = if (isLoading) "Создание..." else "Создать заказ"
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.orderCreated.collect { response ->
                response?.let {
                    Toast.makeText(
                        context,
                        "Заказ №${it.order.id} создан! Ищем мастера...",
                        Toast.LENGTH_LONG
                    ).show()
                    clearInputs()
                }
            }
        }
    }
    
    private fun setupButtons() {
        createButton?.setOnClickListener {
            collectFormData()
            viewModel.createOrder()
        }
        
        testOrderButton?.setOnClickListener {
            // Быстрое создание тестового заказа
            viewModel.createTestOrder()
        }
        
        urgentSwitch?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsUrgent(isChecked)
        }

        arrivalTimeInput?.setOnClickListener {
            showArrivalTimePicker()
        }
        arrivalTimeInput?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showArrivalTimePicker()
        }
    }
    
    private fun collectFormData() {
        viewModel.setDeviceType(deviceTypeDropdown?.text?.toString() ?: "")
        viewModel.setDeviceBrand(deviceBrandInput?.text?.toString() ?: "")
        viewModel.setDeviceModel(deviceModelInput?.text?.toString() ?: "")
        viewModel.setProblemDescription(problemInput?.text?.toString() ?: "")
        viewModel.setAddress(addressInput?.text?.toString() ?: "")
        viewModel.setArrivalTime(arrivalTimeInput?.text?.toString() ?: "")
    }
    
    private fun clearInputs() {
        deviceTypeDropdown?.setText("", false)
        deviceBrandInput?.text?.clear()
        deviceModelInput?.text?.clear()
        problemInput?.text?.clear()
        addressInput?.text?.clear()
        arrivalTimeInput?.text?.clear()
        urgentSwitch?.isChecked = false
    }

    /**
     * Диалог выбора желаемого интервала времени прибытия: "HH:MM - HH:MM".
     */
    private fun showArrivalTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Сначала выбираем время "с"
        TimePickerDialog(requireContext(), { _, startHour, startMinute ->
            val startStr = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)

            // Затем сразу предлагаем выбрать время "до"
            TimePickerDialog(requireContext(), { _, endHour, endMinute ->
                val endStr = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)
                arrivalTimeInput?.setText("$startStr - $endStr")
            }, hour, minute, true).show()

        }, hour, minute, true).show()
    }
}







