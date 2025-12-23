# Быстрый старт - Решение проблемы установки

## Проблема
Ошибка при установке Prisma из-за сетевых проблем (ECONNRESET).

## Решение

### Шаг 1: Очистка
```powershell
npm cache clean --force
```

### Шаг 2: Установка с флагом legacy-peer-deps
```powershell
npm install --legacy-peer-deps
```

### Шаг 3: Если Prisma не установился
```powershell
npm install @prisma/client prisma --legacy-peer-deps
npx prisma generate
```

### Шаг 4: Настройка базы данных
Создайте файл `.env`:
```
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_api_key"
NEXT_PUBLIC_SITE_URL="http://localhost:3000"
```

Затем:
```powershell
npx prisma migrate dev --name init
```

### Шаг 5: Запуск
```powershell
npm run dev
```

## Альтернатива: Использование скрипта
Запустите `install.ps1`:
```powershell
.\install.ps1
```

## Если ничего не помогает
Используйте Docker:
```powershell
docker-compose up -d
```

