package com.example.bestapp.ui.orders

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.models.ApiRejectedAssignment
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class RejectedOrdersAdapter(
    private val onOrderClick: (ApiRejectedAssignment) -> Unit
) : ListAdapter<ApiRejectedAssignment, RejectedOrdersAdapter.RejectedOrderViewHolder>(
    RejectedOrderDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RejectedOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rejected_order, parent, false)
        return RejectedOrderViewHolder(view, onOrderClick)
    }
    
    override fun onBindViewHolder(holder: RejectedOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class RejectedOrderViewHolder(
        itemView: View,
        private val onOrderClick: (ApiRejectedAssignment) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val orderId: TextView = itemView.findViewById(R.id.order_id)
        private val deviceInfo: TextView = itemView.findViewById(R.id.device_info)
        private val clientName: TextView = itemView.findViewById(R.id.client_name)
        private val clientAddress: TextView = itemView.findViewById(R.id.client_address)
        private val problemDescription: TextView = itemView.findViewById(R.id.problem_description)
        private val estimatedCost: TextView = itemView.findViewById(R.id.estimated_cost)
        private val rejectedAt: TextView = itemView.findViewById(R.id.rejected_at)
        private val rejectionReason: TextView = itemView.findViewById(R.id.rejection_reason)
        private val urgencyChip: Chip = itemView.findViewById(R.id.urgency_chip)
        
        fun bind(assignment: ApiRejectedAssignment) {
            val order = assignment.order
            
            orderId.text = "Заказ #${order.id}"
            
            val deviceParts = listOfNotNull(
                order.deviceType,
                order.deviceBrand,
                order.deviceModel
            )
            deviceInfo.text = deviceParts.joinToString(" ")
            
            clientName.text = order.client.name
            clientAddress.text = order.clientAddress
            problemDescription.text = order.problemDescription
            
            estimatedCost.text = if (order.estimatedCost != null) {
                "${order.estimatedCost.toInt()} ₽"
            } else {
                "Цена не указана"
            }
            
            // Форматируем дату отклонения
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = dateFormat.parse(assignment.rejectedAt)
                val displayFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                rejectedAt.text = "Отклонен: ${displayFormat.format(date ?: Date())}"
            } catch (e: Exception) {
                rejectedAt.text = "Отклонен: ${assignment.rejectedAt}"
            }
            
            // Причина отклонения
            rejectionReason.text = assignment.rejectionReason ?: "Причина не указана"
            
            // Срочность
            when (order.urgency) {
                "emergency" -> {
                    urgencyChip.text = "Срочно"
                    urgencyChip.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.context, R.color.md_theme_light_error)
                    )
                }
                "urgent" -> {
                    urgencyChip.text = "Важно"
                    urgencyChip.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.context, R.color.md_theme_light_tertiary)
                    )
                }
                else -> {
                    urgencyChip.text = "Обычный"
                    urgencyChip.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.context, R.color.md_theme_light_outline)
                    )
                }
            }
            
            itemView.setOnClickListener {
                onOrderClick(assignment)
            }
        }
    }
    
    class RejectedOrderDiffCallback : DiffUtil.ItemCallback<ApiRejectedAssignment>() {
        override fun areItemsTheSame(
            oldItem: ApiRejectedAssignment,
            newItem: ApiRejectedAssignment
        ): Boolean = oldItem.id == newItem.id
        
        override fun areContentsTheSame(
            oldItem: ApiRejectedAssignment,
            newItem: ApiRejectedAssignment
        ): Boolean = oldItem == newItem
    }
}

