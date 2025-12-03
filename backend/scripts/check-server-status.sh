#!/bin/bash
# Скрипт для проверки статуса сервера
# Использование: ./check-server-status.sh

echo "🔍 Проверка статуса сервера BestApp..."
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Проверка PM2
echo "1️⃣ Проверка PM2:"
if command -v pm2 &> /dev/null; then
    pm2 status
    echo ""
else
    echo -e "${YELLOW}⚠️ PM2 не установлен${NC}"
    echo ""
fi

# 2. Проверка порта 3000 (разные способы)
echo "2️⃣ Проверка порта 3000:"

# Попытка через ss (обычно установлен)
if command -v ss &> /dev/null; then
    echo "   Используется команда: ss"
    RESULT=$(sudo ss -tulpn | grep :3000)
    if [ -n "$RESULT" ]; then
        echo -e "${GREEN}   ✅ Найдено:${NC}"
        echo "$RESULT" | sed 's/^/   /'
    else
        echo -e "${RED}   ❌ Порт 3000 не прослушивается${NC}"
    fi
# Попытка через lsof
elif command -v lsof &> /dev/null; then
    echo "   Используется команда: lsof"
    RESULT=$(sudo lsof -i :3000)
    if [ -n "$RESULT" ]; then
        echo -e "${GREEN}   ✅ Найдено:${NC}"
        echo "$RESULT" | sed 's/^/   /'
    else
        echo -e "${RED}   ❌ Порт 3000 не прослушивается${NC}"
    fi
# Попытка через /proc/net
elif [ -f /proc/net/tcp ]; then
    echo "   Используется /proc/net/tcp"
    PORT_HEX=$(printf "%04X" 3000)
    RESULT=$(cat /proc/net/tcp | grep ":$PORT_HEX")
    if [ -n "$RESULT" ]; then
        echo -e "${GREEN}   ✅ Порт найден в /proc/net/tcp${NC}"
    else
        echo -e "${RED}   ❌ Порт 3000 не найден${NC}"
    fi
else
    echo -e "${YELLOW}   ⚠️ Не найдены инструменты для проверки порта${NC}"
    echo "   Установите один из: netstat, ss, lsof"
fi
echo ""

# 3. Проверка процесса Node.js
echo "3️⃣ Проверка процессов Node.js:"
NODE_PROCESSES=$(ps aux | grep node | grep -v grep)
if [ -n "$NODE_PROCESSES" ]; then
    echo -e "${GREEN}   ✅ Найдены процессы:${NC}"
    echo "$NODE_PROCESSES" | sed 's/^/   /'
else
    echo -e "${RED}   ❌ Процессы Node.js не найдены${NC}"
fi
echo ""

# 4. Проверка локального подключения
echo "4️⃣ Проверка локального HTTP подключения:"
if command -v curl &> /dev/null; then
    HTTP_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/version/check --connect-timeout 5)
    if [ "$HTTP_RESPONSE" = "200" ] || [ "$HTTP_RESPONSE" = "404" ] || [ "$HTTP_RESPONSE" = "401" ]; then
        echo -e "${GREEN}   ✅ HTTP сервер отвечает (код: $HTTP_RESPONSE)${NC}"
    else
        echo -e "${RED}   ❌ HTTP сервер не отвечает (код: $HTTP_RESPONSE)${NC}"
    fi
else
    echo -e "${YELLOW}   ⚠️ curl не установлен, пропуск HTTP проверки${NC}"
fi
echo ""

# 5. Проверка файрвола
echo "5️⃣ Проверка файрвола:"
if command -v ufw &> /dev/null; then
    echo "   Используется UFW:"
    sudo ufw status | grep 3000 || echo -e "${YELLOW}   ⚠️ Порт 3000 не найден в правилах UFW${NC}"
elif command -v firewall-cmd &> /dev/null; then
    echo "   Используется firewalld:"
    sudo firewall-cmd --list-ports | grep 3000 || echo -e "${YELLOW}   ⚠️ Порт 3000 не найден в правилах firewalld${NC}"
else
    echo -e "${YELLOW}   ⚠️ Файрвол не найден или не настроен${NC}"
fi
echo ""

echo "✅ Проверка завершена!"
echo ""
echo "💡 Если порт 3000 не прослушивается:"
echo "   1. Запустите сервер: pm2 start server.js --name bestapp-backend"
echo "   2. Или: npm start"
echo ""
echo "💡 Если файрвол блокирует:"
echo "   1. UFW: sudo ufw allow 3000/tcp && sudo ufw reload"
echo "   2. firewalld: sudo firewall-cmd --permanent --add-port=3000/tcp && sudo firewall-cmd --reload"
