package com.example.bestapp.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.models.ApiWorkReport
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReportsAdapter(
    private val onReportClick: (ApiWorkReport) -> Unit
) : ListAdapter<ApiWorkReport, ReportsAdapter.ViewHolder>(DiffCallback()) {
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"))
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardReport: MaterialCardView = itemView.findViewById(R.id.card_report)
        private val textOrderId: TextView = itemView.findViewById(R.id.text_order_id)
        private val textDescription: TextView = itemView.findViewById(R.id.text_description)
        private val textCost: TextView = itemView.findViewById(R.id.text_cost)
        private val textStatus: TextView = itemView.findViewById(R.id.text_status)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        
        fun bind(report: ApiWorkReport) {
            textOrderId.text = "Заказ #${report.orderId}"
            textDescription.text = report.workDescription.take(100) + if (report.workDescription.length > 100) "..." else ""
            textCost.text = currencyFormat.format(report.totalCost)
            
            val statusText = when (report.status) {
                "draft" -> "Черновик"
                "pending_signature" -> "Ожидает подписи"
                "signed" -> "Подписан"
                "completed" -> "Завершен"
                else -> report.status
            }
            textStatus.text = statusText
            
            try {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(report.createdAt)
                textDate.text = date?.let { dateFormat.format(it) } ?: report.createdAt
            } catch (e: Exception) {
                textDate.text = report.createdAt
            }
            
            cardReport.setOnClickListener {
                onReportClick(report)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ApiWorkReport>() {
        override fun areItemsTheSame(oldItem: ApiWorkReport, newItem: ApiWorkReport): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ApiWorkReport, newItem: ApiWorkReport): Boolean {
            return oldItem == newItem
        }
    }
}


