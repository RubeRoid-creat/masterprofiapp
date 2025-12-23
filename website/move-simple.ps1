# Simple move script - run in PowerShell directly

$target = "Z:\BestAPP\website"

Write-Host "Creating target directory..." -ForegroundColor Green
New-Item -ItemType Directory -Path $target -Force | Out-Null

Write-Host "Moving files (excluding node_modules, .next, .git)..." -ForegroundColor Green
Get-ChildItem -Path . -Exclude node_modules, .next, .git | Move-Item -Destination $target -Force

Write-Host "Done! Files moved to: $target" -ForegroundColor Green
Write-Host "Next: cd $target" -ForegroundColor Yellow

