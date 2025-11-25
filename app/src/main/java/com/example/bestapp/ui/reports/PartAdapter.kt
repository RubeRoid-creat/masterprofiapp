package com.example.bestapp.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.models.PartUsed
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class PartAdapter(
    private val onRemoveClick: (Int) -> Unit
) : ListAdapter<PartUsed, PartAdapter.ViewHolder>(DiffCallback()) {
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"))
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_part_used, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPart: MaterialCardView = itemView.findViewById(R.id.card_part)
        private val textName: TextView = itemView.findViewById(R.id.text_part_name)
        private val textQuantity: TextView = itemView.findViewById(R.id.text_part_quantity)
        private val textCost: TextView = itemView.findViewById(R.id.text_part_cost)
        private val btnRemove: MaterialButton = itemView.findViewById(R.id.btn_remove_part)
        
        fun bind(part: PartUsed, position: Int) {
            textName.text = part.name
            textQuantity.text = "Количество: ${part.quantity}"
            val totalCost = part.cost * part.quantity
            textCost.text = currencyFormat.format(totalCost)
            
            btnRemove.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<PartUsed>() {
        override fun areItemsTheSame(oldItem: PartUsed, newItem: PartUsed): Boolean {
            return oldItem.name == newItem.name
        }
        
        override fun areContentsTheSame(oldItem: PartUsed, newItem: PartUsed): Boolean {
            return oldItem == newItem
        }
    }
}


