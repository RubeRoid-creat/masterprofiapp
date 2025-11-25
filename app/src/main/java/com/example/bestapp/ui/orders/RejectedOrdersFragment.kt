package com.example.bestapp.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bestapp.R
import com.example.bestapp.api.models.ApiRejectedAssignment
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RejectedOrdersFragment : Fragment() {
    private val viewModel: RejectedOrdersViewModel by viewModels()
    private lateinit var adapter: RejectedOrdersAdapter
    private var recyclerView: RecyclerView? = null
    private var emptyTextView: TextView? = null
    private var swipeRefresh: SwipeRefreshLayout? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rejected_orders, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        recyclerView = view.findViewById(R.id.recycler_rejected_orders)
        emptyTextView = view.findViewById(R.id.text_empty)
        swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe_refresh)
        
        setupRecyclerView()
        setupSwipeRefresh()
        observeUiState()
    }
    
    private fun setupRecyclerView() {
        adapter = RejectedOrdersAdapter { assignment ->
            // Можно открыть детали заказа, если нужно
            Toast.makeText(
                requireContext(),
                "Заказ #${assignment.order.id} отклонен",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = adapter
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh?.setOnRefreshListener {
            viewModel.refresh()
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is RejectedOrdersUiState.Loading -> {
                        swipeRefresh?.isRefreshing = true
                        emptyTextView?.visibility = View.GONE
                    }
                    is RejectedOrdersUiState.Success -> {
                        swipeRefresh?.isRefreshing = false
                        adapter.submitList(state.rejectedAssignments)
                        
                        if (state.rejectedAssignments.isEmpty()) {
                            emptyTextView?.visibility = View.VISIBLE
                            emptyTextView?.text = "Нет отклоненных заказов"
                        } else {
                            emptyTextView?.visibility = View.GONE
                        }
                    }
                    is RejectedOrdersUiState.Error -> {
                        swipeRefresh?.isRefreshing = false
                        Toast.makeText(
                            requireContext(),
                            "Ошибка: ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        emptyTextView?.visibility = View.VISIBLE
                        emptyTextView?.text = "Ошибка загрузки: ${state.message}"
                    }
                }
            }
        }
    }
}

