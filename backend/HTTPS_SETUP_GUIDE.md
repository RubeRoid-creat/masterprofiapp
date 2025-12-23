# 🔒 НАСТРОЙКА HTTPS СЕРТИФИКАТА

## 📋 ВВЕДЕНИЕ

HTTPS критически важен для безопасности приложения:
- 🔐 Шифрование данных между клиентом и сервером
- ✅ Защита от перехвата данных (Man-in-the-Middle атаки)
- 🛡️ Доверие пользователей (браузеры показывают предупреждение для HTTP)
- 📱 Требование для многих API (FCM Push, Payment systems)

---

## 🎯 ВАРИАНТЫ НАСТРОЙКИ HTTPS

### Вариант 1: Let's Encrypt + Certbot (Рекомендуется)
**Бесплатно, автоматическое обновление, проще всего**

### Вариант 2: Nginx Reverse Proxy + Let's Encrypt
**Более гибко, лучше для production**

### Вариант 3: Cloudflare (Самый простой)
**Бесплатный SSL, но с прокси**

---

## 🚀 ВАРИАНТ 1: CERTBOT (ПРЯМОЙ SSL)

### Шаг 1: Установка Certbot

**Debian/Ubuntu:**
```bash
sudo apt update
sudo apt install certbot
```

**CentOS/RHEL:**
```bash
sudo yum install certbot
```

### Шаг 2: Остановка сервера на порту 80

Certbot нужен порт 80 для валидации:

```bash
# Если используете PM2
pm2 stop masterprofi-backend

# Или найдите процесс на порту 80
sudo lsof -i :80
sudo kill -9 <PID>
```

### Шаг 3: Получение сертификата

```bash
sudo certbot certonly --standalone \
  -d masterprofi.ru \
  -d www.masterprofi.ru \
  --email your-email@example.com \
  --agree-tos \
  --non-interactive
```

Сертификаты будут сохранены в:
```
/etc/letsencrypt/live/masterprofi.ru/fullchain.pem
/etc/letsencrypt/live/masterprofi.ru/privkey.pem
```

### Шаг 4: Настройка Node.js сервера для HTTPS

Создайте файл `backend/server-https.js`:

```javascript
import express from 'express';
import https from 'https';
import http from 'http';
import fs from 'fs';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Импортируйте остальной код из server.js
// или переименуйте server.js

const app = express();

// ... весь ваш код middleware и routes ...

// HTTP сервер (редирект на HTTPS)
const httpApp = express();
httpApp.use((req, res) => {
  res.redirect(301, `https://${req.headers.host}${req.url}`);
});

// Запуск HTTP сервера (порт 80)
http.createServer(httpApp).listen(80, () => {
  console.log('🔓 HTTP сервер запущен на порту 80 (редирект на HTTPS)');
});

// HTTPS сервер
const httpsOptions = {
  key: fs.readFileSync('/etc/letsencrypt/live/masterprofi.ru/privkey.pem'),
  cert: fs.readFileSync('/etc/letsencrypt/live/masterprofi.ru/fullchain.pem')
};

https.createServer(httpsOptions, app).listen(443, () => {
  console.log('🔒 HTTPS сервер запущен на порту 443');
});
```

### Шаг 5: Дать Node.js доступ к портам 80 и 443

```bash
# Вариант 1: Использовать setcap (рекомендуется)
sudo setcap 'cap_net_bind_service=+ep' $(which node)

# Вариант 2: Запускать сервер через sudo (не рекомендуется)
sudo pm2 start server-https.js --name masterprofi-backend
```

### Шаг 6: Автоматическое обновление сертификатов

```bash
# Добавить в crontab
sudo crontab -e

