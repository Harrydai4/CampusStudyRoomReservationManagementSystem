# 环境自检：Java / Node / MySQL / 前端是否已构建
# 用法：在 CSRRMupdate 根目录执行  .\scripts\check-env.ps1

$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
Write-Host "=== CSRRMupdate 环境检查 ===" -ForegroundColor Cyan
Write-Host "项目根目录: $root`n"

function Test-Command($name, $cmd) {
    try {
        $out = Invoke-Expression $cmd 2>&1 | Select-Object -First 1
        Write-Host "[OK] $name : $out" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[--] $name : 未找到或执行失败" -ForegroundColor Yellow
        return $false
    }
}

Test-Command "Java" "java -version"
Test-Command "Node" "node -v"
Test-Command "npm" "npm -v"
Test-Command "MySQL 客户端" "mysql --version"

$staticIndex = Join-Path $root "src\main\resources\static\index.html"
if (Test-Path $staticIndex) {
    Write-Host "[OK] 前端已构建: $staticIndex" -ForegroundColor Green
} else {
    Write-Host "[!!] 前端未构建，请运行 .\scripts\build-frontend.ps1" -ForegroundColor Red
}

$uploads = Join-Path $root "uploads"
if (-not (Test-Path $uploads)) {
    New-Item -ItemType Directory -Path $uploads | Out-Null
    Write-Host "[OK] 已创建 uploads 目录" -ForegroundColor Green
}

Write-Host "`n=== 建议下一步 ===" -ForegroundColor Cyan
Write-Host "1. 确保 MySQL 8 已启动（或 docs\06-部署配置\docker-compose.yml）"
Write-Host "2. 有密码时: `$env:DB_PASSWORD='你的密码'"
Write-Host "3. .\mvnw.cmd spring-boot:run"
Write-Host "4. 浏览器 http://localhost:8080"
