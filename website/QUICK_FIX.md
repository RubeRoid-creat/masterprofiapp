# Быстрое решение проблемы с заявками

## Проблема
Заявки не появляются в админ-панели, потому что backend требует пользователя с ролью 'client'.

## Быстрое решение

### Вариант 1: Использовать токен из админ-панели (самый простой)

1. Войдите в админ-панель: http://localhost:5173/admin/login
2. Откройте консоль браузера (F12)
3. Введите: `localStorage.getItem('admin_token')`
4. Скопируйте токен
5. Откройте `.env` файл и добавьте:
   ```env
   ADMIN_USER_TOKEN="ваш_токен"
   ```
6. Перезапустите Next.js сервер

### Вариант 2: Создать пользователя через PowerShell

Откройте PowerShell **напрямую** (не через Cursor) и выполните:

```powershell
$body = '{"email":"website@ispravleno.ru","password":"Website2024!","name":"Website Bot","phone":"+79999999999","role":"client"}'

$response = Invoke-RestMethod -Uri "http://212.74.227.208:3000/api/auth/register" -Method POST -ContentType "application/json" -Body $body

Write-Host "Token: $($response.token)"
```

Затем добавьте токен в `.env` как в варианте 1.

## Проверка

После добавления токена:
1. Перезапустите сервер
2. Создайте заказ
3. Проверьте логи - должно быть "Order sent to admin panel successfully"
4. Проверьте админ-панель → Заказы

