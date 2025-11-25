# Инструкция по деплою через Git

## ✅ Git репозиторий инициализирован!

Первый коммит создан. Теперь можно загрузить код на сервер.

## 📤 Шаг 1: Создайте удаленный репозиторий

### На GitHub:
1. Перейдите на https://github.com
2. Создайте новый репозиторий (например, `bestapp-backend`)
3. **НЕ** инициализируйте его с README

### На GitLab:
1. Перейдите на https://gitlab.com
2. Создайте новый проект
3. Выберите "Create blank project"

### На Bitbucket:
1. Перейдите на https://bitbucket.org
2. Создайте новый репозиторий

## 🔗 Шаг 2: Подключите удаленный репозиторий

```bash
# Замените URL на ваш репозиторий
git remote add origin https://github.com/your-username/bestapp-backend.git

# Или через SSH (рекомендуется)
git remote add origin git@github.com:your-username/bestapp-backend.git
```

## 📤 Шаг 3: Отправьте код

```bash
git branch -M main
git push -u origin main
```

## 🚀 Шаг 4: Развертывание на сервере

### Первоначальная установка:

```bash
# Подключитесь к серверу
ssh user@your-server.com

# Клонируйте репозиторий
git clone https://github.com/your-username/bestapp-backend.git
cd bestapp-backend/backend

# Установите зависимости
npm install --production

# Создайте .env файл
cp ENV_EXAMPLE.txt .env
nano .env  # ОБЯЗАТЕЛЬНО измените JWT_SECRET!

# Инициализируйте базу данных
npm run init-db

# Загрузите тестовые данные
npm run seed

# Запустите через PM2
npm install -g pm2
pm2 start server.js --name bestapp-backend
pm2 save
pm2 startup
```

### Обновление (после изменений в коде):

```bash
# На сервере
cd bestapp-backend/backend
pm2 stop bestapp-backend
git pull origin main
npm install --production
pm2 restart bestapp-backend
pm2 logs bestapp-backend
```

## 🔄 Автоматическое обновление

См. подробную инструкцию в `backend/DEPLOY_GIT.md`

## 📝 Быстрые команды

```bash
# Локально: внести изменения
git add .
git commit -m "Описание изменений"
git push origin main

# На сервере: обновить
cd bestapp-backend/backend
git pull origin main
npm install --production
pm2 restart bestapp-backend
```

## ⚠️ Важно!

1. **НЕ коммитьте:**
   - `.env` файлы
   - `firebase-service-account.json`
   - Базы данных
   - `node_modules/`

2. **Всегда проверяйте перед коммитом:**
   ```bash
   git status
   git diff
   ```

3. **Используйте осмысленные сообщения коммитов:**
   ```bash
   git commit -m "Fix: исправлена ошибка в заявках"
   git commit -m "Feature: добавлена новая функция"
   ```

