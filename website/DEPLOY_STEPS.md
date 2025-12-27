# Пошаговая инструкция по деплою сайта на сервер

## Подготовка на сервере

### Шаг 1: Подключение к серверу

```bash
ssh user@212.74.227.208
# Замените user на ваше имя пользователя
```

### Шаг 2: Создание директории и клонирование репозитория

```bash
# Создайте директорию для проекта
sudo mkdir -p /var/www/ispravleno-website
sudo chown $USER:$USER /var/www/ispravleno-website

# Перейдите в директорию
cd /var/www/ispravleno-website

# Клонируйте репозиторий (если еще не клонирован)
git clone https://github.com/RubeRoid-creat/Ispravleno.git .

# Перейдите в директорию website
cd website
```

### Шаг 3: Настройка переменных окружения

```bash
# Скопируйте пример файла переменных окружения
cp env.example .env

# Откройте файл для редактирования
nano .env
```

Обновите следующие значения в файле `.env`:

```env
# База данных (используйте ту же, что и backend)
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"

# Backend API URL
NEXT_PUBLIC_ADMIN_API_URL="http://212.74.227.208:3000/api"

# Сайт URL (важно: порт 3003!)
NEXT_PUBLIC_SITE_URL="http://212.74.227.208:3003"

# Порт для запуска (важно: 3003!)
PORT=3003

# Node Environment
NODE_ENV=production
```

Сохраните файл (в nano: `Ctrl+O`, `Enter`, `Ctrl+X`)

### Шаг 4: Установка зависимостей

```bash
# Установите Node.js 18+ если еще не установлен
# Проверьте версию: node --version

# Установите зависимости
npm install --legacy-peer-deps

# Генерируйте Prisma клиент
npx prisma generate
```

### Шаг 5: Сборка проекта

```bash
# Соберите production версию
npm run build
```

### Шаг 6: Запуск через PM2 (рекомендуется)

```bash
# Установите PM2 глобально (если еще не установлен)
sudo npm install -g pm2

# Создайте директорию для логов
mkdir -p logs

# Запустите приложение используя ecosystem.config.js (рекомендуется)
pm2 start ecosystem.config.js

# ИЛИ запустите напрямую (если ecosystem.config.js не используется)
PORT=3003 pm2 start npm --name "ispravleno-website" -- run start:standalone

# Сохраните конфигурацию PM2
pm2 save

# Настройте автозапуск при перезагрузке сервера
pm2 startup
# Выполните команду, которую выдаст PM2 (обычно что-то вроде: sudo env PATH=... pm2 startup systemd -u user --hp /home/user)

# Проверьте статус
pm2 list
pm2 logs ispravleno-website
```

### Шаг 7: Настройка файрвола (если нужно)

```bash
# Откройте порт 3003 в файрволе (если используется ufw)
sudo ufw allow 3003/tcp

# Или для firewalld
sudo firewall-cmd --permanent --add-port=3003/tcp
sudo firewall-cmd --reload
```

## Проверка работоспособности

1. Откройте в браузере: `http://212.74.227.208:3003`
2. Проверьте логи: `pm2 logs ispravleno-website`
3. Проверьте статус: `pm2 status`

## Обновление сайта (после изменений в коде)

Если нужно обновить сайт после изменений:

```bash
# Перейдите в директорию проекта
cd /var/www/ispravleno-website/website

# Получите последние изменения из репозитория
git pull origin main

# Переустановите зависимости (если изменились)
npm install --legacy-peer-deps

# Перегенерируйте Prisma клиент (если изменилась схема)
npx prisma generate

# Пересоберите проект
npm run build

# Перезапустите приложение
pm2 restart ispravleno-website

# Или если используете ecosystem.config.js:
# pm2 restart ecosystem.config.js

# Проверьте логи
pm2 logs ispravleno-website
```

## Альтернатива: Запуск через systemd

Если предпочитаете systemd вместо PM2:

### 1. Создайте service файл

```bash
sudo nano /etc/systemd/system/ispravleno-website.service
```

Содержимое файла:

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
StandardOutput=journal
StandardError=journal

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

# Просмотр логов
sudo journalctl -u ispravleno-website -f
```

## Устранение проблем

### Ошибка подключения к базе данных

```bash
# Проверьте DATABASE_URL в .env
cat .env | grep DATABASE_URL

# Проверьте доступность PostgreSQL
sudo systemctl status postgresql
```

### Ошибка порта уже используется

```bash
# Проверьте, что занимает порт 3003
sudo lsof -i :3003

# Если PM2 процесс, остановите его
pm2 stop ispravleno-website
pm2 delete ispravleno-website

# Запустите заново с правильной командой
PORT=3003 pm2 start npm --name "ispravleno-website" -- run start:standalone

# Или используйте ecosystem.config.js
pm2 start ecosystem.config.js
```

### Ошибка "next start does not work with output: standalone"

Если видите это предупреждение, используйте правильную команду запуска:

```bash
# Остановите текущий процесс
pm2 stop ispravleno-website
pm2 delete ispravleno-website

# Запустите с правильной командой для standalone режима
PORT=3003 pm2 start npm --name "ispravleno-website" -- run start:standalone

# Или используйте ecosystem.config.js (рекомендуется)
pm2 start ecosystem.config.js
```

### Ошибка при сборке

```bash
# Очистите кеш и пересоберите
rm -rf .next node_modules
npm install --legacy-peer-deps
npm run build
```

### Проверка логов

```bash
# PM2 логи
pm2 logs ispravleno-website

# Systemd логи
sudo journalctl -u ispravleno-website -n 50 -f

# Логи Next.js (если запущено напрямую)
npm start
```

## Настройка Nginx (опционально, для использования домена)

Если хотите использовать домен вместо IP:порт:

### 1. Установите Nginx

```bash
sudo apt update
sudo apt install nginx
```

### 2. Создайте конфигурацию

```bash
sudo nano /etc/nginx/sites-available/ispravleno-website
```

Содержимое:

```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;

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
