package com.example.bestapp.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.models.RouteOrderItem
import java.util.Locale

class RouteOrdersAdapter(
    private val onOrderClick: (RouteOrderItem) -> Unit
) : ListAdapter<RouteOrderItem, RouteOrdersAdapter.RouteOrderViewHolder>(RouteOrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_order, parent, false)
        return RouteOrderViewHolder(view, onOrderClick)
    }
    
    override fun onBindViewHolder(holder: RouteOrderViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }
    
    class RouteOrderViewHolder(
        itemView: View,
        private val onOrderClick: (RouteOrderItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val tvOrderNumber: TextView = itemView.findViewById(R.id.tv_order_number)
        private val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        private val tvOrderAddress: TextView = itemView.findViewById(R.id.tv_order_address)
        private val tvOrderDevice: TextView = itemView.findViewById(R.id.tv_order_device)
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        
        fun bind(orderItem: RouteOrderItem, orderNumber: Int) {
            val order = orderItem.order
            
            tvOrderNumber.text = orderNumber.toString()
            tvOrderId.text = "Заказ #${order.id}"
            tvOrderAddress.text = "📍 ${order.address}"
            tvOrderDevice.text = order.deviceType
            
            // Форматируем расстояние
            tvDistance.text = formatDistance(orderItem.distanceFromPrevious)
            tvTime.text = "${orderItem.timeFromPrevious} мин"
            
            itemView.setOnClickListener {
                onOrderClick(orderItem)
            }
        }
        
        private fun formatDistance(meters: Double): String {
            return when {
                meters < 1000 -> "${meters.toInt()} м"
                else -> String.format(Locale.getDefault(), "%.1f км", meters / 1000)
            }
        }
    }
    
    private class RouteOrderDiffCallback : DiffUtil.ItemCallback<RouteOrderItem>() {
        override fun areItemsTheSame(oldItem: RouteOrderItem, newItem: RouteOrderItem): Boolean {
            return oldItem.order.id == newItem.order.id
        }
        
        override fun areContentsTheSame(oldItem: RouteOrderItem, newItem: RouteOrderItem): Boolean {
            return oldItem == newItem
        }
    }
}







