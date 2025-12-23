# Скрипт установки зависимостей
Write-Host "Очистка кэша npm..." -ForegroundColor Yellow
npm cache clean --force

Write-Host "Удаление node_modules и package-lock.json..." -ForegroundColor Yellow
if (Test-Path "node_modules") {
    Remove-Item -Recurse -Force "node_modules" -ErrorAction SilentlyContinue
}
if (Test-Path "package-lock.json") {
    Remove-Item -Force "package-lock.json" -ErrorAction SilentlyContinue
}

Write-Host "Установка зависимостей..." -ForegroundColor Green
npm install --legacy-peer-deps

if ($LASTEXITCODE -ne 0) {
    Write-Host "Ошибка при установке. Пробуем установить Prisma отдельно..." -ForegroundColor Red
    npm install @prisma/client@latest --legacy-peer-deps
    npm install prisma@latest --save-dev --legacy-peer-deps
}

Write-Host "Генерация Prisma клиента..." -ForegroundColor Green
npx prisma generate

Write-Host "Установка завершена!" -ForegroundColor Green

