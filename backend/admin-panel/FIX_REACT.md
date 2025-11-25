# Исправление проблемы с React

## Проблема
Ошибка "Invalid hook call" из-за несовместимости React 19 с Material-UI.

## Решение
1. Понизить версию React до 18.3.1
2. Обновить типы
3. Перезапустить dev сервер

## Команды

```bash
cd backend/admin-panel
npm install react@^18.3.1 react-dom@^18.3.1
npm install @types/react@^18.3.12 @types/react-dom@^18.3.1
npm run dev
```

## После исправления
Перезапустите dev сервер, чтобы изменения вступили в силу.

