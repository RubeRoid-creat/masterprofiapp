# Исправление проблемы со статическими файлами (404)

## Проблема

Статические файлы Next.js возвращают 404:
- `_next/static/*.woff2` - 404
- `_next/static/*.css` - 404  
- `_next/static/*.js` - 404

## Причина

В standalone режиме Next.js статические файлы не копируются автоматически. Решение - использовать обычный режим Next.js.

## Решение (УЖЕ ПРИМЕНЕНО)

Конфигурация была изменена на обычный режим Next.js (без standalone). Теперь нужно пересобрать проект на сервере.

### Шаг 1: Пересборка проекта на сервере

```bash
cd /var/www/ispravleno-website/website

# 1. Остановите процесс
pm2 stop ispravleno-website

# 2. Очистите и пересоберите
rm -rf .next node_modules
npm install --legacy-peer-deps
npm run build

# 3. Проверьте, что статические файлы созданы
ls -la .next/static/
ls -la .next/standalone/.next/static/

# 4. Запустите заново
pm2 start ecosystem.config.js
pm2 save

# 5. Проверьте логи
pm2 logs ispravleno-website
```

### Вариант 2: Использовать обычный режим Next.js (без standalone)

Если standalone продолжает вызывать проблемы, можно использовать обычный режим:

```bash
# 1. Измените next.config.js
cd /var/www/ispravleno-website/website
nano next.config.js

# Закомментируйте или удалите строку:
# output: 'standalone',
```

Затем обновите ecosystem.config.js:

```javascript
{
  name: 'ispravleno-website',
  script: 'node_modules/.bin/next',
  args: 'start',
  // или используйте npm
  // script: 'npm',
  // args: 'start',
  env: {
    NODE_ENV: 'production',
    PORT: 3003,
  },
}
```

### Вариант 3: Создать custom server.js

Если нужен standalone режим, создайте server.js:

```bash
cd /var/www/ispravleno-website/website
nano server.js
```

Содержимое:

```javascript
const { createServer } = require('http')
const { parse } = require('url')
const next = require('next')
const path = require('path')
const fs = require('fs')

const dev = process.env.NODE_ENV !== 'production'
const hostname = process.env.HOSTNAME || '0.0.0.0'
const port = parseInt(process.env.PORT || '3003', 10)

const app = next({ dev, hostname, port })
const handle = app.getRequestHandler()

app.prepare().then(() => {
  createServer(async (req, res) => {
    try {
      const parsedUrl = parse(req.url, true)
      await handle(req, res, parsedUrl)
    } catch (err) {
      console.error('Error occurred handling', req.url, err)
      res.statusCode = 500
      res.end('internal server error')
    }
  }).listen(port, hostname, (err) => {
    if (err) throw err
    console.log(`> Ready on http://${hostname}:${port}`)
  })
})
```

Обновите ecosystem.config.js:

```javascript
{
  name: 'ispravleno-website',
  script: 'server.js',
  env: {
    NODE_ENV: 'production',
    PORT: 3003,
    HOSTNAME: '0.0.0.0',
  },
}
```

## Быстрое решение (самое простое)

Используйте обычный режим Next.js вместо standalone:

```bash
cd /var/www/ispravleno-website/website

# 1. Отредактируйте next.config.js
nano next.config.js

# Удалите строку: output: 'standalone',

# 2. Обновите ecosystem.config.js
nano ecosystem.config.js

# Измените на:
# script: 'node_modules/.bin/next',
# args: 'start',

# 3. Пересоберите
npm run build

# 4. Перезапустите
pm2 restart ispravleno-website
```

## Проверка

После исправления проверьте:

1. Откройте сайт в браузере: `http://212.74.227.208:3003`
2. Откройте консоль разработчика (F12)
3. Не должно быть ошибок 404 для `_next/static/*`
4. Сайт должен загружаться полностью со стилями
