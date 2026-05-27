# Test MySQL root login before leader one-click import
param(
    [Parameter(Mandatory = $true)]
    [string]$Password,
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root"
)

$ErrorActionPreference = "Stop"
if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    Write-Host "ERR: mysql not in PATH." -ForegroundColor Red
    exit 1
}

$mysqlExe = (Get-Command mysql).Source
$env:MYSQL_PWD = $Password.Trim()
try {
    $out = & $mysqlExe -h $DbHost -P $Port -u $User --protocol=TCP -e "SELECT 1 AS ok;" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERR: MySQL login failed (check root password)." -ForegroundColor Red
        Write-Host $out -ForegroundColor Yellow
        Write-Host "Try: mysql -h 127.0.0.1 -u root -p" -ForegroundColor Cyan
        exit 1
    }
    Write-Host "[OK] MySQL login success" -ForegroundColor Green
    exit 0
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
