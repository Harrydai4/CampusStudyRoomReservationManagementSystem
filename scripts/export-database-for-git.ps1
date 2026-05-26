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

if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

$common = @("-h", $DbHost, "-P", $Port, "-u", $User, "--set-charset", "--default-character-set=utf8mb4", $Database)
$env:MYSQL_PWD = $Password

function Invoke-MysqldumpFile {
    param([string[]]$ExtraArgs, [string]$OutPath)
    $tmp = [System.IO.Path]::GetTempFileName()
    try {
        & mysqldump @common @ExtraArgs --result-file="$tmp"
        if ($LASTEXITCODE -ne 0) { throw "mysqldump failed" }
        Move-Item -Path $tmp -Destination $OutPath -Force
    } finally {
        if (Test-Path $tmp) { Remove-Item $tmp -Force -ErrorAction SilentlyContinue }
    }
}

try {
    Write-Host "=== Export database for Git / course submission ===" -ForegroundColor Cyan
    Invoke-MysqldumpFile -ExtraArgs @("--no-data", "--routines", "--triggers") -OutPath $schemaFile
    Write-Host "OK: $schemaFile" -ForegroundColor Green

    Invoke-MysqldumpFile -ExtraArgs @("--no-create-info", "--skip-triggers", "--complete-insert") -OutPath $dataFile
    Write-Host "OK: $dataFile" -ForegroundColor Green

    Invoke-MysqldumpFile -ExtraArgs @("--routines", "--triggers", "--complete-insert") -OutPath $fullFile
    Write-Host "OK: $fullFile" -ForegroundColor Green

    Write-Host ""
    Write-Host "Commit these files with your v1.2 push:" -ForegroundColor Yellow
    Write-Host "  docs/06-部署配置/schema.sql"
    Write-Host "  docs/06-部署配置/data.sql"
    Write-Host "  docs/06-部署配置/database-full.sql"
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
