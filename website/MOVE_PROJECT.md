# Перемещение проекта в Z:\BestAPP

## Текущее расположение
- Проект: `Z:\SeoСайтИсправно`
- Целевое расположение: `Z:\BestAPP\website` (или другое название)

## Структура Z:\BestAPP

```
Z:\BestAPP\
├── backend\          # Backend API
│   ├── admin-panel\  # Админ-панель
│   └── ...
└── website\          # Сайт (новое расположение)
```

## Инструкция по перемещению

### Вариант 1: Через PowerShell (рекомендуется)

1. **Остановите сервер Next.js** (если запущен)

2. **Создайте папку для сайта:**
```powershell
New-Item -ItemType Directory -Path "Z:\BestAPP\website" -Force
```

3. **Переместите все файлы:**
```powershell
Move-Item -Path "Z:\SeoСайтИсправно\*" -Destination "Z:\BestAPP\website\" -Force
```

4. **Перейдите в новую папку:**
```powershell
cd Z:\BestAPP\website
```

5. **Проверьте зависимости:**
```powershell
npm install
```

6. **Обновите пути в .env** (если нужно):
```env
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"
NEXT_PUBLIC_ADMIN_API_URL="http://212.74.227.208:3000/api"
ADMIN_USER_TOKEN="ваш_токен"
```

7. **Запустите сервер:**
```powershell
npm run dev
```

### Вариант 2: Вручную

1. Создайте папку `Z:\BestAPP\website`
2. Скопируйте все файлы из `Z:\SeoСайтИсправно` в `Z:\BestAPP\website`
3. Удалите старую папку `Z:\SeoСайтИсправно` (после проверки)
4. Откройте проект из новой папки

## Важные моменты

### 1. Пути в .env
После перемещения проверьте, что все пути в `.env` корректны.

### 2. node_modules
Можно удалить `node_modules` и переустановить:
```powershell
Remove-Item -Recurse -Force node_modules
npm install --legacy-peer-deps
```

### 3. .next
Удалите папку `.next` (она пересоздастся):
```powershell
Remove-Item -Recurse -Force .next
```

### 4. Git (если используется)
Если проект в Git, обновите remote URL или создайте новый репозиторий.

## Проверка после перемещения

1. ✅ Сервер запускается без ошибок
2. ✅ Сайт открывается в браузере
3. ✅ API endpoints работают
4. ✅ Создание заказов работает
5. ✅ Интеграция с админ-панелью работает

## Структура после перемещения

```
Z:\BestAPP\
├── backend\
│   ├── admin-panel\      # Админ-панель (React)
│   ├── routes\           # API routes
│   ├── database\         # База данных
│   └── server.js         # Backend server
└── website\              # Сайт (Next.js)
    ├── app\
    ├── components\
    ├── lib\
    ├── prisma\
    └── ...
```

