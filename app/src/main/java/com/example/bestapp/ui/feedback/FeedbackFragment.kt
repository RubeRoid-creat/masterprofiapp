package com.example.bestapp.ui.feedback

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class FeedbackFragment : Fragment() {
    
    private val apiRepository = ApiRepository()
    private var selectedFeedbackType: String = "suggestion"
    private val selectedAttachments = mutableListOf<Uri>()
    private lateinit var attachmentsAdapter: AttachmentsAdapter
    
    private var inputFeedbackType: TextInputEditText? = null
    private var inputSubject: TextInputEditText? = null
    private var inputMessage: TextInputEditText? = null
    private var recyclerAttachments: RecyclerView? = null
    private var btnAddAttachment: MaterialButton? = null
    private var btnSendFeedback: MaterialButton? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedAttachments.add(uri)
                attachmentsAdapter.submitList(selectedAttachments.toList())
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
        return inflater.inflate(R.layout.fragment_feedback, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
    }
    
    private fun initViews(view: View) {
        inputFeedbackType = view.findViewById(R.id.input_feedback_type)
        inputSubject = view.findViewById(R.id.input_subject)
        inputMessage = view.findViewById(R.id.input_message)
        recyclerAttachments = view.findViewById(R.id.recycler_attachments)
        btnAddAttachment = view.findViewById(R.id.btn_add_attachment)
        btnSendFeedback = view.findViewById(R.id.btn_send_feedback)
        
        // Настройка адаптера для вложений
        attachmentsAdapter = AttachmentsAdapter(
            onRemoveClick = { position ->
                selectedAttachments.removeAt(position)
                attachmentsAdapter.submitList(selectedAttachments.toList())
            }
        )
        recyclerAttachments?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = attachmentsAdapter
        }
        
        // Устанавливаем начальный тип обратной связи
        updateFeedbackTypeDisplay()
    }
    
    private fun setupListeners() {
        inputFeedbackType?.setOnClickListener {
            showFeedbackTypeDialog()
        }
        
        btnAddAttachment?.setOnClickListener {
            if (selectedAttachments.size >= 5) {
                Toast.makeText(context, "Можно прикрепить не более 5 файлов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkPermissionAndPickImage()
        }
        
        btnSendFeedback?.setOnClickListener {
            sendFeedback()
        }
    }
    
    private fun showFeedbackTypeDialog() {
        val types = arrayOf(
            "Предложение" to "suggestion",
            "Сообщение об ошибке" to "bug_report",
            "Жалоба" to "complaint",
            "Благодарность" to "praise",
            "Другое" to "other"
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите тип обращения")
            .setItems(types.map { it.first }.toTypedArray()) { _, which ->
                selectedFeedbackType = types[which].second
                updateFeedbackTypeDisplay()
            }
            .show()
    }
    
    private fun updateFeedbackTypeDisplay() {
        val typeNames = mapOf(
            "suggestion" to "Предложение",
            "bug_report" to "Сообщение об ошибке",
            "complaint" to "Жалоба",
            "praise" to "Благодарность",
            "other" to "Другое"
        )
        inputFeedbackType?.setText(typeNames[selectedFeedbackType] ?: "Предложение")
    }
    
    private fun checkPermissionAndPickImage() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    private fun sendFeedback() {
        val subject = inputSubject?.text?.toString()?.trim()
        val message = inputMessage?.text?.toString()?.trim()
        
        if (subject.isNullOrEmpty()) {
            Toast.makeText(context, "Введите тему", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (message.isNullOrEmpty()) {
            Toast.makeText(context, "Введите сообщение", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            btnSendFeedback?.isEnabled = false
            btnSendFeedback?.text = "Отправка..."
            
            try {
                // Подготавливаем вложения
                val attachmentParts = mutableListOf<okhttp3.MultipartBody.Part>()
                selectedAttachments.forEach { uri ->
                    try {
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes() ?: continue
                        inputStream.close()
                        
                        val file = File.createTempFile("feedback_attachment_", ".jpg", requireContext().cacheDir)
                        file.writeBytes(bytes)
                        
                        val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                        val part = okhttp3.MultipartBody.Part.createFormData("attachments", file.name, requestFile)
                        attachmentParts.add(part)
                    } catch (e: Exception) {
                        Log.e("FeedbackFragment", "Error processing attachment", e)
                    }
                }
                
                val result = apiRepository.createFeedback(
                    feedbackType = selectedFeedbackType,
                    subject = subject,
                    message = message,
                    attachments = if (attachmentParts.isNotEmpty()) attachmentParts else null
                )
                
                result.onSuccess {
                    Toast.makeText(context, "Обратная связь отправлена", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }.onFailure { error ->
                    Toast.makeText(context, "Ошибка отправки: ${error.message}", Toast.LENGTH_SHORT).show()
                    btnSendFeedback?.isEnabled = true
                    btnSendFeedback?.text = "Отправить"
                }
            } catch (e: Exception) {
                Log.e("FeedbackFragment", "Error sending feedback", e)
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSendFeedback?.isEnabled = true
                btnSendFeedback?.text = "Отправить"
            }
        }
    }
    
    // Простой адаптер для вложений
    private class AttachmentsAdapter(
        private val onRemoveClick: (Int) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<Uri, AttachmentsAdapter.ViewHolder>(DiffCallback()) {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position), position)
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imagePhoto: ImageView = itemView.findViewById(R.id.image_photo)
            private val btnRemove: MaterialButton = itemView.findViewById(R.id.btn_remove_photo)
            
            fun bind(uri: Uri, position: Int) {
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(uri)
                    .into(imagePhoto)
                
                btnRemove.setOnClickListener {
                    onRemoveClick(position)
                }
            }
        }
        
        class DiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Uri>() {
            override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem == newItem
            }
            
            override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem == newItem
            }
        }
    }
}
