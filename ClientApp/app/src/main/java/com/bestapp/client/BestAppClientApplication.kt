package com.bestapp.client

import android.app.Application
import android.util.Log
import com.bestapp.client.di.AppContainer
import com.yandex.mapkit.MapKitFactory

class BestAppClientApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализируем Manual DI Container
        AppContainer.init(this)
        
        // Инициализируем Yandex MapKit
        try {
            MapKitFactory.setApiKey("ee942cae-a237-4707-b779-76b1a8de7389")
            MapKitFactory.initialize(this)
            Log.d("BestAppClient", "MapKit initialized successfully")
        } catch (e: Exception) {
            Log.e("BestAppClient", "Error initializing MapKit", e)
        }
    }
}

