# 构建前端并输出到 src/main/resources/static
# 用法：.\scripts\build-frontend.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root "frontend"

Write-Host "=== Build frontend ===" -ForegroundColor Cyan
Set-Location $frontend

if (-not (Test-Path "node_modules")) {
    Write-Host "npm install ..."
    npm install
}

Write-Host "npm run build ..."
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "vite 失败，尝试备用命令 ..." -ForegroundColor Yellow
    node .\node_modules\vite\bin\vite.js build --outDir ..\src\main\resources\static --emptyOutDir
}

Write-Host "[OK] Build done. Restart Spring Boot, then open http://localhost:8080" -ForegroundColor Green
Write-Host "Next: cd `"$root`"; .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
Set-Location $root
