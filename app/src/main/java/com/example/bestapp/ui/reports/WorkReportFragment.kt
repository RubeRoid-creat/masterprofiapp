package com.example.bestapp.ui.reports

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.content.pm.PackageManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.models.PartUsed
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

class WorkReportFragment : Fragment() {
    private val viewModel: WorkReportViewModel by viewModels()
    
    private var toolbar: MaterialToolbar? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null
    private var recyclerTemplates: RecyclerView? = null
    private var inputWorkDescription: TextInputEditText? = null
    private var recyclerParts: RecyclerView? = null
    private var btnAddPart: MaterialButton? = null
    private var inputWorkDuration: TextInputEditText? = null
    private var inputLaborCost: TextInputEditText? = null
    private var textPartsCost: TextView? = null
    private var textTotalCost: TextView? = null
    private var recyclerBeforePhotos: RecyclerView? = null
    private var recyclerAfterPhotos: RecyclerView? = null
    private var btnAddBeforePhoto: MaterialButton? = null
    private var btnAddAfterPhoto: MaterialButton? = null
    private var btnCreateReport: MaterialButton? = null
    
    private lateinit var templateAdapter: ReportTemplateAdapter
    private lateinit var partsAdapter: PartAdapter
    private lateinit var beforePhotosAdapter: PhotoAdapter
    private lateinit var afterPhotosAdapter: PhotoAdapter
    
    private var isBeforePhoto = true
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"))
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                savePhotoFromUri(uri)
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(context, "Нужно разрешение для выбора изображения", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_work_report, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val orderId = arguments?.getLong("orderId", 0L) ?: 0L
        if (orderId > 0) {
            viewModel.orderId = orderId
        }
        
        initViews(view)
        setupToolbar()
        setupAdapters()
        setupButtons()
        observeUiState()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        progressBar = view.findViewById(R.id.progress_loading)
        errorText = view.findViewById(R.id.text_error)
        recyclerTemplates = view.findViewById(R.id.recycler_templates)
        inputWorkDescription = view.findViewById(R.id.input_work_description)
        recyclerParts = view.findViewById(R.id.recycler_parts)
        btnAddPart = view.findViewById(R.id.btn_add_part)
        inputWorkDuration = view.findViewById(R.id.input_work_duration)
        inputLaborCost = view.findViewById(R.id.input_labor_cost)
        textPartsCost = view.findViewById(R.id.text_parts_cost)
        textTotalCost = view.findViewById(R.id.text_total_cost)
        recyclerBeforePhotos = view.findViewById(R.id.recycler_before_photos)
        recyclerAfterPhotos = view.findViewById(R.id.recycler_after_photos)
        btnAddBeforePhoto = view.findViewById(R.id.btn_add_before_photo)
        btnAddAfterPhoto = view.findViewById(R.id.btn_add_after_photo)
        btnCreateReport = view.findViewById(R.id.btn_create_report)
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupAdapters() {
        templateAdapter = ReportTemplateAdapter { template ->
            viewModel.selectTemplate(template)
            Toast.makeText(context, "Шаблон \"${template.name}\" применен", Toast.LENGTH_SHORT).show()
        }
        recyclerTemplates?.apply {
            adapter = templateAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        partsAdapter = PartAdapter { index ->
            viewModel.removePart(index)
        }
        recyclerParts?.apply {
            adapter = partsAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        beforePhotosAdapter = PhotoAdapter { index ->
            viewModel.removeBeforePhoto(index)
        }
        recyclerBeforePhotos?.apply {
            adapter = beforePhotosAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        
        afterPhotosAdapter = PhotoAdapter { index ->
            viewModel.removeAfterPhoto(index)
        }
        recyclerAfterPhotos?.apply {
            adapter = afterPhotosAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }
    
    private fun setupButtons() {
        btnAddPart?.setOnClickListener {
            showAddPartDialog()
        }
        
        inputWorkDescription?.doAfterTextChanged { text ->
            viewModel.setWorkDescription(text?.toString() ?: "")
        }
        
        inputWorkDuration?.doAfterTextChanged { text ->
            val duration = text?.toString()?.toIntOrNull()
            viewModel.setWorkDuration(duration)
        }
        
        inputLaborCost?.doAfterTextChanged { text ->
            val cost = text?.toString()?.toDoubleOrNull() ?: 0.0
            viewModel.setLaborCost(cost)
        }
        
        btnAddBeforePhoto?.setOnClickListener {
            isBeforePhoto = true
            checkPermissionAndOpenPicker()
        }
        
        btnAddAfterPhoto?.setOnClickListener {
            isBeforePhoto = false
            checkPermissionAndOpenPicker()
        }
        
        btnCreateReport?.setOnClickListener {
            createReport()
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
                
                // Обновляем шаблоны
                templateAdapter.submitList(state.templates)
                
                // Обновляем описание работы
                if (inputWorkDescription?.text?.toString() != state.workDescription) {
                    inputWorkDescription?.setText(state.workDescription)
                }
                
                // Обновляем запчасти
                partsAdapter.submitList(state.partsUsed)
                
                // Обновляем стоимость
                textPartsCost?.text = "Стоимость запчастей: ${currencyFormat.format(state.partsCost)}"
                textTotalCost?.text = "Итого: ${currencyFormat.format(state.totalCost)}"
                
                // Обновляем фото
                beforePhotosAdapter.submitList(state.beforePhotos)
                afterPhotosAdapter.submitList(state.afterPhotos)
                
                // Если отчет создан, возвращаемся назад
                if (state.isReportCreated) {
                    Toast.makeText(context, "Отчет создан успешно", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }
    
    private fun showAddPartDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_part, null)
        val inputName = dialogView.findViewById<TextInputEditText>(R.id.input_part_name)
        val inputQuantity = dialogView.findViewById<TextInputEditText>(R.id.input_part_quantity)
        val inputCost = dialogView.findViewById<TextInputEditText>(R.id.input_part_cost)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить запчасть")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = inputName.text?.toString()?.takeIf { it.isNotBlank() }
                val quantity = inputQuantity.text?.toString()?.toIntOrNull() ?: 1
                val cost = inputCost.text?.toString()?.toDoubleOrNull() ?: 0.0
                
                if (name != null) {
                    viewModel.addPart(PartUsed(name, quantity, cost))
                } else {
                    Toast.makeText(context, "Введите название запчасти", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun checkPermissionAndOpenPicker() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    private fun savePhotoFromUri(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File.createTempFile("report_photo_", ".jpg", requireContext().cacheDir)
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            if (isBeforePhoto) {
                viewModel.addBeforePhoto(file.absolutePath)
            } else {
                viewModel.addAfterPhoto(file.absolutePath)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка сохранения фото: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createReport() {
        val state = viewModel.uiState.value
        if (state.workDescription.isBlank()) {
            Toast.makeText(context, "Введите описание выполненной работы", Toast.LENGTH_SHORT).show()
            return
        }
        if (state.totalCost <= 0) {
            Toast.makeText(context, "Укажите стоимость работы", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.createReport()
    }
}

