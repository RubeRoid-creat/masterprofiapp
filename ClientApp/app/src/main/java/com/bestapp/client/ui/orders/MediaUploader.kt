package com.bestapp.client.ui.orders

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.bestapp.client.data.api.models.OrderMediaDto

data class MediaItem(
    val uri: Uri,
    val type: MediaType,
    val name: String,
    val size: Long? = null
)

enum class MediaType {
    PHOTO, VIDEO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaUploader(
    mediaItems: List<MediaItem>,
    onAddPhoto: () -> Unit,
    onAddVideo: () -> Unit,
    onRemove: (Int) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📷 Фото/видео проблемы",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${mediaItems.size}/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Кнопки добавления
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val photoCount = mediaItems.count { it.type == MediaType.PHOTO }
                val videoCount = mediaItems.count { it.type == MediaType.VIDEO }
                
                OutlinedButton(
                    onClick = onAddPhoto,
                    enabled = photoCount < 5 && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Фото (${photoCount}/5)")
                }
                
                OutlinedButton(
                    onClick = onAddVideo,
                    enabled = videoCount < 1 && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Видео (${videoCount}/1)")
                }
            }
            
            // Список загруженных медиа
            if (mediaItems.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(mediaItems.size) { index ->
                        MediaPreviewItem(
                            mediaItem = mediaItems[index],
                            onRemove = { onRemove(index) },
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }
            }
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun MediaPreviewItem(
    mediaItem: MediaItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (mediaItem.type == MediaType.PHOTO) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(mediaItem.uri)
                            .build()
                    ),
                    contentDescription = "Фото",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Видео",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Кнопка удаления
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Удалить",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}





