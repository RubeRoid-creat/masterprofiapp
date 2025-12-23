# Script for creating website user

param(
    [string]$Email = "website@ispravleno.ru",
    [string]$Password = "Website2024!",
    [string]$Name = "Website Bot",
    [string]$Phone = "+79999999999",
    [string]$ApiUrl = "http://212.74.227.208:3000/api"
)

Write-Host "Creating website user..." -ForegroundColor Green
Write-Host "Email: $Email" -ForegroundColor Yellow
Write-Host "API URL: $ApiUrl" -ForegroundColor Yellow

$body = @{
    email = $Email
    password = $Password
    name = $Name
    phone = $Phone
    role = "client"
} | ConvertTo-Json

Write-Host ""
Write-Host "Sending registration request..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "$ApiUrl/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $body
    
    if ($response.token) {
        Write-Host ""
        Write-Host "User created successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "User data:" -ForegroundColor Yellow
        Write-Host "  ID: $($response.user.id)" -ForegroundColor Cyan
        Write-Host "  Email: $($response.user.email)" -ForegroundColor Cyan
        Write-Host "  Role: $($response.user.role)" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Token:" -ForegroundColor Yellow
        Write-Host "  $($response.token)" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Add to .env file:" -ForegroundColor Yellow
        Write-Host "  ADMIN_USER_TOKEN=`"$($response.token)`"" -ForegroundColor Green
        Write-Host ""
        
        # Copy to clipboard
        try {
            $response.token | Set-Clipboard
            Write-Host "Token copied to clipboard!" -ForegroundColor Green
        } catch {
            Write-Host "Could not copy to clipboard" -ForegroundColor Yellow
        }
        
        # Ask to add to .env automatically
        Write-Host "Add token to .env automatically? (Y/N)" -ForegroundColor Yellow
        $answer = Read-Host
        if ($answer -eq 'Y' -or $answer -eq 'y') {
            .\setup-token.ps1 -Token $response.token
        }
    } else {
        Write-Host "Error: token not received" -ForegroundColor Red
        Write-Host "Server response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Yellow
    }
} catch {
    Write-Host ""
    Write-Host "Error creating user:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($errorDetails) {
            Write-Host "Error details:" -ForegroundColor Yellow
            Write-Host "  $($errorDetails.error)" -ForegroundColor Red
            if ($errorDetails.error -like "*already exists*" -or $errorDetails.error -like "*уже существует*") {
                Write-Host ""
                Write-Host "User already exists. Try to login:" -ForegroundColor Yellow
                Write-Host "  .\get-token.ps1 -Email `"$Email`" -Password `"$Password`"" -ForegroundColor Cyan
            }
        } else {
            Write-Host "Details: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
        }
    }
}

