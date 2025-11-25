# Интеграция Android-приложения с Backend API

## Обзор

Этот документ описывает, как интегрировать Android-приложение BestApp с бэкенд API.

## Базовая настройка

### 1. Добавить зависимости в `app/build.gradle.kts`

```kotlin
dependencies {
    // Retrofit для HTTP запросов
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // OkHttp WebSocket для real-time уведомлений
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Gson для JSON
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### 2. Добавить разрешение в `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Для локальной разработки (разрешает незашифрованный трафик) -->
<application
    android:usesCleartextTraffic="true"
    ...>
```

## Структура API клиента

### 1. Создать модели данных

```kotlin
// app/src/main/java/com/example/bestapp/api/models/LoginRequest.kt
data class LoginRequest(
    val email: String,
    val password: String
)

// app/src/main/java/com/example/bestapp/api/models/LoginResponse.kt
data class LoginResponse(
    val message: String,
    val token: String,
    val user: User
)

data class User(
    val id: Long,
    val email: String,
    val name: String,
    val phone: String,
    val role: String,
    val masterId: Long? = null,
    val isOnShift: Boolean? = null
)

// app/src/main/java/com/example/bestapp/api/models/OrderResponse.kt
data class OrderResponse(
    val id: Long,
    val client_id: Long,
    val device_type: String,
    val device_brand: String?,
    val device_model: String?,
    val problem_description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val arrival_time: String?,
    val order_type: String,
    val repair_status: String,
    val estimated_cost: Double?,
    val client_name: String,
    val client_phone: String,
    val created_at: String
)
```

### 2. Создать API Service

```kotlin
// app/src/main/java/com/example/bestapp/api/ApiService.kt
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Авторизация
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>
    
    // Заказы
    @GET("api/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("deviceType") deviceType: String? = null,
        @Query("orderType") orderType: String? = null
    ): Response<List<OrderResponse>>
    
    @GET("api/orders/{id}")
    suspend fun getOrder(@Path("id") id: Long): Response<OrderResponse>
    
    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<CreateOrderResponse>
    
    // Мастера
    @GET("api/masters")
    suspend fun getMasters(
        @Query("specialization") specialization: String? = null,
        @Query("status") status: String? = null,
        @Query("isOnShift") isOnShift: Boolean? = null
    ): Response<List<MasterResponse>>
    
    @POST("api/masters/shift/start")
    suspend fun startShift(@Body location: LocationRequest): Response<MessageResponse>
    
    @POST("api/masters/shift/end")
    suspend fun endShift(): Response<MessageResponse>
    
    // Назначения
    @GET("api/assignments/my")
    suspend fun getMyAssignments(
        @Query("status") status: String? = null
    ): Response<List<AssignmentResponse>>
    
    @POST("api/assignments/{id}/accept")
    suspend fun acceptAssignment(@Path("id") id: Long): Response<MessageResponse>
    
    @POST("api/assignments/{id}/reject")
    suspend fun rejectAssignment(
        @Path("id") id: Long,
        @Body reason: RejectReasonRequest
    ): Response<MessageResponse>
}
```

### 3. Создать Retrofit Client

```kotlin
// app/src/main/java/com/example/bestapp/api/RetrofitClient.kt
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/" // Для эмулятора
    // Для реального устройства используйте IP вашего компьютера: "http://192.168.1.X:3000/"
    
    private var authToken: String? = null
    
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        
        authToken?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        
        chain.proceed(requestBuilder.build())
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
```

### 4. Создать Repository

```kotlin
// app/src/main/java/com/example/bestapp/repository/ApiRepository.kt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiRepository {
    private val api = RetrofitClient.apiService
    
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    // Сохраняем токен
                    RetrofitClient.setAuthToken(response.body()!!.token)
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка входа: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getOrders(deviceType: String? = null): Result<List<OrderResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getOrders(deviceType = deviceType)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка получения заказов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun startShift(latitude: Double, longitude: Double): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.startShift(LocationRequest(latitude, longitude))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка начала смены: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun acceptAssignment(assignmentId: Long): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.acceptAssignment(assignmentId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка принятия заказа: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

### 5. WebSocket для real-time уведомлений

```kotlin
// app/src/main/java/com/example/bestapp/websocket/WebSocketManager.kt
import okhttp3.*
import org.json.JSONObject

class WebSocketManager(private val token: String) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val listeners = mutableListOf<WebSocketListener>()
    
    interface WebSocketListener {
        fun onNewAssignment(assignment: JSONObject)
        fun onOrderStatusChanged(order: JSONObject)
        fun onError(error: String)
    }
    
    fun connect() {
        val request = Request.Builder()
            .url("ws://10.0.2.2:3000/ws")
            .build()
        
        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Отправляем токен для аутентификации
                val authMessage = JSONObject().apply {
                    put("type", "auth")
                    put("token", token)
                }
                webSocket.send(authMessage.toString())
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "new_assignment" -> {
                            listeners.forEach { 
                                it.onNewAssignment(json.getJSONObject("assignment"))
                            }
                        }
                        "order_status_changed" -> {
                            listeners.forEach { 
                                it.onOrderStatusChanged(json.getJSONObject("order"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e.message ?: "Unknown error") }
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listeners.forEach { it.onError(t.message ?: "Connection failed") }
            }
        })
    }
    
    fun addListener(listener: WebSocketListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
    }
}
```

## Использование в коде

### Пример: Вход в систему

```kotlin
class LoginViewModel : ViewModel() {
    private val repository = ApiRepository()
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.login(email, password)
            result.onSuccess { response ->
                // Вход успешен
                // Сохраните токен и информацию о пользователе
                println("Logged in as: ${response.user.name}")
            }.onFailure { error ->
                // Обработка ошибки
                println("Login error: ${error.message}")
            }
        }
    }
}
```

### Пример: Начало смены

```kotlin
fun startShift() {
    viewModelScope.launch {
        val latitude = 56.859611
        val longitude = 35.911896
        
        val result = repository.startShift(latitude, longitude)
        result.onSuccess {
            println("Смена начата")
        }.onFailure { error ->
            println("Ошибка: ${error.message}")
        }
    }
}
```

## Тестирование

### Тестовые учетные данные:

**Мастера:**
- Email: `smirnov@example.com`, Пароль: `password123`
- Email: `kuznetsov@example.com`, Пароль: `password123`
- Email: `popov@example.com`, Пароль: `password123`

**Клиенты:**
- Email: `ivanov@example.com`, Пароль: `password123`
- Email: `petrova@example.com`, Пароль: `password123`

## Важные замечания

1. **Адрес сервера для эмулятора:** `http://10.0.2.2:3000/`
2. **Адрес сервера для реального устройства:** `http://[IP_ВАШЕГО_КОМПЬЮТЕРА]:3000/`
3. **Убедитесь**, что бэкенд запущен перед тестированием
4. **Не забудьте** добавить `android:usesCleartextTraffic="true"` в манифест для HTTP (без SSL)

## Следующие шаги

1. Замените тестовые данные в `DataRepository` на запросы к API
2. Интегрируйте WebSocket для получения уведомлений о новых заказах
3. Добавьте кэширование данных для оффлайн режима
4. Реализуйте обработку ошибок сети







