# Структура проекта BestAPP

## 📂 Общая структура

```
BestAPP/
├── app/                      # 🔧 Приложение для МАСТЕРОВ
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/bestapp/
│   │       └── res/
│   ├── build.gradle.kts
│   └── AndroidManifest.xml
│
├── ClientApp/                # 👤 Приложение для КЛИЕНТОВ (новое)
│   ├── README.md            # Документация клиентского приложения
│   ├── IMPLEMENTATION_PLAN.md
│   └── DIFFERENCES.md
│
├── backend/                  # 🖥️ Backend API (Node.js)
│   ├── routes/
│   ├── services/
│   ├── database/
│   ├── server.js
│   ├── package.json
│   └── README.md
│
├── build.gradle.kts         # Gradle config (мастер-приложение)
├── settings.gradle.kts
├── PROJECT_STRUCTURE.md     # Этот файл
└── SUMMARY_CLIENT_APP.md    # Сводка по клиентскому приложению
```

## 🎯 Три основных компонента

### 1. BestApp (`app/`) - Приложение для мастеров
**Package**: `com.example.bestapp`

**Что делает:**
- Получает заявки на ремонт
- Принимает/Отклоняет заказы
- Показывает заказы на карте
- Строит маршруты к клиентам
- Управляет сменой (начало/конец)
- Отслеживает статистику и доход

**Технологии:**
- Kotlin
- XML Layouts
- Yandex MapKit
- Retrofit
- Room Database

**Статус:** ✅ Реализовано и работает

---

### 2. ClientApp (`ClientApp/`) - Приложение для клиентов
**Package**: `com.bestapp.client`

**Что будет делать:**
- Создание заявок на ремонт
- Просмотр своих заказов
- Отслеживание статуса
- Просмотр информации о мастере
- Оценка работы мастера
- Управление профилем

**Технологии:**
- Kotlin
- Jetpack Compose (современный UI)
- Retrofit
- Hilt (DI)
- DataStore

**Статус:** 📋 Документация готова, ожидается разработка

---

### 3. Backend (`backend/`) - API Сервер
**URL**: `http://localhost:3000/`

**Что делает:**
- Обрабатывает регистрацию/авторизацию
- Управляет заказами
- Назначает мастеров на заказы
- Отслеживает статусы
- Хранит данные в SQLite
- Отправляет WebSocket уведомления

**Технологии:**
- Node.js + Express
- SQLite (sql.js)
- JWT Authentication
- WebSockets

**Статус:** ✅ Реализовано и работает

## 🔄 Взаимодействие компонентов

```
┌───────────────────────────────────────────────────────┐
│                     Backend API                        │
│              http://localhost:3000                     │
│                                                        │
│  ┌─────────────────────────────────────────────┐     │
│  │  • Авторизация (JWT)                        │     │
│  │  • Заказы                                    │     │
│  │  • Мастера                                   │     │
│  │  • Назначения                                │     │
│  │  • WebSocket (реалтайм)                      │     │
│  └─────────────────────────────────────────────┘     │
└───────────────┬────────────────────┬──────────────────┘
                │                     │
      ┌─────────┴────────┐   ┌───────┴──────────┐
      │                   │   │                   │
      ▼                   ▼   ▼                   ▼
┌─────────────┐     ┌─────────────┐
│  BestApp    │     │ ClientApp   │
│  (Мастера)  │     │ (Клиенты)   │
│             │     │             │
│ • Получить  │     │ • Создать   │
│   заказы    │     │   заказ     │
│ • Принять/  │     │ • Просмотр  │
│   Отклонить │     │   заказов   │
│ • На карте  │     │ • Оценить   │
│ • Маршрут   │     │   мастера   │
└─────────────┘     └─────────────┘
```

## 📱 Сценарии использования

### Сценарий 1: Клиент создаёт заказ

```
1. Клиент (ClientApp):
   - Открывает приложение
   - Создаёт заказ
   - Отправляет на сервер
   
2. Backend:
   - Получает заказ
   - Сохраняет в БД
   - Запускает поиск мастера
   - Создаёт назначение
   
3. Мастер (BestApp):
   - Получает уведомление
   - Видит заказ в ленте
   - Принимает заказ
   
4. Backend:
   - Обновляет статус
   - Уведомляет клиента
   
5. Клиент (ClientApp):
   - Получает уведомление
   - Видит назначенного мастера
```

### Сценарий 2: Мастер выполняет заказ

