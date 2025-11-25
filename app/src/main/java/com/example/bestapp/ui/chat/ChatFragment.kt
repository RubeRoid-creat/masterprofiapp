package com.example.bestapp.ui.chat

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
import com.example.bestapp.api.WebSocketChatClient
import com.example.bestapp.api.models.ApiChatMessage
import com.example.bestapp.api.models.SendChatMessageRequest
import com.example.bestapp.auth.AuthManager
import com.example.bestapp.data.PreferencesManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatFragment : Fragment() {
    
    private var orderId: Long = 0
    private var currentUserId: Long = 0
    private val apiRepository = ApiRepository()
    private lateinit var adapter: ChatAdapter
    private var webSocketClient: WebSocketChatClient? = null
    
    private var toolbar: MaterialToolbar? = null
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
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        orderId = arguments?.getLong("orderId") ?: 0L
        if (orderId == 0L) {
            Toast.makeText(context, "Ошибка: не указан ID заказа", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        
        initViews(view)
        setupToolbar()
        setupRecyclerView()
        setupInput()
        loadCurrentUser()
        loadMessages()
        setupWebSocket()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        recyclerMessages = view.findViewById(R.id.recycler_messages)
        inputMessage = view.findViewById(R.id.input_message)
        btnSend = view.findViewById(R.id.btn_send)
        btnAttachImage = view.findViewById(R.id.btn_attach_image)
        progressLoading = view.findViewById(R.id.progress_loading)
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ChatAdapter(currentUserId)
        recyclerMessages?.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true // Прокрутка снизу вверх
            }
            adapter = this@ChatFragment.adapter
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val authManager = AuthManager(requireContext())
                val userId = authManager.userId.first()
                currentUserId = userId ?: 0L
                if (currentUserId == 0L) {
                    Toast.makeText(context, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return@launch
                }
                // Обновляем адаптер с правильным userId
                adapter = ChatAdapter(currentUserId)
                recyclerMessages?.adapter = adapter
            } catch (e: Exception) {
                Log.e("ChatFragment", "Ошибка загрузки пользователя", e)
            }
        }
    }
    
    private fun loadMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                progressLoading?.visibility = View.VISIBLE
                val result = apiRepository.getChatMessages(orderId)
                result.onSuccess { messages ->
                    adapter.submitList(messages) {
                        // Прокручиваем вниз после загрузки
                        if (messages.isNotEmpty()) {
                            recyclerMessages?.scrollToPosition(messages.size - 1)
                        }
                    }
                    // Устанавливаем сообщения в WebSocket клиент
                    webSocketClient?.setMessages(messages)
                    progressLoading?.visibility = View.GONE
                }.onFailure { error ->
                    Log.e("ChatFragment", "Ошибка загрузки сообщений", error)
                    Toast.makeText(context, "Ошибка загрузки сообщений: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressLoading?.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("ChatFragment", "Ошибка загрузки сообщений", e)
                progressLoading?.visibility = View.GONE
            }
        }
    }
    
    private fun setupWebSocket() {
        val prefsManager = PreferencesManager.getInstance(requireContext())
        webSocketClient = WebSocketChatClient(prefsManager)
        
        // Подписываемся на обновления сообщений
        viewLifecycleOwner.lifecycleScope.launch {
            webSocketClient?.messages?.collect { messages ->
                adapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        recyclerMessages?.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }
        
        // Подключаемся и присоединяемся к чату
        webSocketClient?.connect()
        webSocketClient?.joinOrderChat(orderId)
    }
    
    private fun sendMessage() {
        val messageText = inputMessage?.text?.toString()?.trim()
        if (messageText.isNullOrEmpty()) {
            return
        }
        
        // Пытаемся отправить через WebSocket (быстрее)
        if (webSocketClient?.isConnected?.value == true) {
            inputMessage?.setText("") // Очищаем поле ввода
            webSocketClient?.sendMessage(orderId, messageText)
            // Сообщение появится автоматически через WebSocket
        } else {
            // Fallback на REST API
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    inputMessage?.setText("") // Очищаем поле ввода
                    val result = apiRepository.sendChatMessage(orderId, SendChatMessageRequest(messageText))
                    result.onSuccess { message ->
                        // Добавляем новое сообщение в список
                        webSocketClient?.addMessage(message)
                        val currentList = adapter.currentList.toMutableList()
                        currentList.add(message)
                        adapter.submitList(currentList) {
                            recyclerMessages?.scrollToPosition(currentList.size - 1)
                        }
                    }.onFailure { error ->
                        Log.e("ChatFragment", "Ошибка отправки сообщения", error)
                        Toast.makeText(context, "Ошибка отправки: ${error.message}", Toast.LENGTH_SHORT).show()
                        // Возвращаем текст в поле ввода
                        inputMessage?.setText(messageText)
                    }
                } catch (e: Exception) {
                    Log.e("ChatFragment", "Ошибка отправки сообщения", e)
                    inputMessage?.setText(messageText)
                }
            }
        }
    }
    
    private fun checkPermissionAndPickImage() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    private fun uploadImage(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                progressLoading?.visibility = View.VISIBLE
                
                // Получаем файл из URI
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val file = File.createTempFile("chat_image_", ".jpg", requireContext().cacheDir)
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Создаем MultipartBody.Part
                val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                
                val result = apiRepository.sendChatImage(orderId, imagePart)
                result.onSuccess { message ->
                    // Добавляем новое сообщение в список
                    val currentList = adapter.currentList.toMutableList()
                    currentList.add(message)
                    adapter.submitList(currentList) {
                        recyclerMessages?.scrollToPosition(currentList.size - 1)
                    }
                    progressLoading?.visibility = View.GONE
                }.onFailure { error ->
                    Log.e("ChatFragment", "Ошибка загрузки изображения", error)
                    Toast.makeText(context, "Ошибка загрузки: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressLoading?.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("ChatFragment", "Ошибка загрузки изображения", e)
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                progressLoading?.visibility = View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        webSocketClient?.disconnect()
        webSocketClient = null
        toolbar = null
        recyclerMessages = null
        inputMessage = null
        btnSend = null
        btnAttachImage = null
        progressLoading = null
    }
}

