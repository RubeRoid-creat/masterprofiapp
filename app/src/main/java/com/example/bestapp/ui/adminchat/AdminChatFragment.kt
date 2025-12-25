package com.example.bestapp.ui.adminchat

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiAdminChatMessage
import com.example.bestapp.api.models.SendAdminChatMessageRequest
import com.example.bestapp.ui.chat.ChatAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AdminChatFragment : Fragment() {
    
    private var currentUserId: Long = 0
    private val apiRepository = ApiRepository()
    private lateinit var adapter: ChatAdapter
    private var recyclerMessages: RecyclerView? = null
    private var inputMessage: TextInputEditText? = null
    private var btnSend: FloatingActionButton? = null
    private var btnAttachImage: ShapeableImageView? = null
    private var progressLoading: ProgressBar? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImage(uri)
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
        return inflater.inflate(R.layout.fragment_admin_chat, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupInput()
        loadCurrentUser()
        loadMessages()
    }
    
    private fun initViews(view: View) {
        recyclerMessages = view.findViewById(R.id.recycler_messages)
        inputMessage = view.findViewById(R.id.input_message)
        btnSend = view.findViewById(R.id.btn_send)
        btnAttachImage = view.findViewById(R.id.btn_attach_image)
        progressLoading = view.findViewById(R.id.progress_loading)
    }
    
    private fun setupRecyclerView() {
        adapter = ChatAdapter(currentUserId)
        recyclerMessages?.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = this@AdminChatFragment.adapter
        }
    }
    
    private fun setupInput() {
        btnSend?.setOnClickListener {
            sendMessage()
        }
        
        btnAttachImage?.setOnClickListener {
            checkPermissionAndPickImage()
        }
        
        inputMessage?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }
    
    private fun loadCurrentUser() {
        lifecycleScope.launch {
            val prefsManager = com.example.bestapp.data.PreferencesManager.getInstance(requireContext())
            val authManager = com.example.bestapp.auth.AuthManager(requireContext())
            currentUserId = authManager.getUserId() ?: 0L
            
            // Обновляем адаптер с правильным userId
            adapter = ChatAdapter(currentUserId)
            recyclerMessages?.adapter = adapter
        }
    }
    
    private fun loadMessages() {
        lifecycleScope.launch {
            progressLoading?.visibility = View.VISIBLE
            try {
                val result = apiRepository.getAdminChatMessages()
                result.onSuccess { messages ->
                    val chatMessages = messages.map { adminMsg ->
                        com.example.bestapp.api.models.ApiChatMessage(
                            id = adminMsg.id,
                            orderId = 0L, // Нет привязки к заказу
                            senderId = adminMsg.senderId,
                            senderName = adminMsg.senderName,
                            senderRole = adminMsg.senderRole,
                            messageType = adminMsg.messageType,
                            messageText = adminMsg.messageText,
                            imageUrl = adminMsg.imageUrl,
                            imageThumbnailUrl = adminMsg.imageThumbnailUrl,
                            readAt = adminMsg.readAt,
                            createdAt = adminMsg.createdAt
                        )
                    }
                    adapter.submitList(chatMessages) {
                        if (chatMessages.isNotEmpty()) {
                            recyclerMessages?.scrollToPosition(chatMessages.size - 1)
                        }
                    }
                    progressLoading?.visibility = View.GONE
                }.onFailure { error ->
                    Toast.makeText(context, "Ошибка загрузки сообщений: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressLoading?.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("AdminChatFragment", "Error loading messages", e)
                progressLoading?.visibility = View.GONE
            }
        }
    }
    
    private fun sendMessage() {
        val messageText = inputMessage?.text?.toString()?.trim()
        if (messageText.isNullOrEmpty()) {
            return
        }
        
        lifecycleScope.launch {
            inputMessage?.setText("")
            progressLoading?.visibility = View.VISIBLE
            
            val result = apiRepository.sendAdminChatMessage(messageText)
            result.onSuccess { message ->
                val chatMessage = com.example.bestapp.api.models.ApiChatMessage(
                    id = message.id,
                    orderId = 0L,
                    senderId = message.senderId,
                    senderName = message.senderName,
                    senderRole = message.senderRole,
                    messageType = message.messageType,
                    messageText = message.messageText,
                    imageUrl = message.imageUrl,
                    imageThumbnailUrl = message.imageThumbnailUrl,
                    readAt = message.readAt,
                    createdAt = message.createdAt
                )
                val currentList = adapter.currentList.toMutableList()
                currentList.add(chatMessage)
                adapter.submitList(currentList) {
                    recyclerMessages?.scrollToPosition(currentList.size - 1)
                }
                progressLoading?.visibility = View.GONE
            }.onFailure { error ->
                inputMessage?.setText(messageText)
                Toast.makeText(context, "Ошибка отправки: ${error.message}", Toast.LENGTH_SHORT).show()
                progressLoading?.visibility = View.GONE
            }
        }
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
    
    private fun uploadImage(uri: Uri) {
        lifecycleScope.launch {
            progressLoading?.visibility = View.VISIBLE
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Не удалось прочитать файл")
                inputStream.close()
                
                val file = File.createTempFile("admin_chat_image_", ".jpg", requireContext().cacheDir)
                file.writeBytes(bytes)
                
                val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                
                val result = apiRepository.sendAdminChatImage(imagePart)
                result.onSuccess { message ->
                    val chatMessage = com.example.bestapp.api.models.ApiChatMessage(
                        id = message.id,
                        orderId = 0L,
                        senderId = message.senderId,
                        senderName = message.senderName,
                        senderRole = message.senderRole,
                        messageType = message.messageType,
                        messageText = message.messageText,
                        imageUrl = message.imageUrl,
                        imageThumbnailUrl = message.imageThumbnailUrl,
                        readAt = message.readAt,
                        createdAt = message.createdAt
                    )
                    val currentList = adapter.currentList.toMutableList()
                    currentList.add(chatMessage)
                    adapter.submitList(currentList) {
                        recyclerMessages?.scrollToPosition(currentList.size - 1)
                    }
                    progressLoading?.visibility = View.GONE
                }.onFailure { error ->
                    Toast.makeText(context, "Ошибка загрузки изображения: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressLoading?.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("AdminChatFragment", "Error uploading image", e)
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                progressLoading?.visibility = View.GONE
            }
        }
    }
}