```
1. Мастер (BestApp):
   - Начинает смену ("На смену")
   - Видит новые заказы
   - Принимает заказ
   - Смотрит детали
   - Строит маршрут
   
2. Backend:
   - Обновляет статусы
   - Уведомляет клиента
   
3. Клиент (ClientApp):
   - Видит статус "Мастер в пути"
   - Видит информацию о мастере
   
4. Мастер (BestApp):
   - Завершает ремонт
   - Закрывает заказ
   
5. Клиент (ClientApp):
   - Получает уведомление
   - Оценивает работу мастера
```

## 🗄️ База данных

### Таблицы (Backend SQLite)

```sql
users                  # Все пользователи (мастера + клиенты)
clients                # Данные клиентов
masters                # Данные мастеров
orders                 # Все заказы
order_assignments      # Назначения заказов мастерам
order_status_history   # История изменения статусов
master_specializations # Специализации мастеров
master_locations       # Локации мастеров
```

## 🔐 Аутентификация

### JWT токены

```javascript
// При входе:
POST /api/auth/login
→ Получаем токен

// Все запросы:
Authorization: Bearer <token>
```

### Роли пользователей

- `client` - Клиент (ClientApp)
- `master` - Мастер (BestApp)
- `admin` - Администратор (будущее)

## 📊 API Endpoints

### Для всех
```
POST /api/auth/login
POST /api/auth/register
```

### Для клиентов (ClientApp)
```
POST /api/orders              # Создать заказ
GET  /api/orders              # Мои заказы
GET  /api/orders/:id          # Детали заказа
POST /api/orders/:id/rate     # Оценить мастера
```

### Для мастеров (BestApp)
```
GET  /api/orders              # Новые заказы
GET  /api/assignments/my      # Мои назначения
POST /api/assignments/:id/accept  # Принять
POST /api/assignments/:id/reject  # Отклонить
POST /api/masters/shift/start     # Начать смену
POST /api/masters/shift/end       # Закончить смену
```

## 🚀 Запуск проекта

### Backend
```bash
cd backend
npm install
npm run init-db    # Инициализация БД
npm start          # Запуск сервера
```

### BestApp (Мастера)
```bash
cd BestAPP
./gradlew assembleDebug
./gradlew installDebug
```

### ClientApp (Клиенты)
```bash
cd ClientApp
# Пока не реализовано
# См. IMPLEMENTATION_PLAN.md
```

## 📱 Тестовые учетные данные

### Мастера (BestApp)
```
Email: smirnov@example.com
Password: password123

Email: kuznetsov@example.com
Password: password123
```

### Клиенты (ClientApp)
```
Email: client1@example.com
Password: password123

Email: client2@example.com
Password: password123
```

## 🎨 Дизайн

### BestApp (Мастера)
- **Цвета**: Зелёные/Тёмно-зелёные
- **Акцент**: Оперативность, быстрый доступ
- **UI**: XML Layouts, Material Design

### ClientApp (Клиенты)
- **Цвета**: Синие/Светло-синие
- **Акцент**: Простота, понятность
- **UI**: Jetpack Compose, Material Design 3

## 📦 Версии и зависимости

### BestApp
```kotlin
Kotlin: 1.9.20
Android SDK: 34
Yandex MapKit: 4.4.0
Retrofit: 2.9.0
```

### Backend
```json
Node.js: 22.x
Express: 4.18.2
sql.js: 1.10.2
ws: 8.16.0
```

### ClientApp (планируется)
```kotlin
Kotlin: 1.9.20
Compose: 1.5.4
Hilt: 2.48
Retrofit: 2.9.0
```

## 📚 Документация

- **Backend API**: `backend/README.md`
- **Android Integration**: `backend/ANDROID_INTEGRATION.md`
- **Client App Plan**: `ClientApp/IMPLEMENTATION_PLAN.md`
- **Differences**: `ClientApp/DIFFERENCES.md`
- **Summary**: `SUMMARY_CLIENT_APP.md`

## ✅ Текущий статус

| Компонент | Статус | Готовность |
|-----------|--------|------------|
| Backend API | ✅ Работает | 100% |
| BestApp (Мастера) | ✅ Работает | 95% |
| ClientApp (Клиенты) | 📋 Планируется | 0% |

## 🔮 Roadmap

### Ближайшие задачи:
1. ✅ Backend API
2. ✅ BestApp (мастера)
3. 📋 ClientApp (клиенты) - в разработке
4. ⏳ WebSocket notifications
5. ⏳ Admin panel
6. ⏳ Analytics dashboard

### Долгосрочные планы:
- Интеграция платежей
- Чат между клиентом и мастером
- Отслеживание мастера на карте (real-time)
- Система лояльности
- Мобильное приложение для iOS

---

**Автор**: Development Team  
**Последнее обновление**: 19 ноября 2025  
**Версия**: 1.0







