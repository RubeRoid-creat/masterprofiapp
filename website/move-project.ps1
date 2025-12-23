# Script to move project to Z:\BestAPP\website

# Use current directory as source (more reliable with Cyrillic paths)
$sourcePath = (Get-Location).Path
$targetPath = "Z:\BestAPP\website"

Write-Host "Moving project from $sourcePath to $targetPath" -ForegroundColor Green
Write-Host ""

# Check if source exists
if (-not (Test-Path $sourcePath)) {
    Write-Host "Error: Source path does not exist: $sourcePath" -ForegroundColor Red
    exit 1
}

# Create target directory
if (-not (Test-Path $targetPath)) {
    Write-Host "Creating target directory: $targetPath" -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $targetPath -Force | Out-Null
} else {
    Write-Host "Target directory already exists: $targetPath" -ForegroundColor Yellow
    $answer = Read-Host "Continue? (Y/N)"
    if ($answer -ne 'Y' -and $answer -ne 'y') {
        Write-Host "Cancelled." -ForegroundColor Yellow
        exit 0
    }
}

# Check if server is running
Write-Host ""
Write-Host "IMPORTANT: Make sure Next.js server is stopped!" -ForegroundColor Red
Write-Host "Press any key to continue or Ctrl+C to cancel..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# Move files
Write-Host ""
Write-Host "Moving files..." -ForegroundColor Cyan

try {
    # Get all items except node_modules, .next, and other build artifacts
    $items = Get-ChildItem -Path $sourcePath -Exclude node_modules, .next, .git
    
    foreach ($item in $items) {
        $destination = Join-Path $targetPath $item.Name
        Write-Host "  Moving: $($item.Name)" -ForegroundColor Gray
        Move-Item -Path $item.FullName -Destination $destination -Force
    }
    
    Write-Host ""
    Write-Host "Files moved successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. cd $targetPath" -ForegroundColor Cyan
    Write-Host "2. Remove node_modules and .next if needed:" -ForegroundColor Cyan
    Write-Host "   Remove-Item -Recurse -Force node_modules, .next" -ForegroundColor Gray
    Write-Host "3. Reinstall dependencies:" -ForegroundColor Cyan
    Write-Host "   npm install --legacy-peer-deps" -ForegroundColor Gray
    Write-Host "4. Start server:" -ForegroundColor Cyan
    Write-Host "   npm run dev" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Old directory: $sourcePath" -ForegroundColor Yellow
    Write-Host "You can delete it after verifying everything works." -ForegroundColor Yellow
    
} catch {
    Write-Host ""
    Write-Host "Error moving files:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

