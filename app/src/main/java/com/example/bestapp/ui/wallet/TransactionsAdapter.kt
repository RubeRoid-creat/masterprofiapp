package com.example.bestapp.ui.wallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.models.ApiTransaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsAdapter : ListAdapter<ApiTransaction, TransactionsAdapter.ViewHolder>(DiffCallback()) {
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeText: TextView = itemView.findViewById(R.id.text_type)
        private val amountText: TextView = itemView.findViewById(R.id.text_amount)
        private val descriptionText: TextView = itemView.findViewById(R.id.text_description)
        private val dateText: TextView = itemView.findViewById(R.id.text_date)
        private val statusText: TextView = itemView.findViewById(R.id.text_status)
        
        fun bind(transaction: ApiTransaction) {
            val typeName = when (transaction.transactionType) {
                "income" -> "Начисление"
                "payout" -> "Выплата"
                "refund" -> "Возврат"
                "commission" -> "Комиссия"
                else -> transaction.transactionType
            }
            typeText.text = typeName
            
            val amount = transaction.amount
            val sign = if (transaction.transactionType == "income") "+" else "-"
            amountText.text = "$sign${currencyFormat.format(amount)}"
            amountText.setTextColor(
                itemView.context.getColor(
                    if (transaction.transactionType == "income") 
                        android.R.color.holo_green_dark 
                    else 
                        android.R.color.holo_red_dark
                )
            )
            
            descriptionText.text = transaction.description ?: 
                (transaction.orderNumber?.let { "Заказ #$it" } ?: "Транзакция")
            
            try {
                val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .parse(transaction.createdAt)
                dateText.text = date?.let { dateFormat.format(it) } ?: transaction.createdAt
            } catch (e: Exception) {
                dateText.text = transaction.createdAt
            }
            
            val statusName = when (transaction.status) {
                "pending" -> "Ожидает"
                "completed" -> "Завершено"
                "failed" -> "Ошибка"
                "cancelled" -> "Отменено"
                else -> transaction.status
            }
            statusText.text = statusName
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ApiTransaction>() {
        override fun areItemsTheSame(oldItem: ApiTransaction, newItem: ApiTransaction): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ApiTransaction, newItem: ApiTransaction): Boolean {
            return oldItem == newItem
        }
    }
}



