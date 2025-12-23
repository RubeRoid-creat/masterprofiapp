package com.bestapp.client.ui.home

data class NewsItem(
    val id: Long,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val timestamp: Long,
    val category: String? = null
)







