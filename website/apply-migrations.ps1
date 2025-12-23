# PowerShell script for applying database migrations
# Uses Node.js and Prisma

Write-Host "Applying migrations for website tables..." -ForegroundColor Green
Write-Host ""

# Check for Node.js
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Node.js not found. Please install Node.js to continue." -ForegroundColor Red
    exit 1
}

# Check for .env file
if (-not (Test-Path ".env")) {
    Write-Host "[WARNING] .env file not found. Create it with DATABASE_URL settings" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Example .env:" -ForegroundColor Cyan
    Write-Host 'DATABASE_URL="postgresql://user:password@localhost:5432/bestapp"' -ForegroundColor Gray
    exit 1
}

# Check for node_modules
if (-not (Test-Path "node_modules")) {
    Write-Host "[INFO] Installing dependencies (this may take a few minutes)..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to install dependencies" -ForegroundColor Red
        exit 1
    }
}

# Check for Prisma client
if (-not (Test-Path "node_modules/@prisma/client")) {
    Write-Host "[INFO] Prisma client not found, installing..." -ForegroundColor Yellow
    npm install @prisma/client prisma
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to install Prisma" -ForegroundColor Red
        exit 1
    }
}

# Generate Prisma client
if (-not (Test-Path "node_modules/.prisma")) {
    Write-Host "[INFO] Generating Prisma client..." -ForegroundColor Yellow
    npx prisma generate
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to generate Prisma client" -ForegroundColor Red
        Write-Host "[TIP] Make sure DATABASE_URL in .env file is configured correctly" -ForegroundColor Yellow
        exit 1
    }
}

# Apply migrations via Node.js script
Write-Host "[INFO] Applying migrations..." -ForegroundColor Yellow
node scripts/apply-migrations.js

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[SUCCESS] Migrations applied successfully!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[ERROR] Failed to apply migrations" -ForegroundColor Red
    exit 1
}
