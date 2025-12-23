# Скрипт для получения токена администратора

param(
    [Parameter(Mandatory=$true)]
    [string]$Email,
    
    [Parameter(Mandatory=$true)]
    [string]$Password,
    
    [string]$ApiUrl = "http://212.74.227.208:3000/api"
)

Write-Host "Получение токена для: $Email" -ForegroundColor Green

$body = @{
    email = $Email
    password = $Password
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$ApiUrl/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $body
    
    if ($response.token) {
        Write-Host ""
        Write-Host "Токен получен успешно!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Добавьте в .env файл:" -ForegroundColor Yellow
        Write-Host "ADMIN_USER_TOKEN=`"$($response.token)`"" -ForegroundColor Cyan
        Write-Host ""
        
        # Копируем в буфер обмена (если доступно)
        try {
            $response.token | Set-Clipboard
            Write-Host "Токен скопирован в буфер обмена!" -ForegroundColor Green
        } catch {
            Write-Host "Не удалось скопировать в буфер обмена" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Ошибка: токен не получен" -ForegroundColor Red
        Write-Host "Ответ сервера: $($response | ConvertTo-Json)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Ошибка при получении токена:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        Write-Host "Детали: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    }
}

