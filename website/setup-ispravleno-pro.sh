#!/bin/bash
# Скрипт для настройки домена ispravleno.pro
# Использование: ./setup-ispravleno-pro.sh

set -e

DOMAIN="ispravleno.pro"

echo "Настройка домена: $DOMAIN"
echo "=========================================="

# Шаг 1: Проверка и установка Nginx
echo "Шаг 1: Проверка Nginx..."
if ! command -v nginx &> /dev/null; then
    echo "Nginx не установлен. Устанавливаю..."
    sudo apt update
    sudo apt install nginx -y
    sudo systemctl start nginx
    sudo systemctl enable nginx
    echo "✓ Nginx установлен и запущен"
else
    echo "✓ Nginx уже установлен"
fi

# Шаг 2: Проверка DNS
echo ""
echo "Шаг 2: Проверка DNS записей..."
DOMAIN_IP=$(dig +short $DOMAIN | tail -1)
EXPECTED_IP="212.74.227.208"

if [ "$DOMAIN_IP" != "$EXPECTED_IP" ]; then
    echo "⚠ Предупреждение: DNS запись для $DOMAIN указывает на $DOMAIN_IP, ожидается $EXPECTED_IP"
    echo "   Убедитесь, что DNS записи настроены правильно и подождите распространения изменений"
    read -p "Продолжить? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✓ DNS запись настроена правильно: $DOMAIN -> $DOMAIN_IP"
fi

# Шаг 3: Копирование конфигурации
echo ""
echo "Шаг 3: Настройка конфигурации Nginx..."
cd /var/www/ispravleno-website/website

# Создаем конфигурацию с доменом
sudo tee /etc/nginx/sites-available/ispravleno-website > /dev/null <<EOF
server {
    listen 80;
    server_name ${DOMAIN} www.${DOMAIN};

    # Логи
    access_log /var/log/nginx/ispravleno-website-access.log;
    error_log /var/log/nginx/ispravleno-website-error.log;

    # Максимальный размер загружаемых файлов
    client_max_body_size 20M;

    # Проксирование на Next.js приложение
    location / {
        proxy_pass http://localhost:3003;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        
        # Таймауты
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Кэширование статических файлов Next.js
    location /_next/static/ {
        proxy_pass http://localhost:3003;
        proxy_cache_valid 200 60m;
        add_header Cache-Control "public, immutable";
        expires 1y;
    }

    # Кэширование публичных файлов
    location /public/ {
        proxy_pass http://localhost:3003;
        proxy_cache_valid 200 60m;
        add_header Cache-Control "public, max-age=3600";
    }
}
EOF

echo "✓ Конфигурация Nginx создана"

# Шаг 4: Активация конфигурации
echo ""
echo "Шаг 4: Активация конфигурации..."
if [ -L /etc/nginx/sites-enabled/ispravleno-website ]; then
    sudo rm /etc/nginx/sites-enabled/ispravleno-website
fi
sudo ln -s /etc/nginx/sites-available/ispravleno-website /etc/nginx/sites-enabled/

# Проверка конфигурации
if sudo nginx -t; then
    echo "✓ Конфигурация Nginx корректна"
    sudo systemctl reload nginx
    echo "✓ Nginx перезагружен"
else
    echo "✗ Ошибка в конфигурации Nginx"
    exit 1
fi

# Шаг 5: Установка SSL
echo ""
echo "Шаг 5: Настройка SSL сертификата..."
if ! command -v certbot &> /dev/null; then
    echo "Certbot не установлен. Устанавливаю..."
    sudo apt install certbot python3-certbot-nginx -y
    echo "✓ Certbot установлен"
else
    echo "✓ Certbot уже установлен"
fi

echo ""
echo "Получение SSL сертификата для ${DOMAIN}..."
sudo certbot --nginx -d ${DOMAIN} -d www.${DOMAIN} --non-interactive --agree-tos --redirect || {
    echo "⚠ Certbot требует интерактивного режима или уже настроен"
    echo "Для ручной настройки SSL выполните:"
    echo "  sudo certbot --nginx -d ${DOMAIN} -d www.${DOMAIN}"
}

# Шаг 6: Обновление .env
echo ""
echo "Шаг 6: Обновление переменных окружения..."
if [ -f .env ]; then
    # Определяем протокол (HTTP или HTTPS)
    if sudo certbot certificates 2>/dev/null | grep -q "$DOMAIN"; then
        SITE_URL="https://${DOMAIN}"
        echo "✓ SSL сертификат найден, используется HTTPS"
    else
        SITE_URL="http://${DOMAIN}"
        echo "⚠ SSL не настроен, используется HTTP (настройте позже через certbot)"
    fi
    
    # Обновляем NEXT_PUBLIC_SITE_URL
    if grep -q "NEXT_PUBLIC_SITE_URL" .env; then
        sed -i "s|NEXT_PUBLIC_SITE_URL=.*|NEXT_PUBLIC_SITE_URL=\"${SITE_URL}\"|" .env
        echo "✓ .env файл обновлен: NEXT_PUBLIC_SITE_URL=\"${SITE_URL}\""
    else
        echo "NEXT_PUBLIC_SITE_URL=\"${SITE_URL}\"" >> .env
        echo "✓ NEXT_PUBLIC_SITE_URL добавлен в .env"
    fi
else
    echo "⚠ Файл .env не найден. Создайте его вручную."
fi

# Шаг 7: Обновление next.config.js
echo ""
echo "Шаг 7: Обновление next.config.js..."
if [ -f next.config.js ]; then
    # Проверяем, есть ли домен в domains
    if ! grep -q "$DOMAIN" next.config.js; then
        # Добавляем домен в domains массив
        sed -i "s|domains: \['localhost', '212.74.227.208'\]|domains: ['localhost', '212.74.227.208', '${DOMAIN}', 'www.${DOMAIN}']|" next.config.js
        echo "✓ next.config.js обновлен"
    else
        echo "✓ next.config.js уже содержит домен"
    fi
else
    echo "⚠ Файл next.config.js не найден"
fi

echo ""
echo "=========================================="
echo "Настройка завершена!"
echo ""
echo "Следующие шаги:"
echo "1. Пересоберите проект: npm run build"
echo "2. Перезапустите PM2: pm2 restart ispravleno-website"
echo "3. Проверьте сайт: http://${DOMAIN} или https://${DOMAIN}"
echo ""
echo "Если SSL не был установлен автоматически, выполните:"
echo "  sudo certbot --nginx -d ${DOMAIN} -d www.${DOMAIN}"
