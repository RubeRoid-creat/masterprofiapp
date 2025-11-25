# 🔄 Обновление Backend на сервере

## Быстрое обновление

```bash
# Подключитесь к серверу
ssh root@your-server.com

# Перейдите в папку проекта
cd masterprofiapp/backend

# Остановите сервер
pm2 stop bestapp-backend

# Получите обновления
git pull origin main

# Установите новые зависимости (если есть)
npm install --production

# Перезапустите сервер
pm2 restart bestapp-backend

# Проверьте логи
pm2 logs bestapp-backend --lines 50
```

## Что было обновлено:

1. ✅ **Firebase теперь опциональный** - приложение работает без `firebase-service-account.json`
2. ✅ **Расширена статистика мастера** - добавлены данные за сегодня, неделю, месяц
3. ✅ **Главная страница** - теперь загружает актуальные данные через API

## Ошибки в логах (не критичные):

### Firebase ошибка (можно игнорировать):
```
⚠️ Firebase service account file not found
⚠️ Push-уведомления будут работать в режиме заглушки
```
**Решение:** Если нужны push-уведомления, загрузите `firebase-service-account.json` на сервер.

### Redis ошибка (можно игнорировать):
```
Redis не доступен, работаем без кэширования
```
**Решение:** Redis опционален, приложение работает без него.

## Проверка после обновления:

```bash
# Проверьте статус
pm2 status

# Проверьте логи
pm2 logs bestapp-backend

# Проверьте API
curl http://localhost:3000/api/promotions/types
```

---

**Готово!** Сервер обновлен и работает.

