package com.example.bestapp.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import com.example.bestapp.R
import com.example.bestapp.api.models.ApiChatMessage
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val currentUserId: Long
) : ListAdapter<ApiChatMessage, ChatAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view, currentUserId)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        itemView: View,
        private val currentUserId: Long
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val messageContainer: ViewGroup = itemView.findViewById(R.id.message_container)
        private val messageSender: TextView = itemView.findViewById(R.id.message_sender)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val messageImage: ImageView = itemView.findViewById(R.id.message_image)
        private val messageTime: TextView = itemView.findViewById(R.id.message_time)

        fun bind(message: ApiChatMessage) {
            val isMyMessage = message.senderId == currentUserId
            
            // Выравнивание: мои сообщения справа, чужие слева
            val layoutParams = messageContainer.layoutParams as ViewGroup.MarginLayoutParams
            val marginSize = (itemView.context.resources.displayMetrics.density * 100).toInt() // 100dp
            if (isMyMessage) {
                layoutParams.marginStart = marginSize
                layoutParams.marginEnd = 0
                messageContainer.layoutParams = layoutParams
                messageContainer.layoutDirection = View.LAYOUT_DIRECTION_RTL
            } else {
                layoutParams.marginStart = 0
                layoutParams.marginEnd = marginSize
                messageContainer.layoutParams = layoutParams
                messageContainer.layoutDirection = View.LAYOUT_DIRECTION_LTR
            }

            // Отправитель (только для чужих сообщений)
            if (isMyMessage) {
                messageSender.visibility = View.GONE
            } else {
                messageSender.visibility = View.VISIBLE
                messageSender.text = message.senderName
            }

            // Текст или изображение
            when (message.messageType) {
                "text" -> {
                    messageText.visibility = View.VISIBLE
                    messageImage.visibility = View.GONE
                    messageText.text = message.messageText
                }
                "image" -> {
                    messageText.visibility = View.GONE
                    messageImage.visibility = View.VISIBLE
                    message.imageUrl?.let { imageUrl ->
                        val fullUrl = if (imageUrl.startsWith("http")) {
                            imageUrl
                        } else {
                            // Используем продакшн‑backend
                            "http://212.74.227.208:3000$imageUrl"
                        }
                        loadImageFromUrl(messageImage, fullUrl)
                    }
                }
                else -> {
                    messageText.visibility = View.GONE
                    messageImage.visibility = View.GONE
                }
            }

            // Время
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = dateFormat.parse(message.createdAt)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                messageTime.text = date?.let { timeFormat.format(it) } ?: ""
            } catch (e: Exception) {
                messageTime.text = ""
            }
        }
        
        private fun loadImageFromUrl(imageView: android.widget.ImageView, imageUrl: String) {
            object : AsyncTask<String, Void, Bitmap?>() {
                override fun doInBackground(vararg params: String): Bitmap? {
                    return try {
                        val url = URL(params[0])
                        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                        connection.doInput = true
                        connection.connect()
                        val input: InputStream = connection.inputStream
                        BitmapFactory.decodeStream(input)
                    } catch (e: Exception) {
                        null
                    }
                }

                override fun onPostExecute(result: Bitmap?) {
                    if (result != null) {
                        imageView.setImageBitmap(result)
                    } else {
                        imageView.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                }
            }.execute(imageUrl)
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ApiChatMessage>() {
        override fun areItemsTheSame(oldItem: ApiChatMessage, newItem: ApiChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ApiChatMessage, newItem: ApiChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}

