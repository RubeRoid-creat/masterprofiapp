# Инструкция по развертыванию Backend на сервере

## Подготовка к деплою

### 1. Упаковка файлов

Используйте скрипт `package-backend.js` для создания архива:

```bash
node scripts/package-backend.js
```

Это создаст архив `backend-deploy.zip` со всеми необходимыми файлами (без node_modules, логов, баз данных).

### 2. Файлы для загрузки на сервер

**Обязательные файлы:**
- `server.js` - главный файл сервера
- `config.js` - конфигурация
- `package.json` - зависимости
- `database/` - схемы БД
- `routes/` - API маршруты
- `services/` - бизнес-логика
- `middleware/` - middleware
- `scripts/` - утилиты
- `websocket.js` - WebSocket сервер

**НЕ загружать:**
- `node_modules/` - установится на сервере
- `*.sqlite`, `*.db` - базы данных
- `backups/` - резервные копии
- `logs/`, `*.log` - логи
- `.env` - создастся на сервере
- `firebase-service-account.json` - загрузить отдельно (секретный файл)

## Развертывание на сервере

### Вариант 1: Через SSH (Linux/Unix сервер)

1. **Загрузите архив на сервер:**
```bash
scp backend-deploy.zip user@your-server.com:/home/user/
```

2. **Подключитесь к серверу:**
```bash
ssh user@your-server.com
```

3. **Распакуйте архив:**
```bash
cd /home/user
unzip backend-deploy.zip -d bestapp-backend
cd bestapp-backend
```

4. **Установите зависимости:**
```bash
npm install --production
```

5. **Создайте файл `.env`:**
```bash
nano .env
```

Содержимое `.env`:
```env
PORT=3000
NODE_ENV=production
JWT_SECRET=your-very-secret-key-change-this
DATABASE_PATH=./database.sqlite
DATABASE_TYPE=sqlite

# Или для PostgreSQL:
# DATABASE_TYPE=postgresql
# POSTGRES_HOST=localhost
# POSTGRES_PORT=5432
# POSTGRES_DB=bestapp
# POSTGRES_USER=bestapp_user
# POSTGRES_PASSWORD=your_password

# Firebase (если используется)
FIREBASE_SERVICE_ACCOUNT=./firebase-service-account.json

# Настройки бэкапов
BACKUP_ENABLED=true
BACKUP_INTERVAL=86400000
MAX_BACKUPS=30
```

6. **Загрузите Firebase ключ (если используется):**
```bash
# Загрузите firebase-service-account.json через scp
scp firebase-service-account.json user@your-server.com:/home/user/bestapp-backend/
```

7. **Инициализируйте базу данных:**
```bash
npm run init-db
```

8. **Загрузите тестовые данные (опционально):**
```bash
node scripts/seed-test-data.js
```

9. **Запустите сервер:**

**С PM2 (рекомендуется):**
```bash
npm install -g pm2
pm2 start server.js --name bestapp-backend
pm2 save
pm2 startup
```

**Или через systemd:**
Создайте файл `/etc/systemd/system/bestapp-backend.service`:
```ini
[Unit]
Description=BestApp Backend API
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/home/user/bestapp-backend
ExecStart=/usr/bin/node server.js
Restart=always
RestartSec=10
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
```

Затем:
```bash
sudo systemctl enable bestapp-backend
sudo systemctl start bestapp-backend
sudo systemctl status bestapp-backend
```

### Вариант 2: Через FTP/SFTP

1. Подключитесь к серверу через FTP клиент (FileZilla, WinSCP)
2. Загрузите все файлы из архива в папку на сервере
3. Выполните шаги 4-9 из варианта 1 через SSH

### Вариант 3: Через Git (если есть репозиторий)

1. **На сервере:**
```bash
git clone https://your-repo-url.git bestapp-backend
cd bestapp-backend/backend
npm install --production
```

2. **Создайте `.env` файл** (см. шаг 5 варианта 1)

3. **Инициализируйте БД и запустите** (см. шаги 7-9 варианта 1)

## Настройка Nginx (опционально)

Если используете Nginx как reverse proxy:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # WebSocket support
    location /ws {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## Проверка работы

После запуска проверьте:

1. **Логи сервера:**
```bash
pm2 logs bestapp-backend
# или
journalctl -u bestapp-backend -f
```

2. **Доступность API:**
```bash
curl http://localhost:3000/api/orders
```

3. **Статус процесса:**
```bash
pm2 status
# или
sudo systemctl status bestapp-backend
```

## Обновление

1. Загрузите новую версию на сервер
2. Остановите сервер: `pm2 stop bestapp-backend` или `sudo systemctl stop bestapp-backend`
3. Распакуйте/обновите файлы
4. Установите зависимости: `npm install --production`
5. Запустите: `pm2 restart bestapp-backend` или `sudo systemctl start bestapp-backend`

## Резервное копирование

База данных автоматически бэкапится (если включено в конфиге).
Ручной бэкап:
```bash
npm run backup
```

Бэкапы хранятся в папке `backups/`

## Безопасность

⚠️ **ВАЖНО:**
- Измените `JWT_SECRET` в `.env` на случайную строку
- Не коммитьте `.env` и `firebase-service-account.json` в Git
- Используйте HTTPS в production
- Настройте firewall (откройте только нужные порты)
- Регулярно обновляйте зависимости: `npm audit` и `npm update`

