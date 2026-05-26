# 导出课设/ GitHub 所需的数据库 SQL（结构 + 演示数据）
# 用法：.\scripts\export-database-for-git.ps1
#       .\scripts\export-database-for-git.ps1 -User root -Password 123456
#
# 输出到 docs/06-部署配置/：
#   schema.sql      — 仅表结构、视图、索引
#   data.sql        — 演示数据（INSERT）
#   database-full.sql — 结构+数据合一（便于「打包上传」）

param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = "",
    [string]$Database = "study_room_reservation"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root "docs\06-部署配置"

if (-not (Get-Command mysqldump -ErrorAction SilentlyContinue)) {
    Write-Host "ERR: mysqldump not in PATH. Add MySQL Server bin to PATH." -ForegroundColor Red
    exit 1
}

if (-not $Password) {
    $localProps = Join-Path $root "src\main\resources\application-local.properties"
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^\s*spring\.datasource\.password\s*=' } | Select-Object -First 1
        if ($line -match '=\s*(.+)$') { $Password = $Matches[1].Trim() }
    }
}
if (-not $Password) {
    $sec = Read-Host "MySQL password for $User" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}

$schemaFile = Join-Path $outDir "schema.sql"
$dataFile = Join-Path $outDir "data.sql"
$fullFile = Join-Path $outDir "database-full.sql"

$common = @("-h", $DbHost, "-P", $Port, "-u", $User, "--set-charset", "--default-character-set=utf8mb4", $Database)
$env:MYSQL_PWD = $Password

try {
    Write-Host "=== Export database for Git / course submission ===" -ForegroundColor Cyan
    & mysqldump @common --no-data --routines --triggers --result-file="$schemaFile"
    if ($LASTEXITCODE -ne 0) { throw "schema export failed" }
    Write-Host "OK: $schemaFile" -ForegroundColor Green

    & mysqldump @common --no-create-info --skip-triggers --complete-insert --result-file="$dataFile"
    if ($LASTEXITCODE -ne 0) { throw "data export failed" }
    Write-Host "OK: $dataFile" -ForegroundColor Green

    & mysqldump @common --routines --triggers --complete-insert --result-file="$fullFile"
    if ($LASTEXITCODE -ne 0) { throw "full export failed" }
    Write-Host "OK: $fullFile" -ForegroundColor Green

    Write-Host ""
    Write-Host "Commit these files with your v1.2 push:" -ForegroundColor Yellow
    Write-Host "  docs/06-部署配置/schema.sql"
    Write-Host "  docs/06-部署配置/data.sql"
    Write-Host "  docs/06-部署配置/database-full.sql"
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
