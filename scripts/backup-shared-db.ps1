# Backup shared MySQL database
# Usage: .\scripts\backup-shared-db.ps1 -Password csrrm_shared_123

param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "study",
    [string]$Password = "csrrm_shared_123",
    [string]$Database = "study_room_reservation"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backupDir = Join-Path $root "backups"
if (-not (Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir | Out-Null }

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outFile = Join-Path $backupDir "${Database}-${stamp}.sql"

if (-not (Get-Command mysqldump -ErrorAction SilentlyContinue)) {
    Write-Host "ERR: mysqldump not found. Add MySQL bin to PATH." -ForegroundColor Red
    exit 1
}

$env:MYSQL_PWD = $Password
try {
    mysqldump -h $DbHost -P $Port -u $User --single-transaction --routines --triggers $Database | Set-Content -Path $outFile -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw "mysqldump failed" }
    Write-Host "OK: backup saved to $outFile" -ForegroundColor Green
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

Write-Host "Also backup the uploads/ folder on the app server." -ForegroundColor Yellow
