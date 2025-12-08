#!/bin/bash
# Скрипт для быстрого обновления сервера

echo "🔄 Обновление сервера MasterProfi..."

# Переходим в директорию backend
cd "$(dirname "$0")"

# Останавливаем сервер
echo "⏸️  Останавливаем сервер..."
pm2 stop all

# Получаем последние изменения
echo "📥 Загружаем изменения из GitHub..."
git pull origin main

# Устанавливаем зависимости (если есть новые)
echo "📦 Проверяем зависимости..."
npm install --production

# Запускаем сервер
echo "▶️  Запускаем сервер..."
if pm2 list | grep -q "server"; then
    pm2 restart server
else
    pm2 start server.js --name "server"
fi

# Сохраняем конфигурацию PM2
pm2 save

echo "✅ Сервер успешно обновлен!"
echo ""
echo "📊 Статус процессов:"
pm2 list

echo ""
echo "📝 Последние логи (нажмите Ctrl+C для выхода):"
pm2 logs server --lines 20
