# CSRRM shared MySQL via Docker
# Run from project root: .\scripts\setup-shared-mysql-docker.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$composeDir = Join-Path $root "docs\06-部署配置"

Write-Host "=== CSRRM shared MySQL (Docker) ===" -ForegroundColor Cyan
Write-Host "Dir: $composeDir"
Write-Host ""

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "ERR: docker not found. Install Docker Desktop first." -ForegroundColor Red
    exit 1
}

Push-Location $composeDir
try {
    docker compose -f docker-compose.shared.yml up -d
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "OK: MySQL container started." -ForegroundColor Green
Write-Host ""
Write-Host "Default credentials:"
Write-Host "  host: localhost or this PC LAN IP"
Write-Host "  port: 3306"
Write-Host "  database: study_room_reservation"
Write-Host "  user: study"
Write-Host "  password: csrrm_shared_123"
Write-Host "  root password: csrrm_root_123"
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. ipconfig  (find IPv4 for teammates)"
Write-Host "  2. Edit src\main\resources\application-shared.properties"
Write-Host "     password = csrrm_shared_123"
Write-Host "  3. .\scripts\test-db-connection.ps1 -Password csrrm_shared_123"
Write-Host "  4. .\scripts\start-shared-server.ps1"
Write-Host ""
Write-Host "Doc: docs\01-使用指南\07-多人共用一套系统与数据库.md" -ForegroundColor Yellow
