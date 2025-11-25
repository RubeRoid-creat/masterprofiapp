# Развертывание Backend через Git

## 📋 Подготовка локально

### 1. Инициализация Git репозитория (если еще не сделано)

```bash
cd Z:\BestAPP
git init
```

### 2. Добавление файлов в Git

```bash
# Добавить все файлы
git add .

# Создать первый коммит
git commit -m "Initial commit: BestApp backend"
```

### 3. Создание удаленного репозитория

Создайте репозиторий на GitHub, GitLab, Bitbucket или другом сервисе.

### 4. Подключение удаленного репозитория

```bash
# Замените URL на ваш репозиторий
git remote add origin https://github.com/your-username/bestapp-backend.git

# Или через SSH
git remote add origin git@github.com:your-username/bestapp-backend.git
```

### 5. Отправка кода на сервер

```bash
git branch -M main
git push -u origin main
```

## 🚀 Развертывание на сервере

### Вариант 1: Первоначальная установка

```bash
# Подключитесь к серверу
ssh user@your-server.com

# Клонируйте репозиторий
git clone https://github.com/your-username/bestapp-backend.git
cd bestapp-backend/backend

# Установите зависимости
npm install --production

# Создайте файл .env
cp ENV_EXAMPLE.txt .env
nano .env  # ОБЯЗАТЕЛЬНО измените JWT_SECRET!

# Инициализируйте базу данных
npm run init-db

# Загрузите тестовые данные (опционально)
npm run seed

# Запустите через PM2
npm install -g pm2
pm2 start server.js --name bestapp-backend
pm2 save
pm2 startup
```

### Вариант 2: Обновление существующего деплоя

```bash
# Подключитесь к серверу
ssh user@your-server.com

# Перейдите в папку проекта
cd bestapp-backend/backend

# Остановите сервер
pm2 stop bestapp-backend

# Получите обновления
git pull origin main

# Установите новые зависимости (если есть)
npm install --production

# Перезапустите сервер
pm2 restart bestapp-backend

# Проверьте логи
pm2 logs bestapp-backend
```

## 🔄 Автоматическое обновление (через webhook)

### Настройка webhook на GitHub/GitLab

1. Перейдите в настройки репозитория → Webhooks
2. Добавьте новый webhook:
   - **URL**: `http://your-server.com:3000/api/webhook/deploy` (или создайте отдельный endpoint)
   - **Content type**: `application/json`
   - **Events**: `push`

### Создание endpoint для webhook

Создайте файл `backend/routes/deploy.js`:

```javascript
import express from 'express';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);
const router = express.Router();

// Секретный ключ для защиты webhook
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || 'your-webhook-secret';

router.post('/deploy', async (req, res) => {
  try {
    // Проверка секрета (рекомендуется)
    const secret = req.headers['x-webhook-secret'];
    if (secret !== WEBHOOK_SECRET) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    console.log('🔄 Получен webhook для обновления...');

    // Выполняем git pull
    const { stdout, stderr } = await execAsync('git pull origin main');
    console.log('Git pull:', stdout);

    // Устанавливаем зависимости
    await execAsync('npm install --production');
    console.log('✅ Зависимости обновлены');

    // Перезапускаем PM2
    await execAsync('pm2 restart bestapp-backend');
    console.log('✅ Сервер перезапущен');

    res.json({ 
      message: 'Deployment successful',
      output: stdout 
    });
  } catch (error) {
    console.error('❌ Ошибка деплоя:', error);
    res.status(500).json({ 
      error: 'Deployment failed',
      message: error.message 
    });
  }
});

export default router;
```

Подключите в `server.js`:

```javascript
import deployRouter from './routes/deploy.js';
app.use('/api/webhook', deployRouter);
```

## 🔐 Безопасность

### 1. Используйте SSH ключи вместо паролей

```bash
# На локальной машине
ssh-keygen -t ed25519 -C "your_email@example.com"

# Скопируйте публичный ключ на сервер
ssh-copy-id user@your-server.com
```

### 2. Защитите .env файл

```bash
# На сервере добавьте .env в .gitignore (уже добавлен)
# Создайте .env вручную на сервере
```

### 3. Используйте секретный ключ для webhook

Добавьте в `.env` на сервере:
```env
WEBHOOK_SECRET=your-very-secret-webhook-key
```

## 📝 Рекомендуемый workflow

1. **Локальная разработка:**
   ```bash
   # Внесли изменения
   git add .
   git commit -m "Описание изменений"
   git push origin main
   ```

2. **На сервере:**
   ```bash
   # Автоматически через webhook или вручную
   git pull origin main
   npm install --production
   pm2 restart bestapp-backend
   ```

## 🎯 Преимущества Git деплоя

✅ Версионность кода  
✅ Легкое откатывание изменений (`git revert`)  
✅ История изменений  
✅ Возможность работы в команде  
✅ Автоматизация через webhooks  
✅ Резервная копия кода в облаке  

## ⚠️ Важные замечания

1. **НЕ коммитьте:**
   - `.env` файлы
   - `firebase-service-account.json`
   - Базы данных (`*.sqlite`)
   - `node_modules/`
   - Логи и бэкапы

2. **Всегда проверяйте изменения перед деплоем:**
   ```bash
   git diff origin/main
   ```

3. **Используйте теги для версионирования:**
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

