# Устранение проблемы: заявка не появляется в админ-панели

## Проблема

Backend требует:
1. Авторизацию с токеном пользователя
2. Пользователь должен иметь роль 'client' или 'admin'
3. Пользователь должен иметь запись в таблице `clients` (с `user_id`)

При создании заказа с сайта у нас нет пользователя в системе backend, поэтому заказ не создается.

## Решения

### Решение 1: Создать пользователя для сайта (рекомендуется)

1. **Создать пользователя через API:**

```powershell
$body = @{
    email = "website@ispravleno.ru"
    password = "secure_password_here"
    name = "Website Bot"
    phone = "+79999999999"
    role = "client"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://212.74.227.208:3000/api/auth/register" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body

# Сохранить токен
$token = $response.token
```

2. **Использовать этот токен в `.env`:**
```env
ADMIN_USER_TOKEN="полученный_токен"
```

### Решение 2: Добавить endpoint для сайта в backend

Добавить в `Z:\BestAPP\backend\routes\orders.js` специальный endpoint:

```javascript
// Создать заказ с сайта (без авторизации, но с проверкой API ключа)
router.post('/from-website', async (req, res) => {
  const apiKey = req.headers['x-website-api-key'];
  if (apiKey !== process.env.WEBSITE_API_KEY) {
    return res.status(401).json({ error: 'Invalid API key' });
  }
  
  // Создать или найти пользователя по телефону/email
  // Создать заказ от имени этого пользователя
  // ...
});
```

### Решение 3: Создать пользователя автоматически при создании заказа

Модифицировать `lib/admin-api.ts` для:
1. Проверки существования пользователя по телефону/email
2. Создания пользователя, если его нет
3. Получения токена
4. Создания заказа

## Проверка текущей ошибки

Проверьте логи сервера Next.js при создании заказа. Должна быть ошибка типа:
- `401 Unauthorized` - неверный токен
- `403 Forbidden` - недостаточно прав
- `400 Bad Request` - неверный формат данных
- `500 Internal Server Error` - ошибка на сервере

## Быстрое решение

Создайте пользователя для сайта и используйте его токен:

```powershell
# 1. Создать пользователя
.\get-token.ps1 -Email "website@ispravleno.ru" -Password "ваш_пароль"

# 2. Или зарегистрировать через API
$body = @{
    email = "website@ispravleno.ru"
    password = "ваш_пароль"
    name = "Website"
    phone = "+79999999999"
    role = "client"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://212.74.227.208:3000/api/auth/register" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body

# 3. Использовать токен из ответа
.\setup-token.ps1 -Token $response.token
```

