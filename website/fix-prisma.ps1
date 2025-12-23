# Script for fixing Prisma generate issue

Write-Host "Attempting to fix Prisma issue..." -ForegroundColor Yellow

# Set environment variable for Prisma mirror
Write-Host "Setting Prisma environment variable..." -ForegroundColor Green
$env:PRISMA_ENGINES_MIRROR = "https://binaries.prisma.sh"

# Try generation with retries
Write-Host "Generating Prisma client..." -ForegroundColor Green
$maxAttempts = 3
$attempt = 1

while ($attempt -le $maxAttempts) {
    Write-Host "Attempt $attempt of $maxAttempts..." -ForegroundColor Yellow
    try {
        npx prisma generate
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Success!" -ForegroundColor Green
            break
        }
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
    }
    
    if ($attempt -lt $maxAttempts) {
        Write-Host "Waiting 5 seconds before retry..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
    $attempt++
}

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Failed to generate Prisma client automatically." -ForegroundColor Red
    Write-Host "Try the following options:" -ForegroundColor Yellow
    Write-Host "1. Use Docker: docker-compose up -d" -ForegroundColor Cyan
    Write-Host "2. Download engines manually from https://github.com/prisma/prisma-engines/releases" -ForegroundColor Cyan
    Write-Host "3. Use VPN or different network" -ForegroundColor Cyan
    Write-Host "4. Work temporarily without DB (interface will work)" -ForegroundColor Cyan
}

