package com.example.bestapp.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bestapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File
import android.graphics.BitmapFactory

class PhotoAdapter(
    private val onRemoveClick: (Int) -> Unit
) : ListAdapter<String, PhotoAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPhoto: MaterialCardView = itemView.findViewById(R.id.card_photo)
        private val imagePhoto: ImageView = itemView.findViewById(R.id.image_photo)
        private val btnRemove: MaterialButton = itemView.findViewById(R.id.btn_remove_photo)
        
        fun bind(photoPath: String, position: Int) {
            // Загружаем изображение
            val file = File(photoPath)
            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imagePhoto.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // Если не удалось загрузить, пробуем через Glide
                    try {
                        Glide.with(itemView.context)
                            .load(file)
                            .into(imagePhoto)
                    } catch (e2: Exception) {
                        // Игнорируем ошибку
                    }
                }
            } else {
                // Если это URL
                try {
                    Glide.with(itemView.context)
                        .load(photoPath)
                        .into(imagePhoto)
                } catch (e: Exception) {
                    // Игнорируем ошибку
                }
            }
            
            btnRemove.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
        
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

