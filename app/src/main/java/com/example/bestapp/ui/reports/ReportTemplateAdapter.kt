package com.example.bestapp.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.models.ApiReportTemplate
import com.google.android.material.card.MaterialCardView

class ReportTemplateAdapter(
    private val onTemplateClick: (ApiReportTemplate) -> Unit
) : ListAdapter<ApiReportTemplate, ReportTemplateAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_template, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardTemplate: MaterialCardView = itemView.findViewById(R.id.card_template)
        private val textName: TextView = itemView.findViewById(R.id.text_template_name)
        private val textDescription: TextView = itemView.findViewById(R.id.text_template_description)
        
        fun bind(template: ApiReportTemplate) {
            textName.text = template.name
            textDescription.text = template.description ?: ""
            
            cardTemplate.setOnClickListener {
                onTemplateClick(template)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ApiReportTemplate>() {
        override fun areItemsTheSame(oldItem: ApiReportTemplate, newItem: ApiReportTemplate): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ApiReportTemplate, newItem: ApiReportTemplate): Boolean {
            return oldItem == newItem
        }
    }
}


