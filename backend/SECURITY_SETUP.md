# 🔒 НАСТРОЙКА БЕЗОПАСНОСТИ И EMAIL

## ✅ ЧТО БЫЛО ДОБАВЛЕНО

### 1. **Rate Limiting** - Защита от DDoS атак
Автоматическая защита от злоупотреблений API:
- ✅ Глобальный лимит: 100 запросов за 15 минут
- ✅ Строгий лимит для авторизации: 10 попыток за 15 минут
- ✅ Лимит для кодов подтверждения: 3 попытки за 10 минут
- ✅ Автоблокировка IP при превышении лимита (на 1 час)
- ✅ Заголовки `X-RateLimit-*` в ответах

### 2. **Security Headers** - Защита от XSS и других атак
- ✅ `X-Content-Type-Options: nosniff`
- ✅ `X-Frame-Options: DENY`
- ✅ `X-XSS-Protection: 1; mode=block`
- ✅ `Content-Security-Policy`
- ✅ `Strict-Transport-Security` (HTTPS only)

### 3. **Request Sanitization** - Очистка входных данных
- ✅ Защита от SQL injection
- ✅ Защита от XSS атак
- ✅ Блокировка опасных JavaScript конструкций
- ✅ Проверка всех входных параметров (body, query, params)

### 4. **HTTPS Redirect** - Автоматический редирект на HTTPS
- ✅ Работает только в production режиме
- ✅ 301 редирект с HTTP на HTTPS
- ✅ Поддержка прокси (X-Forwarded-Proto)

### 5. **Security Audit Logger** - Логирование подозрительной активности
- ✅ Детектирование подозрительных запросов
- ✅ Логирование медленных запросов (>5 сек)
- ✅ Логирование неудачных попыток авторизации
- ✅ Аудит для расследований

### 6. **Улучшенный Email Service**
- ✅ Поддержка множества SMTP провайдеров
- ✅ Пул соединений для надежности
- ✅ Rate limiting для отправки
- ✅ Автоматическая проверка соединения
- ✅ Режим разработки (консоль) и production (SMTP)

---

## 📧 НАСТРОЙКА EMAIL SMTP

### Вариант 1: Gmail (Рекомендуется для тестирования)

1. **Включите 2FA** в аккаунте Gmail
2. **Создайте App Password:**
   - Перейдите: https://myaccount.google.com/apppasswords
   - Создайте пароль для приложения "МастерПрофи"
   - Скопируйте сгенерированный пароль

3. **Настройте .env:**
```env
EMAIL_SMTP_ENABLED=true
EMAIL_FROM=your-email@gmail.com
EMAIL_SMTP_HOST=smtp.gmail.com
EMAIL_SMTP_PORT=587
EMAIL_SMTP_SECURE=false
EMAIL_SMTP_USER=your-email@gmail.com
EMAIL_SMTP_PASSWORD=your-app-password-here
```

### Вариант 2: Yandex Mail

1. **Включите IMAP/SMTP** в настройках Яндекс.Почты
2. **Создайте пароль приложения** (если включена 2FA)

3. **Настройте .env:**
```env
EMAIL_SMTP_ENABLED=true
EMAIL_FROM=your-email@yandex.ru
EMAIL_SMTP_HOST=smtp.yandex.ru
EMAIL_SMTP_PORT=465
EMAIL_SMTP_SECURE=true
EMAIL_SMTP_USER=your-email@yandex.ru
EMAIL_SMTP_PASSWORD=your-password
```

### Вариант 3: Mail.ru

```env
EMAIL_SMTP_ENABLED=true
EMAIL_FROM=your-email@mail.ru
EMAIL_SMTP_HOST=smtp.mail.ru
EMAIL_SMTP_PORT=465
EMAIL_SMTP_SECURE=true
EMAIL_SMTP_USER=your-email@mail.ru
EMAIL_SMTP_PASSWORD=your-password
```

### Вариант 4: Корпоративная почта / Другие SMTP

```env
EMAIL_SMTP_ENABLED=true
EMAIL_FROM=noreply@masterprofi.ru
EMAIL_SMTP_HOST=smtp.your-provider.com
EMAIL_SMTP_PORT=587
EMAIL_SMTP_SECURE=false
EMAIL_SMTP_USER=your-username
EMAIL_SMTP_PASSWORD=your-password
```

---

## 🔧 НАСТРОЙКА RATE LIMITING

### Переменные окружения (.env):

```env
# Включить/выключить rate limiting
RATE_LIMIT_ENABLED=true

# Временное окно (миллисекунды)
# 900000 = 15 минут
RATE_LIMIT_WINDOW_MS=900000

# Максимум запросов за окно
RATE_LIMIT_MAX_REQUESTS=100
```

### Настройка для разных окружений:

**Development (разработка):**
```env
RATE_LIMIT_ENABLED=false
```

