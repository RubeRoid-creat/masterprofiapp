package com.example.bestapp

import android.app.Application
import android.util.Log
import com.example.bestapp.api.RetrofitClient
import com.yandex.mapkit.MapKitFactory

class BestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем RetrofitClient для загрузки сохраненного токена
        RetrofitClient.initialize(this)
        
        try {
            // 1. Устанавливаем API ключ
            MapKitFactory.setApiKey("ee942cae-a237-4707-b779-76b1a8de7389")
            
            // 2. Инициализируем MapKit сразу в Application
            MapKitFactory.initialize(this)
            
            Log.d("BestApp", "MapKit initialized successfully")
        } catch (e: Exception) {
            Log.e("BestApp", "Error initializing MapKit", e)
        }
    }
}

