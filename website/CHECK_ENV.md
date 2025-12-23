# Проверка настроек .env

## Важно для Next.js

В Next.js переменные окружения работают так:
- **С префиксом `NEXT_PUBLIC_`** - доступны на клиенте и сервере
- **Без префикса** - доступны ТОЛЬКО на сервере

## Правильная настройка .env

```env
# Database
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"

# Site
NEXT_PUBLIC_SITE_URL="http://localhost:3000"

# Яндекс.Карты
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_key"

# Интеграция с админ-панелью
# ВАЖНО: БЕЗ префикса NEXT_PUBLIC_, так как используется только на сервере
NEXT_PUBLIC_ADMIN_API_URL="http://212.74.227.208:3000/api"
ADMIN_USER_TOKEN="ваш_токен_здесь"
```

## Проверка

### 1. Убедитесь, что файл называется `.env`

Не `.env.local`, не `.env.development`, а именно `.env`

### 2. Проверьте формат

```env
ADMIN_USER_TOKEN=ваш_токен
```

или

```env
ADMIN_USER_TOKEN="ваш_токен"
```

Оба варианта работают.

### 3. Перезапустите сервер

После изменения `.env` **обязательно** перезапустите сервер:
1. Остановите (Ctrl+C)
2. Запустите снова: `npm run dev`

### 4. Проверьте логи

При создании заказа в логах должно быть:
```
Token check: { hasToken: true, tokenLength: XXX, ... }
Sending order to admin panel: { ... }
```

Если `hasToken: false` - токен не читается из .env

## Отладка

Добавьте временно в `lib/admin-api.ts` для проверки:

```typescript
console.log('All env vars:', {
  ADMIN_USER_TOKEN: process.env.ADMIN_USER_TOKEN ? 'SET' : 'NOT SET',
  NEXT_PUBLIC_ADMIN_API_URL: process.env.NEXT_PUBLIC_ADMIN_API_URL,
});
```

Это поможет увидеть, читается ли переменная.

