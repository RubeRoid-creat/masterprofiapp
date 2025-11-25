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
import com.example.bestapp.data.Order
import com.example.bestapp.data.OrderRequestStatus
import com.example.bestapp.data.OrderType
import com.google.android.material.chip.Chip

class OrdersAdapter(
    private val onOrderClick: (Order) -> Unit,
    private val onOrderSelected: ((Order, Boolean) -> Unit)? = null
) : ListAdapter<Order, OrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {
    
    private val selectedOrders = mutableSetOf<Long>()
    var isSelectionMode = false
        set(value) {
            field = value
            if (!value) {
                selectedOrders.clear()
            }
            notifyDataSetChanged()
        }
    
    fun getSelectedOrders(): Set<Long> = selectedOrders.toSet()
    
    fun clearSelection() {
        selectedOrders.clear()
        notifyDataSetChanged()
    }
    
    fun toggleSelection(orderId: Long) {
        if (selectedOrders.contains(orderId)) {
            selectedOrders.remove(orderId)
        } else {
            selectedOrders.add(orderId)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_preview, parent, false)
        return OrderViewHolder(view, onOrderClick, onOrderSelected) { orderId ->
            toggleSelection(orderId)
        }
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        val isSelected = selectedOrders.contains(order.id)
        holder.bind(order, isSelectionMode, isSelected)
    }

    class OrderViewHolder(
        itemView: View,
        private val onOrderClick: (Order) -> Unit,
        private val onOrderSelected: ((Order, Boolean) -> Unit)?,
        private val onToggleSelection: (Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val orderId: TextView = itemView.findViewById(R.id.order_id)
        private val requestStatusChip: Chip = itemView.findViewById(R.id.request_status_chip)
        private val orderTypeChip: Chip = itemView.findViewById(R.id.order_type_chip)
        private val orderPriorityBadge: Chip = itemView.findViewById(R.id.order_priority_badge)
        private val orderDevice: TextView = itemView.findViewById(R.id.order_device)
        private val orderClient: TextView = itemView.findViewById(R.id.order_client)
        private val orderPhone: TextView = itemView.findViewById(R.id.order_phone)
        private val orderAddress: TextView = itemView.findViewById(R.id.order_address)
        private val orderProblem: TextView = itemView.findViewById(R.id.order_problem)
        private val orderArrivalTime: TextView = itemView.findViewById(R.id.order_arrival_time)
        private val orderTimer: TextView = itemView.findViewById(R.id.order_timer)
        private val orderDate: TextView = itemView.findViewById(R.id.order_date)
        private val orderCost: TextView = itemView.findViewById(R.id.order_cost)
        private val orderDistance: TextView = itemView.findViewById(R.id.order_distance)
        private val orderTime: TextView = itemView.findViewById(R.id.order_time)
        private val distanceSeparator: View = itemView.findViewById(R.id.distance_separator)
        private val timeContainer: View = itemView.findViewById(R.id.time_container)
        private val orderCheckbox: androidx.appcompat.widget.AppCompatCheckBox = itemView.findViewById(R.id.order_checkbox)
        
        private var currentTimer: com.example.bestapp.ui.common.CountdownTimerView? = null
        private var currentOrder: Order? = null

        fun bind(order: Order, selectionMode: Boolean, isSelected: Boolean) {
            currentOrder = order
            val context = itemView.context
            
            // Номер заявки
            orderId.text = "Заявка №${order.id}"
            
            // Статус заявки
            requestStatusChip.text = order.requestStatus.displayName
            val (statusBg, statusText) = when (order.requestStatus) {
                OrderRequestStatus.WARRANTY -> Pair(R.color.order_warranty_bg, R.color.order_warranty_text)
                OrderRequestStatus.REPEAT -> Pair(R.color.order_repeat_bg, R.color.order_repeat_text)
                OrderRequestStatus.NEW -> Pair(R.color.md_theme_light_surfaceVariant, R.color.md_theme_light_onSurfaceVariant)
            }
            requestStatusChip.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, statusBg)
            )
            requestStatusChip.setTextColor(ContextCompat.getColor(context, statusText))
            
            // Тип заказа
            orderTypeChip.text = order.orderType.displayName
            val (typeBg, typeText) = if (order.orderType == OrderType.URGENT) {
                Pair(R.color.order_urgent_bg, R.color.order_urgent_text)
            } else {
                Pair(R.color.md_theme_light_surfaceVariant, R.color.md_theme_light_onSurfaceVariant)
            }
            orderTypeChip.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, typeBg)
            )
            orderTypeChip.setTextColor(ContextCompat.getColor(context, typeText))
            
            // Приоритет (показываем для срочных заказов)
            if (order.orderType == OrderType.URGENT || order.urgency == "emergency" || order.urgency == "urgent") {
                orderPriorityBadge.visibility = View.VISIBLE
            } else {
                orderPriorityBadge.visibility = View.GONE
            }
            
            orderDevice.text = order.getDeviceFullName()
            orderClient.text = "👤 ${order.clientName}"
            orderPhone.text = "📞 ${order.clientPhone}"
            orderAddress.text = "📍 ${order.clientAddress}"
            orderProblem.text = order.problemDescription
            
            // Расстояние и время в пути
            if (order.distance != null) {
                val distanceKm = order.distance / 1000.0
                orderDistance.text = if (distanceKm < 1) {
                    "${order.distance.toInt()} м"
                } else {
                    String.format("%.1f км", distanceKm)
                }
                orderDistance.visibility = View.VISIBLE
                distanceSeparator.visibility = View.VISIBLE
                
                // Рассчитываем примерное время в пути (средняя скорость 40 км/ч в городе)
                val estimatedTimeMinutes = (order.distance / 1000.0 / 40.0 * 60.0).toInt() + 1
                orderTime.text = "$estimatedTimeMinutes мин"
                timeContainer.visibility = View.VISIBLE
            } else {
                orderDistance.visibility = View.GONE
                distanceSeparator.visibility = View.GONE
                timeContainer.visibility = View.GONE
            }
            
            // Останавливаем предыдущий таймер
            currentTimer?.stop()
            currentTimer = null
            
            // Время приезда
            if (order.arrivalTime != null) {
                orderArrivalTime.text = "⏰ Приезд: ${order.arrivalTime}"
                orderArrivalTime.visibility = View.VISIBLE
            } else {
                orderArrivalTime.visibility = View.GONE
            }
            
            // Таймер обратного отсчета (для срочных заказов с назначением)
            if (order.expiresAt != null && order.status == com.example.bestapp.data.RepairStatus.NEW) {
                val expiresDate = order.expiresAt
                val now = java.util.Date()
                
                if (expiresDate.after(now)) {
                    // Таймер еще не истек
                    orderTimer.visibility = View.VISIBLE
                    currentTimer = com.example.bestapp.ui.common.CountdownTimerView(
                        orderTimer,
                        expiresDate,
                        onExpired = {
                            // Когда время истекает, обновляем список
                            orderTimer.text = "⏱️ Время истекло"
                            orderTimer.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                        }
                    )
                } else {
                    // Время уже истекло
                    orderTimer.text = "⏱️ Время истекло"
                    orderTimer.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                    orderTimer.visibility = View.VISIBLE
                }
            } else {
                orderTimer.visibility = View.GONE
            }
            
            orderDate.text = order.getFormattedCreatedDate()
            orderCost.text = order.getFormattedCost()
            
            // Режим выбора
            if (selectionMode && order.status == com.example.bestapp.data.RepairStatus.NEW) {
                orderCheckbox.visibility = View.VISIBLE
                orderCheckbox.isChecked = isSelected
                orderCheckbox.setOnCheckedChangeListener { _, checked ->
                    try {
                        onToggleSelection(order.id)
                        onOrderSelected?.invoke(order, checked)
                    } catch (e: Exception) {
                        android.util.Log.e("OrdersAdapter", "Ошибка при изменении выбора", e)
                    }
                }
            } else {
                orderCheckbox.visibility = View.GONE
                orderCheckbox.setOnCheckedChangeListener(null)
            }
            
            itemView.setOnClickListener {
                if (selectionMode && order.status == com.example.bestapp.data.RepairStatus.NEW) {
                    // В режиме выбора переключаем чекбокс
                    val newChecked = !orderCheckbox.isChecked
                    orderCheckbox.isChecked = newChecked
                    onToggleSelection(order.id)
                    onOrderSelected?.invoke(order, newChecked)
                } else {
                    // Обычный режим - открываем детали
                    onOrderClick(order)
                }
            }
            
            itemView.setOnLongClickListener {
                try {
                    if (order.status == com.example.bestapp.data.RepairStatus.NEW) {
                        // Долгое нажатие включает режим выбора
                        // Сначала переключаем выбор, затем уведомляем callback
                        onToggleSelection(order.id)
                        onOrderSelected?.invoke(order, true)
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OrdersAdapter", "Ошибка при долгом нажатии", e)
                    false
                }
            }
        }
    }

    private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}
