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
import java.util.Locale

class OrdersAdapter(
    private val onOrderClick: (Order) -> Unit,
    private val onOrderSelected: ((Order, Boolean) -> Unit)? = null,
    private val onAcceptOrder: ((Order) -> Unit)? = null,
    private val onRejectOrder: ((Order) -> Unit)? = null
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
        return OrderViewHolder(view, onOrderClick, onOrderSelected, onAcceptOrder, onRejectOrder) { orderId ->
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
        private val onAcceptOrder: ((Order) -> Unit)?,
        private val onRejectOrder: ((Order) -> Unit)?,
        private val onToggleSelection: (Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val orderId: TextView = itemView.findViewById(R.id.order_id)
        private val orderArrivalTime: TextView = itemView.findViewById(R.id.order_arrival_time)
        private val orderDevice: TextView = itemView.findViewById(R.id.order_device)
        private val orderProblem: TextView = itemView.findViewById(R.id.order_problem)
        private val orderCheckbox: androidx.appcompat.widget.AppCompatCheckBox = itemView.findViewById(R.id.order_checkbox)
        private val actionButtonsContainer: View = itemView.findViewById(R.id.action_buttons_container)
        private val btnAcceptOrder: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btn_accept_order)
        private val btnRejectOrder: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btn_reject_order)
        private val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        
        private var currentOrder: Order? = null

        fun bind(order: Order, selectionMode: Boolean, isSelected: Boolean) {
            currentOrder = order
            val context = itemView.context
            
            // Номер заявки
            orderId.text = "#${order.id}"
            
            // Цвет индикатора статуса
            val indicatorColor = when (order.requestStatus) {
                OrderRequestStatus.WARRANTY -> R.color.order_warranty_text
                OrderRequestStatus.REPEAT -> R.color.order_repeat_text
                OrderRequestStatus.NEW -> R.color.md_theme_light_primary
            }
            statusIndicator?.setBackgroundColor(ContextCompat.getColor(context, indicatorColor))
            
            // Время приезда
            val arrivalTimeText = order.arrivalTime ?: order.desiredRepairDate
            if (arrivalTimeText != null) {
                // Если время в формате даты и времени, извлекаем только время
                val timeOnly = if (arrivalTimeText.contains(" ")) {
                    arrivalTimeText.split(" ").lastOrNull() ?: arrivalTimeText
                } else {
                    arrivalTimeText
                }
                orderArrivalTime.text = timeOnly
            } else {
                orderArrivalTime.text = "—"
            }
            
            // Тип техники и бренд (без модели)
            orderDevice.text = "${order.deviceType} ${order.deviceBrand}"
            
            // Проблема
            orderProblem.text = order.problemDescription
            
            // Показываем кнопки Принять/Отклонить только для pending заявок
            val isPendingAssignment = order.assignmentStatus == "pending" && order.assignmentId != null
            if (isPendingAssignment && !selectionMode) {
                actionButtonsContainer.visibility = View.VISIBLE
                btnAcceptOrder.setOnClickListener {
                    onAcceptOrder?.invoke(order)
                }
                btnRejectOrder.setOnClickListener {
                    onRejectOrder?.invoke(order)
                }
            } else {
                actionButtonsContainer.visibility = View.GONE
                btnAcceptOrder.setOnClickListener(null)
                btnRejectOrder.setOnClickListener(null)
            }
            
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
                } else if (isPendingAssignment) {
                    // Для pending заявок не открываем детали - можно только принять/отклонить через кнопки
                    // Ничего не делаем при клике на карточку
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
