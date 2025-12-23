# Диагностика проблем с backend API

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Backend API Diagnostics" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$SERVER = "212.74.227.208"
$PORT = 3000

# Тест 1: Проверка доступности сервера
Write-Host "[1/5] Checking server availability..." -ForegroundColor Yellow
$portTest = Test-NetConnection -ComputerName $SERVER -Port $PORT -WarningAction SilentlyContinue

if ($portTest.TcpTestSucceeded) {
    Write-Host "  [OK] Server is reachable on port $PORT" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Server is not reachable on port $PORT" -ForegroundColor Red
    Write-Host "  [INFO] Backend server may not be running" -ForegroundColor Yellow
    exit 1
}

# Тест 2: Проверка основного API endpoint
Write-Host "[2/5] Checking main API endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://${SERVER}:${PORT}/api" -Method GET -ErrorAction Stop
    Write-Host "  [OK] Main API responds: $($response.StatusCode)" -ForegroundColor Green
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "  [WARNING] Main API returns 404 (may be normal)" -ForegroundColor Yellow
    } else {
        Write-Host "  [FAIL] Main API error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Тест 3: Проверка роута /api/orders
Write-Host "[3/5] Checking /api/orders endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://${SERVER}:${PORT}/api/orders" -Method GET -ErrorAction Stop
    Write-Host "  [INFO] /api/orders responds: $($response.StatusCode)" -ForegroundColor Cyan
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "  [WARNING] /api/orders returns: $statusCode" -ForegroundColor Yellow
    if ($statusCode -eq 401) {
        Write-Host "  [INFO] 401 Unauthorized is expected (requires auth)" -ForegroundColor Cyan
    }
}

# Тест 4: Проверка роута /api/orders/from-website
Write-Host "[4/5] Checking /api/orders/from-website endpoint..." -ForegroundColor Yellow
try {
    $testBody = @{
        name = "Test User"
        phone = "+79001234567"
        email = "test@test.com"
        device_type = "Тест"
        device_brand = "Test"
        problem_description = "Test"
        address = "Test Address"
        latitude = 55.751244
        longitude = 37.618423
        desired_repair_date = "2025-12-15"
        arrival_time = "10:00"
        urgency = "planned"
        priority = "regular"
    } | ConvertTo-Json -Depth 10

    $response = Invoke-RestMethod -Uri "http://${SERVER}:${PORT}/api/orders/from-website" -Method POST -Body $testBody -ContentType "application/json" -ErrorAction Stop
    Write-Host "  [OK] from-website endpoint works!" -ForegroundColor Green
    Write-Host "  [INFO] Response: $($response | ConvertTo-Json -Depth 2)" -ForegroundColor Cyan
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "  [FAIL] from-website endpoint returns: $statusCode" -ForegroundColor Red
    
    try {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "  [ERROR] Response: $errorBody" -ForegroundColor Red
    } catch {
        Write-Host "  [ERROR] Could not read error response" -ForegroundColor Red
    }
}

# Тест 5: Список доступных роутов
Write-Host "[5/5] Checking available routes..." -ForegroundColor Yellow
$testRoutes = @(
    "/api/auth/login",
    "/api/auth/register",
    "/api/masters",
    "/api/orders",
    "/api/version"
)

foreach ($route in $testRoutes) {
    try {
        $response = Invoke-WebRequest -Uri "http://${SERVER}:${PORT}${route}" -Method GET -ErrorAction Stop
        Write-Host "  [OK] $route : $($response.StatusCode)" -ForegroundColor Green
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 404) {
            Write-Host "  [FAIL] $route : 404 Not Found" -ForegroundColor Red
        } elseif ($statusCode -eq 401) {
            Write-Host "  [OK] $route : 401 (exists, requires auth)" -ForegroundColor Green
        } else {
            Write-Host "  [WARNING] $route : $statusCode" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Diagnosis complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Possible issues:" -ForegroundColor Yellow
Write-Host "  1. Backend server needs to be restarted" -ForegroundColor Gray
Write-Host "  2. Routes are not properly registered in server.js" -ForegroundColor Gray
Write-Host "  3. Express app is not handling POST requests correctly" -ForegroundColor Gray
Write-Host ""
Write-Host "To fix:" -ForegroundColor Yellow
Write-Host "  1. SSH to server: ssh root@${SERVER}" -ForegroundColor Gray
Write-Host "  2. Check PM2 status: pm2 list" -ForegroundColor Gray
Write-Host "  3. Restart backend: pm2 restart backend" -ForegroundColor Gray
Write-Host "  4. Check logs: pm2 logs backend --lines 100" -ForegroundColor Gray
Write-Host ""

