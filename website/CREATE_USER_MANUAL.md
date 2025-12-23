# Создание пользователя для сайта - Ручная инструкция

## Проблема

Backend требует пользователя с ролью 'client' для создания заказов. Нужно создать такого пользователя.

## Решение

Выполните команды в PowerShell **напрямую** (не через Cursor):

### Шаг 1: Создать пользователя

Откройте PowerShell и выполните:

```powershell
$body = @{
    email = "website@ispravleno.ru"
    password = "Website2024!"
    name = "Website Bot"
    phone = "+79999999999"
    role = "client"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://212.74.227.208:3000/api/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body

$response.token
```

### Шаг 2: Сохранить токен

Скопируйте полученный токен и добавьте в файл `.env`:

```env
ADMIN_USER_TOKEN="ваш_токен_здесь"
```

### Шаг 3: Если пользователь уже существует

Если получите ошибку "уже существует", войдите под этим пользователем:

```powershell
$body = @{
    email = "website@ispravleno.ru"
    password = "Website2024!"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://212.74.227.208:3000/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body

$response.token
```

### Альтернатива: Использовать существующего пользователя

Если у вас уже есть пользователь с ролью 'client' или 'admin', используйте его токен:

1. Войдите в админ-панель: http://localhost:5173/admin/login
2. Откройте консоль браузера (F12)
3. Введите: `localStorage.getItem('admin_token')`
4. Скопируйте токен
5. Добавьте в `.env`: `ADMIN_USER_TOKEN="токен"`

## После добавления токена

1. Перезапустите Next.js сервер
2. Создайте тестовый заказ
3. Проверьте логи сервера
4. Проверьте админ-панель → Заказы

