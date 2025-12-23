# Как получить токен для интеграции

## Способ 1: Через консоль браузера (безопасный способ)

1. Войдите в админ-панель: http://localhost:5173/admin/login
2. После успешного входа откройте консоль разработчика (F12)
3. В консоли введите (наберите вручную, не копируйте):
```javascript
localStorage.getItem('admin_token')
```
4. Нажмите Enter
5. Скопируйте полученный токен (строка в кавычках)

## Способ 2: Через Application/Storage в DevTools

1. Войдите в админ-панель
2. Откройте DevTools (F12)
3. Перейдите на вкладку **Application** (или **Хранилище**)
4. В левой панели найдите **Local Storage** → `http://localhost:5173`
5. Найдите ключ `admin_token`
6. Скопируйте его значение

## Способ 3: Через API напрямую (без браузера)

### PowerShell:

```powershell
$body = @{
    email = "ваш_email@example.com"
    password = "ваш_пароль"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://212.74.227.208:3000/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body

# Токен будет в $response.token
Write-Host "Токен: $($response.token)"
```

### curl (если установлен):

```bash
curl -X POST http://212.74.227.208:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ваш_email","password":"ваш_пароль"}'
```

## Способ 4: Создать скрипт для получения токена

Создайте файл `get-token.js`:

```javascript
const fetch = require('node-fetch');

async function getToken() {
  const response = await fetch('http://212.74.227.208:3000/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'ваш_email@example.com',
      password: 'ваш_пароль'
    })
  });
  
  const data = await response.json();
  console.log('Токен:', data.token);
}

getToken();
```

Запустите:
```powershell
node get-token.js
```

## После получения токена

1. Откройте файл `.env` в папке сайта (`Z:\SeoСайтИсправно\.env`)
2. Добавьте строку:
```env
ADMIN_USER_TOKEN="ваш_токен_здесь"
```
3. Перезапустите Next.js сервер:
```powershell
npm run dev
```

## Проверка

После добавления токена создайте тестовый заказ на сайте и проверьте:
1. Логи сервера - должно быть сообщение об успешной отправке
2. Админ-панель → Заказы - должен появиться новый заказ