# Добавить строку (обновление каждый день в 3:00)
0 3 * * * certbot renew --quiet --post-hook "pm2 restart masterprofi-backend"
```

---

## 🌐 ВАРИАНТ 2: NGINX REVERSE PROXY (Рекомендуется для Production)

### Шаг 1: Установка Nginx

```bash
sudo apt update
sudo apt install nginx
```

### Шаг 2: Установка Certbot для Nginx

```bash
sudo apt install python3-certbot-nginx
```

### Шаг 3: Настройка Nginx

Создайте файл `/etc/nginx/sites-available/masterprofi`:

```nginx
# HTTP → HTTPS редирект
server {
    listen 80;
    listen [::]:80;
    server_name masterprofi.ru www.masterprofi.ru 212.74.227.208;
    
    # Let's Encrypt валидация
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    # Редирект на HTTPS
    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS сервер
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name masterprofi.ru www.masterprofi.ru;
    
    # SSL сертификаты (будут добавлены Certbot)
    ssl_certificate /etc/letsencrypt/live/masterprofi.ru/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/masterprofi.ru/privkey.pem;
    
    # SSL настройки
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    # Размер загружаемых файлов
    client_max_body_size 10M;
    
    # Прокси на Node.js сервер
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        
        # WebSocket поддержка
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        
        # Заголовки для прокси
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Таймауты
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Статические файлы с кешированием
    location /uploads/ {
        proxy_pass http://localhost:3000/uploads/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # Админ панель
    location /admin/ {
        proxy_pass http://localhost:3000/admin/;
    }
}
```

### Шаг 4: Активация конфигурации

```bash
# Создать символическую ссылку
sudo ln -s /etc/nginx/sites-available/masterprofi /etc/nginx/sites-enabled/

# Проверить конфигурацию
sudo nginx -t

# Перезапустить Nginx
sudo systemctl restart nginx
```

### Шаг 5: Получение SSL сертификата

```bash
sudo certbot --nginx -d masterprofi.ru -d www.masterprofi.ru
```

Certbot автоматически обновит конфигурацию Nginx!

### Шаг 6: Автообновление сертификатов

```bash
# Проверить автообновление
sudo certbot renew --dry-run

# Уже настроено автоматически через systemd timer
sudo systemctl status certbot.timer
```

### Шаг 7: Обновить Node.js сервер

В `backend/.env`:
```env
PORT=3000
NODE_ENV=production
```

Node.js теперь работает на порту 3000, а Nginx проксирует на 80/443!

---

## ☁️ ВАРИАНТ 3: CLOUDFLARE (Самый простой)

### Преимущества:
- ✅ Бесплатный SSL сертификат
- ✅ Не нужно устанавливать ничего на сервере
- ✅ DDoS защита включена
- ✅ CDN для статики
- ✅ Простая настройка через веб-интерфейс

### Недостатки:
- ⚠️ Cloudflare видит весь трафик
- ⚠️ Небольшая задержка из-за прокси

### Шаг 1: Зарегистрируйтесь на Cloudflare

1. Перейдите на https://cloudflare.com
2. Создайте аккаунт
3. Добавьте ваш домен `masterprofi.ru`

### Шаг 2: Обновите NS записи у регистратора

Cloudflare покажет NS серверы:
```
jane.ns.cloudflare.com
todd.ns.cloudflare.com
```

Обновите их в панели регистратора домена.

### Шаг 3: Настройте DNS записи

В панели Cloudflare → DNS:
```
Type: A
Name: @
Content: 212.74.227.208
Proxy: Enabled (оранжевое облако)

Type: A
Name: www
Content: 212.74.227.208
Proxy: Enabled
```

### Шаг 4: Включите SSL

В панели Cloudflare → SSL/TLS:
- Режим: **Full (strict)** или **Flexible**
- Always Use HTTPS: **On**
- Automatic HTTPS Rewrites: **On**

### Шаг 5: (Опционально) Origin Certificate

Для Full (strict) режима:
1. SSL/TLS → Origin Server → Create Certificate
2. Скачайте сертификат и ключ
3. Установите на сервер (см. Вариант 1)

---

## ✅ ПРОВЕРКА HTTPS

### 1. Проверка через браузер

Откройте: `https://masterprofi.ru`

Должен быть замок 🔒 в адресной строке

### 2. Проверка SSL Labs

Перейдите: https://www.ssllabs.com/ssltest/

Введите ваш домен и проверьте рейтинг (желательно A или A+)

### 3. Проверка через curl

```bash
curl -I https://masterprofi.ru

# Должно быть:
# HTTP/2 200
# strict-transport-security: max-age=31536000
```

### 4. Проверка автоматического редиректа

```bash
curl -I http://masterprofi.ru

# Должно быть:
# HTTP/1.1 301 Moved Permanently
# Location: https://masterprofi.ru/
```

---

## 🔧 ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ

### Android приложение (Master App)

В `app/src/main/java/com/example/bestapp/api/RetrofitClient.kt`:

```kotlin
object RetrofitClient {
    private const val BASE_URL = "https://masterprofi.ru/" // Было: http://212.74.227.208:3000/
    
    // ... rest of the code
}
```

### Admin Panel

В `backend/admin-panel/src/api/api.js`:

```javascript
const API_BASE_URL = 'https://masterprofi.ru/api'; // Было: http://212.74.227.208:3000/api
```

### WebSocket

В приложении обновите WebSocket URL:

```kotlin
private const val WS_URL = "wss://masterprofi.ru/ws" // Было: ws://212.74.227.208:3000/ws
```

**Важно:** `wss://` вместо `ws://` для защищенного WebSocket!

---

## 📊 СРАВНЕНИЕ ВАРИАНТОВ

| Критерий | Certbot Direct | Nginx Proxy | Cloudflare |
|----------|----------------|-------------|------------|
| Сложность | Средняя | Высокая | Низкая |
| Надежность | Хорошая | Отличная | Отличная |
| Производительность | Отличная | Отличная | Хорошая |
| Безопасность | Хорошая | Отличная | Хорошая |
| Стоимость | Бесплатно | Бесплатно | Бесплатно |
| **Рекомендация** | Dev/Staging | **Production** | Быстрый старт |

---

## 🐛 TROUBLESHOOTING

### Ошибка: "Address already in use" на порту 80/443

```bash
# Найти процесс
sudo lsof -i :80
sudo lsof -i :443

# Остановить Nginx если запущен
sudo systemctl stop nginx

# Или Apache
sudo systemctl stop apache2
```

### Certbot ошибка: "Connection refused"

Убедитесь что:
1. Порт 80 открыт в файрволе
2. DNS запись указывает на ваш сервер
3. Нет других процессов на порту 80

```bash
# Проверка DNS
dig masterprofi.ru

# Проверка файрвола
sudo ufw status
sudo ufw allow 80
sudo ufw allow 443
```

### Node.js не может слушать порт 443

```bash
# Дать разрешение Node.js
sudo setcap 'cap_net_bind_service=+ep' $(which node)

# Или использовать Nginx прокси (рекомендуется)
```

### SSL сертификат не обновляется

```bash
# Проверить задачу обновления
sudo systemctl status certbot.timer

# Запустить обновление вручную
sudo certbot renew --dry-run

# Проверить логи
sudo tail -f /var/log/letsencrypt/letsencrypt.log
```

---

## 📚 ДОПОЛНИТЕЛЬНЫЕ РЕСУРСЫ

- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Certbot Documentation](https://certbot.eff.org/)
- [Nginx SSL Configuration](https://nginx.org/en/docs/http/configuring_https_servers.html)
- [Cloudflare SSL Guide](https://developers.cloudflare.com/ssl/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)

---

**Дата создания:** 23 декабря 2025  
**Версия:** 1.0  
**Рекомендация:** Используйте **Nginx Reverse Proxy** для production
