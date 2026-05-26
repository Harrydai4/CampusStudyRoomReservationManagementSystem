# 从仓库 SQL 恢复本地 MySQL（clone 后首次导入）
# 用法：.\scripts\import-database-local.ps1
#       .\scripts\import-database-local.ps1 -Password 123456

param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = "",
    [switch]$UseFullDump
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$cfgDir = Join-Path $root "docs\06-部署配置"

if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    Write-Host "ERR: mysql not in PATH." -ForegroundColor Red
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

$initSql = Join-Path $cfgDir "init-shared-mysql.sql"
$schemaSql = Join-Path $cfgDir "schema.sql"
$dataSql = Join-Path $cfgDir "data.sql"
$fullSql = Join-Path $cfgDir "database-full.sql"

$env:MYSQL_PWD = $Password

function Invoke-SqlFile {
    param([string]$Path, [string]$DatabaseName = "")
    $mysqlExe = (Get-Command mysql).Source
    $args = @("-h", $DbHost, "-P", "$Port", "-u", $User)
    if ($DatabaseName) { $args += $DatabaseName }
    $argLine = ($args | ForEach-Object { if ($_ -match '\s') { "`"$_`"" } else { $_ } }) -join " "
    $cmd = "`"$mysqlExe`" $argLine < `"$Path`""
    cmd /c $cmd
    if ($LASTEXITCODE -ne 0) { throw "mysql failed: $Path" }
}

try {
    Write-Host "=== Import CSRRM database from repo SQL ===" -ForegroundColor Cyan

    if ($UseFullDump -and (Test-Path $fullSql)) {
        if (Test-Path $initSql) { Invoke-SqlFile -Path $initSql }
        Invoke-SqlFile -Path $fullSql -DatabaseName "study_room_reservation"
        Write-Host "OK: imported database-full.sql" -ForegroundColor Green
        return
    }

    foreach ($f in @($initSql, $schemaSql, $dataSql)) {
        if (-not (Test-Path $f)) {
            Write-Host "ERR: missing $f — run export-database-for-git.ps1 on a machine with data first." -ForegroundColor Red
            exit 1
        }
    }

    Invoke-SqlFile -Path $initSql
    Invoke-SqlFile -Path $schemaSql -DatabaseName "study_room_reservation"
    Invoke-SqlFile -Path $dataSql -DatabaseName "study_room_reservation"
    Write-Host "OK: init + schema + data imported" -ForegroundColor Green
    Write-Host "Demo login: student 202301010101 / 123456 , admin admin / admin123" -ForegroundColor Yellow
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
