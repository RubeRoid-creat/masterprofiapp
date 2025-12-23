# Настройка масштабирования (Фаза 6.2)

## Что реализовано

### 1. Кэширование с Redis ✅

- **Сервис кэширования**: `backend/services/cache-service.js`
- **Поддержка Redis**: Опциональная (приложение работает и без Redis)
- **Кэшируемые данные**:
  - Список мастеров (5 минут)
  - Категории услуг (1 час)
  - Шаблоны услуг (30 минут)

### 2. Пагинация ✅

- **Списки с пагинацией**:
  - `/api/masters?limit=20&offset=0`
  - `/api/orders?limit=20&offset=0`
  - `/api/services/templates?limit=20&offset=0`

### 3. Оптимизация запросов ✅

- Добавлены индексы в схему БД
- Оптимизированы запросы с JOIN
- Кэширование частых запросов

## Установка и настройка

### Redis (опционально)

1. **Установка Redis на Windows:**
   ```bash
   # Используйте WSL или Docker
   docker run -d -p 6379:6379 redis:latest
   ```

2. **Настройка переменных окружения:**
   ```env
   REDIS_URL=redis://localhost:6379
   ```

3. **Проверка работы:**
   - Если Redis недоступен, приложение продолжит работу без кэширования
   - В логах будет сообщение: `⚠️ Redis не доступен, работаем без кэширования`

### Использование кэша

#### В коде:

```javascript
import { withCache, CacheKeys, invalidateMastersCache } from '../services/cache-service.js';

// Кэширование результата функции
const masters = await withCache(CacheKeys.masters.all, async () => {
  return query.all('SELECT * FROM masters');
}, 300); // TTL в секундах

// Инвалидация кэша при изменении данных
await invalidateMastersCache();
```

#### API endpoints с кэшированием:

- `GET /api/masters` - список мастеров (кэш 5 мин)
- `GET /api/services/categories` - категории услуг (кэш 1 час)
- `GET /api/services/templates` - шаблоны услуг (кэш 30 мин)

## Пагинация

### Формат запроса:

```
GET /api/orders?limit=20&offset=0
```

### Формат ответа (с пагинацией):

```json
{
  "data": [...],
  "pagination": {
    "limit": 20,
    "offset": 0,
    "count": 20,
    "hasMore": true
  }
}
```

### Формат ответа (без пагинации):

```json
[...]
```

## Миграция на PostgreSQL ✅

### Подготовка

1. **Установка PostgreSQL:**
   ```bash
   # Windows (через Chocolatey)
   choco install postgresql
   
   # Или скачайте с официального сайта
   # https://www.postgresql.org/download/windows/
   ```

2. **Создание базы данных:**
   ```sql
   CREATE DATABASE bestapp;
   CREATE USER bestapp_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE bestapp TO bestapp_user;
   ```

3. **Настройка переменных окружения:**
   ```env
   DATABASE_TYPE=postgresql
   POSTGRES_HOST=localhost
   POSTGRES_PORT=5432
   POSTGRES_DB=bestapp
   POSTGRES_USER=bestapp_user
   POSTGRES_PASSWORD=your_password
   ```

### Миграция данных

1. **Инициализация схемы PostgreSQL:**
   ```bash
   psql -U bestapp_user -d bestapp -f backend/database/postgres-schema.sql
   ```

2. **Запуск миграции данных:**
   ```bash
   npm run migrate-to-postgres
   ```

3. **Проверка миграции:**
   - Проверьте количество записей в таблицах
   - Убедитесь, что все данные перенесены корректно

### Переключение на PostgreSQL

После успешной миграции установите:
```env
DATABASE_TYPE=postgresql
```

Приложение автоматически переключится на PostgreSQL при следующем запуске.

### Обратная совместимость

- По умолчанию используется SQLite (для разработки)
- PostgreSQL включается через переменную окружения `DATABASE_TYPE=postgresql`
- Все запросы работают одинаково через единый интерфейс `query`

## Производительность

### До оптимизации:
- Запрос списка мастеров: ~50-100ms
- Запрос категорий услуг: ~30-50ms

### После оптимизации (с Redis):
- Запрос списка мастеров: ~1-5ms (из кэша)
- Запрос категорий услуг: ~1-3ms (из кэша)

### Рекомендации:
- Используйте пагинацию для больших списков
- Настройте Redis для продакшена
- Мониторьте использование памяти Redis

## Дополнительные оптимизации ✅

### Составные индексы
Добавлены составные индексы для оптимизации частых запросов:
- `idx_orders_client_status` - для фильтрации заказов клиента по статусу
- `idx_orders_master_status` - для фильтрации заказов мастера по статусу
- `idx_orders_status_created` - для сортировки заказов по статусу и дате
- `idx_masters_status_shift` - для поиска доступных мастеров
- `idx_reviews_master_created` - для получения отзывов мастера
- И другие...

### Оптимизация запросов
- Добавлен `LIMIT 1` для запросов, которые возвращают одну запись
- Используются существующие индексы
- Оптимизированы JOIN запросы

### Утилиты для оптимизации
Создан модуль `backend/services/query-optimizer.js` с функциями:
- `buildOptimizedQuery()` - построение оптимизированных запросов
- `buildOrdersQuery()` - оптимизированные запросы для заказов
- `buildMastersQuery()` - оптимизированные запросы для мастеров

