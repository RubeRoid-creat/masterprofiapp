# Инструкция по применению миграций для веб-сайта

## Проблема с Prisma CDN
Если у вас проблемы с доступом к `binaries.prismacdn.com` (DNS блокировка), используйте прямое применение SQL миграций.

## Шаги для настройки:

### 1. Настройте файл .env
Создайте файл `.env` в корне папки `/website` со следующим содержимым:

```env
DATABASE_URL="postgresql://USERNAME:PASSWORD@HOST:PORT/DATABASE"
```

**Примеры:**
- Локальная БД: `DATABASE_URL="postgresql://postgres:password@localhost:5432/bestapp"`
- Удаленная БД: `DATABASE_URL="postgresql://user:pass@212.74.227.208:5432/bestapp"`

### 2. Установите зависимости
```powershell
npm install
```

### 3. Примените миграции

#### Вариант A: Прямое применение SQL (рекомендуется при проблемах с Prisma)
```powershell
node scripts/apply-migrations-direct.js
```

#### Вариант B: Через Prisma (требует доступ к binaries.prismacdn.com)
```powershell
npx prisma generate
npx prisma migrate deploy
```

### 4. Проверьте таблицы
После успешного применения миграций будут созданы следующие таблицы:
- `news` - новости сайта
- `prices` - прайс-лист услуг и запчастей
- `forum_topics` - темы форума
- `forum_replies` - ответы в форуме
- `contact_messages` - сообщения из формы обратной связи

## Решение проблем

### Ошибка: "getaddrinfo ENOTFOUND binaries.prismacdn.com"
**Причина:** DNS не может разрешить адрес Prisma CDN (возможно блокировка провайдером)  
**Решение:** Используйте прямое применение SQL миграций (Вариант A)

### Ошибка: "пользователь не может быть аутентифицирован"
**Причина:** Неверные данные подключения в DATABASE_URL  
**Решение:** Проверьте правильность username, password, host, port и database name

### Ошибка: "database does not exist"
**Причина:** База данных еще не создана  
**Решение:** Создайте базу данных вручную:
```sql
CREATE DATABASE bestapp;
```

## Дополнительные команды

### Откат миграций (удаление всех таблиц)
```sql
DROP TABLE IF EXISTS forum_replies CASCADE;
DROP TABLE IF EXISTS forum_topics CASCADE;
DROP TABLE IF EXISTS contact_messages CASCADE;
DROP TABLE IF EXISTS prices CASCADE;
DROP TABLE IF EXISTS news CASCADE;
```

### Проверка применен ных таблиц
```sql
\dt  -- В psql консоли
```

или через Node.js:
```javascript
const { Client } = require('pg');
const client = new Client({connectionString: process.env.DATABASE_URL});
await client.connect();
const res = await client.query("SELECT tablename FROM pg_tables WHERE schemaname='public'");
console.log(res.rows);
```

