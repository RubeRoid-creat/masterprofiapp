# Simple Git Push Script
Write-Host "Git Push - Variant 1 Security Fixes" -ForegroundColor Cyan

# Check if .git exists
if (-Not (Test-Path ".git")) {
    Write-Host "Initializing git repository..." -ForegroundColor Yellow
    git init
    git remote add origin https://github.com/RubeRoid-creat/masterprofiapp.git
}

Write-Host "Adding files..." -ForegroundColor Yellow
git add -A

Write-Host "Creating commit..." -ForegroundColor Yellow
git commit -m "Variant 1: Security fixes - Rate limiting, SMTP email, security middleware, HTTPS redirect, documentation"

Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
git push -u origin main

Write-Host "Done!" -ForegroundColor Green
Write-Host "https://github.com/RubeRoid-creat/masterprofiapp" -ForegroundColor Cyan
