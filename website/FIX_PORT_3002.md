# Исправление: Порт 3002 уже занят

## Проблема

Ошибка: `EADDRINUSE: address already in use 0.0.0.0:3002`

Порт 3002 уже используется другим процессом.

## Решение

### Шаг 1: Найдите процесс, который занимает порт 3002

```bash
# Вариант 1: Используя lsof
sudo lsof -i :3002

# Вариант 2: Используя netstat
sudo netstat -tlnp | grep 3002

# Вариант 3: Используя ss
sudo ss -tlnp | grep 3002

# Вариант 4: Используя fuser
sudo fuser 3002/tcp
```

Вы увидите что-то вроде:
```
node    12345  user   23u  IPv4 123456      0t0  TCP *:3002 (LISTEN)
```

Где `12345` - это PID процесса.

### Шаг 2: Остановите процесс

**Если это старый PM2 процесс:**

```bash
# Посмотрите все PM2 процессы
pm2 list

# Если видите процесс на порту 3002, остановите его
pm2 stop <process-name>
pm2 delete <process-name>

# Или остановите все процессы и перезапустите нужный
pm2 stop all
pm2 delete all
```

**Если это другой Node.js процесс:**

```bash
# Найдите PID из шага 1, затем убейте процесс
kill -9 <PID>

# Например, если PID = 12345:
kill -9 12345
```

**Если это systemd service:**

```bash
# Найдите сервис
sudo systemctl list-units | grep 3002

# Или проверьте все сервисы
sudo systemctl list-units --type=service | grep ispravleno

# Остановите сервис
sudo systemctl stop <service-name>
sudo systemctl disable <service-name>
```

### Шаг 3: Проверьте, что порт освобожден

```bash
sudo lsof -i :3002
# Должно быть пусто (ничего не выведет)
```

### Шаг 4: Запустите сайт заново

```bash
cd /var/www/ispravleno-website/website

# Используя ecosystem.config.js
pm2 start ecosystem.config.js

# Или напрямую
PORT=3002 pm2 start npm --name "ispravleno-website" -- run start:standalone

pm2 save
```

### Шаг 5: Проверьте логи

```bash
pm2 logs ispravleno-website
```

Должно быть:
- ✅ `Local: http://localhost:3002`
- ✅ `Ready in XXXms`
- ❌ Нет ошибки `EADDRINUSE`

## Альтернатива: Использовать другой порт

Если порт 3002 нужно оставить для другого сервиса, используйте порт 3003:

### 1. Обновите .env файл

```bash
cd /var/www/ispravleno-website/website
nano .env
```

Измените:
```env
PORT=3003
NEXT_PUBLIC_SITE_URL="http://212.74.227.208:3003"
```

### 2. Обновите ecosystem.config.js

```bash
nano ecosystem.config.js
```

Измените `PORT: 3002` на `PORT: 3003`

### 3. Запустите на новом порту

```bash
PORT=3003 pm2 start npm --name "ispravleno-website" -- run start:standalone
pm2 save
```

### 4. Откройте файрвол для нового порта (если нужно)

```bash
sudo ufw allow 3003/tcp
```

## Быстрая команда для проверки и очистки

```bash
# Найти и убить процесс на порту 3002
sudo lsof -ti:3002 | xargs kill -9

# Проверить, что порт свободен
sudo lsof -i :3002

# Если пусто, запустить сайт
cd /var/www/ispravleno-website/website
PORT=3002 pm2 start npm --name "ispravleno-website" -- run start:standalone
pm2 save
```
