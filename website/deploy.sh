#!/bin/bash

# Скрипт деплоя сайта на сервер
# Использование: ./deploy.sh [production|staging]

set -e

ENV=${1:-production}
SERVER_USER=${SERVER_USER:-root}
SERVER_HOST=${SERVER_HOST:-212.74.227.208}
SERVER_PATH=${SERVER_PATH:-/var/www/ispravleno-website/website}
PORT=${PORT:-3003}

echo "🚀 Начало деплоя на сервер..."
echo "Сервер: $SERVER_USER@$SERVER_HOST"
echo "Путь: $SERVER_PATH"
echo "Порт: $PORT"
echo ""

# Проверка наличия .env файла
if [ ! -f .env ]; then
    echo "⚠️  Файл .env не найден!"
    echo "Создайте файл .env на основе .env.example"
    exit 1
fi

# Сборка проекта
echo "📦 Сборка проекта..."
npm install --legacy-peer-deps
npx prisma generate
npm run build

# Создание архива для передачи
echo "📦 Создание архива..."
tar -czf deploy.tar.gz \
    --exclude='node_modules' \
    --exclude='.next' \
    --exclude='.git' \
    --exclude='*.log' \
    .

# Копирование на сервер
echo "📤 Копирование на сервер..."
scp deploy.tar.gz $SERVER_USER@$SERVER_HOST:/tmp/

# Выполнение команд на сервере
echo "🔧 Развертывание на сервере..."
ssh $SERVER_USER@$SERVER_HOST << EOF
    set -e
    
    # Создание директории если не существует
    mkdir -p $SERVER_PATH
    cd $SERVER_PATH
    
    # Распаковка архива
    tar -xzf /tmp/deploy.tar.gz -C $SERVER_PATH
    
    # Установка зависимостей
    npm install --legacy-peer-deps --production
    
    # Генерация Prisma клиента
    npx prisma generate
    
    # Применение миграций (если нужно)
    # npx prisma migrate deploy
    
    # Перезапуск приложения (PM2)
    if command -v pm2 &> /dev/null; then
        pm2 restart ispravleno-website || pm2 start npm --name "ispravleno-website" -- start
    fi
    
    # Или перезапуск systemd
    if systemctl is-active --quiet ispravleno-website; then
        systemctl restart ispravleno-website
    fi
    
    # Очистка
    rm /tmp/deploy.tar.gz
    
    echo "✅ Деплой завершен!"
EOF

# Очистка локального архива
rm deploy.tar.gz

echo "✅ Деплой успешно завершен!"
echo "Сайт доступен по адресу: http://$SERVER_HOST:$PORT"
