# Инструкция по деплою сайта на сервер

> **Для быстрого старта:** См. [DEPLOY_STEPS.md](./DEPLOY_STEPS.md) - пошаговая инструкция по деплою

## Сервер
- **IP:** 212.74.227.208
- **Порт:** 3003 (3000 - backend API, 3001 - админ-панель)

## Предварительные требования

1. Node.js 18+ установлен на сервере
2. PostgreSQL или SQLite (для разработки)
3. Доступ по SSH к серверу
4. Backend API запущен на `http://212.74.227.208:3000`

## Вариант 1: Деплой через Docker (Рекомендуется)

### 1. Подготовка на сервере

```bash
# Подключитесь к серверу
ssh user@212.74.227.208

# Создайте директорию для проекта
mkdir -p /var/www/ispravleno-website
cd /var/www/ispravleno-website

# Клонируйте репозиторий (или загрузите файлы)
git clone https://github.com/RubeRoid-creat/masterprofiapp.git .
cd website
```

### 2. Настройка переменных окружения

Создайте файл `.env`:

```env
# База данных
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"

# API Backend
NEXT_PUBLIC_ADMIN_API_URL="http://212.74.227.208:3000/api"

# Сайт
NEXT_PUBLIC_SITE_URL="http://212.74.227.208:3003"

# Яндекс.Карты (опционально)
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_yandex_maps_api_key"

# Порт (для production)
PORT=3003
```

### 3. Запуск через Docker

```bash
# Соберите Docker образ
docker build -t ispravleno-website .

# Запустите контейнер
docker run -d \
  --name ispravleno-website \
  -p 3003:3000 \
  --env-file .env \
  --restart unless-stopped \
  ispravleno-website
```

Или используйте docker-compose:

```bash
docker-compose up -d
```

## Вариант 2: Деплой без Docker

### 1. Установка зависимостей

```bash
cd /var/www/ispravleno-website/website
npm install --legacy-peer-deps --production
```

### 2. Настройка переменных окружения

Создайте `.env` файл на основе `env.example`:
```bash
cp env.example .env
# Отредактируйте .env файл с вашими данными
```

### 3. Генерация Prisma клиента

```bash
npx prisma generate
```

### 4. Применение миграций (если используется PostgreSQL)

```bash
npx prisma migrate deploy
```

### 5. Сборка проекта

```bash
npm run build
```

### 6. Запуск в production режиме

#### С помощью PM2 (Рекомендуется)

```bash
# Установите PM2 глобально
npm install -g pm2

# Запустите приложение
pm2 start npm --name "ispravleno-website" -- start

# Сохраните конфигурацию PM2
pm2 save

# Настройте автозапуск при перезагрузке
pm2 startup
```

#### Или напрямую

```bash
NODE_ENV=production PORT=3003 npm start
```

## Вариант 3: Деплой через Systemd

### 1. Создайте service файл

Создайте файл `/etc/systemd/system/ispravleno-website.service`:

```ini
[Unit]
Description=Исправлено Website
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/var/www/ispravleno-website/website
Environment=NODE_ENV=production
Environment=PORT=3003
EnvironmentFile=/var/www/ispravleno-website/website/.env
ExecStart=/usr/bin/node .next/standalone/server.js
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 2. Запустите service

```bash
# Перезагрузите systemd
sudo systemctl daemon-reload

# Запустите service
sudo systemctl start ispravleno-website

# Включите автозапуск
sudo systemctl enable ispravleno-website

# Проверьте статус
sudo systemctl status ispravleno-website
```

## Настройка Nginx (обратный прокси)

### 1. Установите Nginx

```bash
sudo apt update
sudo apt install nginx
```

### 2. Создайте конфигурацию

Создайте файл `/etc/nginx/sites-available/ispravleno-website`:

```nginx
server {
    listen 80;
    server_name 212.74.227.208;

    location / {
        proxy_pass http://localhost:3003;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. Активируйте конфигурацию

```bash
sudo ln -s /etc/nginx/sites-available/ispravleno-website /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Настройка HTTPS (Let's Encrypt)

```bash
# Установите Certbot
sudo apt install certbot python3-certbot-nginx

# Получите сертификат
sudo certbot --nginx -d your-domain.com

# Certbot автоматически настроит автоматическое обновление
```

## Обновление сайта

### Если используете Git:

```bash
cd /var/www/ispravleno-website/website
git pull origin main
npm install --legacy-peer-deps
npm run build

# Перезапустите приложение
pm2 restart ispravleno-website
# или
sudo systemctl restart ispravleno-website
```

### Если используете Docker:

```bash
cd /var/www/ispravleno-website/website
git pull origin main
docker-compose down
docker-compose build
docker-compose up -d
```

## Проверка работоспособности

1. Откройте в браузере: `http://212.74.227.208:3003`
2. Проверьте логи:
   - PM2: `pm2 logs ispravleno-website`
   - Systemd: `sudo journalctl -u ispravleno-website -f`
   - Docker: `docker logs ispravleno-website`

## Важные замечания

1. **База данных:** Убедитесь, что PostgreSQL/Prisma настроен правильно
2. **Порты:** Backend на 3000, Админ-панель на 3001, Website на 3003
3. **Переменные окружения:** Всегда используйте `.env` файл для секретов
4. **Firewall:** Убедитесь, что порты 3003 (и 80/443 для Nginx) открыты

## Troubleshooting

### Ошибка подключения к базе данных
- Проверьте `DATABASE_URL` в `.env`
- Убедитесь, что PostgreSQL запущен
- Проверьте права доступа

### Ошибка подключения к Backend API
- Проверьте, что backend запущен на порту 3000
- Проверьте `NEXT_PUBLIC_ADMIN_API_URL` в `.env`
- Проверьте файрвол

### Ошибки сборки
- Очистите кеш: `rm -rf .next node_modules`
- Переустановите зависимости: `npm install --legacy-peer-deps`
- Проверьте версию Node.js: `node --version` (должна быть 18+)
