package com.example.bestapp.ui.common

import android.os.CountDownTimer
import android.widget.TextView
import java.util.Date

/**
 * Компонент для отображения таймера обратного отсчета
 */
class CountdownTimerView(
    private val textView: TextView,
    private val expiresAt: Date,
    private val onExpired: (() -> Unit)? = null
) {
    private var timer: CountDownTimer? = null
    private val updateInterval = 1000L // Обновление каждую секунду
    
    init {
        start()
    }
    
    fun start() {
        val now = Date().time
        val expires = expiresAt.time
        val remaining = expires - now
        
        if (remaining <= 0) {
            textView.text = "Время истекло"
            onExpired?.invoke()
            return
        }
        
        timer?.cancel()
        
        timer = object : CountDownTimer(remaining, updateInterval) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerText(millisUntilFinished)
            }
            
            override fun onFinish() {
                textView.text = "Время истекло"
                onExpired?.invoke()
            }
        }.start()
    }
    
    fun stop() {
        timer?.cancel()
        timer = null
    }
    
    private fun updateTimerText(millisUntilFinished: Long) {
        val seconds = millisUntilFinished / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        val text = when {
            days > 0 -> "${days}д ${hours % 24}ч ${minutes % 60}м"
            hours > 0 -> "${hours}ч ${minutes % 60}м ${seconds % 60}с"
            minutes > 0 -> "${minutes}м ${seconds % 60}с"
            else -> "${seconds}с"
        }
        
        textView.text = "⏱️ Осталось: $text"
    }
    
    fun isExpired(): Boolean {
        return Date().time >= expiresAt.time
    }
    
    fun getRemainingMillis(): Long {
        val now = Date().time
        val expires = expiresAt.time
        return (expires - now).coerceAtLeast(0)
    }
}




