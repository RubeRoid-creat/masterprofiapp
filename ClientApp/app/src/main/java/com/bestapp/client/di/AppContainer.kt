package com.bestapp.client.di

import android.content.Context
import com.bestapp.client.data.api.ApiService
import com.bestapp.client.data.api.MediaApiService
import com.bestapp.client.data.local.PreferencesManager
import com.bestapp.client.data.repository.ApiRepository
import com.bestapp.client.data.websocket.WebSocketService
import com.bestapp.client.services.FcmService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual Dependency Injection Container
 * Проще и надёжнее чем Hilt для нашего случая
 */
object AppContainer {
    
    private lateinit var _appContext: Context
    // Продакшн‑backend на сервере
    const val BASE_URL = "http://212.74.227.208:3000/"
    const val WS_BASE_URL = "ws://212.74.227.208:3000/ws"
    
    val appContext: Context
        get() = if (::_appContext.isInitialized) _appContext else throw IllegalStateException("AppContainer not initialized")
    
    fun init(context: Context) {
        _appContext = context.applicationContext
    }
    
    // PreferencesManager
    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(appContext)
    }
    
    // Logging Interceptor
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }
    
    // Auth Interceptor
    private val authInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            
            try {
                val token = runBlocking { 
                    kotlinx.coroutines.withTimeout(100) {
                        preferencesManager.authToken.first()
                    }
                }
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            } catch (e: Exception) {
                // No token available, proceed without auth header
            }
            
            chain.proceed(requestBuilder.build())
        }
    }
    
    // OkHttpClient
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // ApiService
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    // MediaApiService
    val mediaApiService: MediaApiService by lazy {
        retrofit.create(MediaApiService::class.java)
    }
    
    // ApiRepository
    val apiRepository: ApiRepository by lazy {
        ApiRepository(apiService, preferencesManager)
    }
    
    // WebSocketService
    val webSocketService: WebSocketService by lazy {
        WebSocketService(preferencesManager, WS_BASE_URL)
    }
    
    // FcmService
    val fcmService: FcmService by lazy {
        FcmService(apiService, appContext)
    }
}

