package com.example.bestapp.api

import android.content.Context
import android.util.Log
import com.example.bestapp.data.PreferencesManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    
    // Продакшн‑backend на сервере
    private const val BASE_URL = "http://212.74.227.208:3000/"
    
    @Volatile
    private var authToken: String? = null
    
    private var prefsManager: PreferencesManager? = null
    
    @Volatile
    private var appContext: Context? = null
    
    /**
     * Инициализация с контекстом для загрузки сохраненного токена
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        prefsManager = PreferencesManager.getInstance(context)
        authToken = prefsManager?.getAuthToken()
        if (authToken != null) {
            Log.d(TAG, "✅ RetrofitClient initialized. Token loaded from storage: ${authToken!!.take(30)}...")
        } else {
            Log.w(TAG, "⚠️ RetrofitClient initialized. NO TOKEN in storage. User needs to login.")
        }
    }
    
    fun setAuthToken(token: String?) {
        val oldToken = authToken
        authToken = token
        
        // Если prefsManager не инициализирован, пробуем получить его через Application
        if (prefsManager == null) {
            Log.w(TAG, "⚠️ PrefsManager is null! Trying to initialize...")
            try {
                val context = android.app.Application::class.java.getMethod("getInstance").invoke(null) as? android.content.Context
                    ?: throw Exception("Cannot get Application context")
                initialize(context)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cannot initialize PrefsManager: ${e.message}")
            }
        }
        
        // Сохраняем токен
        prefsManager?.setAuthToken(token)
        
        if (token != null) {
            Log.d(TAG, "✅ Token saved to memory: ${token.take(30)}...")
            // Проверяем, что токен действительно сохранился
            val savedToken = prefsManager?.getAuthToken()
            if (savedToken == token) {
                Log.d(TAG, "✅ Token verified in storage")
            } else {
                Log.e(TAG, "❌ Token verification failed! Saved: ${savedToken?.take(30)}, Expected: ${token.take(30)}")
            }
        } else {
            Log.d(TAG, "Token cleared from memory and storage")
        }
        
        // Если токен изменился, логируем это
        if (oldToken != token) {
            if (token != null) {
                Log.d(TAG, "🔄 Token updated from '${oldToken?.take(10) ?: "null"}' to '${token.take(10)}'. New token will be used in next requests.")
            } else {
                Log.w(TAG, "🔄 Token removed. Next requests will fail without authentication.")
            }
        }
    }
    
    fun getAuthToken(): String? = authToken
    
    // Logging interceptor для отладки
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Auth interceptor для добавления токена в заголовки
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        // Не добавляем токен для публичных эндпоинтов (логин, регистрация, проверка версии)
        val path = originalRequest.url.encodedPath
        val isPublicEndpoint = path.contains("/api/auth/login") ||
                              path.contains("/api/auth/register") ||
                              path.contains("/api/version/check")
        
        if (!isPublicEndpoint) {
            // Всегда проверяем актуальный токен (может измениться после логина)
            // Сначала проверяем в памяти, потом в prefsManager, потом пытаемся загрузить напрямую
            var currentToken = authToken
            
            if (currentToken == null) {
                // Пробуем получить из prefsManager
                currentToken = prefsManager?.getAuthToken()
                
                // Если prefsManager null, пытаемся инициализировать через сохраненный контекст
                if (currentToken == null && prefsManager == null && appContext != null) {
                    try {
                        val tempPrefsManager = PreferencesManager.getInstance(appContext!!)
                        currentToken = tempPrefsManager.getAuthToken()
                        if (currentToken != null) {
                            // Обновляем prefsManager для будущих запросов
                            prefsManager = tempPrefsManager
                            authToken = currentToken
                            Log.d(TAG, "✅ Loaded token from PreferencesManager directly")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to load token from PreferencesManager: ${e.message}")
                    }
                }
            }
            
            if (currentToken != null && currentToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $currentToken")
                Log.d(TAG, "✅ Adding Authorization header to ${originalRequest.method} ${originalRequest.url}")
                Log.d(TAG, "   Token: Bearer ${currentToken.take(30)}...")
            } else {
                Log.e(TAG, "❌ NO AUTH TOKEN! Request: ${originalRequest.method} ${originalRequest.url}")
                Log.e(TAG, "   Token was not loaded from storage. User needs to login again.")
                Log.e(TAG, "   authToken in memory: ${authToken != null}, prefsManager: ${prefsManager != null}")
            }
        } else {
            Log.d(TAG, "Public endpoint, skipping auth token: ${originalRequest.method} ${originalRequest.url}")
        }
        
        val newRequest = requestBuilder.build()
        val response = chain.proceed(newRequest)
        
        // Логируем ответ
        if (response.code == 401) {
            Log.e(TAG, "⚠️ 401 Unauthorized for ${originalRequest.method} ${originalRequest.url}")
            if (isPublicEndpoint) {
                Log.e(TAG, "   This is a public endpoint - check credentials or server error")
            } else {
                Log.e(TAG, "   This means token is missing or invalid")
                // Очищаем токен при 401, чтобы пользователь перелогинился
                authToken = null
                prefsManager?.setAuthToken(null)
            }
        }
        
        response
    }
    
    // OkHttp клиент с interceptors (создается лениво)
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Retrofit instance (создается лениво)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // API Service instance (создается лениво)
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

