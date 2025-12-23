# Fix Git Branch and Push
Write-Host "=== Fixing Git branch and pushing ===" -ForegroundColor Cyan

# Check current branch
$currentBranch = git branch --show-current
Write-Host "Current branch: $currentBranch" -ForegroundColor Yellow

if ([string]::IsNullOrEmpty($currentBranch)) {
    Write-Host "No branch found. Creating main branch..." -ForegroundColor Yellow
    git branch -M main
    $currentBranch = "main"
}

Write-Host "Branch ready: $currentBranch" -ForegroundColor Green

# Push with upstream
Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
git push --set-upstream origin $currentBranch --force

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "==================================" -ForegroundColor Green
    Write-Host "  SUCCESS! Pushed to GitHub!" -ForegroundColor Green  
    Write-Host "==================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Repository: https://github.com/RubeRoid-creat/masterprofiapp" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "ERROR: Failed to push" -ForegroundColor Red
    Write-Host ""
    Write-Host "You may need to authenticate:" -ForegroundColor Yellow
    Write-Host "git config --global user.name 'Your Name'" -ForegroundColor White
    Write-Host "git config --global user.email 'your@email.com'" -ForegroundColor White
    Write-Host ""
}
