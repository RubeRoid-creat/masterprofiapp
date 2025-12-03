# 🚀 Быстрая проверка сервера

## Если `netstat` не установлен

### Альтернативные команды для проверки порта 3000:

```bash
# 1. Используйте ss (обычно установлен по умолчанию)
sudo ss -tulpn | grep 3000

# 2. Или через lsof (если установлен)
sudo lsof -i :3000

# 3. Или проверьте процессы Node.js
ps aux | grep node

# 4. Или проверьте через /proc
cat /proc/net/tcp | grep 0BB8  # 0BB8 = 3000 в шестнадцатеричной системе
```

## Быстрая проверка всех необходимых вещей:

```bash
# 1. Проверка PM2
pm2 status

# 2. Проверка порта (используйте ss вместо netstat)
sudo ss -tulpn | grep 3000

# 3. Проверка процессов Node.js
ps aux | grep node | grep -v grep

# 4. Проверка локального доступа
curl http://localhost:3000/api/version/check

# 5. Проверка внешнего доступа
curl http://212.74.227.208:3000/api/version/check

# 6. Проверка файрвола (UFW)
sudo ufw status

# 7. Логи сервера
pm2 logs bestapp-backend --lines 20
```

## Если порт 3000 не прослушивается:

```bash
# Запустите сервер через PM2
cd ~/masterprofiapp/backend
pm2 start server.js --name bestapp-backend
pm2 save

# Или запустите напрямую для проверки
npm start
```

## Если файрвол блокирует:

```bash
# Откройте порт 3000
sudo ufw allow 3000/tcp
sudo ufw reload

# Проверьте статус
sudo ufw status
```

## Установка netstat (опционально):

Если нужна команда `netstat`:

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install net-tools

# CentOS/RHEL
sudo yum install net-tools
```

Но обычно `ss` работает лучше и установлен по умолчанию!
