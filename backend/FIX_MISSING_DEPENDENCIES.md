# 🔧 Исправление ошибки "Cannot find package 'nodemailer'"

## Проблема

Сервер не запускается с ошибкой:
```
Error [ERR_MODULE_NOT_FOUND]: Cannot find package 'nodemailer'
```

## Решение

На сервере выполните следующие команды:

### 1. Перейдите в папку проекта:

```bash
cd ~/masterprofiapp/backend
# или
cd /root/masterprofiapp/backend
```

### 2. Установите зависимости:

```bash
# Установка всех зависимостей из package.json
npm install --production

# Или если нужно установить все (включая dev-зависимости):
npm install
```

### 3. Проверьте, что nodemailer установлен:

```bash
# Проверка установленных пакетов
npm list nodemailer

# Или проверьте папку node_modules
ls node_modules | grep nodemailer
```

### 4. Перезапустите сервер:

```bash
# Остановите текущий процесс PM2
pm2 stop bestapp-backend

# Удалите старый процесс (если нужно)
pm2 delete bestapp-backend

# Запустите заново
pm2 start server.js --name bestapp-backend
pm2 save

# Проверьте логи
pm2 logs bestapp-backend --lines 50
```

## Быстрое исправление (одной командой):

```bash
cd ~/masterprofiapp/backend && \
npm install --production && \
pm2 restart bestapp-backend && \
pm2 logs bestapp-backend --lines 20
```

## Если проблема сохраняется:

1. **Удалите node_modules и переустановите:**
   ```bash
   cd ~/masterprofiapp/backend
   rm -rf node_modules
   npm install --production
   pm2 restart bestapp-backend
   ```

2. **Проверьте версию Node.js:**
   ```bash
   node --version
   # Должна быть версия 18.x или выше
   ```

3. **Проверьте package.json:**
   ```bash
   cat package.json | grep nodemailer
   # Должно показать: "nodemailer": "^6.10.1"
   ```

## Проверка после исправления:

```bash
# 1. Проверьте, что сервер запустился
pm2 status

# 2. Проверьте логи на ошибки
pm2 logs bestapp-backend --err

# 3. Проверьте доступность
curl http://localhost:3000/api/version/check
```

## Дополнительные зависимости

Если после установки появятся ошибки с другими пакетами, установите все зависимости:

```bash
npm install
```

Это установит все пакеты, указанные в `package.json`, включая:
- nodemailer
- express
- jsonwebtoken
- bcryptjs
- multer
- и другие...
