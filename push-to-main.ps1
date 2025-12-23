# Push to main branch (GitHub default)
Write-Host "=== Fixing branch name and pushing to main ===" -ForegroundColor Cyan

# Rename master to main
Write-Host "Renaming master -> main..." -ForegroundColor Yellow
git branch -M main

# Push to main
Write-Host "Pushing to main branch..." -ForegroundColor Yellow
git push -u origin main --force

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=============================================" -ForegroundColor Green
    Write-Host "   SUCCESS! Pushed to main branch!" -ForegroundColor Green
    Write-Host "=============================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Check your repository:" -ForegroundColor Cyan
    Write-Host "https://github.com/RubeRoid-creat/masterprofiapp" -ForegroundColor White
    Write-Host ""
    Write-Host "Files pushed:" -ForegroundColor Yellow
    Write-Host "  - backend/middleware/rate-limiter.js" -ForegroundColor White
    Write-Host "  - backend/middleware/security.js" -ForegroundColor White
    Write-Host "  - backend/SECURITY_SETUP.md" -ForegroundColor White
    Write-Host "  - backend/HTTPS_SETUP_GUIDE.md" -ForegroundColor White
    Write-Host "  - backend/VARIANT_1_COMPLETE.md" -ForegroundColor White
    Write-Host "  - CRITICAL_FIXES_COMPLETE.md" -ForegroundColor White
    Write-Host "  + All other project files" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "ERROR: Failed to push" -ForegroundColor Red
    Write-Host ""
}
