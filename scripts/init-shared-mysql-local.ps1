# One-shot: init shared MySQL on THIS PC (uses root password from application-local.properties)
# Run from project root: .\scripts\init-shared-mysql-local.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$localProps = Join-Path $root "src\main\resources\application-local.properties"
$sqlFile = Get-ChildItem -Path (Join-Path $root "docs") -Recurse -Filter "init-shared-mysql.sql" -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $sqlFile -or -not (Test-Path $sqlFile)) {
    $sqlFile = Join-Path $PSScriptRoot "init-shared-mysql.sql"
}
if (-not (Test-Path $sqlFile)) {
    Write-Host "ERR: init-shared-mysql.sql not found under docs/ or scripts/" -ForegroundColor Red
    exit 1
}
$sharedProps = Join-Path $root "src\main\resources\application-shared.properties"
$sharedExample = Join-Path $root "src\main\resources\application-shared.properties.example"

$rootPassword = ""
if (Test-Path $localProps) {
    $line = Select-String -Path $localProps -Pattern "^\s*spring\.datasource\.password=(.+)$" | Select-Object -First 1
    if ($line) { $rootPassword = $line.Matches.Groups[1].Value.Trim() }
}
if (-not $rootPassword) {
    Write-Host "ERR: set spring.datasource.password in application-local.properties (root password)" -ForegroundColor Red
    exit 1
}

$mysqlCandidates = @(
    "D:\tools\MySQL\MySQL Server 8.0\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)
$mysql = $mysqlCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $mysql) {
    $mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
}
if (-not $mysql) {
    Write-Host "ERR: mysql.exe not found" -ForegroundColor Red
    exit 1
}

Write-Host "=== Init shared MySQL ===" -ForegroundColor Cyan
$env:MYSQL_PWD = $rootPassword
Get-Content $sqlFile -Encoding UTF8 | & $mysql -u root
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERR: init SQL failed (check root password)" -ForegroundColor Red
    exit 1
}
Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue

if (-not (Test-Path $sharedProps)) {
    Copy-Item $sharedExample $sharedProps
}
$uploadDir = ($root -replace '\\', '/') + "/uploads"
@"
# Shared MySQL (auto-generated). Do NOT commit.

spring.datasource.url=jdbc:mysql://127.0.0.1:3306/study_room_reservation?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
spring.datasource.username=study
spring.datasource.password=csrrm_shared_123

app.upload.dir=$uploadDir

app.demo.sync-accounts-on-startup=true
"@ | Set-Content $sharedProps -Encoding UTF8

$env:MYSQL_PWD = "csrrm_shared_123"
& $mysql -h 127.0.0.1 -u study study_room_reservation -e "SELECT 1 AS ok;"
Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERR: study user test failed" -ForegroundColor Red
    exit 1
}

$lanIp = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.PrefixOrigin -ne "WellKnown" } |
    Select-Object -First 1 -ExpandProperty IPAddress

Write-Host ""
Write-Host "OK: shared database ready." -ForegroundColor Green
Write-Host ""
Write-Host "MySQL (for teammates / JDBC):"
Write-Host "  host: $lanIp  (same WiFi) or 127.0.0.1 (this PC only)"
Write-Host "  port: 3306"
Write-Host "  database: study_room_reservation"
Write-Host "  user: study"
Write-Host "  password: csrrm_shared_123"
Write-Host ""
Write-Host "Start web for team: .\scripts\start-shared-server.ps1"
Write-Host "  then open http://${lanIp}:8080"
