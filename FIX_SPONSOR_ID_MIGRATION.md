# Исправление ошибки БД: отсутствует колонка sponsor_id

## Проблема

При запуске сервера возникает ошибка:
```
Error: no such column: sponsor_id
Ошибка инициализации БД: Error: no such column: sponsor_id
```

Это происходит потому, что:
1. База данных была создана до того, как была добавлена колонка `sponsor_id` в таблицу `users`
2. При попытке создать индекс `idx_users_sponsor_id` возникает ошибка, так как колонки не существует

## Решение

Добавлено автоматическое добавление колонки `sponsor_id` при:
1. Инициализации базы данных (в `db.js`)
2. Запуске сервера (в `server.js`)

## Автоматическое исправление

Сервер автоматически проверит и добавит колонку `sponsor_id` при следующем запуске.

## Ручное исправление (если нужно)

### Вариант 1: Запустить скрипт миграции

```bash
cd backend
npm run ensure-sponsor-id
```

### Вариант 2: Выполнить SQL напрямую

Подключитесь к базе данных и выполните:

```sql
-- Проверяем, существует ли колонка
PRAGMA table_info(users);

-- Если колонки нет, добавляем её
ALTER TABLE users ADD COLUMN sponsor_id INTEGER;

-- Создаем индекс
CREATE INDEX IF NOT EXISTS idx_users_sponsor_id ON users(sponsor_id);
```

### Вариант 3: Через Node.js скрипт

```bash
cd backend
node scripts/ensure-sponsor-id-column.js
```

## Проверка после исправления

После исправления проверьте:

1. **Проверьте структуру таблицы:**
   ```sql
   PRAGMA table_info(users);
   ```
   Должна быть колонка `sponsor_id INTEGER`

2. **Проверьте индексы:**
   ```sql
   PRAGMA index_list(users);
   ```
   Должен быть индекс `idx_users_sponsor_id`

3. **Перезапустите сервер:**
   ```bash
   pm2 restart bestapp-backend
   # или
   npm start
   ```

## Что было изменено:

1. **backend/database/db.js** - добавлена автоматическая обработка ошибки "no such column: sponsor_id"
2. **backend/server.js** - добавлена проверка и автоматическое добавление колонки при старте
3. **backend/scripts/ensure-sponsor-id-column.js** - создан скрипт для ручного добавления колонки
4. **backend/package.json** - добавлен npm скрипт `ensure-sponsor-id`

## После исправления:

Сервер должен запуститься без ошибок, и приложение сможет подключиться к серверу.

---

**Важно:** После исправления перезапустите сервер:
```bash
pm2 restart bestapp-backend
```




