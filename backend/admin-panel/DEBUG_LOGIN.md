# Отладка проблемы входа

## Проверка

### 1. Backend сервер запущен?
```bash
cd backend
npm start
```
Должен быть доступен на `http://localhost:3000`

### 2. Проверка API напрямую

Откройте в браузере или используйте curl:
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"admin123"}'
```

Должен вернуть JSON с токеном.

### 3. Проверка в консоли браузера

1. Откройте DevTools (F12)
2. Перейдите на вкладку **Console**
3. Попробуйте войти
4. Проверьте сообщения:
   - `API Base URL: http://localhost:3000/api` - должен показать правильный URL
   - `Attempting login with:` - должен показать email
   - Если есть ошибки сети - backend не запущен

### 4. Проверка Network запросов

1. Откройте DevTools (F12)
2. Перейдите на вкладку **Network**
3. Попробуйте войти
4. Найдите запрос к `/api/auth/login`:
   - **Status**: должен быть 200 (успех) или 401 (неверный пароль)
   - Если **Status: (failed)** или **CORS error** - проблема с подключением к backend
   - Если **Status: 404** - неправильный URL API

### 5. Проверка переменных окружения

Создайте файл `.env` в `backend/admin-panel/`:
```
VITE_API_URL=http://localhost:3000/api
```

После создания `.env` перезапустите dev сервер админ-панели.

### 6. Данные для входа

- **Email**: `admin@test.com`
- **Пароль**: `admin123`

### 7. Если все еще не работает

Проверьте логи backend сервера - там должны быть сообщения:
```
[POST /api/auth/login] Request received
[POST /api/auth/login] Email: admin@test.com, Password provided: true
[POST /api/auth/login] User found: id=3, role=admin
[POST /api/auth/login] Password verified for user: admin@test.com
```

Если этих сообщений нет - запрос не доходит до backend.