**Staging (тестирование):**
```env
RATE_LIMIT_ENABLED=true
RATE_LIMIT_MAX_REQUESTS=200
```

**Production (продакшн):**
```env
RATE_LIMIT_ENABLED=true
RATE_LIMIT_MAX_REQUESTS=100
RATE_LIMIT_WINDOW_MS=900000
```

---

## 🚀 АКТИВАЦИЯ ИЗМЕНЕНИЙ

### 1. Обновите .env файл

Скопируйте настройки из `ENV_EXAMPLE.txt` и заполните:

```bash
cp ENV_EXAMPLE.txt .env
nano .env  # или используйте любой редактор
```

### 2. Перезапустите сервер

**С PM2:**
```bash
pm2 restart masterprofi-backend
pm2 logs --lines 50
```

**Без PM2:**
```bash
npm start
```

### 3. Проверьте логи

Вы должны увидеть:
```
✅ [EMAIL SERVICE] SMTP сервер готов к отправке писем
🔒 Security middleware активирован
✅ Rate limiting включен: 100 запросов за 15 минут
```

---

## 🧪 ТЕСТИРОВАНИЕ

### Проверка Email:

```bash
# Используйте endpoint для отправки кода
curl -X POST http://localhost:3000/api/verification-codes/send-email-code \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"email": "test@example.com"}'
```

### Проверка Rate Limiting:

```bash
# Отправьте много запросов подряд
for i in {1..110}; do
  curl -s http://localhost:3000/api/version
  echo "Request $i"
done

# После 100 запросов должна быть ошибка 429
```

### Проверка Security Headers:

```bash
curl -I http://localhost:3000/api/version
```

Вы должны увидеть заголовки:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

---

## 🔍 МОНИТОРИНГ

### Логи безопасности:

**Подозрительные запросы:**
```
⚠️ Подозрительный запрос: { ip: '192.168.1.1', method: 'GET', url: '/api/../../../etc/passwd' }
```

**Блокировки IP:**
```
⛔ IP заблокирован за превышение лимита: 192.168.1.1 (105 запросов)
```

**Неудачные попытки авторизации:**
```
🔐 Неудачная попытка доступа: { ip: '192.168.1.1', url: '/api/admin', status: 401 }
```

### API для статистики:

Можно добавить эндпоинт для админов:

```javascript
// В routes/admin.js
import { getRateLimitStats } from '../middleware/rate-limiter.js';

router.get('/security/stats', authenticateToken, requireRole('admin'), (req, res) => {
  const stats = getRateLimitStats();
  res.json(stats);
});
```

---

## ⚙️ ДОПОЛНИТЕЛЬНЫЕ НАСТРОЙКИ

### Белый/Черный список IP:

```javascript
// В server.js или отдельном файле конфигурации
import { blockIP, addToWhitelist } from './middleware/security.js';

// Блокировка конкретного IP
blockIP('192.168.1.100');

// Добавление в whitelist (опционально)
addToWhitelist('192.168.1.1');
```

### Настройка для обхода rate limiting для конкретных IP:

Создайте `.env` переменную:
```env
RATE_LIMIT_WHITELIST=192.168.1.1,192.168.1.2,10.0.0.0/8
```

---

## 📊 МЕТРИКИ ПРОИЗВОДИТЕЛЬНОСТИ

### До улучшений:
- ❌ Нет защиты от DDoS
- ❌ Открыт для XSS атак
- ❌ Email только в режиме разработки
- ❌ Нет аудита безопасности

### После улучшений:
- ✅ Rate limiting на всех эндпоинтах
- ✅ Security headers на всех ответах
- ✅ Санитизация входных данных
- ✅ HTTPS редирект в production
- ✅ Production-ready Email SMTP
- ✅ Детальный security audit log

---

## 🐛 TROUBLESHOOTING

### Email не отправляется

1. **Проверьте логи:**
```bash
pm2 logs masterprofi-backend --lines 100 | grep EMAIL
```

2. **Проверьте настройки SMTP:**
```bash
# Тест соединения
telnet smtp.gmail.com 587
```

3. **Проверьте пароль приложения** (для Gmail с 2FA)

### Rate Limiting блокирует легитимные запросы

1. **Увеличьте лимит в .env:**
```env
RATE_LIMIT_MAX_REQUESTS=200
```

2. **Или временно отключите:**
```env
RATE_LIMIT_ENABLED=false
```

### HTTPS редирект не работает

Проверьте, что `NODE_ENV=production` в `.env`

---

## 📚 ДОПОЛНИТЕЛЬНЫЕ РЕСУРСЫ

- [Nodemailer документация](https://nodemailer.com/)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
- [OWASP Security Best Practices](https://owasp.org/www-project-top-ten/)
- [Express Security Best Practices](https://expressjs.com/en/advanced/best-practice-security.html)

---

**Дата создания:** 23 декабря 2025  
**Версия:** 1.0  
**Статус:** ✅ Готово к использованию
