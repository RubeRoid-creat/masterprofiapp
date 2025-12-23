# Быстрое применение миграций

## ⚡ Самый простой способ

```powershell
cd website
.\apply-migrations.ps1
```

Или через npm:

```bash
cd website
npm run migrate
```

## Альтернативные способы

### Через Node.js напрямую

```bash
cd website
node scripts/apply-migrations.js
```

### Через psql (если PostgreSQL установлен локально)

```bash
psql -U postgres -d bestapp -f prisma/migrations/website_tables.sql
```

## Требования

1. ✅ Настроен `.env` файл с `DATABASE_URL`
2. ✅ Установлены зависимости: `npm install`
3. ✅ Сгенерирован Prisma клиент: `npx prisma generate`

## Что делает скрипт

- Создает таблицы: `news`, `prices`, `forum_topics`, `forum_replies`, `contact_messages`
- Создает индексы для оптимизации
- Создает триггеры для автоматического обновления `updated_at`
- Проверяет, что все таблицы созданы успешно

## Если возникли ошибки

### Ошибка: "Cannot find module '@prisma/client'"
```bash
npm install
npx prisma generate
```

### Ошибка: "Can't reach database server"
Проверьте `DATABASE_URL` в `.env` файле:
```env
DATABASE_URL="postgresql://user:password@host:5432/bestapp"
```

### Ошибка: "relation already exists"
Это нормально - таблицы уже существуют. Скрипт пропустит их создание.
