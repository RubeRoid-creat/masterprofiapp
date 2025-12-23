# Решение проблемы с Prisma при сетевых ошибках

## Проблема
Prisma не может скачать движки из-за ECONNRESET.

## Решение 1: Использование переменной окружения для зеркала

Установите переменную окружения перед генерацией:
```powershell
$env:PRISMA_ENGINES_MIRROR="https://binaries.prisma.sh"
npx prisma generate
```

## Решение 2: Офлайн установка движков

1. Скачайте движки вручную с https://github.com/prisma/prisma-engines/releases
2. Поместите в папку `node_modules/.prisma/client/`
3. Запустите `npx prisma generate` снова

## Решение 3: Использование Docker (рекомендуется)

Если проблемы с сетью продолжаются, используйте Docker:
```powershell
docker-compose up -d
```

## Решение 4: Временный обходной путь

Можно временно закомментировать использование Prisma в API routes и использовать mock-данные для разработки интерфейса.

