# Исправление запуска сайта в PM2

## Проблема

При запуске сайта через PM2 возникают ошибки:
1. ❌ Порт 3001 уже занят (админ-панель)
2. ⚠️ Предупреждение: `"next start" does not work with "output: standalone"`

## Решение

### Шаг 1: Остановите текущий процесс

```bash
pm2 stop ispravleno-website
pm2 delete ispravleno-website
```

### Шаг 2: Обновите код

```bash
cd /var/www/ispravleno-website/website
git pull origin main
```

### Шаг 3: Пересоберите проект

```bash
npm install --legacy-peer-deps
npx prisma generate
npm run build
```

### Шаг 4: Запустите с правильной командой

**Вариант 1: Используя ecosystem.config.js (рекомендуется)**

```bash
# Создайте директорию для логов
mkdir -p logs

# Запустите через ecosystem.config.js
pm2 start ecosystem.config.js

# Сохраните конфигурацию
pm2 save
```

**Вариант 2: Напрямую через PM2**

```bash
PORT=3003 pm2 start npm --name "ispravleno-website" -- run start:standalone
pm2 save
```

### Шаг 5: Проверьте статус

```bash
# Проверьте список процессов
pm2 list

# Проверьте логи
pm2 logs ispravleno-website

# Должно быть:
# - Local: http://localhost:3003 (не 3001 или 3002!)
# - Ready in XXXms
```

### Шаг 6: Откройте сайт

Откройте в браузере: `http://212.74.227.208:3003`

## Проверка конфигурации

Убедитесь, что в `.env` файле указан правильный порт:

```bash
cat .env | grep PORT
# Должно быть: PORT=3002
```

## Если порт 3002 уже занят

Если видите ошибку `EADDRINUSE: address already in use 0.0.0.0:3002`:

```bash
# 1. Найдите процесс, который занимает порт
sudo lsof -i :3002

# 2. Остановите старый PM2 процесс (если есть)
pm2 stop ispravleno-website
pm2 delete ispravleno-website

# 3. Если это другой процесс, убейте его
# Найдите PID из команды выше, затем:
kill -9 <PID>

# 4. Проверьте, что порт свободен
sudo lsof -i :3002
# Должно быть пусто

# 5. Запустите заново
pm2 start ecosystem.config.js
```

Подробнее см. [FIX_PORT_3002.md](./FIX_PORT_3002.md)

## Если все еще не работает

1. Проверьте, что порт 3003 свободен:
```bash
sudo lsof -i :3003
```

2. Проверьте логи на ошибки:
```bash
pm2 logs ispravleno-website --lines 50
```

3. Убедитесь, что проект собран:
```bash
ls -la .next/standalone/server.js
# Файл должен существовать
```

4. Если файла нет, пересоберите:
```bash
npm run build
```
