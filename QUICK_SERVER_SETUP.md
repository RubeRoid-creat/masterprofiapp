# ⚡ Быстрая установка на сервере

## 📋 Минимальные команды для развертывания

```bash
# 1. Подключитесь к серверу
ssh user@your-server.com

# 2. Установите Node.js и PM2 (если не установлены)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g pm2

# 3. Клонируйте репозиторий
cd /home/user
git clone https://github.com/RubeRoid-creat/masterprofiapp.git
cd masterprofiapp/backend

# 4. Установите зависимости
npm install --production

# 5. Создайте .env файл
cp ENV_EXAMPLE.txt .env
nano .env  # ИЗМЕНИТЕ JWT_SECRET!

# 6. Инициализируйте БД
npm run init-db
npm run seed

# 7. Запустите сервер
pm2 start server.js --name bestapp-backend
pm2 save
pm2 startup  # Выполните команду, которую покажет PM2
```

## ✅ Проверка

```bash
pm2 status
pm2 logs bestapp-backend

# Проверка доступности (публичный эндпоинт)
curl http://localhost:3000/api/promotions/types

# Если нужно проверить с авторизацией, сначала залогиньтесь:
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"master@test.com","password":"password123"}'
```

## 🔄 Обновление кода

```bash
cd masterprofiapp/backend
pm2 stop bestapp-backend
git pull origin main
npm install --production
pm2 restart bestapp-backend
```

---

📖 **Подробная инструкция:** см. `SERVER_DEPLOY.md`

