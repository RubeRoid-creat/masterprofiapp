package com.example.bestapp.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiVerificationDocument
import com.example.bestapp.data.DocumentType
import com.example.bestapp.data.VerificationDocument
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.UUID

class VerificationFragment : Fragment() {

    private val documents = mutableListOf<VerificationDocument>()
    private val certificates = mutableListOf<VerificationDocument>()
    private val portfolio = mutableListOf<String>()
    private val uploadedDocuments = mutableListOf<ApiVerificationDocument>()
    private val apiRepository = ApiRepository()
    
    private var currentDocumentType = DocumentType.PASSPORT
    private var inputInn: TextInputEditText? = null
    private var verificationStatus: String? = null
    private var masterInn: String? = null
    
    // Множественный выбор изображений
    private val multipleImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val clipData = result.data?.clipData
            if (clipData != null) {
                // Множественный выбор
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    addDocument(uri)
                }
                Toast.makeText(context, "Добавлено ${clipData.itemCount} изображений", Toast.LENGTH_SHORT).show()
            } else {
                // Одиночный выбор (fallback)
                result.data?.data?.let { uri ->
                    addDocument(uri)
                }
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openMultipleImagePicker()
        } else {
            Toast.makeText(context, "Нужно разрешение для выбора изображений", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_verification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        val recyclerDocs = view.findViewById<RecyclerView>(R.id.recycler_documents)
        val recyclerCerts = view.findViewById<RecyclerView>(R.id.recycler_certificates)
        val recyclerPortfolio = view.findViewById<RecyclerView>(R.id.recycler_portfolio)
        val recyclerUploaded = view.findViewById<RecyclerView>(R.id.recycler_uploaded_documents)
        
        recyclerDocs.layoutManager = LinearLayoutManager(context)
        recyclerCerts.layoutManager = LinearLayoutManager(context)
        recyclerPortfolio.layoutManager = GridLayoutManager(context, 3)
        
        // Для загруженных документов используем GridLayoutManager
        recyclerUploaded?.layoutManager = GridLayoutManager(context, 2)
        
        inputInn = view.findViewById(R.id.input_inn)
        val btnAddDoc = view.findViewById<MaterialButton>(R.id.btn_add_document)
        val btnAddCert = view.findViewById<MaterialButton>(R.id.btn_add_certificate)
        val btnAddPortfolio = view.findViewById<MaterialButton>(R.id.btn_add_portfolio)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btn_submit)
        
        btnAddDoc.setOnClickListener {
            currentDocumentType = DocumentType.PASSPORT
            checkPermissionAndOpenPicker()
        }
        
        btnAddCert.setOnClickListener {
            currentDocumentType = DocumentType.CERTIFICATE
            checkPermissionAndOpenPicker()
        }
        
        btnAddPortfolio.setOnClickListener {
            currentDocumentType = DocumentType.PORTFOLIO
            checkPermissionAndOpenPicker()
        }
        
        btnSubmit.setOnClickListener {
            submitVerification()
        }
        
        // Загружаем уже загруженные документы и статус верификации
        loadVerificationData()
    }
    
    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                openMultipleImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    
    private fun openMultipleImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        multipleImagePickerLauncher.launch(Intent.createChooser(intent, "Выберите изображения"))
    }
    
    private fun loadVerificationData() {
        lifecycleScope.launch {
            try {
                // Загружаем документы с сервера
                val documentsResult = apiRepository.getVerificationDocuments()
                documentsResult.onSuccess { docs ->
                    uploadedDocuments.clear()
                    uploadedDocuments.addAll(docs)
                    updateUploadedDocumentsView()
                    
                    // Определяем статус верификации по документам
                    val statuses = docs.map { it.status }.distinct()
                    verificationStatus = when {
                        statuses.contains("approved") && !statuses.contains("pending") && !statuses.contains("rejected") -> "verified"
                        statuses.contains("pending") -> "pending"
                        statuses.contains("rejected") -> "rejected"
                        else -> "not_verified"
                    }
                    
                    // Загружаем ИНН из профиля мастера
                    loadMasterInn()
                    
                    // Обновляем UI в зависимости от статуса
                    updateUIForStatus()
                }.onFailure { error ->
                    Log.e("VerificationFragment", "Ошибка загрузки документов", error)
                    // Если документов нет, показываем форму загрузки
                    updateUIForStatus()
                }
            } catch (e: Exception) {
                Log.e("VerificationFragment", "Ошибка загрузки данных верификации", e)
                updateUIForStatus()
            }
        }
    }
    
    private fun loadMasterInn() {
        lifecycleScope.launch {
            try {
                // Получаем ИНН из профиля мастера через API
                // Предполагаем, что ИНН уже сохранен в профиле после первой загрузки документа
                // Если нужно, можно добавить отдельный endpoint для получения ИНН
                masterInn?.let { inn ->
                    inputInn?.setText(inn)
                }
            } catch (e: Exception) {
                Log.e("VerificationFragment", "Ошибка загрузки ИНН", e)
            }
        }
    }
    
    private fun updateUIForStatus() {
        view?.let { v ->
            val statusView = v.findViewById<TextView>(R.id.verification_status_text)
            val statusCard = v.findViewById<MaterialCardView>(R.id.verification_status_card)
            val formContainer = v.findViewById<ViewGroup>(R.id.verification_form_container)
            val btnSubmit = v.findViewById<MaterialButton>(R.id.btn_submit)
            
            when (verificationStatus) {
                "verified" -> {
                    // Показываем успешную верификацию
                    statusView?.text = "✅ Вы успешно верифицированы!"
                    statusCard?.visibility = View.VISIBLE
                    formContainer?.visibility = View.GONE
                    btnSubmit?.visibility = View.GONE
                }
                "pending" -> {
                    // Показываем статус "на проверке"
                    statusView?.text = "⏳ Ваши документы на проверке. Пожалуйста, подождите."
                    statusCard?.visibility = View.VISIBLE
                    formContainer?.visibility = View.GONE
                    btnSubmit?.visibility = View.GONE
                }
                "rejected" -> {
                    // Показываем отклонение с возможностью повторной загрузки
                    statusView?.text = "❌ Верификация отклонена. Пожалуйста, загрузите документы заново."
                    statusCard?.visibility = View.VISIBLE
                    formContainer?.visibility = View.VISIBLE
                    btnSubmit?.visibility = View.VISIBLE
                }
                else -> {
                    // Показываем форму загрузки
                    statusCard?.visibility = View.GONE
                    formContainer?.visibility = View.VISIBLE
                    btnSubmit?.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun updateUploadedDocumentsView() {
        view?.let { v ->
            val recyclerUploaded = v.findViewById<RecyclerView>(R.id.recycler_uploaded_documents)
            if (recyclerUploaded != null && uploadedDocuments.isNotEmpty()) {
                recyclerUploaded.visibility = View.VISIBLE
                recyclerUploaded.adapter = UploadedDocumentsAdapter(uploadedDocuments) { document ->
                    // Показываем детали документа или удаляем его
                    showDocumentDetails(document)
                }
            } else {
                recyclerUploaded?.visibility = View.GONE
            }
        }
    }
    
    private fun showDocumentDetails(document: ApiVerificationDocument) {
        val statusText = when (document.status) {
            "pending" -> "На проверке"
            "approved" -> "Одобрен"
            "rejected" -> "Отклонен: ${document.rejectionReason ?: "Причина не указана"}"
            else -> "Неизвестный статус"
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(document.documentName)
            .setMessage("Тип: ${document.documentType}\nСтатус: $statusText\nЗагружен: ${document.createdAt}")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun addDocument(uri: Uri) {
        val fileName = "document_${System.currentTimeMillis()}.jpg"
        val doc = VerificationDocument(
            id = UUID.randomUUID().toString(),
            type = currentDocumentType,
            fileName = fileName,
            filePath = uri.toString()
        )
        
        when (currentDocumentType) {
            DocumentType.PASSPORT, DocumentType.DIPLOMA -> documents.add(doc)
            DocumentType.CERTIFICATE -> certificates.add(doc)
            DocumentType.PORTFOLIO -> portfolio.add(uri.toString())
        }
        
        updateRecyclerViews()
    }
    
    private fun updateRecyclerViews() {
        val recyclerDocs = view?.findViewById<RecyclerView>(R.id.recycler_documents)
        val recyclerCerts = view?.findViewById<RecyclerView>(R.id.recycler_certificates)
        val recyclerPortfolio = view?.findViewById<RecyclerView>(R.id.recycler_portfolio)
        
        recyclerDocs?.visibility = if (documents.isNotEmpty()) View.VISIBLE else View.GONE
        recyclerCerts?.visibility = if (certificates.isNotEmpty()) View.VISIBLE else View.GONE
        recyclerPortfolio?.visibility = if (portfolio.isNotEmpty()) View.VISIBLE else View.GONE
        
        // Обновляем адаптеры
        recyclerDocs?.adapter = DocumentsAdapter(documents) { doc ->
            documents.remove(doc)
            updateRecyclerViews()
        }
        recyclerCerts?.adapter = DocumentsAdapter(certificates) { doc ->
            certificates.remove(doc)
            updateRecyclerViews()
        }
        recyclerPortfolio?.adapter = PortfolioAdapter(portfolio) { uri ->
            portfolio.remove(uri)
            updateRecyclerViews()
        }
    }
    
    private fun submitVerification() {
        // Проверка ИНН
        val inn = inputInn?.text?.toString()?.trim()
        if (inn.isNullOrEmpty()) {
            Toast.makeText(context, "Необходимо указать ИНН", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Валидация ИНН (10 или 12 цифр)
        val innRegex = Regex("^\\d{10}$|^\\d{12}$")
        if (!innRegex.matches(inn)) {
            Toast.makeText(context, "ИНН должен содержать 10 или 12 цифр", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (documents.isEmpty() && certificates.isEmpty() && portfolio.isEmpty()) {
            Toast.makeText(context, "Необходимо загрузить хотя бы один документ", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Загружаем документы на сервер
        lifecycleScope.launch {
            var successCount = 0
            var errorCount = 0
            val errorMessages = mutableListOf<String>()
            
            // Собираем все задачи для параллельного выполнения
            val allDocuments = mutableListOf<kotlin.Pair<Uri, kotlin.Triple<String, String, DocumentType>>>()
            
            // Добавляем основные документы
            for (doc in documents) {
                val uri = Uri.parse(doc.filePath)
                val docType = when (doc.type) {
                    DocumentType.PASSPORT -> "passport"
                    DocumentType.DIPLOMA -> "diploma"
                    else -> "other"
                }
                allDocuments.add(kotlin.Pair(uri, kotlin.Triple(docType, doc.fileName, doc.type)))
            }
            
            // Добавляем сертификаты
            for (cert in certificates) {
                val uri = Uri.parse(cert.filePath)
                allDocuments.add(kotlin.Pair(uri, kotlin.Triple("certificate", cert.fileName, DocumentType.CERTIFICATE)))
            }
            
            // Добавляем портфолио
            for (portfolioUri in portfolio) {
                val uri = Uri.parse(portfolioUri)
                allDocuments.add(kotlin.Pair(uri, kotlin.Triple("portfolio", "portfolio_${System.currentTimeMillis()}.jpg", DocumentType.PORTFOLIO)))
            }
            
            // Выполняем все загрузки параллельно и ждем их завершения
            val results = allDocuments.map { (uri, params) ->
                async {
                    try {
                        val (docType, docName, _) = params
                        apiRepository.uploadVerificationDocument(
                            fileUri = uri,
                            documentType = docType,
                            documentName = docName,
                            inn = inn,
                            context = requireContext()
                        )
                    } catch (e: Exception) {
                        Log.e("VerificationFragment", "Error processing document", e)
                        Result.failure<com.example.bestapp.api.models.UploadDocumentResponse>(
                            Exception("Ошибка обработки документа: ${e.message}")
                        )
                    }
                }
            }
            
            // Ожидаем завершения всех загрузок
            for (deferred in results) {
                try {
                    val result = deferred.await()
                    result.onSuccess {
                        successCount++
                        Log.d("VerificationFragment", "Документ успешно загружен")
                    }.onFailure { error ->
                        errorCount++
                        val errorMsg = error.message ?: "Неизвестная ошибка"
                        errorMessages.add(errorMsg)
                        Log.e("VerificationFragment", "Error uploading document: $errorMsg", error)
                    }
                } catch (e: Exception) {
                    errorCount++
                    val errorMsg = e.message ?: "Неизвестная ошибка"
                    errorMessages.add(errorMsg)
                    Log.e("VerificationFragment", "Error waiting for upload", e)
                }
            }
            
            // Показываем результат
            if (errorCount == 0 && successCount > 0) {
                Toast.makeText(context, "Документы успешно загружены ($successCount)", Toast.LENGTH_SHORT).show()
                // Перезагружаем данные и обновляем UI
                loadVerificationData()
            } else if (errorCount > 0) {
                val errorText = if (errorMessages.isNotEmpty()) {
                    "Ошибки: ${errorMessages.take(2).joinToString(", ")}${if (errorMessages.size > 2) "..." else ""}"
                } else {
                    "Загружено: $successCount, ошибок: $errorCount"
                }
                Toast.makeText(
                    context,
                    errorText,
                    Toast.LENGTH_LONG
                ).show()
                Log.e("VerificationFragment", "Upload summary: success=$successCount, errors=$errorCount, messages=$errorMessages")
            } else {
                Toast.makeText(context, "Не удалось загрузить документы", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// Адаптер для отображения загруженных документов
class UploadedDocumentsAdapter(
    private val documents: List<ApiVerificationDocument>,
    private val onItemClick: (ApiVerificationDocument) -> Unit
) : RecyclerView.Adapter<UploadedDocumentsAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.document_image)
        val statusText: TextView = itemView.findViewById(R.id.document_status)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_uploaded_verification_document, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val document = documents[position]
        val baseUrl = com.example.bestapp.api.RetrofitClient.BASE_URL
        val fullUrl = if (document.fileUrl.startsWith("http")) {
            document.fileUrl
        } else {
            baseUrl.removeSuffix("/") + document.fileUrl
        }
        
        Glide.with(holder.itemView.context)
            .load(fullUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imageView)
        
        val statusText = when (document.status) {
            "pending" -> "⏳ На проверке"
            "approved" -> "✅ Одобрен"
            "rejected" -> "❌ Отклонен"
            else -> "❓ Неизвестно"
        }
        holder.statusText.text = statusText
        
        holder.itemView.setOnClickListener {
            onItemClick(document)
        }
    }
    
    override fun getItemCount() = documents.size
}

// Адаптер для локальных документов
class DocumentsAdapter(
    private val documents: List<VerificationDocument>,
    private val onDelete: (VerificationDocument) -> Unit
) : RecyclerView.Adapter<DocumentsAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.document_name)
        val typeText: TextView = itemView.findViewById(R.id.document_type)
        val deleteBtn: ImageView = itemView.findViewById(R.id.btn_delete)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verification_document, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val document = documents[position]
        holder.nameText.text = document.fileName
        holder.typeText.text = document.type.displayName
        
        holder.deleteBtn.setOnClickListener {
            onDelete(document)
        }
    }
    
    override fun getItemCount() = documents.size
}

// Адаптер для портфолио
class PortfolioAdapter(
    private val portfolio: List<String>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<PortfolioAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.portfolio_image)
        val deleteBtn: ImageView = itemView.findViewById(R.id.btn_delete)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_portfolio_image, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = Uri.parse(portfolio[position])
        Glide.with(holder.itemView.context)
            .load(uri)
            .into(holder.imageView)
        
        holder.deleteBtn.setOnClickListener {
            onDelete(portfolio[position])
        }
    }
    
    override fun getItemCount() = portfolio.size
}