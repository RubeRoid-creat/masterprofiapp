# Скрипт для добавления токена в .env

param(
    [Parameter(Mandatory=$true)]
    [string]$Token
)

$envFile = ".env"

Write-Host "Добавление токена в .env файл..." -ForegroundColor Green

# Проверяем существование .env
if (-not (Test-Path $envFile)) {
    Write-Host "Создание .env файла..." -ForegroundColor Yellow
    @"
# Database
DATABASE_URL="postgresql://user:password@localhost:5432/ispravleno"

# Site
NEXT_PUBLIC_SITE_URL="http://localhost:3000"

# Яндекс.Карты
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_yandex_maps_api_key"

# Интеграция с админ-панелью
NEXT_PUBLIC_ADMIN_API_URL="http://212.74.227.208:3000/api"
ADMIN_USER_TOKEN="$Token"
"@ | Out-File -FilePath $envFile -Encoding UTF8
    Write-Host ".env файл создан!" -ForegroundColor Green
} else {
    # Читаем существующий .env
    $content = Get-Content $envFile -Raw
    
    # Проверяем, есть ли уже ADMIN_USER_TOKEN
    if ($content -match "ADMIN_USER_TOKEN=") {
        # Заменяем существующий токен
        $content = $content -replace 'ADMIN_USER_TOKEN="[^"]*"', "ADMIN_USER_TOKEN=`"$Token`""
        Write-Host "Токен обновлен в .env" -ForegroundColor Green
    } else {
        # Добавляем токен в конец файла
        $content += "`nADMIN_USER_TOKEN=`"$Token`"`n"
        Write-Host "Токен добавлен в .env" -ForegroundColor Green
    }
    
    Set-Content -Path $envFile -Value $content -Encoding UTF8
}

Write-Host ""
Write-Host "Токен успешно добавлен в .env!" -ForegroundColor Green
Write-Host "Перезапустите Next.js сервер для применения изменений." -ForegroundColor Yellow
Write-Host ""
Write-Host "Команда: npm run dev" -ForegroundColor Cyan

