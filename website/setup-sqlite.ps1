# Setup with SQLite (no PostgreSQL needed)

Write-Host "Setting up project with SQLite..." -ForegroundColor Green

# Check if schema needs to be updated
$schemaContent = Get-Content "prisma/schema.prisma" -Raw
if ($schemaContent -notmatch 'provider = "sqlite"') {
    Write-Host "Updating Prisma schema to use SQLite..." -ForegroundColor Yellow
    
    # Backup original schema
    Copy-Item "prisma/schema.prisma" "prisma/schema.prisma.backup"
    
    # Update schema
    $newSchema = $schemaContent -replace 'provider = "postgresql"', 'provider = "sqlite"' -replace 'url\s*=\s*env\("DATABASE_URL"\)', 'url      = "file:./dev.db"'
    Set-Content "prisma/schema.prisma" -Value $newSchema
    
    Write-Host "Schema updated. Original saved as schema.prisma.backup" -ForegroundColor Green
}

# Create .env if it doesn't exist
if (-not (Test-Path ".env")) {
    Write-Host "Creating .env file..." -ForegroundColor Yellow
    @"
DATABASE_URL="file:./dev.db"
NEXT_PUBLIC_YANDEX_MAPS_API_KEY="your_api_key"
NEXT_PUBLIC_SITE_URL="http://localhost:3000"
"@ | Out-File -FilePath ".env" -Encoding UTF8
    Write-Host ".env file created" -ForegroundColor Green
}

# Install Prisma if needed
Write-Host "Installing Prisma..." -ForegroundColor Yellow
npm install @prisma/client prisma --legacy-peer-deps

# Generate Prisma client
Write-Host "Generating Prisma client..." -ForegroundColor Yellow
npx prisma generate

# Run migrations
Write-Host "Running migrations..." -ForegroundColor Yellow
npx prisma migrate dev --name init

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green
Write-Host "You can now run: npm run dev" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: SQLite is for development only. Use PostgreSQL for production." -ForegroundColor Yellow

