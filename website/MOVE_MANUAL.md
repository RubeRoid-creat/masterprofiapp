# Перемещение проекта - Ручная инструкция

## Выполните команды в PowerShell напрямую (не через Cursor)

### Шаг 1: Остановите сервер Next.js
Нажмите Ctrl+C в терминале, где запущен `npm run dev`

### Шаг 2: Перейдите в папку проекта
```powershell
cd Z:\SeoСайтИсправно
```

### Шаг 3: Создайте целевую папку
```powershell
New-Item -ItemType Directory -Path "Z:\BestAPP\website" -Force
```

### Шаг 4: Переместите файлы
```powershell
Get-ChildItem -Path . -Exclude node_modules, .next, .git | Move-Item -Destination "Z:\BestAPP\website\" -Force
```

### Шаг 5: Перейдите в новую папку
```powershell
cd Z:\BestAPP\website
```

### Шаг 6: Переустановите зависимости (опционально)
```powershell
Remove-Item -Recurse -Force node_modules, .next -ErrorAction SilentlyContinue
npm install --legacy-peer-deps
```

### Шаг 7: Запустите сервер
```powershell
npm run dev
```

## Альтернатива: Копирование вместо перемещения

Если хотите сначала скопировать, а потом удалить старую папку:

```powershell
# Копировать
Get-ChildItem -Path "Z:\SeoСайтИсправно" -Exclude node_modules, .next, .git | Copy-Item -Destination "Z:\BestAPP\website\" -Recurse -Force

# После проверки удалить старую папку
Remove-Item -Recurse -Force "Z:\SeoСайтИсправно"
```

## Проверка после перемещения

1. ✅ Сервер запускается: `npm run dev`
2. ✅ Сайт открывается: http://localhost:3000
3. ✅ API работает: http://localhost:3000/api/orders
4. ✅ Создание заказов работает
5. ✅ Интеграция с админ-панелью работает

## Важно

- Файл `.env` будет перемещен вместе с проектом
- Проверьте, что все пути в `.env` корректны
- `node_modules` и `.next` можно не перемещать - они пересоздадутся

