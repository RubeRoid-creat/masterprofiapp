@echo off
cd /d z:\BestAPP
echo Проверка статуса Git...
git status
echo.
echo Добавление изменений...
git add -A
echo.
echo Создание коммита...
git commit -m "Исправлена ошибка 404 в MLM: сервер возвращает пустую статистику, улучшена обработка ошибок"
echo.
echo Настройка remote...
git remote set-url origin https://github.com/RubeRoid-creat/masterprofiapp.git
echo.
echo Отправка в GitHub...
git push origin main
echo.
echo Готово!
pause




