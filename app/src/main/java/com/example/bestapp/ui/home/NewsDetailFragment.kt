package com.example.bestapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.data.NewsCategory
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsDetailFragment : Fragment() {
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var newsImage: ImageView
    private lateinit var newsCategory: Chip
    private lateinit var newsTitle: TextView
    private lateinit var newsDate: TextView
    private lateinit var newsContent: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_news_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews(view)
        setupToolbar()
        displayNews()
    }
    
    private fun setupViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        newsImage = view.findViewById(R.id.news_image)
        newsCategory = view.findViewById(R.id.news_category)
        newsTitle = view.findViewById(R.id.news_title)
        newsDate = view.findViewById(R.id.news_date)
        newsContent = view.findViewById(R.id.news_content)
    }
    
    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun displayNews() {
        val arguments = arguments ?: return
        
        val newsCategoryStr = arguments.getString("newsCategory", "tips")
        val newsTitleStr = arguments.getString("newsTitle", "")
        val newsContentStr = arguments.getString("newsContent", "")
        val newsImageUrl = arguments.getString("newsImageUrl", "")
        val newsPublishedAt = arguments.getString("newsPublishedAt", "")
        
        // Парсим категорию
        val category = when (newsCategoryStr.lowercase()) {
            "tips" -> NewsCategory.TIPS
            "industry" -> NewsCategory.INDUSTRY
            "guides" -> NewsCategory.GUIDES
            "tools" -> NewsCategory.TOOLS
            "trends" -> NewsCategory.TRENDS
            else -> NewsCategory.TIPS
        }
        
        // Категория
        newsCategory.text = category.displayName
        
        // Цвета категорий
        val (bgColor, textColor) = when (category) {
            NewsCategory.TIPS -> Pair(R.color.news_tips_bg, R.color.news_tips_text)
            NewsCategory.INDUSTRY -> Pair(R.color.news_industry_bg, R.color.news_industry_text)
            NewsCategory.GUIDES -> Pair(R.color.news_guides_bg, R.color.news_guides_text)
            NewsCategory.TOOLS -> Pair(R.color.news_tools_bg, R.color.news_tools_text)
            NewsCategory.TRENDS -> Pair(R.color.news_trends_bg, R.color.news_trends_text)
        }
        
        newsCategory.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), bgColor)
        )
        newsCategory.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        
        // Заголовок
        newsTitle.text = newsTitleStr
        
        // Дата
        val date = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(newsPublishedAt) 
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(newsPublishedAt) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale("ru"))
        newsDate.text = formatter.format(date)
        
        // Содержание
        newsContent.text = newsContentStr
        
        // Изображение (если есть) - пока скрываем, так как Picasso может быть не подключен
        if (!newsImageUrl.isNullOrBlank()) {
            // TODO: Добавить загрузку изображения через Glide или другой библиотекой
            newsImage.visibility = View.GONE
        } else {
            newsImage.visibility = View.GONE
        }
    }
}
