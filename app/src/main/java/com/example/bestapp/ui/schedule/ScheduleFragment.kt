package com.example.bestapp.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment() {
    private val viewModel: ScheduleViewModel by viewModels()
    
    private var toolbar: MaterialToolbar? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null
    private var btnSelectDate: MaterialButton? = null
    private var cardSelectedDate: MaterialCardView? = null
    private var textSelectedDate: TextView? = null
    private var switchAvailable: SwitchMaterial? = null
    private var layoutStartTime: TextInputLayout? = null
    private var layoutEndTime: TextInputLayout? = null
    private var inputStartTime: TextInputEditText? = null
    private var inputEndTime: TextInputEditText? = null
    private var inputNote: TextInputEditText? = null
    private var btnSaveSchedule: MaterialButton? = null
    private var btnDeleteSchedule: MaterialButton? = null
    private var btnSetWeek: MaterialButton? = null
    private var btnSetMonth: MaterialButton? = null
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val dateFormatApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupToolbar()
        setupButtons()
        observeUiState()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        progressBar = view.findViewById(R.id.progress_loading)
        errorText = view.findViewById(R.id.text_error)
        btnSelectDate = view.findViewById(R.id.btn_select_date)
        cardSelectedDate = view.findViewById(R.id.card_selected_date)
        textSelectedDate = view.findViewById(R.id.text_selected_date)
        switchAvailable = view.findViewById(R.id.switch_available)
        layoutStartTime = view.findViewById(R.id.layout_start_time)
        layoutEndTime = view.findViewById(R.id.layout_end_time)
        inputStartTime = view.findViewById(R.id.input_start_time)
        inputEndTime = view.findViewById(R.id.input_end_time)
        inputNote = view.findViewById(R.id.input_note)
        btnSaveSchedule = view.findViewById(R.id.btn_save_schedule)
        btnDeleteSchedule = view.findViewById(R.id.btn_delete_schedule)
        btnSetWeek = view.findViewById(R.id.btn_set_week)
        btnSetMonth = view.findViewById(R.id.btn_set_month)
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupButtons() {
        btnSelectDate?.setOnClickListener {
            showDatePicker()
        }
        
        layoutStartTime?.setOnClickListener {
            showTimePicker(true)
        }
        inputStartTime?.setOnClickListener {
            showTimePicker(true)
        }
        inputStartTime?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showTimePicker(true)
        }
        
        layoutEndTime?.setOnClickListener {
            showTimePicker(false)
        }
        inputEndTime?.setOnClickListener {
            showTimePicker(false)
        }
        inputEndTime?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showTimePicker(false)
        }
        
        btnSaveSchedule?.setOnClickListener {
            saveSchedule()
        }
        
        btnDeleteSchedule?.setOnClickListener {
            deleteSchedule()
        }
        
        btnSetWeek?.setOnClickListener {
            showBatchScheduleDialog(7)
        }
        
        btnSetMonth?.setOnClickListener {
            showBatchScheduleDialog(30)
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
                
                state.selectedDate?.let { date ->
                    showDateInfo(date)
                } ?: run {
                    hideDateInfo()
                }
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            val dateStr = dateFormatApi.format(selectedCalendar.time)
            viewModel.selectDate(dateStr)
        }, year, month, day).show()
    }
    
    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            if (isStartTime) {
                inputStartTime?.setText(timeStr)
            } else {
                inputEndTime?.setText(timeStr)
            }
        }, hour, minute, true).show()
    }
    
    private fun showDateInfo(date: String) {
        cardSelectedDate?.visibility = View.VISIBLE
        
        val dateObj = dateFormatApi.parse(date)
        textSelectedDate?.text = "Дата: ${dateFormat.format(dateObj ?: Date())}"
        
        val scheduleItem = viewModel.getScheduleForDate(date)
        scheduleItem?.let {
            switchAvailable?.isChecked = it.isAvailable
            inputStartTime?.setText(it.startTime ?: "")
            inputEndTime?.setText(it.endTime ?: "")
            inputNote?.setText(it.note ?: "")
        } ?: run {
            switchAvailable?.isChecked = true
            inputStartTime?.setText("")
            inputEndTime?.setText("")
            inputNote?.setText("")
        }
    }
    
    private fun hideDateInfo() {
        cardSelectedDate?.visibility = View.GONE
    }
    
    private fun saveSchedule() {
        val selectedDate = viewModel.uiState.value.selectedDate ?: return
        
        val isAvailable = switchAvailable?.isChecked ?: true
        val startTime = inputStartTime?.text?.toString()?.takeIf { it.isNotBlank() }
        val endTime = inputEndTime?.text?.toString()?.takeIf { it.isNotBlank() }
        val note = inputNote?.text?.toString()?.takeIf { it.isNotBlank() }
        
        viewModel.createOrUpdateSchedule(selectedDate, startTime, endTime, isAvailable, note)
        Toast.makeText(context, "Расписание сохранено", Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteSchedule() {
        val selectedDate = viewModel.uiState.value.selectedDate ?: return
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить расписание")
            .setMessage("Вы уверены, что хотите удалить расписание на эту дату?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteSchedule(selectedDate)
                Toast.makeText(context, "Расписание удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showBatchScheduleDialog(days: Int) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val startCalendar = Calendar.getInstance()
            startCalendar.set(selectedYear, selectedMonth, selectedDay)
            val startDate = dateFormatApi.format(startCalendar.time)
            
            val endCalendar = Calendar.getInstance()
            endCalendar.set(selectedYear, selectedMonth, selectedDay)
            endCalendar.add(Calendar.DAY_OF_MONTH, days - 1)
            val endDate = dateFormatApi.format(endCalendar.time)
            
            // Показываем диалог для настройки времени
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Настройка расписания")
                .setMessage("Установить расписание с ${dateFormat.format(startCalendar.time)} по ${dateFormat.format(endCalendar.time)}?")
                .setPositiveButton("Установить") { _, _ ->
                    val startTime = inputStartTime?.text?.toString()?.takeIf { it.isNotBlank() }
                    val endTime = inputEndTime?.text?.toString()?.takeIf { it.isNotBlank() }
                    val isAvailable = switchAvailable?.isChecked ?: true
                    
                    // Только рабочие дни (понедельник-пятница)
                    val daysOfWeek = if (isAvailable) listOf(1, 2, 3, 4, 5) else null
                    
                    viewModel.createBatchSchedule(startDate, endDate, startTime, endTime, isAvailable, daysOfWeek)
                    Toast.makeText(context, "Расписание установлено", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }, year, month, day).show()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}


