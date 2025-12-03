# 📘 Инструкция по настройке платежных систем

## 🎯 Краткое описание

Система интегрирована с **ЮKassa** (прием платежей) и **ЮMoney** (выплаты мастерам).

---

## ⚙️ Настройка ЮKassa (прием платежей от клиентов)

### Шаг 1: Регистрация в ЮKassa

1. Перейдите на https://yookassa.ru/
2. Зарегистрируйте магазин
3. Пройдите верификацию (требуется ИНН, ОГРН для юридических лиц)

### Шаг 2: Получение ключей

1. Войдите в личный кабинет: https://yookassa.ru/my
2. Перейдите в раздел "Настройки" → "Ключи API"
3. Скопируйте:
   - **Shop ID** (идентификатор магазина)
   - **Secret Key** (секретный ключ)

### Шаг 3: Настройка webhook

1. В личном кабинете перейдите в "Настройки" → "Уведомления"
2. Укажите URL для webhook:
   ```
   https://your-domain.com/api/payments/webhook/yookassa
   ```
3. Выберите события:
   - ✅ `payment.succeeded` (обязательно)
   - ✅ `payment.canceled` (рекомендуется)

### Шаг 4: Добавление переменных окружения

Добавьте в файл `.env`:

```env
YOOKASSA_SHOP_ID=your_shop_id_here
YOOKASSA_SECRET_KEY=your_secret_key_here
APP_URL=https://your-domain.com
```

### Шаг 5: Тестирование

Для тестирования используйте тестовые карты:
- **Успешный платеж**: 5555 5555 5555 4444
- **Отклоненный платеж**: 5555 5555 5555 4477
- CVV: любые 3 цифры
- Срок действия: любая будущая дата

---

## 💰 Настройка ЮMoney (выплаты мастерам)

### Шаг 1: Регистрация в ЮMoney

1. Создайте кошелек на https://yoomoney.ru/
2. Пройдите идентификацию (для работы с API)

### Шаг 2: Создание приложения

1. Перейдите в раздел разработчика: https://yoomoney.ru/docs/payment-buttons
2. Создайте новое приложение
3. Получите:
   - **Client ID**
   - **Client Secret**

### Шаг 3: Получение OAuth токена

1. Авторизуйтесь в ЮMoney:
   ```
   https://yoomoney.ru/oauth/authorize?client_id=YOUR_CLIENT_ID&response_type=code&redirect_uri=YOUR_REDIRECT_URI
   ```
2. Получите код авторизации из redirect_uri
3. Обменяйте код на токен:
   ```bash
   curl -X POST https://yoomoney.ru/oauth/token \
     -d "code=YOUR_CODE" \
     -d "client_id=YOUR_CLIENT_ID" \
     -d "grant_type=authorization_code" \
     -d "redirect_uri=YOUR_REDIRECT_URI" \
     -u "YOUR_CLIENT_ID:YOUR_CLIENT_SECRET"
   ```
4. Сохраните полученный `access_token`

### Шаг 4: Добавление переменных окружения

Добавьте в файл `.env`:

```env
YOOMONEY_ACCESS_TOKEN=your_access_token_here
YOOMONEY_CLIENT_ID=your_client_id_here
YOOMONEY_CLIENT_SECRET=your_client_secret_here
```

### Шаг 5: Настройка мастера

Для выплаты мастеру через ЮMoney, мастер должен:
1. Зарегистрировать кошелек ЮMoney
2. Указать номер кошелька в профиле (13-15 цифр)
3. При запросе выплаты выбрать метод `yoomoney` и указать номер кошелька

---

## 📊 Использование API

### Создание платежа через ЮKassa

**Endpoint:** `POST /api/payments/create-yookassa`

**Request:**
```json
{
  "orderId": 123,
  "amount": 5000,
  "returnUrl": "https://your-app.com/payment/success"
}
```

**Response:**
```json
{
  "message": "Платеж создан",
  "payment": {
    "id": 456,
    "orderId": 123,
    "amount": 5000,
    "status": "processing"
  },
  "yooKassa": {
    "paymentId": "2c5f4f8e-0001-5000-9000-000000000000",
    "confirmationUrl": "https://yookassa.ru/checkout/payments/...",
    "status": "pending"
  }
}
```

### Выплата мастеру через ЮMoney

**Endpoint:** `POST /api/masters/wallet/payout`

**Request:**
```json
{
  "amount": 1000,
  "payoutMethod": "yoomoney",
  "payoutDetails": {
    "yoomoneyWallet": "410011234567890"
  }
}
```

**Response:**
```json
{
  "message": "Выплата успешно выполнена через ЮMoney",
  "transaction": { ... },
  "yooMoney": {
    "requestId": "12345678",
    "status": "success"
  }
}
```

---

## 🔐 Безопасность

