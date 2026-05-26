# Test shared MySQL connection
# Usage:
#   .\scripts\test-db-connection.ps1
#   .\scripts\test-db-connection.ps1 -DbHost 127.0.0.1 -Password csrrm_shared_123

param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "study",
    [string]$Password = "",
    [string]$Database = "study_room_reservation"
)

$ErrorActionPreference = "Continue"
Write-Host "=== MySQL connection test ===" -ForegroundColor Cyan
Write-Host "Target: ${DbHost}:${Port}  db=${Database}  user=${User}"
Write-Host ""

$tcp = Test-NetConnection -ComputerName $DbHost -Port $Port -WarningAction SilentlyContinue
if (-not $tcp.TcpTestSucceeded) {
    Write-Host "FAIL: cannot reach ${DbHost}:${Port}" -ForegroundColor Red
    Write-Host "      Check MySQL/Docker, firewall, IP." -ForegroundColor Red
    exit 1
}
Write-Host "OK: port $Port is open" -ForegroundColor Green

if (-not $Password) {
    Write-Host "SKIP: no -Password given, port check only." -ForegroundColor Yellow
    exit 0
}

if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    Write-Host "SKIP: mysql client not in PATH, port check only." -ForegroundColor Yellow
    exit 0
}

$env:MYSQL_PWD = $Password
try {
    $null = mysql -h $DbHost -P $Port -u $User $Database -N -e "SELECT 1;" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "OK: login succeeded" -ForegroundColor Green
    } else {
        Write-Host "FAIL: login failed (wrong user/password or DB missing)" -ForegroundColor Red
        exit 1
    }
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
