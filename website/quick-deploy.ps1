# PowerShell скрипт для быстрого деплоя сайта на сервер
# Использование: .\quick-deploy.ps1

param(
    [string]$ServerUser = "root",
    [string]$ServerHost = "212.74.227.208",
    [string]$ServerPath = "/var/www/ispravleno-website/website",
    [int]$Port = 3003
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Начало деплоя сайта на сервер..." -ForegroundColor Cyan
Write-Host "Сервер: ${ServerUser}@${ServerHost}" -ForegroundColor Yellow
Write-Host "Путь: ${ServerPath}" -ForegroundColor Yellow
Write-Host "Порт: ${Port}" -ForegroundColor Yellow
Write-Host ""

# Проверка наличия .env файла
if (-not (Test-Path ".env")) {
    Write-Host "⚠️  Файл .env не найден!" -ForegroundColor Yellow
    Write-Host "Создайте файл .env на основе .env.example" -ForegroundColor Yellow
    exit 1
}

# Проверка SSH подключения
Write-Host "🔌 Проверка SSH подключения..." -ForegroundColor Cyan
try {
    ssh -o ConnectTimeout=5 "${ServerUser}@${ServerHost}" "echo 'SSH connection OK'" 2>&1 | Out-Null
    Write-Host "✅ SSH подключение работает" -ForegroundColor Green
} catch {
    Write-Host "❌ Не удалось подключиться к серверу" -ForegroundColor Red
    Write-Host "Проверьте SSH ключи и доступ к серверу" -ForegroundColor Yellow
    exit 1
}

# Сборка проекта
Write-Host "📦 Сборка проекта..." -ForegroundColor Cyan
npm install --legacy-peer-deps
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Ошибка установки зависимостей" -ForegroundColor Red
    exit 1
}

npx prisma generate
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Ошибка генерации Prisma клиента" -ForegroundColor Red
    exit 1
}

npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Ошибка сборки проекта" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Проект успешно собран" -ForegroundColor Green

# Создание архива
Write-Host "📦 Создание архива для передачи..." -ForegroundColor Cyan
$archiveName = "deploy-$(Get-Date -Format 'yyyyMMdd-HHmmss').tar.gz"

# Используем WSL tar или 7zip если доступен
if (Get-Command wsl -ErrorAction SilentlyContinue) {
    wsl tar -czf "../$archiveName" --exclude='node_modules' --exclude='.next' --exclude='.git' --exclude='*.log' .
    $archivePath = "..\$archiveName"
} elseif (Get-Command 7z -ErrorAction SilentlyContinue) {
    Write-Host "Используйте WSL или создайте архив вручную" -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "⚠️  Для создания архива требуется WSL или 7zip" -ForegroundColor Yellow
    Write-Host "Создайте архив вручную и загрузите на сервер" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $archivePath)) {
    Write-Host "❌ Ошибка создания архива" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Архив создан: $archivePath" -ForegroundColor Green

# Копирование на сервер
Write-Host "📤 Копирование на сервер..." -ForegroundColor Cyan
scp $archivePath "${ServerUser}@${ServerHost}:/tmp/deploy.tar.gz"
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Ошибка копирования на сервер" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Файлы скопированы на сервер" -ForegroundColor Green

# Выполнение команд на сервере
Write-Host "🔧 Развертывание на сервере..." -ForegroundColor Cyan

$remoteScript = @"
set -e

# Создание директории если не существует
mkdir -p $ServerPath
cd $ServerPath

# Распаковка архива
tar -xzf /tmp/deploy.tar.gz -C $ServerPath

# Установка зависимостей
npm install --legacy-peer-deps --production

# Генерация Prisma клиента
npx prisma generate

# Перезапуск приложения (PM2)
if command -v pm2 &> /dev/null; then
    pm2 restart ispravleno-website || pm2 start npm --name "ispravleno-website" -- start
    pm2 save
fi

# Или перезапуск systemd
if systemctl is-active --quiet ispravleno-website 2>/dev/null; then
    systemctl restart ispravleno-website
fi

# Очистка
rm /tmp/deploy.tar.gz

echo "✅ Деплой завершен!"
"@

$remoteScript | ssh "${ServerUser}@${ServerHost}" bash

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Ошибка развертывания на сервере" -ForegroundColor Red
    exit 1
}

# Очистка локального архива
Remove-Item $archivePath -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "✅ Деплой успешно завершен!" -ForegroundColor Green
Write-Host "Сайт доступен по адресу: http://${ServerHost}:${Port}" -ForegroundColor Cyan
