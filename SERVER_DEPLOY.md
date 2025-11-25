# 🚀 Развертывание на сервере - Пошаговая инструкция

## ✅ Шаг 1: Код уже в GitHub

Репозиторий: `https://github.com/RubeRoid-creat/masterprofiapp`

## 📋 Шаг 2: Подключение к серверу

```bash
ssh user@your-server.com
# или
ssh root@your-server-ip
```

## 🔧 Шаг 3: Установка необходимого ПО

### Установка Node.js (если не установлен)

```bash
# Для Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Проверка
node --version
npm --version
```

### Установка PM2 (менеджер процессов)

```bash
sudo npm install -g pm2
```

### Установка Git (если не установлен)

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install git

# CentOS/RHEL
sudo yum install git
```

## 📥 Шаг 4: Клонирование репозитория

```bash
# Перейдите в нужную директорию
cd /home/user
# или
cd /var/www

# Клонируйте репозиторий
git clone https://github.com/RubeRoid-creat/masterprofiapp.git
cd masterprofiapp/backend
```

## ⚙️ Шаг 5: Настройка окружения

```bash
# Установите зависимости
npm install --production

# Создайте файл .env из примера
cp ENV_EXAMPLE.txt .env

# Отредактируйте .env (ОБЯЗАТЕЛЬНО!)
nano .env
```

### Важные настройки в .env:

```env
PORT=3000
NODE_ENV=production
JWT_SECRET=ВАШ_СЛУЧАЙНЫЙ_СЕКРЕТНЫЙ_КЛЮЧ_ИЗМЕНИТЕ_ЭТО
DATABASE_PATH=./database.sqlite
DATABASE_TYPE=sqlite
```

**⚠️ ВАЖНО:** Измените `JWT_SECRET` на случайную строку (минимум 32 символа)!

## 🗄️ Шаг 6: Инициализация базы данных

```bash
# Создайте базу данных и схему
npm run init-db

# Загрузите тестовые данные (клиенты и заказы)
npm run seed
```

## 🚀 Шаг 7: Запуск сервера

### Через PM2 (рекомендуется)

```bash
# Запустите сервер
pm2 start server.js --name bestapp-backend

# Сохраните конфигурацию PM2
pm2 save

# Настройте автозапуск при перезагрузке сервера
pm2 startup
# Выполните команду, которую покажет PM2
```

### Проверка работы

```bash
# Проверьте статус
pm2 status

# Посмотрите логи
pm2 logs bestapp-backend

# Проверьте API
curl http://localhost:3000/api/orders
```

## 🔄 Шаг 8: Обновление (когда нужно обновить код)

```bash
# Перейдите в папку проекта
cd /home/user/masterprofiapp/backend
# или где вы клонировали

# Остановите сервер
pm2 stop bestapp-backend

# Получите обновления
git pull origin main

# Установите новые зависимости (если есть)
npm install --production

# Перезапустите сервер
pm2 restart bestapp-backend

# Проверьте логи
pm2 logs bestapp-backend
```

## 🌐 Шаг 9: Настройка Nginx (опционально, для домена)

Если у вас есть домен и нужно настроить Nginx как reverse proxy:

```bash
# Установите Nginx
sudo apt-get install nginx

# Создайте конфигурацию
sudo nano /etc/nginx/sites-available/bestapp
```

Содержимое файла:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # WebSocket support
    location /ws {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Активируйте конфигурацию:

```bash
sudo ln -s /etc/nginx/sites-available/bestapp /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 🔥 Шаг 10: Настройка Firewall

```bash
# Ubuntu/Debian (UFW)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 3000/tcp  # Backend (если не используете Nginx)
sudo ufw enable

# Или для CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --reload
```

## 📊 Полезные команды PM2

```bash
# Просмотр статуса
pm2 status

# Просмотр логов
pm2 logs bestapp-backend

# Просмотр логов в реальном времени
pm2 logs bestapp-backend --lines 50

# Перезапуск
pm2 restart bestapp-backend

# Остановка
pm2 stop bestapp-backend

# Удаление из PM2
pm2 delete bestapp-backend

# Мониторинг
pm2 monit
```

## 🔐 Безопасность

1. **Измените JWT_SECRET** в `.env`
2. **Не коммитьте** `.env` файл (уже в .gitignore)
3. **Используйте HTTPS** в production (настройте SSL сертификат)
4. **Регулярно обновляйте** зависимости: `npm audit` и `npm update`
5. **Настройте резервное копирование** базы данных

## ✅ Проверка работоспособности

После запуска проверьте:

```bash
# 1. Статус PM2
pm2 status

# 2. Логи сервера
pm2 logs bestapp-backend

# 3. API endpoint
curl http://localhost:3000/api/orders

# 4. WebSocket (если настроен)
curl http://localhost:3000/ws
```

## 🆘 Решение проблем

### Сервер не запускается

```bash
# Проверьте логи
pm2 logs bestapp-backend --err

# Проверьте, занят ли порт
sudo netstat -tulpn | grep 3000

# Проверьте .env файл
cat .env
```

### Ошибки базы данных

```bash
# Пересоздайте базу данных
rm database.sqlite
npm run init-db
npm run seed
```

### Проблемы с зависимостями

```bash
# Удалите node_modules и переустановите
rm -rf node_modules
npm install --production
```

## 📝 Быстрая справка

```bash
# Обновление кода
cd /path/to/masterprofiapp/backend
pm2 stop bestapp-backend
git pull origin main
npm install --production
pm2 restart bestapp-backend

# Просмотр логов
pm2 logs bestapp-backend

# Перезапуск
pm2 restart bestapp-backend
```

---

**Готово!** Ваш backend должен быть доступен по адресу `http://your-server-ip:3000` или через домен, если настроен Nginx.

