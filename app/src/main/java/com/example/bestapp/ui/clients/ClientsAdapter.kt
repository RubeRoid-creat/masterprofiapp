package com.example.bestapp.ui.clients

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.data.Client

class ClientsAdapter(
    private val onClientClick: (Client) -> Unit,
    private val getOrdersCount: (Long) -> Int
) : ListAdapter<Client, ClientsAdapter.ClientViewHolder>(ClientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client, parent, false)
        return ClientViewHolder(view, onClientClick, getOrdersCount)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClientViewHolder(
        itemView: View,
        private val onClientClick: (Client) -> Unit,
        private val getOrdersCount: (Long) -> Int
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val clientAvatar: TextView = itemView.findViewById(R.id.client_avatar)
        private val clientName: TextView = itemView.findViewById(R.id.client_name)
        private val clientPhone: TextView = itemView.findViewById(R.id.client_phone)
        private val clientEmail: TextView = itemView.findViewById(R.id.client_email)
        private val clientOrdersCount: TextView = itemView.findViewById(R.id.client_orders_count)

        fun bind(client: Client) {
            clientName.text = client.name
            clientPhone.text = client.getFormattedPhone()
            
            // Показываем email если есть
            if (!client.email.isNullOrEmpty()) {
                clientEmail.visibility = View.VISIBLE
                clientEmail.text = client.email
            } else {
                clientEmail.visibility = View.GONE
            }
            
            // Инициалы для аватара
            val initials = getInitials(client.name)
            clientAvatar.text = initials
            
            // Количество заказов
            val ordersCount = getOrdersCount(client.id)
            clientOrdersCount.text = ordersCount.toString()
            
            itemView.setOnClickListener {
                onClientClick(client)
            }
        }
        
        private fun getInitials(name: String): String {
            val parts = name.trim().split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
                parts.size == 1 -> parts[0].take(2)
                else -> "?"
            }.uppercase()
        }
    }

    private class ClientDiffCallback : DiffUtil.ItemCallback<Client>() {
        override fun areItemsTheSame(oldItem: Client, newItem: Client): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Client, newItem: Client): Boolean {
            return oldItem == newItem
        }
    }
}








