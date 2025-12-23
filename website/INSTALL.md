# Инструкция по установке (решение проблем)

## Проблема с установкой Prisma

Если при установке возникает ошибка с Prisma (ECONNRESET), выполните следующие шаги:

### Вариант 1: Использование скрипта (рекомендуется)

Запустите PowerShell скрипт:
```powershell
.\install.ps1
```

### Вариант 2: Ручная установка

1. **Очистите кэш и удалите node_modules:**
```powershell
npm cache clean --force
Remove-Item -Recurse -Force node_modules -ErrorAction SilentlyContinue
Remove-Item -Force package-lock.json -ErrorAction SilentlyContinue
```

2. **Установите зависимости с флагом legacy-peer-deps:**
```powershell
npm install --legacy-peer-deps
```

3. **Если Prisma не установился, установите его отдельно:**
```powershell
npm install @prisma/client@latest --legacy-peer-deps
npm install prisma@latest --save-dev --legacy-peer-deps
```

4. **Сгенерируйте Prisma клиент:**
```powershell
npx prisma generate
```

### Вариант 3: Установка без Prisma (для тестирования)

Если проблемы с сетью продолжаются, можно временно убрать Prisma:

1. Откройте `package.json` и удалите строки с `@prisma/client` и `prisma`
2. Установите зависимости: `npm install`
3. Запустите проект: `npm run dev`

**Важно:** Без Prisma API endpoints не будут работать, но вы сможете проверить интерфейс сайта.

### Вариант 4: Использование yarn

Если npm не работает, попробуйте yarn:

```powershell
yarn install
yarn prisma generate
```

## После успешной установки

1. **Создайте файл .env:**
```
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_api_key"
NEXT_PUBLIC_SITE_URL="http://localhost:3000"
```

2. **Настройте базу данных:**
```powershell
npx prisma migrate dev --name init
```

3. **Запустите проект:**
```powershell
npm run dev
```

## Решение проблем с блокированными файлами

Если появляются ошибки EBUSY или EPERM:

1. Закройте все процессы, использующие файлы (VS Code, терминалы)
2. Перезапустите компьютер
3. Попробуйте установку снова

## Альтернатива: Docker

Если проблемы продолжаются, используйте Docker:

```powershell
docker-compose up -d
```

Это установит все зависимости в изолированной среде.

