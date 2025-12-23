# Настройка проекта без Docker

## Вариант 1: Локальная установка PostgreSQL (рекомендуется)

### Шаг 1: Установить PostgreSQL

**Windows:**
1. Скачать с https://www.postgresql.org/download/windows/
2. Установить с настройками по умолчанию
3. Запомнить пароль для пользователя `postgres`

### Шаг 2: Создать базу данных

Открыть pgAdmin или командную строку PostgreSQL:

```sql
CREATE DATABASE ispravleno;
```

Или через командную строку:
```powershell
psql -U postgres
CREATE DATABASE ispravleno;
\q
```

### Шаг 3: Настроить .env файл

Создать файл `.env` в корне проекта:
```
DATABASE_URL="postgresql://postgres:ВАШ_ПАРОЛЬ@localhost:5432/ispravleno"
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_api_key"
NEXT_PUBLIC_SITE_URL="http://localhost:3000"
```

### Шаг 4: Установить Prisma и применить миграции

```powershell
# Если Prisma еще не установлен
npm install @prisma/client prisma --legacy-peer-deps

# Генерация Prisma клиента
npx prisma generate

# Применить миграции
npx prisma migrate dev --name init
```

### Шаг 5: Запустить проект

```powershell
npm run dev
```

## Вариант 2: Использование SQLite (для тестирования)

Если не хотите устанавливать PostgreSQL, можно использовать SQLite:

### Шаг 1: Изменить схему Prisma

Открыть `prisma/schema.prisma` и изменить:
```prisma
datasource db {
  provider = "sqlite"
  url      = "file:./dev.db"
}
```

### Шаг 2: Применить миграции

```powershell
npx prisma migrate dev --name init
npx prisma generate
```

### Шаг 3: Запустить проект

```powershell
npm run dev
```

**Примечание:** SQLite подходит только для разработки, для продакшена нужен PostgreSQL.

## Вариант 3: Работа без БД (для демонстрации интерфейса)

Если нужно только протестировать интерфейс:

### Шаг 1: Установить зависимости без Prisma

```powershell
.\setup-without-prisma.ps1
```

### Шаг 2: Использовать mock API

Переименовать файлы в `app/api/`:
- `route-mock.ts.example` → `route.ts` (для заказов)

### Шаг 3: Запустить проект

```powershell
npm run dev
```

**Ограничения:** Данные не сохраняются, API возвращает mock-данные.

## Вариант 4: Установить Docker Desktop

Если хотите использовать Docker:

1. Скачать Docker Desktop для Windows: https://www.docker.com/products/docker-desktop
2. Установить и перезапустить компьютер
3. Запустить Docker Desktop
4. Выполнить: `docker-compose up -d`

## Рекомендация

Для быстрого старта используйте **Вариант 2 (SQLite)** - не требует установки дополнительного ПО.

Для продакшена используйте **Вариант 1 (PostgreSQL)**.

