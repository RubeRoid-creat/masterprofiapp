package com.example.bestapp.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.data.DocumentType
import com.example.bestapp.data.VerificationDocument
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.UUID

class VerificationFragment : Fragment() {

    private val documents = mutableListOf<VerificationDocument>()
    private val certificates = mutableListOf<VerificationDocument>()
    private val portfolio = mutableListOf<String>()
    private val apiRepository = ApiRepository()
    
    private var currentDocumentType = DocumentType.PASSPORT
    private var inputInn: TextInputEditText? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                addDocument(uri)
            }
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
        
        recyclerDocs.layoutManager = LinearLayoutManager(context)
        recyclerCerts.layoutManager = LinearLayoutManager(context)
        recyclerPortfolio.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        inputInn = view.findViewById(R.id.input_inn)
        val btnAddDoc = view.findViewById<MaterialButton>(R.id.btn_add_document)
        val btnAddCert = view.findViewById<MaterialButton>(R.id.btn_add_certificate)
        val btnAddPortfolio = view.findViewById<MaterialButton>(R.id.btn_add_portfolio)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btn_submit)
        
        btnAddDoc.setOnClickListener {
            currentDocumentType = DocumentType.PASSPORT
            openImagePicker()
        }
        
        btnAddCert.setOnClickListener {
            currentDocumentType = DocumentType.CERTIFICATE
            openImagePicker()
        }
        
        btnAddPortfolio.setOnClickListener {
            currentDocumentType = DocumentType.PORTFOLIO
            openImagePicker()
        }
        
        btnSubmit.setOnClickListener {
            submitVerification()
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
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
        Toast.makeText(context, "Документ добавлен", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateRecyclerViews() {
        val recyclerDocs = view?.findViewById<RecyclerView>(R.id.recycler_documents)
        val recyclerCerts = view?.findViewById<RecyclerView>(R.id.recycler_certificates)
        val recyclerPortfolio = view?.findViewById<RecyclerView>(R.id.recycler_portfolio)
        
        recyclerDocs?.visibility = if (documents.isNotEmpty()) View.VISIBLE else View.GONE
        recyclerCerts?.visibility = if (certificates.isNotEmpty()) View.VISIBLE else View.GONE
        recyclerPortfolio?.visibility = if (portfolio.isNotEmpty()) View.VISIBLE else View.GONE
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
        
        if (documents.isEmpty()) {
            Toast.makeText(context, R.string.verification_document_required, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Загружаем документы на сервер
        lifecycleScope.launch {
            var successCount = 0
            var errorCount = 0
            
            // Загружаем основной документ (паспорт)
            for (doc in documents) {
                try {
                    val uri = Uri.parse(doc.filePath)
                    val result = apiRepository.uploadVerificationDocument(
                        fileUri = uri,
                        documentType = when (doc.type) {
                            DocumentType.PASSPORT -> "passport"
                            DocumentType.DIPLOMA -> "diploma"
                            else -> "other"
                        },
                        documentName = doc.fileName,
                        inn = inn,
                        context = requireContext()
                    )
                    
                    result.onSuccess {
                        successCount++
                    }.onFailure { error ->
                        errorCount++
                        android.util.Log.e("VerificationFragment", "Error uploading document: ${error.message}")
                    }
                } catch (e: Exception) {
                    errorCount++
                    android.util.Log.e("VerificationFragment", "Error processing document", e)
                }
            }
            
            // Загружаем сертификаты
            for (cert in certificates) {
                try {
                    val uri = Uri.parse(cert.filePath)
                    val result = apiRepository.uploadVerificationDocument(
                        fileUri = uri,
                        documentType = "certificate",
                        documentName = cert.fileName,
                        inn = inn,
                        context = requireContext()
                    )
                    
                    result.onSuccess {
                        successCount++
                    }.onFailure { error ->
                        errorCount++
                        android.util.Log.e("VerificationFragment", "Error uploading certificate: ${error.message}")
                    }
                } catch (e: Exception) {
                    errorCount++
                    android.util.Log.e("VerificationFragment", "Error processing certificate", e)
                }
            }
            
            if (errorCount == 0) {
                Toast.makeText(context, "Документы успешно загружены", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(
                    context,
                    "Загружено: $successCount, ошибок: $errorCount",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}







