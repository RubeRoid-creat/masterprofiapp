# Интеграция веб-сайта с системой "Исправлено"

## Обзор

Веб-сайт полностью интегрирован с основной системой:
- ✅ Использует общую PostgreSQL базу данных
- ✅ Заказы создаются напрямую в backend API
- ✅ Автоматическое создание пользователей и клиентов
- ✅ Синхронизация данных через общую БД

## Архитектура интеграции

```
Веб-сайт (Next.js)
    ↓
POST /api/orders (сайт)
    ↓
lib/admin-api.ts
    ↓
POST /api/orders/from-website (backend)
    ↓
PostgreSQL (общая БД)
    ↓
Админ-панель, Приложения мастера/клиента
```

## Настройка

### 1. Переменные окружения

Создайте файл `.env` в папке `website/`:

```env
# Подключение к PostgreSQL (та же БД, что использует backend)
DATABASE_URL="postgresql://user:password@localhost:5432/bestapp"

# URL backend API
NEXT_PUBLIC_ADMIN_API_URL="http://212.74.227.208:3000/api"

# URL сайта
NEXT_PUBLIC_SITE_URL="http://localhost:3000"

# Яндекс.Карты API (опционально)
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_api_key"
```

### 2. Применение миграций базы данных

**Вариант 1: Через PowerShell скрипт (рекомендуется)**

```powershell
cd website
.\apply-migrations.ps1
```

**Вариант 2: Через npm скрипт**

```bash
cd website
npm run migrate
```

**Вариант 3: Через Node.js напрямую**

```bash
cd website
node scripts/apply-migrations.js
```

**Вариант 4: Через psql (если установлен PostgreSQL)**

```bash
psql -U postgres -d bestapp -f website/prisma/migrations/website_tables.sql
```

### 3. Генерация Prisma клиента

```bash
cd website
npx prisma generate
```

### 4. Запуск сайта

```bash
cd website
npm install
npm run dev
```

Сайт будет доступен по адресу: http://localhost:3000

## Функционал интеграции

### Создание заказов

При создании заказа на сайте:

1. **Автоматическое создание пользователя**
   - Если пользователь с таким email не существует, создается новый пользователь с ролью `client`
   - Генерируется случайный пароль (пользователь может восстановить доступ через email)

2. **Автоматическое создание клиента**
   - Если у пользователя нет записи в таблице `clients`, она создается автоматически

3. **Создание заказа**
   - Заказ создается в основной БД с `order_source = 'website'`
   - Автоматически запускается процесс назначения мастеров
   - Заказ виден в админ-панели и приложениях

### Таблицы сайта

Сайт использует следующие таблицы в общей PostgreSQL БД:

- `news` - Новости
- `prices` - Прайс услуг и запчастей
- `forum_topics` - Темы форума
- `forum_replies` - Ответы на форуме
- `contact_messages` - Сообщения обратной связи

Эти таблицы не конфликтуют с основной БД и используются только сайтом.

## API Endpoints

### Создание заказа с сайта

**Endpoint:** `POST /api/orders/from-website` (backend)

**Не требует авторизации** - автоматически создает пользователя и клиента

**Формат запроса:**
```json
{
  "name": "Иван Иванов",
  "phone": "+79001234567",
  "email": "ivan@example.com",
  "device_type": "Холодильник",
  "device_brand": "Samsung",
  "problem_description": "Не работает",
  "address": "Москва, ул. Примерная, д. 1",
  "desired_repair_date": "2024-01-15",
  "arrival_time": "14:00"
}
```

**Ответ:**
```json
{
  "message": "Заказ успешно создан",
  "order": {
    "id": 123,
    "order_number": "#0123-КЛ",
    ...
  }
}
```

## Проверка интеграции

### 1. Проверка создания заказа

1. Откройте сайт: http://localhost:3000/order
2. Заполните форму заказа
3. Отправьте заказ
4. Проверьте в админ-панели: http://212.74.227.208:3000/admin/orders
5. Заказ должен появиться с `order_source = 'website'`

### 2. Проверка создания пользователя

1. После создания заказа проверьте в БД:
```sql
SELECT * FROM users WHERE email = 'email_из_заказа';
SELECT * FROM clients WHERE user_id = (SELECT id FROM users WHERE email = 'email_из_заказа');
```

### 3. Проверка таблиц сайта

```sql
-- Проверка таблиц
SELECT * FROM news LIMIT 5;
SELECT * FROM prices LIMIT 5;
SELECT * FROM forum_topics LIMIT 5;
```

## Устранение неполадок

### Ошибка подключения к БД

**Проблема:** `Error: P1001: Can't reach database server`

**Решение:**
1. Проверьте, что PostgreSQL запущен
2. Проверьте правильность `DATABASE_URL` в `.env`
3. Убедитесь, что БД существует и доступна

### Ошибка создания заказа

**Проблема:** `Failed to create order in backend`

**Решение:**
1. Проверьте доступность backend API: `http://212.74.227.208:3000/api`
2. Проверьте логи backend сервера
3. Убедитесь, что endpoint `/api/orders/from-website` доступен

### Таблицы не найдены

**Проблема:** `relation "news" does not exist`

**Решение:**
1. Выполните миграцию: `\i website/prisma/migrations/website_tables.sql`
2. Или через Prisma: `npx prisma migrate dev`

## Дальнейшее развитие

### Планируемые улучшения

1. **Синхронизация новостей и прайса**
   - Управление через админ-панель
   - Автоматическая синхронизация с сайтом

2. **Геокодинг адресов**
   - Интеграция с Яндекс.Геокодер API
   - Автоматическое определение координат

3. **Уведомления**
   - Email уведомления о создании заказа
   - SMS уведомления (опционально)

4. **Восстановление доступа**
   - Пользователи с сайта могут восстановить пароль через email
   - Ссылка на восстановление в email после создания заказа

## Поддержка

При возникновении проблем:
1. Проверьте логи сайта: `npm run dev` (консоль)
2. Проверьте логи backend: `pm2 logs bestapp-backend`
3. Проверьте подключение к БД: `psql -U postgres -d bestapp`
