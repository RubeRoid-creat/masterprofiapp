package com.example.bestapp.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestapp.data.Client
import com.example.bestapp.data.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClientsViewModel : ViewModel() {
    private val repository = DataRepository
    
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()
    
    private val _filteredClients = MutableStateFlow<List<Client>>(emptyList())
    val filteredClients: StateFlow<List<Client>> = _filteredClients.asStateFlow()
    
    init {
        loadClients()
    }
    
    private fun loadClients() {
        viewModelScope.launch {
            repository.clients.collect { clientsList ->
                _clients.value = clientsList.sortedBy { it.name }
                _filteredClients.value = _clients.value
            }
        }
    }
    
    fun searchClients(query: String) {
        if (query.isEmpty()) {
            _filteredClients.value = _clients.value
            return
        }
        
        val searchQuery = query.lowercase()
        _filteredClients.value = _clients.value.filter { client ->
            client.name.lowercase().contains(searchQuery) ||
            client.phone.contains(searchQuery) ||
            client.email?.lowercase()?.contains(searchQuery) == true
        }
    }
    
    fun getOrdersCountForClient(clientId: Long): Int {
        return repository.getOrdersByClient(clientId).size
    }
    
    fun refreshClients() {
        loadClients()
    }
}








