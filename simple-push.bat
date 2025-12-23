@echo off
cd /d %~dp0
git add -A
git commit -m "Исправлена ошибка 404 в MLM: сервер возвращает пустую статистику, улучшена обработка ошибок"
git remote set-url origin https://github.com/RubeRoid-creat/masterprofiapp.git
git push origin main
pause




