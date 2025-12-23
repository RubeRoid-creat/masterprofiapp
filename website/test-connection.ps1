# Скрипт проверки подключения к PostgreSQL на удаленном сервере

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PostgreSQL Connection Test Tool" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Параметры подключения
$SERVER = "212.74.227.208"
$PORT = 5432
$DATABASE = "bestapp_website"
$USERNAME = "masterprofi"

# Запросить пароль
Write-Host "Testing connection to PostgreSQL server..." -ForegroundColor Yellow
Write-Host "Server: $SERVER" -ForegroundColor Gray
Write-Host "Port: $PORT" -ForegroundColor Gray
Write-Host "Database: $DATABASE" -ForegroundColor Gray
Write-Host "Username: $USERNAME" -ForegroundColor Gray
Write-Host ""

# Тест 1: Проверка доступности сервера
Write-Host "[1/4] Checking server availability..." -ForegroundColor Yellow
$pingResult = Test-NetConnection -ComputerName $SERVER -WarningAction SilentlyContinue

if ($pingResult.PingSucceeded) {
    Write-Host "  [OK] Server is reachable (RTT: $($pingResult.PingReplyDetails.RoundtripTime)ms)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Server is not reachable" -ForegroundColor Red
    exit 1
}

# Тест 2: Проверка порта PostgreSQL
Write-Host "[2/4] Checking PostgreSQL port..." -ForegroundColor Yellow
$portResult = Test-NetConnection -ComputerName $SERVER -Port $PORT -WarningAction SilentlyContinue

if ($portResult.TcpTestSucceeded) {
    Write-Host "  [OK] Port $PORT is open" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Port $PORT is closed or filtered" -ForegroundColor Red
    Write-Host ""
    Write-Host "Possible solutions:" -ForegroundColor Yellow
    Write-Host "  1. PostgreSQL is not running on the server" -ForegroundColor Gray
    Write-Host "  2. Firewall is blocking port $PORT" -ForegroundColor Gray
    Write-Host "  3. PostgreSQL is not configured for remote connections" -ForegroundColor Gray
    Write-Host ""
    Write-Host "See POSTGRESQL_SERVER_SETUP.md for detailed setup instructions" -ForegroundColor Cyan
    exit 1
}

# Тест 3: Проверка DNS (для Prisma CDN)
Write-Host "[3/4] Checking Prisma CDN availability..." -ForegroundColor Yellow
try {
    $dnsResult = Resolve-DnsName -Name "binaries.prismacdn.com" -ErrorAction Stop
    Write-Host "  [OK] Prisma CDN is accessible" -ForegroundColor Green
    Write-Host "  [INFO] You can use 'npx prisma generate' normally" -ForegroundColor Cyan
} catch {
    Write-Host "  [WARNING] Prisma CDN is not accessible" -ForegroundColor Yellow
    Write-Host "  [INFO] Use 'node scripts/apply-migrations-direct.js' instead" -ForegroundColor Cyan
}

# Тест 4: Проверка подключения к базе данных
Write-Host "[4/4] Testing database connection..." -ForegroundColor Yellow

# Проверка наличия .env файла
if (-not (Test-Path ".env")) {
    Write-Host "  [WARNING] .env file not found" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Create .env file with:" -ForegroundColor Cyan
    Write-Host "DATABASE_URL=`"postgresql://${USERNAME}:YOUR_PASSWORD@${SERVER}:${PORT}/${DATABASE}`"" -ForegroundColor Gray
    Write-Host ""
} else {
    Write-Host "  [OK] .env file exists" -ForegroundColor Green
    
    # Попытка подключения через Node.js
    if (Test-Path "node_modules/pg") {
        Write-Host "  [INFO] Testing actual database connection..." -ForegroundColor Cyan
        
        $testScript = @"
const { Client } = require('pg');
require('dotenv').config();

const client = new Client({ connectionString: process.env.DATABASE_URL });

client.connect()
  .then(() => {
    console.log('  [OK] Database connection successful');
    return client.query('SELECT version()');
  })
  .then((res) => {
    console.log('  [INFO] PostgreSQL version:', res.rows[0].version.split(' ')[0], res.rows[0].version.split(' ')[1]);
    return client.end();
  })
  .catch((err) => {
    console.log('  [FAIL] Database connection failed:', err.message);
    process.exit(1);
  });
"@
        
        $testScript | Out-File -FilePath "temp_test_connection.js" -Encoding UTF8
        node temp_test_connection.js
        Remove-Item "temp_test_connection.js" -ErrorAction SilentlyContinue
    } else {
        Write-Host "  [INFO] pg module not installed. Run 'npm install' first" -ForegroundColor Cyan
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. If port test failed, setup PostgreSQL on server (see POSTGRESQL_SERVER_SETUP.md)" -ForegroundColor Gray
Write-Host "  2. Create .env file with DATABASE_URL" -ForegroundColor Gray
Write-Host "  3. Run: node scripts/apply-migrations-direct.js" -ForegroundColor Gray
Write-Host ""

