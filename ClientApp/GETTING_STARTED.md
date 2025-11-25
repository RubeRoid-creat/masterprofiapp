# Начало разработки ClientApp

## 🚀 Quick Start

Это руководство поможет вам начать разработку клиентского приложения с нуля.

## Предварительные требования

- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17+
- Gradle 8.2+
- Запущенный Backend API (`http://localhost:3000`)

## Шаг 1: Создание проекта в Android Studio

### 1.1 Создать новый проект
```
File → New → New Project
→ Empty Activity (Compose)
```

### 1.2 Настройки проекта
```
Name: BestApp Client
Package: com.bestapp.client
Save location: Z:\BestAPP\ClientApp
Language: Kotlin
Minimum SDK: API 24 (Android 7.0)
Build configuration language: Kotlin DSL
```

## Шаг 2: Настройка Gradle

### 2.1 `build.gradle.kts` (Project level)
```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

### 2.2 `build.gradle.kts` (App level)
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.bestapp.client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bestapp.client"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coil (Images)
    implementation("io.coil-kt:coil-compose:2.5.0")
}
```

## Шаг 3: Создание структуры пакетов

```
app/src/main/java/com/bestapp/client/
├── BestAppClientApplication.kt    # Application class
├── ui/
│   ├── theme/                     # Compose theme
│   ├── navigation/                # Navigation
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   ├── RegisterScreen.kt
│   │   └── AuthViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── orders/
│   │   ├── CreateOrderScreen.kt
│   │   ├── OrdersListScreen.kt
│   │   ├── OrderDetailScreen.kt
│   │   └── OrdersViewModel.kt
│   └── profile/
│       ├── ProfileScreen.kt
│       └── ProfileViewModel.kt
├── data/
│   ├── api/
│   │   ├── ApiService.kt
│   │   ├── RetrofitClient.kt
│   │   └── models/
│   │       ├── LoginRequest.kt
│   │       ├── Order.kt
│   │       └── ...
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   └── OrderRepository.kt
│   └── local/
│       └── PreferencesManager.kt
└── di/
    ├── AppModule.kt
    └── NetworkModule.kt
```

## Шаг 4: Базовые файлы

### 4.1 Application Class
```kotlin
// BestAppClientApplication.kt
@HiltAndroidApp
class BestAppClientApplication : Application()
```

### 4.2 Manifest
```xml
<application
    android:name=".BestAppClientApplication"
    android:usesCleartextTraffic="true"
    ...>
```

### 4.3 Theme
```kotlin
// ui/theme/Color.kt
val PrimaryBlue = Color(0xFF2196F3)
val SecondaryBlue = Color(0xFF03A9F4)

// ui/theme/Theme.kt
@Composable
fun BestAppClientTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlue,
            secondary = SecondaryBlue
        ),
        content = content
    )
}
```

## Шаг 5: API Setup

### 5.1 API Service
```kotlin
interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<Order>
    
    @GET("api/orders")
    suspend fun getOrders(): Response<List<Order>>
}
```

### 5.2 Retrofit Client
```kotlin
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/"
    
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

## Шаг 6: Dependency Injection

### 6.1 Network Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
```

## Шаг 7: Навигация

### 7.1 Navigation Setup
```kotlin
// ui/navigation/NavGraph.kt
@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("create_order") { CreateOrderScreen(navController) }
        composable("orders") { OrdersListScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
    }
}
```

## Шаг 8: Первый запуск

### 8.1 MainActivity
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BestAppClientTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
```

## Шаг 9: Тестирование

### 9.1 Запустить Backend
```bash
cd backend
npm start
```

### 9.2 Запустить приложение
```bash
./gradlew installDebug
```

## 📋 Checklist

- [ ] Создан проект в Android Studio
- [ ] Настроен `build.gradle.kts`
- [ ] Добавлены все зависимости
- [ ] Создана структура пакетов
- [ ] Настроен Hilt
- [ ] Создан ApiService
- [ ] Настроена навигация
- [ ] Запущен Backend
- [ ] Собрано и установлено приложение

## 🎯 Следующие шаги

После базовой настройки:

1. **Реализовать экраны авторизации**
   - LoginScreen
   - RegisterScreen
   - AuthViewModel

2. **Создать главный экран**
   - HomeScreen
   - Список активных заказов

3. **Форма создания заказа**
   - CreateOrderScreen
   - Валидация
   - API интеграция

4. **Список заказов**
   - OrdersListScreen
   - OrderDetailScreen

## 📚 Полезные ссылки

- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Retrofit Guide](https://square.github.io/retrofit/)
- [Material Design 3](https://m3.material.io/)

## 💡 Советы

1. **Используйте Preview** для быстрого просмотра UI
2. **Логируйте API запросы** через OkHttp Interceptor
3. **Тестируйте на эмуляторе** (10.0.2.2 для localhost)
4. **Следуйте Material Design 3** guidelines
5. **Используйте State Hoisting** в Compose

## 🐛 Troubleshooting

### Проблема: Не могу подключиться к Backend
**Решение**: Используйте `http://10.0.2.2:3000/` для эмулятора

### Проблема: Hilt ошибки
**Решение**: Убедитесь, что Application класс аннотирован `@HiltAndroidApp`

### Проблема: Compose не работает
**Решение**: Проверьте версии в `build.gradle.kts`

---

**Удачи в разработке!** 🚀







