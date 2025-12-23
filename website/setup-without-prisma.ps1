# Setup project without Prisma (for testing UI)

Write-Host "Setting up project without Prisma..." -ForegroundColor Yellow
Write-Host "This will allow you to test the UI, but API endpoints won't work." -ForegroundColor Yellow

# Install dependencies without Prisma
Write-Host "Installing dependencies..." -ForegroundColor Green
npm install --legacy-peer-deps --ignore-scripts

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green
Write-Host "You can now run: npm run dev" -ForegroundColor Green
Write-Host "Note: API endpoints will return errors, but UI will work." -ForegroundColor Yellow