### Webhook проверка

ЮKassa отправляет подписанные webhook'и. Для проверки подписи используйте:
- IP-адреса ЮKassa: `185.71.76.0/27`, `185.71.77.0/27`, `77.75.153.0/25`
- Проверку подписи через SDK (реализуется автоматически)

### Переменные окружения

⚠️ **НИКОГДА не коммитьте `.env` файл в Git!**

Убедитесь, что `.env` добавлен в `.gitignore`.

---

## 🧪 Тестирование

### Тестовый режим ЮKassa

1. Используйте тестовые ключи из личного кабинета
2. Используйте тестовые карты (см. выше)
3. Платежи не будут реально списывать средства

### Тестовый режим ЮMoney

1. Используйте тестовый аккаунт
2. Создайте тестовый кошелек
3. Проверьте выплаты небольшими суммами

---

## 📝 Чек-лист запуска

- [ ] Зарегистрирован магазин в ЮKassa
- [ ] Получены Shop ID и Secret Key
- [ ] Настроен webhook URL
- [ ] Добавлены переменные окружения в `.env`
- [ ] Зарегистрирован кошелек ЮMoney
- [ ] Получен OAuth токен ЮMoney
- [ ] Добавлены переменные окружения ЮMoney
- [ ] Протестировано создание платежа
- [ ] Протестирован webhook
- [ ] Протестирована выплата мастеру

---

## 🆘 Решение проблем

### Ошибка "ЮKassa не настроен"
- Проверьте наличие `YOOKASSA_SHOP_ID` и `YOOKASSA_SECRET_KEY` в `.env`
- Убедитесь, что файл `.env` находится в директории `backend/`
- Перезапустите сервер после изменения `.env`

### Webhook не приходит
- Проверьте доступность URL извне (используйте ngrok для локальной разработки)
- Убедитесь, что URL указан правильно в настройках ЮKassa
- Проверьте логи сервера на наличие ошибок

### Ошибка выплаты через ЮMoney
- Проверьте формат номера кошелька (13-15 цифр)
- Убедитесь, что токен валидный и не истек
- Проверьте баланс вашего кошелька ЮMoney

---

## 📚 Дополнительные ресурсы

