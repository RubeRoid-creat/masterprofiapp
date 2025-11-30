# 🔗 URL-адреса проекта "МастерПрофи"

## 📦 GitHub Репозиторий

**Основной репозиторий:**
- URL: `https://github.com/RubeRoid-creat/masterprofiapp`
- Клонирование: `git clone https://github.com/RubeRoid-creat/masterprofiapp.git`

## 🌐 Сервер Production

**IP-адрес сервера:**
- IP: `212.74.227.208`
- Порт: `3000`
- Base URL: `http://212.74.227.208:3000/`
- API Base URL: `http://212.74.227.208:3000/api`
- WebSocket URL: `ws://212.74.227.208:3000/ws`

## 📱 Где используются URL-ы

### Android Приложение (Master App)
- `app/src/main/java/com/example/bestapp/api/RetrofitClient.kt`
  - BASE_URL: `http://212.74.227.208:3000/`
- `app/src/main/java/com/example/bestapp/api/WebSocketChatClient.kt`
  - WebSocket: `ws://212.74.227.208:3000/ws`

### Админ Панель
- `backend/admin-panel/src/api/api.js`
  - API_BASE_URL: `http://212.74.227.208:3000/api`
- `backend/admin-panel/src/pages/Verification.jsx`
  - Изображения документов: `http://212.74.227.208:3000/uploads/...`

### Конфигурация Backend
- `backend/version-config.json`
  - Download URL: `http://212.74.227.208:3000/updates`

### Документация
- `SERVER_DEPLOY.md` - инструкции по деплою
- `QUICK_SERVER_SETUP.md` - быстрая настройка
- `backend/DEPLOY_GIT.md` - деплой через Git

## 🔄 Обновление URL-ов

Если необходимо изменить URL сервера, обновите следующие файлы:

1. **Android App Master:**
   - `app/src/main/java/com/example/bestapp/api/RetrofitClient.kt`
   - `app/src/main/java/com/example/bestapp/api/WebSocketChatClient.kt`
   - `app/src/main/java/com/example/bestapp/ui/chat/ChatAdapter.kt`

2. **Админ Панель:**
   - `backend/admin-panel/src/api/api.js`
   - `backend/admin-panel/src/pages/Verification.jsx`

3. **Backend Config:**
   - `backend/version-config.json`

4. **Документация:**
   - `SERVER_DEPLOY.md`
   - `QUICK_SERVER_SETUP.md`

## ⚙️ Переменные окружения

Для админ-панели можно использовать переменную окружения:
```bash
VITE_API_URL=http://212.74.227.208:3000/api
```

---

**Последнее обновление:** $(date)


