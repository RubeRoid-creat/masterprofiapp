# План реализации клиентского приложения

## Фаза 1: Базовая структура ✅
- [x] Создать папку проекта
- [ ] Настроить Gradle конфигурацию
- [ ] Добавить зависимости (Retrofit, Compose, Hilt)
- [ ] Создать базовую структуру пакетов

## Фаза 2: Авторизация
- [ ] Экран входа (LoginScreen)
- [ ] Экран регистрации (RegisterScreen)
- [ ] AuthViewModel для управления состоянием
- [ ] API интеграция авторизации
- [ ] Сохранение токена (DataStore)

## Фаза 3: Главный экран
- [ ] HomeScreen с активными заказами
- [ ] Кнопка "Создать заказ"
- [ ] Список последних заказов
- [ ] Bottom Navigation

## Фаза 4: Создание заказа
- [ ] OrderCreateScreen с формой
- [ ] Выбор типа техники
- [ ] Описание проблемы
- [ ] Выбор адреса
- [ ] Выбор времени
- [ ] Валидация и отправка

## Фаза 5: Список заказов
- [ ] OrdersListScreen
- [ ] Фильтрация по статусу
- [ ] Детали заказа (OrderDetailScreen)
- [ ] Информация о мастере

## Фаза 6: Профиль
- [ ] ProfileScreen
- [ ] Редактирование данных
- [ ] История заказов
- [ ] Настройки уведомлений

## Фаза 7: Дополнительные функции
- [ ] Push-уведомления
- [ ] Рейтинг мастера
- [ ] Поддержка чата
- [ ] Отслеживание мастера на карте

## Технический стек

### Архитектура
- MVVM (Model-View-ViewModel)
- Clean Architecture principles
- Repository pattern

### Библиотеки
```kotlin
// UI
implementation("androidx.compose.ui:ui:1.5.4")
implementation("androidx.compose.material3:material3:1.1.2")
implementation("androidx.navigation:navigation-compose:2.7.5")

// Network
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// DI
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// Async
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

## Экраны приложения

### 1. Splash Screen
- Проверка авторизации
- Загрузка конфигурации

### 2. Auth Flow
- Login
- Register
- Forgot Password

### 3. Main Flow
- Home (Dashboard)
- Create Order
- Orders List
- Order Details
- Profile
- Settings

## API Интеграция

### Модели данных
```kotlin
data class CreateOrderRequest(
    val deviceType: String,
    val deviceBrand: String?,
    val deviceModel: String?,
    val problemDescription: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val arrivalTime: String?,
    val orderType: String = "regular"
)

data class Order(
    val id: Long,
    val clientId: Long,
    val deviceType: String,
    val status: String,
    val address: String,
    val masterName: String?,
    val masterPhone: String?,
    val createdAt: String,
    val estimatedCost: Double?
)
```

## UI/UX принципы

1. **Простота** - минимум шагов для создания заказа
2. **Наглядность** - чёткие статусы и информация
3. **Отзывчивость** - быстрая обратная связь
4. **Доступность** - поддержка accessibility

## Тестирование

### Unit тесты
- ViewModels
- Repository
- Use Cases

### UI тесты
- Навигация
- Создание заказа
- Авторизация

### Integration тесты
- API взаимодействие
- Локальное хранилище

## Deployment

1. **Internal Testing** - тестирование командой
2. **Alpha** - закрытое тестирование
3. **Beta** - открытое тестирование
4. **Production** - публикация в Google Play

## Приоритеты для MVP

### Must Have (P0)
- ✅ Регистрация/Вход
- ✅ Создание заказа
- ✅ Просмотр списка заказов
- ✅ Просмотр деталей заказа

### Should Have (P1)
- Уведомления о статусе
- Информация о мастере
- Рейтинг мастера

### Nice to Have (P2)
- Чат с мастером
- Отслеживание на карте
- История платежей
- Повторный заказ

## Timeline (оценка)

- **Неделя 1-2**: Базовая структура + Авторизация
- **Неделя 3**: Главный экран + Навигация
- **Неделя 4**: Создание заказа
- **Неделя 5**: Список заказов + Детали
- **Неделя 6**: Профиль + Полировка
- **Неделя 7**: Тестирование + Баг-фиксы
- **Неделя 8**: Release подготовка

**Total: ~2 месяца до MVP**







