@echo off
chcp 65001 >nul
cd /d z:\BestAPP

echo ========================================
echo Проверка Git репозитория
echo ========================================
echo.

echo 1. Текущий remote:
git remote -v
echo.

echo 2. Текущая ветка:
git branch --show-current
echo.

echo 3. Статус изменений:
git status
echo.

echo 4. Последние коммиты:
git log --oneline -5
echo.

echo ========================================
echo Добавление изменений
echo ========================================
git add backend/routes/mlm.js
git add backend/services/mlm-service.js
git add app/src/main/java/com/example/bestapp/ui/mlm/MLMViewModel.kt
git add app/src/main/java/com/example/bestapp/ui/mlm/MLMFragment.kt
git add app/src/main/java/com/example/bestapp/api/ApiRepository.kt
git add -A

echo.
echo Статус после добавления:
git status --short
echo.

echo ========================================
echo Создание коммита
echo ========================================
git commit -m "Исправлена ошибка 404 в MLM: сервер возвращает пустую статистику, улучшена обработка ошибок на клиенте и сервере"
echo.

echo ========================================
echo Настройка remote и отправка
echo ========================================
git remote set-url origin https://github.com/RubeRoid-creat/masterprofiapp.git
echo.

echo Проверка remote:
git remote -v
echo.

echo Отправка в GitHub...
git push origin main
echo.

echo ========================================
echo Финальный статус
echo ========================================
git status
echo.
echo Последний коммит:
git log --oneline -1
echo.

echo Готово!
pause




