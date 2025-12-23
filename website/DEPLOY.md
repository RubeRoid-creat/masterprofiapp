# Инструкция по деплою

## Локальная установка и запуск

1. **Установите зависимости:**
```bash
npm install
```

2. **Настройте переменные окружения:**
Создайте файл `.env`:
```
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_yandex_maps_api_key"
NEXT_PUBLIC_SITE_URL="http://localhost:3000"
```

3. **Настройте базу данных:**
```bash
npx prisma generate
npx prisma migrate dev --name init
```

4. **Запустите проект:**
```bash
npm run dev
```

Сайт будет доступен по адресу: http://localhost:3000

## Деплой на сервер

### Вариант 1: Docker Compose (рекомендуется)

1. **Настройте переменные окружения:**
Создайте файл `.env` на сервере с правильными значениями:
```
DATABASE_URL="postgresql://user:password@db:5432/ispravleno"
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_yandex_maps_api_key"
NEXT_PUBLIC_SITE_URL="https://ispravleno.ru"
NODE_ENV="production"
```

2. **Запустите через Docker Compose:**
```bash
docker-compose up -d
```

3. **Примените миграции базы данных:**
```bash
docker-compose exec app npx prisma migrate deploy
```

### Вариант 2: Прямой деплой

1. **Соберите проект:**
```bash
npm run build
```

2. **Запустите production сервер:**
```bash
npm start
```

3. **Настройте reverse proxy (Nginx):**
```nginx
server {
    listen 80;
    server_name ispravleno.ru;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

## Настройка Яндекс.Карт

1. Получите API ключ на https://developer.tech.yandex.ru/
2. Добавьте ключ в переменную окружения `NEXT_PUBLIC_YANDEX_MAPS_API_KEY`
3. Обновите файл `app/map/page.tsx`, заменив `YOUR_API_KEY` на ваш ключ

## Настройка приложений

1. Поместите файлы приложений в папку `public/apps/`:
   - `master.apk` - приложение мастера для Android
   - `master.ipa` - приложение мастера для iOS
   - `client.apk` - приложение клиента для Android
   - `client.ipa` - приложение клиента для iOS

2. Файлы будут доступны по адресам:
   - `/apps/master.apk`
   - `/apps/master.ipa`
   - `/apps/client.apk`
   - `/apps/client.ipa`

## Интеграция с админ-панелью

Все данные доступны через API endpoints. См. `INTEGRATION.md` для подробностей.

## Обновление сайта

1. Остановите сервер
2. Обновите код из репозитория
3. Установите зависимости: `npm install`
4. Примените миграции: `npx prisma migrate deploy`
5. Пересоберите проект: `npm run build`
6. Запустите сервер: `npm start`

## Резервное копирование

Рекомендуется настроить автоматическое резервное копирование базы данных:
```bash
pg_dump -U postgres ispravleno > backup.sql
```

