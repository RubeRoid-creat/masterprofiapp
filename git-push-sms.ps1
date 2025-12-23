# Commit and push SMS integration
Write-Host "=== SMS Integration - Commit & Push ===" -ForegroundColor Cyan

Write-Host "Adding files..." -ForegroundColor Yellow
git add -A

Write-Host "Creating commit..." -ForegroundColor Yellow
$commitMessage = @"
SMS Integration: Production-ready SMS.ru + Twilio

SMS SERVICE:
- Full SMS.ru API integration (native HTTPS, no npm packages)
- Twilio fallback mechanism
- Phone number validation and formatting
- Balance checking (SMS.ru)
- SMS status tracking
- Service verification

ADMIN ENDPOINTS:
- GET /api/admin/services/sms/status - Check SMS service
- GET /api/admin/services/sms/balance - Check balance
- GET /api/admin/services/health - Check all services

TESTING:
- backend/scripts/test-sms.js - Full test script

DOCUMENTATION:
- backend/SMS_SETUP_GUIDE.md (600+ lines)
- backend/SMS_INTEGRATION_COMPLETE.md

FILES:
+ backend/SMS_SETUP_GUIDE.md
+ backend/scripts/test-sms.js  
+ backend/SMS_INTEGRATION_COMPLETE.md
* backend/services/sms-service.js (+200 lines)
* backend/routes/admin.js (+80 lines)

Ready for production with SMS.ru!
"@

git commit -m $commitMessage

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
    git push origin main
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor Green
        Write-Host "   SUCCESS! SMS Integration pushed!" -ForegroundColor Green
        Write-Host "==========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Repository: https://github.com/RubeRoid-creat/masterprofiapp" -ForegroundColor Cyan
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "ERROR: Failed to push" -ForegroundColor Red
    }
} else {
    Write-Host ""
    Write-Host "ERROR: Failed to commit" -ForegroundColor Red
}
