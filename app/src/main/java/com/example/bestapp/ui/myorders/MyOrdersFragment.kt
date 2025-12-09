package com.example.bestapp.ui.myorders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.ui.orders.OrdersAdapter
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MyOrdersFragment : Fragment() {
    
    private val viewModel: MyOrdersViewModel by viewModels()
    private lateinit var ordersAdapter: OrdersAdapter
    
    private var recyclerMyOrders: RecyclerView? = null
    private var searchBar: TextInputEditText? = null
    private var textEmpty: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_my_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupSearchBar()
        observeData()
        setupNavigationResultListener()
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем заказы при возврате на экран
        // Это гарантирует, что завершенные заказы исчезнут из списка
        viewModel.refreshOrders()
    }
    
    private fun setupNavigationResultListener() {
        // Слушаем результат навигации от OrderDetailsFragment
        // Если заказ был завершен, обновляем список
        parentFragmentManager.setFragmentResultListener("order_completed", this) { _, _ ->
            viewModel.refreshOrders()
        }
    }
    
    private fun initViews(view: View) {
        recyclerMyOrders = view.findViewById(R.id.recycler_my_orders)
        searchBar = view.findViewById(R.id.search_bar)
        textEmpty = view.findViewById(R.id.text_empty)
    }
    
    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter(
            onOrderClick = { order ->
                // Переход к детальному экрану заказа
                val bundle = Bundle().apply {
                    putLong("orderId", order.id)
                }
                findNavController().navigate(R.id.action_my_orders_to_order_details, bundle)
            },
            onOrderSelected = null // Для "Мои заказы" не нужен режим выбора
        )
        recyclerMyOrders?.apply {
            adapter = ordersAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupSearchBar() {
        searchBar?.doAfterTextChanged { text ->
            viewModel.searchOrders(text?.toString() ?: "")
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredMyOrders.collect { orders ->
                ordersAdapter.submitList(orders)
                updateEmptyState(orders.isEmpty())
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerMyOrders?.visibility = View.GONE
            textEmpty?.visibility = View.VISIBLE
        } else {
            recyclerMyOrders?.visibility = View.VISIBLE
            textEmpty?.visibility = View.GONE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        recyclerMyOrders = null
        searchBar = null
        textEmpty = null
    }
}



