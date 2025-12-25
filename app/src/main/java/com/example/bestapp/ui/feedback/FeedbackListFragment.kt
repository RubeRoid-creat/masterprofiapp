package com.example.bestapp.ui.feedback

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiFeedback
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FeedbackListFragment : Fragment() {
    
    private val apiRepository = ApiRepository()
    private lateinit var adapter: FeedbackAdapter
    private var recyclerFeedback: RecyclerView? = null
    private var textEmpty: TextView? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_feedback_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerFeedback = view.findViewById(R.id.recycler_feedback)
        textEmpty = view.findViewById(R.id.text_empty)
        
        adapter = FeedbackAdapter()
        recyclerFeedback?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FeedbackListFragment.adapter
        }
        
        loadFeedbackList()
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на экран
        loadFeedbackList()
    }
    
    private fun loadFeedbackList() {
        lifecycleScope.launch {
            try {
                val result = apiRepository.getFeedbackList()
                result.onSuccess { feedbackList ->
                    if (feedbackList.isEmpty()) {
                        textEmpty?.visibility = View.VISIBLE
                        recyclerFeedback?.visibility = View.GONE
                    } else {
                        textEmpty?.visibility = View.GONE
                        recyclerFeedback?.visibility = View.VISIBLE
                        adapter.submitList(feedbackList)
                    }
                }.onFailure { error ->
                    Toast.makeText(context, "Ошибка загрузки: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FeedbackListFragment", "Error loading feedback", error)
                }
            } catch (e: Exception) {
                Log.e("FeedbackListFragment", "Error loading feedback", e)
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private class FeedbackAdapter : androidx.recyclerview.widget.ListAdapter<ApiFeedback, FeedbackAdapter.ViewHolder>(DiffCallback()) {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feedback, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val subject: TextView = itemView.findViewById(R.id.feedback_subject)
            private val type: TextView = itemView.findViewById(R.id.feedback_type)
            private val message: TextView = itemView.findViewById(R.id.feedback_message)
            private val status: Chip = itemView.findViewById(R.id.feedback_status)
            private val date: TextView = itemView.findViewById(R.id.feedback_date)
            private val cardAdminResponse: MaterialCardView = itemView.findViewById(R.id.card_admin_response)
            private val adminResponse: TextView = itemView.findViewById(R.id.feedback_admin_response)
            private val respondedAt: TextView = itemView.findViewById(R.id.feedback_responded_at)
            
            fun bind(feedback: ApiFeedback) {
                subject.text = feedback.subject
                
                val typeNames = mapOf(
                    "suggestion" to "Предложение",
                    "bug_report" to "Сообщение об ошибке",
                    "complaint" to "Жалоба",
                    "praise" to "Благодарность",
                    "other" to "Другое"
                )
                type.text = typeNames[feedback.feedbackType] ?: feedback.feedbackType
                
                message.text = feedback.message
                
                val statusNames = mapOf(
                    "new" to "Новое",
                    "in_progress" to "В работе",
                    "resolved" to "Решено",
                    "closed" to "Закрыто"
                )
                status.text = statusNames[feedback.status] ?: feedback.status
                
                when (feedback.status) {
                    "new" -> status.setChipBackgroundColorResource(android.R.color.darker_gray)
                    "in_progress" -> status.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                    "resolved" -> status.setChipBackgroundColorResource(android.R.color.holo_green_light)
                    "closed" -> status.setChipBackgroundColorResource(android.R.color.darker_gray)
                }
                
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val dateObj = dateFormat.parse(feedback.createdAt)
                    val displayFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    date.text = "Создано: ${displayFormat.format(dateObj ?: Date())}"
                } catch (e: Exception) {
                    date.text = "Создано: ${feedback.createdAt}"
                }
                
                // Показываем ответ администрации, если есть
                if (!feedback.adminResponse.isNullOrEmpty()) {
                    cardAdminResponse.visibility = View.VISIBLE
                    adminResponse.text = feedback.adminResponse
                    
                    if (!feedback.respondedAt.isNullOrEmpty()) {
                        try {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val dateObj = dateFormat.parse(feedback.respondedAt)
                            val displayFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                            respondedAt.text = displayFormat.format(dateObj ?: Date())
                        } catch (e: Exception) {
                            respondedAt.text = feedback.respondedAt
                        }
                    } else {
                        respondedAt.visibility = View.GONE
                    }
                } else {
                    cardAdminResponse.visibility = View.GONE
                }
            }
        }
        
        class DiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<ApiFeedback>() {
            override fun areItemsTheSame(oldItem: ApiFeedback, newItem: ApiFeedback): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: ApiFeedback, newItem: ApiFeedback): Boolean {
                return oldItem == newItem
            }
        }
    }
}
