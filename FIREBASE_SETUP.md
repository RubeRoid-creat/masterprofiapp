# Настройка Firebase Cloud Messaging

## ⚠️ Важно: Для работы push-уведомлений необходимо настроить Firebase

### Шаги настройки:

1. **Создайте проект в Firebase Console**
   - Перейдите на https://console.firebase.google.com/
   - Создайте новый проект или используйте существующий
   - Добавьте Android приложение:
     - Package name: `com.bestapp.client`
     - App nickname (опционально): "BestApp Client"
     - Debug signing certificate SHA-1 (опционально, для тестирования)

2. **Скачайте google-services.json**
   - В Firebase Console перейдите в настройки проекта (⚙️ → "Настройки проекта")
   - Перейдите на вкладку "Ваши приложения" → выберите Android приложение
   - Скачайте файл `google-services.json`
   - **Важно:** Поместите его в `ClientApp/app/` (на том же уровне, что и `app/build.gradle.kts`)
   - После добавления файла синхронизируйте проект Gradle (Sync Now)

3. **Настройте Backend для отправки уведомлений (сервисный ключ Firebase Admin SDK)**

   **Способ 1: Через Firebase Console**
   - В Firebase Console откройте ваш проект
   - Нажмите на иконку ⚙️ (шестеренка) рядом с "Обзор проекта" → выберите "Настройки проекта"
   - Перейдите на вкладку **"Сервисные аккаунты"** (Service accounts)
   - Нажмите кнопку **"Создать новый закрытый ключ"** (Generate new private key)
   - Подтвердите создание ключа
   - JSON файл автоматически скачается (например, `your-project-firebase-adminsdk-xxxxx.json`)
   - Сохраните этот файл в папку `backend/` (например, `backend/firebase-service-account.json`)

   **Способ 2: Через Google Cloud Console (рекомендуется, если вкладка не видна в Firebase)**
   - Перейдите на https://console.cloud.google.com/
   - **Важно:** Убедитесь, что выбран правильный проект (ваш Firebase проект должен быть в списке)
   - В левом меню найдите раздел **"IAM и администрирование"** (IAM and administration)
   - Нажмите на **"IAM и администрирование"** → откроется подменю
   - В подменю выберите **"Учетные записи служб"** (Service accounts) - это и есть "Сервисные аккаунты"
   - Нажмите кнопку **"Создать сервисный аккаунт"** (Create service account) вверху страницы
   - Заполните форму:
     - **Имя сервисного аккаунта:** `firebase-admin`
     - **Идентификатор:** автоматически заполнится (можно оставить как есть)
     - **Описание:** `Firebase Admin SDK для отправки push-уведомлений`
   - Нажмите **"Создать и продолжить"** (Create and continue)
   - В разделе "Предоставить этому сервисному аккаунту доступ к проекту" (Grant this service account access to project):
     - Нажмите **"Выбрать роль"** (Select a role)
     - Выберите роль: **"Редактор"** (Editor) или **"Владелец"** (Owner)
     - Нажмите **"Продолжить"** (Continue)
   - Нажмите **"Готово"** (Done)
   - Найдите созданный сервисный аккаунт в списке (обычно называется `firebase-admin@your-project.iam.gserviceaccount.com`)
   - Нажмите на него, чтобы открыть детали
   - Перейдите на вкладку **"Ключи"** (Keys) вверху
   - Нажмите **"Добавить ключ"** (Add key) → **"Создать новый ключ"** (Create new key)
   - Выберите формат **JSON** → нажмите **"Создать"** (Create)
   - JSON файл автоматически скачается (например, `your-project-firebase-adminsdk-xxxxx.json`)
   - Сохраните файл в папку `backend/` и переименуйте в `firebase-service-account.json` для удобства

   **После получения ключа:**
   - Сохраните скачанный JSON файл в папку `backend/` проекта
   - Переименуйте файл в `firebase-service-account.json` (для удобства)
   - **Полный путь к файлу:** `Z:\BestAPP\backend\firebase-service-account.json`
   - Путь уже настроен в `backend/config.js`:
     ```javascript
     firebaseServiceAccount: './firebase-service-account.json'
     ```
     (Файл должен находиться в той же папке, что и `config.js`, то есть в `backend/`)
   - **Важно:** Файл `firebase-service-account.json` уже добавлен в `.gitignore`, чтобы не коммитить ключ в репозиторий!

4. **Установите Firebase Admin SDK на backend**
   - Откройте терминал в папке проекта
   - Перейдите в папку `backend`:
     ```bash
     cd backend
     ```
   - Установите Firebase Admin SDK:
     ```bash
     npm install firebase-admin
     ```
   - После установки проверьте, что пакет добавлен в `package.json` в разделе `dependencies`

5. **Активируйте Firebase Admin SDK в коде**
   - ✅ Код уже раскомментирован и готов к работе
   - Убедитесь, что файл `firebase-service-account.json` находится в папке `backend/`
   - При запуске сервера вы должны увидеть сообщение: `✅ Firebase Admin SDK инициализирован`

### Текущий статус:

✅ **Backend готов:**
- Таблица `fcm_tokens` создана
- Endpoints для регистрации токенов работают
- Сервис для отправки уведомлений создан (заглушка)

✅ **ClientApp готов:**
- Google Services Gradle plugin добавлен (версия 4.4.4)
- Firebase BoM добавлен (версия 34.6.0)
- Firebase Cloud Messaging зависимости добавлены
- FcmService создан для регистрации токенов
- MyFirebaseMessagingService создан для обработки уведомлений
- Интеграция в AuthViewModel выполнена (автоматическая регистрация токена)

⚠️ **Требуется:**
- Файл `google-services.json` в `ClientApp/app/`
- Настройка Firebase Admin SDK на backend
- Раскомментирование кода отправки уведомлений

### Тестирование:

После настройки Firebase:
1. Запустите приложение
2. Войдите в систему
3. FCM токен автоматически зарегистрируется
4. При изменении статуса заказа вы получите push-уведомление

---

**Примечание:** Пока Firebase не настроен, push-уведомления работать не будут, но WebSocket уведомления продолжают работать.

