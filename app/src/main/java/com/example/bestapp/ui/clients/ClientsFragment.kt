package com.example.bestapp.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ClientsFragment : Fragment() {
    
    private val viewModel: ClientsViewModel by viewModels()
    private lateinit var clientsAdapter: ClientsAdapter
    
    private var recyclerClients: RecyclerView? = null
    private var searchBar: TextInputEditText? = null
    private var textEmpty: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_clients, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupSearchBar()
        observeData()
    }
    
    private fun initViews(view: View) {
        recyclerClients = view.findViewById(R.id.recycler_clients)
        searchBar = view.findViewById(R.id.search_bar)
        textEmpty = view.findViewById(R.id.text_empty)
    }
    
    private fun setupRecyclerView() {
        clientsAdapter = ClientsAdapter(
            onClientClick = { client ->
                Toast.makeText(
                    context,
                    "Открыть детали клиента: ${client.name}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            getOrdersCount = { clientId ->
                viewModel.getOrdersCountForClient(clientId)
            }
        )
        recyclerClients?.apply {
            adapter = clientsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupSearchBar() {
        searchBar?.doAfterTextChanged { text ->
            viewModel.searchClients(text?.toString() ?: "")
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredClients.collect { clients ->
                clientsAdapter.submitList(clients)
                updateEmptyState(clients.isEmpty())
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerClients?.visibility = View.GONE
            textEmpty?.visibility = View.VISIBLE
        } else {
            recyclerClients?.visibility = View.VISIBLE
            textEmpty?.visibility = View.GONE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        recyclerClients = null
        searchBar = null
        textEmpty = null
    }
}

