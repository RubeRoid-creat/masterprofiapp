# Быстрое исправление проблемы

## Проблема
Ошибка "Невозможно прочитать свойства null (чтение 'useMemo')"

## Решение

1. **Перезапустите dev сервер:**
   ```bash
   cd backend/admin-panel
   # Остановите текущий сервер (Ctrl+C)
   npm run dev
   ```

2. **Очистите кеш браузера:**
   - Нажмите Ctrl+Shift+R (или Cmd+Shift+R на Mac)
   - Или откройте DevTools (F12) → вкладка Network → поставьте галочку "Disable cache"

3. **Проверьте, что backend запущен:**
   ```bash
   cd backend
   npm start
   ```

## Что было исправлено:

1. ✅ React понижен до версии 18.3.1 (совместим с Material-UI)
2. ✅ ErrorBoundary упрощен (не использует Material-UI внутри)
3. ✅ Добавлена дедупликация React в vite.config.js
4. ✅ Структура App переработана для правильной работы ErrorBoundary

## Если проблема сохраняется:

1. Удалите `node_modules` и `package-lock.json`
2. Выполните `npm install` заново
3. Перезапустите dev сервер

