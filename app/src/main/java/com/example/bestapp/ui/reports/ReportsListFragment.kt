package com.example.bestapp.ui.reports

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReportsListFragment : Fragment() {
    private val viewModel: ReportsListViewModel by viewModels()
    
    private var toolbar: MaterialToolbar? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null
    private var recyclerReports: RecyclerView? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_reports_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupToolbar()
        setupRecyclerView()
        observeUiState()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        progressBar = view.findViewById(R.id.progress_loading)
        errorText = view.findViewById(R.id.text_error)
        recyclerReports = view.findViewById(R.id.recycler_reports)
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupRecyclerView() {
        val adapter = ReportsAdapter { report ->
            // Переход к деталям отчета
            Toast.makeText(context, "Отчет #${report.id}", Toast.LENGTH_SHORT).show()
        }
        recyclerReports?.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(context)
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                adapter.submitList(state.reports)
            }
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
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadReports()
    }
}

