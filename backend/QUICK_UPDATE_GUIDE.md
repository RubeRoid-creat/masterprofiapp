# 🚀 Быстрое обновление сервера

## Проблема
После изменений в коде сервер показывает ошибку "доступ запрещен", потому что **сервер не был перезапущен**.

## Решение за 3 шага

### 1. Подключитесь к серверу
```bash
ssh root@212.74.227.208
```

### 2. Выполните скрипт обновления
```bash
cd ~/masterprofiapp/backend
chmod +x update-server.sh
./update-server.sh
```

### 3. Проверьте работу
```bash
curl http://localhost:3000/api/version
```

---

## Альтернативный способ (вручную)

Если скрипт не работает:

```bash
# 1. Перейдите в директорию
cd ~/masterprofiapp/backend

# 2. Обновите код
git pull origin main

# 3. Перезапустите сервер
pm2 restart all
# или
pm2 restart 0

# 4. Проверьте логи
pm2 logs --lines 50
```

---

## Проверка статуса

```bash
# Список процессов PM2
pm2 list

# Логи в реальном времени
pm2 logs server

# Информация о процессе
pm2 show server
```

---

## Что делать если PM2 не установлен

```bash
# Установите PM2
npm install -g pm2

# Запустите сервер
cd ~/masterprofiapp/backend
pm2 start server.js --name "server"
pm2 save
pm2 startup
```

---

## Частые проблемы

### Ошибка "Process not found"
```bash
# Запустите сервер заново
cd ~/masterprofiapp/backend
pm2 start server.js --name "server"
```

### Порт 3000 уже занят
```bash
# Найдите процесс
lsof -i :3000
# или
netstat -tulpn | grep :3000

# Убейте процесс
kill -9 <PID>
```

### Нет доступа к Git
```bash
# Проверьте SSH ключи
ls -la ~/.ssh/

# Настройте Git
git config --global user.email "your@email.com"
git config --global user.name "Your Name"
```

---

## Автоматическое обновление (после каждого push в GitHub)

Настройте GitHub Webhook или используйте GitHub Actions для автоматического деплоя.
