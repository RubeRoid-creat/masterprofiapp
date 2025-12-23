# Решение проблемы с Prisma Generate

## Проблема
Ошибка `ECONNRESET` при выполнении `npx prisma generate` - Prisma не может скачать движки.

## Решения (по порядку)

### Решение 1: Использование скрипта
```powershell
.\fix-prisma.ps1
```

Или упрощенная версия:
```powershell
.\fix-prisma-simple.ps1
```

### Решение 2: Установка переменной окружения
```powershell
$env:PRISMA_ENGINES_MIRROR = "https://binaries.prisma.sh"
npx prisma generate
```

### Решение 3: Использование Docker
Если проблемы с сетью продолжаются, используйте Docker:
```powershell
docker-compose up -d
```
Docker скачает все зависимости в изолированной среде.

### Решение 4: Ручная установка движков
1. Перейдите на https://github.com/prisma/prisma-engines/releases
2. Скачайте последнюю версию для вашей платформы
3. Распакуйте в `node_modules/.prisma/client/`
4. Запустите `npx prisma generate` снова

### Решение 5: Временная работа без БД
Если нужно протестировать интерфейс, можно временно закомментировать использование Prisma в API routes. Интерфейс будет работать, но данные не будут сохраняться.

### Решение 6: Использование другого источника
Попробуйте использовать другой зеркало:
```powershell
$env:PRISMA_ENGINES_MIRROR = "https://cdn.jsdelivr.net/npm/@prisma/engines"
npx prisma generate
```

## Проверка после генерации
После успешной генерации проверьте:
```powershell
npx prisma migrate dev --name init
```

Если миграции прошли успешно, можно запускать проект:
```powershell
npm run dev
```

## Важно
- Проблема связана с сетевыми ограничениями или блокировкой доступа к серверам Prisma
- Docker - самый надежный способ обойти эту проблему
- После успешной генерации один раз, Prisma будет работать нормально

