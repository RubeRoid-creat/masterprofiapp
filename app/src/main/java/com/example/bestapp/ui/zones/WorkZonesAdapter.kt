package com.example.bestapp.ui.zones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.data.MapWorkZone
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class WorkZonesAdapter(
    private val onZoneToggle: (MapWorkZone, Boolean) -> Unit,
    private val onZoneDelete: (MapWorkZone) -> Unit
) : ListAdapter<MapWorkZone, WorkZonesAdapter.ZoneViewHolder>(ZoneDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_zone, parent, false)
        return ZoneViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ZoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val zoneName: TextView = itemView.findViewById(R.id.zone_name)
        private val zonePointsCount: TextView = itemView.findViewById(R.id.zone_points_count)
        private val switchActive: SwitchMaterial = itemView.findViewById(R.id.switch_zone_active)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete_zone)
        
        fun bind(zone: MapWorkZone) {
            zoneName.text = zone.name
            zonePointsCount.text = "${zone.points.size} точек"
            switchActive.isChecked = zone.isActive
            
            switchActive.setOnCheckedChangeListener { _, isChecked ->
                onZoneToggle(zone, isChecked)
            }
            
            btnDelete.setOnClickListener {
                onZoneDelete(zone)
            }
        }
    }
    
    class ZoneDiffCallback : DiffUtil.ItemCallback<MapWorkZone>() {
        override fun areItemsTheSame(oldItem: MapWorkZone, newItem: MapWorkZone): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: MapWorkZone, newItem: MapWorkZone): Boolean {
            return oldItem == newItem
        }
    }
}

