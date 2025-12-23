package com.example.bestapp.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.bestapp.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    
    // Продакшн‑backend на сервере
    const val BASE_URL = "http://212.74.227.208:3000/"
    
    /**
     * Проверка наличия интернет-подключения
     */
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) {
            Log.w(TAG, "⚠️ Context is null, cannot check network")
            return true // Предполагаем, что сеть есть, если контекст недоступен
        }
        
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                             capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            Log.d(TAG, "Network check: ${if (isConnected) "✅ Connected" else "❌ Not connected"}")
            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network", e)
            true // Предполагаем, что сеть есть при ошибке проверки
        }
    }
    
    /**
     * Проверка доступности сервера
     * @return Pair<Boolean, String> где Boolean - доступен ли сервер, String - сообщение
     */
    suspend fun checkServerAvailability(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        // Сначала проверяем сеть
        if (appContext != null && !isNetworkAvailable(appContext)) {
            val message = "Нет подключения к интернету. Проверьте Wi-Fi или мобильные данные."
            Log.e(TAG, "❌ $message")
            return@withContext Pair(false, message)
        }
        
        try {
            Log.d(TAG, "🔍 Проверка доступности сервера: $BASE_URL")
            
            // Проверяем доступность через простой HTTP запрос
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            // Пробуем подключиться к корню сервера
            val request = okhttp3.Request.Builder()
                .url("$BASE_URL")
                .get()
                .build()
            
            Log.d(TAG, "📡 Отправка запроса к серверу...")
            val response = client.newCall(request).execute()
            
            val isAvailable = response.isSuccessful || response.code in 200..499
            val message = if (isAvailable) {
                "✅ Сервер доступен (код: ${response.code})"
            } else {
                "⚠️ Сервер отвечает, но с ошибкой (код: ${response.code})"
            }
            Log.d(TAG, "Server availability check: $message")
            response.close()
            Pair(isAvailable, message)
        } catch (e: Exception) {
            val errorType = when (e) {
                is java.net.ConnectException -> {
                    Log.e(TAG, "❌ ConnectException: Не удалось подключиться к серверу")
                    Log.e(TAG, "   Возможные причины:")
                    Log.e(TAG, "   1. Сервер не запущен")
                    Log.e(TAG, "   2. Неправильный IP-адрес или порт")
                    Log.e(TAG, "   3. Файрвол блокирует подключение")
                    Log.e(TAG, "   4. Приложение находится в другой сети")
                    "Сервер не отвечает. Проверьте:\n• Запущен ли сервер\n• Правильность IP: 212.74.227.208:3000\n• Настройки сети"
                }
                is java.net.SocketTimeoutException -> {
                    Log.e(TAG, "❌ SocketTimeoutException: Превышено время ожидания")
                    "Превышено время ожидания. Сервер не отвечает."
                }
                is java.net.UnknownHostException -> {
                    Log.e(TAG, "❌ UnknownHostException: Не удалось найти сервер")
                    Log.e(TAG, "   URL: $BASE_URL")
                    "Не удалось найти сервер. Проверьте адрес: $BASE_URL"
                }
                else -> {
                    Log.e(TAG, "❌ Неизвестная ошибка: ${e.javaClass.simpleName}")
                    Log.e(TAG, "   Сообщение: ${e.message}")
                    "Ошибка подключения: ${e.message ?: "Неизвестная ошибка"}"
                }
            }
            val errorMsg = "$errorType\nURL: $BASE_URL"
            Log.e(TAG, "❌ $errorMsg", e)
            Pair(false, errorMsg)
        }
    }
    
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
        
        // Проверяем наличие сети перед запросом
        if (appContext != null && !isNetworkAvailable(appContext)) {
            Log.e(TAG, "❌ No network connection for ${originalRequest.method} ${originalRequest.url}")
        }
        
        val response = chain.proceed(newRequest)
        
        // Логируем ошибки подключения
        if (response.code >= 500) {
            Log.e(TAG, "⚠️ Server error ${response.code} for ${originalRequest.method} ${originalRequest.url}")
        } else if (response.code == 404) {
            Log.w(TAG, "⚠️ Not found (404) for ${originalRequest.method} ${originalRequest.url}")
        }
        
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
        Log.d(TAG, "🔧 Initializing OkHttpClient for BASE_URL: $BASE_URL")
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d(TAG, "📡 Request: ${request.method} ${request.url}")
                Log.d(TAG, "   Headers: ${request.headers}")
                try {
                    val response = chain.proceed(request)
                    Log.d(TAG, "📥 Response: ${response.code} ${response.message}")
                    response
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Request failed: ${request.method} ${request.url}", e)
                    Log.e(TAG, "   Error type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "   Error message: ${e.message}")
                    throw e
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS) // Увеличено с 15 до 30 секунд
            .readTimeout(60, TimeUnit.SECONDS) // Увеличено с 30 до 60 секунд
            .writeTimeout(60, TimeUnit.SECONDS) // Увеличено с 30 до 60 секунд
            .retryOnConnectionFailure(true) // Автоматическая повторная попытка при ошибках подключения
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

