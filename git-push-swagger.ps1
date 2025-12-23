# Commit and push Swagger/OpenAPI documentation
Write-Host "=== Swagger/OpenAPI Documentation - Commit & Push ===" -ForegroundColor Cyan

Write-Host "Adding files..." -ForegroundColor Yellow
git add -A

Write-Host "Creating commit..." -ForegroundColor Yellow
$commitMessage = @"
Swagger/OpenAPI: Interactive API Documentation

SWAGGER INTEGRATION:
- swagger-jsdoc for OpenAPI 3.0 spec generation
- swagger-ui-express for interactive UI
- JWT Bearer authentication support
- Comprehensive schemas and responses

FEATURES:
- Interactive API testing from browser
- Auto-generated documentation from JSDoc
- JWT token authorization in UI
- Request/response examples
- OpenAPI 3.0 standard compliance

ENDPOINTS:
- GET /api-docs - Swagger UI interface
- GET /api-docs.json - OpenAPI JSON spec

DOCUMENTED:
- Authentication endpoints (register, login, me, refresh)
- User, Order, Master schemas
- Error responses
- Security definitions

CONFIGURATION:
- backend/swagger.js (200+ lines)
- 13 API categories/tags defined
- Production and dev server URLs
- Custom Swagger UI styling

DOCUMENTATION:
- backend/SWAGGER_API_DOCS_COMPLETE.md (comprehensive guide)
- backend/routes/auth.swagger.js (auth endpoints)

BENEFITS:
- Single source of truth for API
- Testing without Postman
- Auto client generation support
- Better developer onboarding

FILES:
+ backend/swagger.js (200+ lines)
* backend/server.js (+Swagger UI integration)
+ backend/routes/auth.swagger.js (180+ lines)
+ backend/SWAGGER_API_DOCS_COMPLETE.md (600+ lines)
* backend/package.json (added swagger dependencies)

Access: http://212.74.227.208:3000/api-docs
"@

git commit -m $commitMessage

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
    git push origin main
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor Green
        Write-Host "   SUCCESS! Swagger Documentation pushed!" -ForegroundColor Green
        Write-Host "==========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Repository: https://github.com/RubeRoid-creat/masterprofiapp" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Swagger UI: http://212.74.227.208:3000/api-docs" -ForegroundColor Yellow
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "ERROR: Failed to push" -ForegroundColor Red
    }
} else {
    Write-Host ""
    Write-Host "ERROR: Failed to commit" -ForegroundColor Red
}
