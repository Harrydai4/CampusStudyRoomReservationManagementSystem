# Start CSRRM for LAN / shared team access
# Run from project root: .\scripts\start-shared-server.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$sharedProps = Join-Path $root "src\main\resources\application-shared.properties"
$sharedExample = Join-Path $root "src\main\resources\application-shared.properties.example"

if (-not (Test-Path $sharedProps)) {
    Write-Host "WARN: application-shared.properties missing, copying from example..." -ForegroundColor Yellow
    Copy-Item $sharedExample $sharedProps
    Write-Host "ERR: edit password in application-shared.properties then run again." -ForegroundColor Red
    notepad $sharedProps
    exit 1
}

$propsText = Get-Content $sharedProps -Raw -Encoding UTF8
if ($propsText -match '在这里填写|password=\s*$|password=\s*#') {
    Write-Host "ERR: set spring.datasource.password in application-shared.properties" -ForegroundColor Red
    Write-Host "     Docker default: csrrm_shared_123" -ForegroundColor Yellow
    notepad $sharedProps
    exit 1
}

$uploadDir = Join-Path $root "uploads"
if (-not (Test-Path $uploadDir)) {
    New-Item -ItemType Directory -Path $uploadDir | Out-Null
}
$env:UPLOAD_DIR = $uploadDir
$env:SERVER_ADDRESS = "0.0.0.0"
$env:SERVER_PORT = "8080"

Write-Host "=== CSRRM shared server ===" -ForegroundColor Cyan
Write-Host "Root:   $root"
Write-Host "Upload: $uploadDir"
Write-Host "Config: application-shared.properties"
Write-Host ""

$ipv4 = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.PrefixOrigin -ne "WellKnown" } |
    Select-Object -First 1 -ExpandProperty IPAddress

if ($ipv4) {
    Write-Host "Team access URL:" -ForegroundColor Green
    Write-Host "  http://${ipv4}:8080" -ForegroundColor Green
} else {
    Write-Host "WARN: run ipconfig to find your IPv4" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Press Ctrl+C to stop."
Write-Host ""
.\mvnw.cmd spring-boot:run