- [Документация ЮKassa](https://yookassa.ru/developers/api)
- [Документация ЮMoney API](https://yoomoney.ru/docs)
- [Интеграция онлайн-кассы (ФЗ-54)](https://yookassa.ru/developers/payments/receipts)



## 🎯 Краткое описание

Система интегрирована с **ЮKassa** (прием платежей) и **ЮMoney** (выплаты мастерам).

---

## ⚙️ Настройка ЮKassa (прием платежей от клиентов)

### Шаг 1: Регистрация в ЮKassa

1. Перейдите на https://yookassa.ru/
2. Зарегистрируйте магазин
3. Пройдите верификацию (требуется ИНН, ОГРН для юридических лиц)

### Шаг 2: Получение ключей

1. Войдите в личный кабинет: https://yookassa.ru/my
2. Перейдите в раздел "Настройки" → "Ключи API"
3. Скопируйте:
   - **Shop ID** (идентификатор магазина)
   - **Secret Key** (секретный ключ)

### Шаг 3: Настройка webhook

1. В личном кабинете перейдите в "Настройки" → "Уведомления"
2. Укажите URL для webhook:
   ```
   https://your-domain.com/api/payments/webhook/yookassa
   ```
3. Выберите события:
   - ✅ `payment.succeeded` (обязательно)
   - ✅ `payment.canceled` (рекомендуется)

### Шаг 4: Добавление переменных окружения

Добавьте в файл `.env`:

```env
YOOKASSA_SHOP_ID=your_shop_id_here
YOOKASSA_SECRET_KEY=your_secret_key_here
APP_URL=https://your-domain.com
```

### Шаг 5: Тестирование

Для тестирования используйте тестовые карты:
- **Успешный платеж**: 5555 5555 5555 4444
- **Отклоненный платеж**: 5555 5555 5555 4477
- CVV: любые 3 цифры
- Срок действия: любая будущая дата

---

## 💰 Настройка ЮMoney (выплаты мастерам)

### Шаг 1: Регистрация в ЮMoney

1. Создайте кошелек на https://yoomoney.ru/
2. Пройдите идентификацию (для работы с API)

### Шаг 2: Создание приложения

1. Перейдите в раздел разработчика: https://yoomoney.ru/docs/payment-buttons
2. Создайте новое приложение
3. Получите:
   - **Client ID**
   - **Client Secret**

### Шаг 3: Получение OAuth токена

1. Авторизуйтесь в ЮMoney:
   ```
   https://yoomoney.ru/oauth/authorize?client_id=YOUR_CLIENT_ID&response_type=code&redirect_uri=YOUR_REDIRECT_URI
   ```
2. Получите код авторизации из redirect_uri
3. Обменяйте код на токен:
   ```bash
   curl -X POST https://yoomoney.ru/oauth/token \
     -d "code=YOUR_CODE" \
     -d "client_id=YOUR_CLIENT_ID" \
     -d "grant_type=authorization_code" \
     -d "redirect_uri=YOUR_REDIRECT_URI" \
     -u "YOUR_CLIENT_ID:YOUR_CLIENT_SECRET"
   ```
4. Сохраните полученный `access_token`

### Шаг 4: Добавление переменных окружения

Добавьте в файл `.env`:

```env
YOOMONEY_ACCESS_TOKEN=your_access_token_here
YOOMONEY_CLIENT_ID=your_client_id_here
YOOMONEY_CLIENT_SECRET=your_client_secret_here
```

### Шаг 5: Настройка мастера

Для выплаты мастеру через ЮMoney, мастер должен:
1. Зарегистрировать кошелек ЮMoney
2. Указать номер кошелька в профиле (13-15 цифр)
3. При запросе выплаты выбрать метод `yoomoney` и указать номер кошелька

---

## 📊 Использование API

### Создание платежа через ЮKassa

**Endpoint:** `POST /api/payments/create-yookassa`

**Request:**
```json
{
  "orderId": 123,
  "amount": 5000,
  "returnUrl": "https://your-app.com/payment/success"
}
```

**Response:**
```json
{
  "message": "Платеж создан",
  "payment": {
    "id": 456,
    "orderId": 123,
    "amount": 5000,
    "status": "processing"
  },
  "yooKassa": {
    "paymentId": "2c5f4f8e-0001-5000-9000-000000000000",
    "confirmationUrl": "https://yookassa.ru/checkout/payments/...",
    "status": "pending"
  }
}
```

### Выплата мастеру через ЮMoney

**Endpoint:** `POST /api/masters/wallet/payout`

**Request:**
```json
{
  "amount": 1000,
  "payoutMethod": "yoomoney",
  "payoutDetails": {
    "yoomoneyWallet": "410011234567890"
  }
}
```

**Response:**
```json
{
  "message": "Выплата успешно выполнена через ЮMoney",
  "transaction": { ... },
  "yooMoney": {
    "requestId": "12345678",
    "status": "success"
  }
}
```

---

## 🔐 Безопасность

### Webhook проверка

ЮKassa отправляет подписанные webhook'и. Для проверки подписи используйте:
- IP-адреса ЮKassa: `185.71.76.0/27`, `185.71.77.0/27`, `77.75.153.0/25`
- Проверку подписи через SDK (реализуется автоматически)

### Переменные окружения

⚠️ **НИКОГДА не коммитьте `.env` файл в Git!**

Убедитесь, что `.env` добавлен в `.gitignore`.

---

## 🧪 Тестирование

### Тестовый режим ЮKassa

1. Используйте тестовые ключи из личного кабинета
2. Используйте тестовые карты (см. выше)
3. Платежи не будут реально списывать средства

### Тестовый режим ЮMoney

1. Используйте тестовый аккаунт
2. Создайте тестовый кошелек
3. Проверьте выплаты небольшими суммами

---

## 📝 Чек-лист запуска

- [ ] Зарегистрирован магазин в ЮKassa
- [ ] Получены Shop ID и Secret Key
- [ ] Настроен webhook URL
- [ ] Добавлены переменные окружения в `.env`
- [ ] Зарегистрирован кошелек ЮMoney
- [ ] Получен OAuth токен ЮMoney
- [ ] Добавлены переменные окружения ЮMoney
- [ ] Протестировано создание платежа
- [ ] Протестирован webhook
- [ ] Протестирована выплата мастеру

---

## 🆘 Решение проблем

### Ошибка "ЮKassa не настроен"
- Проверьте наличие `YOOKASSA_SHOP_ID` и `YOOKASSA_SECRET_KEY` в `.env`
- Убедитесь, что файл `.env` находится в директории `backend/`
- Перезапустите сервер после изменения `.env`

### Webhook не приходит
- Проверьте доступность URL извне (используйте ngrok для локальной разработки)
- Убедитесь, что URL указан правильно в настройках ЮKassa
- Проверьте логи сервера на наличие ошибок

### Ошибка выплаты через ЮMoney
- Проверьте формат номера кошелька (13-15 цифр)
- Убедитесь, что токен валидный и не истек
- Проверьте баланс вашего кошелька ЮMoney

---

## 📚 Дополнительные ресурсы

- [Документация ЮKassa](https://yookassa.ru/developers/api)
- [Документация ЮMoney API](https://yoomoney.ru/docs)
- [Интеграция онлайн-кассы (ФЗ-54)](https://yookassa.ru/developers/payments/receipts)



