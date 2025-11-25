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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.data.DocumentType
import com.example.bestapp.data.VerificationDocument
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.util.UUID

class VerificationFragment : Fragment() {

    private val documents = mutableListOf<VerificationDocument>()
    private val certificates = mutableListOf<VerificationDocument>()
    private val portfolio = mutableListOf<String>()
    
    private var currentDocumentType = DocumentType.PASSPORT
    
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
        if (documents.isEmpty()) {
            Toast.makeText(context, R.string.verification_document_required, Toast.LENGTH_SHORT).show()
            return
        }
        
        // TODO: Сохранить документы на сервер
        Toast.makeText(context, R.string.verification_success, Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }
}







