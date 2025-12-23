package com.example.bestapp.ui.home

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
import com.example.bestapp.data.News
import com.example.bestapp.data.NewsCategory
import com.google.android.material.chip.Chip

class NewsAdapter(
    private val onNewsClick: (News) -> Unit
) : ListAdapter<News, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view, onNewsClick)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NewsViewHolder(
        itemView: View,
        private val onNewsClick: (News) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val newsCategory: Chip = itemView.findViewById(R.id.news_category)
        private val newsTitle: TextView = itemView.findViewById(R.id.news_title)
        private val newsSummary: TextView = itemView.findViewById(R.id.news_summary)
        private val newsDate: TextView = itemView.findViewById(R.id.news_date)

        fun bind(news: News) {
            val context = itemView.context
            newsCategory.text = news.category.displayName
            
            // Цвета категорий
            val (bgColor, textColor) = when (news.category) {
                NewsCategory.TIPS -> Pair(R.color.news_tips_bg, R.color.news_tips_text)
                NewsCategory.INDUSTRY -> Pair(R.color.news_industry_bg, R.color.news_industry_text)
                NewsCategory.GUIDES -> Pair(R.color.news_guides_bg, R.color.news_guides_text)
                NewsCategory.TOOLS -> Pair(R.color.news_tools_bg, R.color.news_tools_text)
                NewsCategory.TRENDS -> Pair(R.color.news_trends_bg, R.color.news_trends_text)
            }
            
            newsCategory.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, bgColor)
            )
            newsCategory.setTextColor(ContextCompat.getColor(context, textColor))

            newsTitle.text = news.title
            newsSummary.text = news.summary
            newsDate.text = news.getFormattedDate()

            itemView.setOnClickListener {
                onNewsClick(news)
            }
        }
    }

    private class NewsDiffCallback : DiffUtil.ItemCallback<News>() {
        override fun areItemsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem == newItem
        }
    }
}

