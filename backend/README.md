# BestApp Backend

REST API сервер для мобильного приложения BestApp - сервис вызова мастеров по ремонту техники.

## Технологии

- **Node.js** - серверная платформа
- **Express.js** - веб-фреймворк
- **SQLite** - база данных (better-sqlite3)
- **JWT** - аутентификация
- **WebSocket** - real-time уведомления
- **bcryptjs** - хеширование паролей

## Установка и запуск

### 1. Установка зависимостей

```bash
cd backend
npm install
```

### 2. Инициализация базы данных

```bash
npm run init-db
```

Эта команда создаст базу данных SQLite и заполнит её тестовыми данными.

### 3. Запуск сервера

**Режим разработки (с автоперезагрузкой):**
```bash
npm run dev
```

**Обычный режим:**
```bash
npm start
```

Сервер будет доступен по адресу: `http://localhost:3000`

## API Endpoints

### Аутентификация

#### POST `/api/auth/register`
Регистрация нового пользователя

**Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "Иван Иванов",
  "phone": "79991234567",
  "role": "client" // или "master"
}
```

#### POST `/api/auth/login`
Вход в систему

**Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "Иван Иванов",
    "role": "client"
  }
}
```

### Заказы

> Требуется авторизация (Bearer token в заголовке Authorization)

#### GET `/api/orders`
Получить список заказов

**Query параметры:**
- `status` - фильтр по статусу
- `deviceType` - фильтр по типу техники
- `orderType` - фильтр по типу заказа (regular/urgent)

#### GET `/api/orders/:id`
Получить заказ по ID

#### POST `/api/orders`
Создать новый заказ (только для клиентов)

**Body:**
```json
{
  "deviceType": "Стиральная машина",
  "deviceBrand": "Samsung",
  "deviceModel": "WW80R42",
  "problemDescription": "Не сливает воду",
  "address": "Тверь, ул. Советская, д. 34",
  "latitude": 56.859611,
  "longitude": 35.911896,
  "arrivalTime": "14:00 - 16:00",
  "orderType": "urgent"
}
```

#### PUT `/api/orders/:id`
Обновить заказ

#### DELETE `/api/orders/:id`
Отменить заказ

### Мастера

#### GET `/api/masters`
Получить список мастеров

**Query параметры:**
- `specialization` - фильтр по специализации
- `status` - фильтр по статусу
- `isOnShift` - фильтр по смене (true/false)

#### GET `/api/masters/:id`
Получить мастера по ID

#### PUT `/api/masters/profile`
Обновить профиль мастера (только для мастеров)

#### POST `/api/masters/shift/start`
Начать смену (только для мастеров)

**Body:**
```json
{
  "latitude": 56.859611,
  "longitude": 35.911896
}
```

#### POST `/api/masters/shift/end`
Завершить смену (только для мастеров)

#### GET `/api/masters/stats/me`
Получить статистику мастера

### Назначения заказов

#### GET `/api/assignments/my`
Получить мои назначения (только для мастеров)

**Query параметры:**
- `status` - фильтр по статусу (pending/accepted/rejected/expired)

#### GET `/api/assignments/order/:orderId/active`
Получить активное назначение для заказа

#### POST `/api/assignments/:id/accept`
Принять заказ (только для мастеров)

#### POST `/api/assignments/:id/reject`
Отклонить заказ (только для мастеров)

**Body:**
```json
{
  "reason": "Занят другим заказом"
}
```

#### GET `/api/assignments/order/:orderId/history`
Получить историю назначений для заказа

## WebSocket

WebSocket сервер доступен по адресу: `ws://localhost:3000/ws`

### Подключение и аутентификация

1. Подключиться к `ws://localhost:3000/ws`
2. Отправить сообщение с токеном:

```json
{
  "type": "auth",
  "token": "your-jwt-token"
}
```

### Типы событий

#### Мастерам

**new_assignment** - новое назначение заказа
```json
{
  "type": "new_assignment",
  "assignment": {
    "id": 1,
    "order_id": 5,
    "master_id": 2,
    "status": "pending",
    "expires_at": "2025-11-19T16:00:00.000Z",
    "device_type": "Стиральная машина",
    "problem_description": "Не сливает воду",
    "address": "Тверь, ул. Советская, д. 34",
    "estimated_cost": 5000
  }
}
```

## Тестовые данные

После запуска `npm run init-db` в базе будут созданы тестовые пользователи:

### Клиенты
- Email: `ivanov@example.com`, Пароль: `password123`
- Email: `petrova@example.com`, Пароль: `password123`

### Мастера
- Email: `smirnov@example.com`, Пароль: `password123`
- Email: `kuznetsov@example.com`, Пароль: `password123`
- Email: `popov@example.com`, Пароль: `password123`

## Структура проекта

```
backend/
├── config.js              # Конфигурация
├── server.js              # Главный файл сервера
├── database/
│   ├── db.js             # Подключение к БД
│   └── schema.sql        # Схема БД
├── routes/
│   ├── auth.js           # Маршруты аутентификации
│   ├── orders.js         # Маршруты заказов
│   ├── masters.js        # Маршруты мастеров
│   └── assignments.js    # Маршруты назначений
├── services/
│   └── assignment-service.js  # Логика назначения заказов
├── middleware/
│   └── auth.js           # Middleware авторизации
├── scripts/
│   └── init-db.js        # Скрипт инициализации БД
└── websocket.js          # WebSocket сервер
```

## Логика назначения заказов

1. Клиент создает заказ через POST `/api/orders`
2. Система автоматически ищет подходящих мастеров по специализации
3. Заказ назначается первому доступному мастеру (по рейтингу)
4. Мастер получает уведомление через WebSocket
5. У мастера есть 5 минут на ответ (accept/reject)
6. Если мастер принимает - заказ переходит в работу
7. Если мастер отклоняет или время истекает - заказ назначается следующему мастеру
8. Процесс повторяется пока кто-то не примет заказ или не закончатся мастера

## Переменные окружения

Создайте файл `.env` в корне директории backend:

```env
PORT=3000
JWT_SECRET=your-secret-key-change-in-production
DATABASE_PATH=./database.sqlite
NODE_ENV=development
```

## Разработка

### Добавление нового маршрута

1. Создайте файл в папке `routes/`
2. Импортируйте его в `server.js`
3. Подключите через `app.use()`

### Работа с базой данных

Используйте утилиту `query` из `database/db.js`:

```javascript
import { query } from '../database/db.js';

// SELECT
const user = query.get('SELECT * FROM users WHERE id = ?', [userId]);
const users = query.all('SELECT * FROM users');

// INSERT/UPDATE/DELETE
const result = query.run('INSERT INTO users (name, email) VALUES (?, ?)', [name, email]);
const insertedId = result.lastInsertRowid;
```

## Лицензия

MIT







