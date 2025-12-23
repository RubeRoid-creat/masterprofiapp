# Скрипт для коммита и отправки изменений в GitHub
cd z:\BestAPP

Write-Host "=== Проверка статуса Git ===" -ForegroundColor Cyan
git status

Write-Host "`n=== Добавление файлов ===" -ForegroundColor Cyan
git add app/src/main/java/com/example/bestapp/api/ApiRepository.kt
git add app/src/main/java/com/example/bestapp/api/RetrofitClient.kt
git add ERROR_HANDLING_IMPROVEMENTS.md
git add SERVER_CONNECTION_TROUBLESHOOTING.md

Write-Host "`n=== Статус после добавления ===" -ForegroundColor Cyan
git status --short

Write-Host "`n=== Создание коммита ===" -ForegroundColor Cyan
$commitMessage = "Улучшена обработка ошибок подключения к серверу: добавлена проверка доступности, улучшены сообщения об ошибках, включена автоматическая повторная попытка, добавлена документация"
git commit -m $commitMessage

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=== Коммит успешно создан ===" -ForegroundColor Green
} else {
    Write-Host "`n=== Ошибка при создании коммита ===" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== Проверка remote ===" -ForegroundColor Cyan
git remote -v

Write-Host "`n=== Отправка в GitHub ===" -ForegroundColor Cyan
git push origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=== Изменения успешно отправлены в GitHub! ===" -ForegroundColor Green
} else {
    Write-Host "`n=== Ошибка при отправке в GitHub ===" -ForegroundColor Red
}

Write-Host "`n=== Финальный статус ===" -ForegroundColor Cyan
git status




