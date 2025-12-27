# Исправлено - Сайт сервиса по ремонту бытовой техники

## Описание

Сайт для сервисного центра по ремонту бытовой техники "Исправлено". Включает все необходимые страницы, SEO-оптимизацию, интеграцию с приложениями мастера и клиента.

## Технологии

- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- PostgreSQL
- Prisma ORM
- Яндекс.Карты API

## Быстрый старт

### Если возникают проблемы с установкой Prisma

**Проблема с `npx prisma generate` (ECONNRESET)?**

Используйте скрипты:
- `fix-prisma-simple.ps1` - быстрое решение
- `fix-prisma.ps1` - полная версия

Или используйте Docker:
```powershell
docker-compose up -d
```

### Стандартная установка

1. Установите зависимости:
```bash
npm install --legacy-peer-deps
```

Если возникают ошибки, используйте скрипт:
```powershell
.\install.ps1
```

2. Настройте переменные окружения:
Создайте файл `.env`:
```
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_yandex_maps_api_key"
NEXT_PUBLIC_SITE_URL="http://localhost:3000"
```

3. Настройте базу данных:
```bash
npx prisma generate
npx prisma migrate dev --name init
```

4. Запустите проект:
```bash
npm run dev
```

Сайт будет доступен по адресу: http://localhost:3000

Подробные инструкции:
- `DEPLOY.md` - деплой на сервер
- `INSTALL.md` - решение проблем с установкой
- `INTEGRATION.md` - интеграция с админ-панелью

## Структура проекта

- `/app` - страницы приложения
- `/components` - переиспользуемые компоненты
- `/prisma` - схема базы данных
- `/app/api` - API endpoints

## Страницы

- `/` - Главная (о нас, новости)
- `/order` - Создание заказа
- `/price` - Прайс услуг и запчастей
- `/forum` - Форум
- `/map` - Карта услуг и магазинов
- `/apps` - Загрузка приложений
- `/contacts` - Контакты и обратная связь

## API Endpoints

- `POST /api/orders` - Создание заказа
- `GET /api/prices` - Получение прайса
- `GET /api/forum/topics` - Получение тем форума
- `POST /api/forum/topics` - Создание темы
- `POST /api/contacts` - Отправка сообщения обратной связи
- `GET /api/news` - Получение новостей

## SEO

- Мета-теги на всех страницах
- Schema.org разметка
- Sitemap.xml
- Robots.txt
- Оптимизация для поисковых систем

## Деплой

Проект готов к деплою на сервер. Используйте:

```bash
npm run build
npm start
```

## Интеграция с системой

✅ **Полная интеграция с основной системой "Исправлено"**

- Заказы создаются напрямую в backend API
- Автоматическое создание пользователей и клиентов
- Использование общей PostgreSQL базы данных
- Синхронизация с админ-панелью и приложениями

**Подробная документация:** См. раздел "Интеграция с системой" выше

### Быстрая настройка

1. Настройте `.env`:
```env
DATABASE_URL="postgresql://user:password@localhost:5432/bestapp"
NEXT_PUBLIC_ADMIN_API_URL="http://212.74.227.208:3000/api"
```

2. Примените миграции:
```bash
psql -U postgres -d bestapp -f prisma/migrations/website_tables.sql
npx prisma generate
```

3. Запустите сайт:
```bash
npm run dev
```

