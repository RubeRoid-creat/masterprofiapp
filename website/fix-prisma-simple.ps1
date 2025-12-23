# Simple Prisma fix script

$env:PRISMA_ENGINES_MIRROR = "https://binaries.prisma.sh"
Write-Host "Trying to generate Prisma client..." -ForegroundColor Green
npx prisma generate

if ($LASTEXITCODE -eq 0) {
    Write-Host "Success!" -ForegroundColor Green
} else {
    Write-Host "Failed. Try: docker-compose up -d" -ForegroundColor Red
}

