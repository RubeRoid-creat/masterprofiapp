package com.bestapp.client.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Icons8 icon IDs for media types
 */
object Icons8 {
    const val PHOTO = "53386"
    const val VIDEO = "35090"
    const val DOCUMENT = "DHOunydDcKfC"
    const val ATTACH = "123834"
    const val EXPAND_DOWN = "2760"
    const val EXPAND_UP = "40021"
    
    /**
     * Get Icons8 URL for icon
     */
    fun getUrl(iconId: String, size: Int = 24): String {
        return "https://img.icons8.com/?id=$iconId&format=png&size=$size"
    }
}

@Composable
fun Icons8Icon(
    iconId: String,
    contentDescription: String? = null,
    size: Int = 24,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color? = null
) {
    AsyncImage(
        model = Icons8.getUrl(iconId, size),
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp),
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}

