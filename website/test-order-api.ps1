# Test order creation API

Write-Host "Testing order creation..." -ForegroundColor Green

$orderData = @{
    address = "Test Address"
    equipmentType = "Холодильник"
    problemType = "Не работает"
    brand = "Samsung"
    date = (Get-Date).ToString("yyyy-MM-dd")
    time = "14:00"
    description = "Test order"
    name = "Test User"
    phone = "+79991234567"
    email = "test@example.com"
} | ConvertTo-Json

Write-Host "Sending order to: http://localhost:3000/api/orders" -ForegroundColor Yellow
Write-Host "Order data: $orderData" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "http://localhost:3000/api/orders" `
        -Method POST `
        -ContentType "application/json" `
        -Body $orderData
    
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3 | Write-Host
} catch {
    Write-Host ""
    Write-Host "Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "Details: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    }
}

