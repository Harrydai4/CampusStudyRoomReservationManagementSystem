# 从仓库 SQL 恢复本地 MySQL（每次一键启动默认先清空再导入）
# 用法：.\scripts\import-database-local.ps1
#       .\scripts\import-database-local.ps1 -Password 123456
#       .\scripts\import-database-local.ps1 -UseFullDump -CleanDatabase:$false  # 仅追加式导入（不推荐）

param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = "",
    [switch]$UseFullDump,
    [bool]$CleanDatabase = $true
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$cfgDir = Join-Path $root "docs\06-部署配置"
if (-not (Test-Path -LiteralPath $cfgDir)) {
    $docsDir = Join-Path $root "docs"
    if (Test-Path -LiteralPath $docsDir) {
        $found = Get-ChildItem -LiteralPath $docsDir -Directory |
            Where-Object { $_.Name -match '^06-' } | Select-Object -First 1
        if ($found) { $cfgDir = $found.FullName }
    }
}

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

$initLeaderSql = Join-Path $cfgDir "init-local-leader.sql"
$initSql = Join-Path $cfgDir "init-shared-mysql.sql"
$schemaSql = Join-Path $cfgDir "schema.sql"
$dataSql = Join-Path $cfgDir "data.sql"
$fullSql = Join-Path $cfgDir "database-full.sql"

$env:MYSQL_PWD = $Password
$dbName = "study_room_reservation"

function Invoke-SqlFile {
    param([string]$Path, [string]$DatabaseName = "")
    $mysqlExe = (Get-Command mysql).Source
    $args = @("-h", $DbHost, "-P", "$Port", "--protocol=TCP", "-u", $User)
    if ($DatabaseName) { $args += $DatabaseName }
    $argLine = ($args | ForEach-Object { if ($_ -match '\s') { "`"$_`"" } else { $_ } }) -join " "
    $cmd = "`"$mysqlExe`" $argLine < `"$Path`" 2>&1"
    $out = cmd /c $cmd
    if ($LASTEXITCODE -ne 0) {
        $msg = ($out | Out-String).Trim()
        if ($msg -match 'Access denied') {
            throw "MySQL Access denied: wrong root password or no permission. $msg"
        }
        throw "mysql failed ($Path): $msg"
    }
}

function Invoke-SqlCommand {
    param([string]$Sql, [string]$DatabaseName = "")
    $mysqlExe = (Get-Command mysql).Source
    $args = @("-h", $DbHost, "-P", "$Port", "--protocol=TCP", "-u", $User, "-N", "-B", "-e", $Sql)
    if ($DatabaseName) { $args += $DatabaseName }
    $out = & $mysqlExe @args 2>&1
    if ($LASTEXITCODE -ne 0) {
        $msg = ($out | Out-String).Trim()
        throw "mysql command failed: $msg"
    }
    return $out
}

function Reset-Database {
    Write-Host ">> Drop and recreate database: $dbName" -ForegroundColor Yellow
    Invoke-SqlCommand "DROP DATABASE IF EXISTS ``$dbName``;" | Out-Null
    if (Test-Path -LiteralPath $initLeaderSql) {
        Invoke-SqlFile -Path $initLeaderSql
    } else {
        Invoke-SqlCommand "CREATE DATABASE ``$dbName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
    }
    Write-Host "OK: database $dbName is empty and ready for import" -ForegroundColor Green
}

try {
    Write-Host "=== Import CSRRM database from repo SQL ===" -ForegroundColor Cyan

    if ($UseFullDump -and (Test-Path -LiteralPath $fullSql)) {
        if ($CleanDatabase) {
            Reset-Database
        } else {
            if (Test-Path -LiteralPath $initLeaderSql) {
                Invoke-SqlFile -Path $initLeaderSql
            } elseif (Test-Path -LiteralPath $initSql) {
                Write-Host "WARN: using init-shared-mysql.sql (needs CREATE USER privilege)" -ForegroundColor Yellow
                Invoke-SqlFile -Path $initSql
            }
        }
        Invoke-SqlFile -Path $fullSql -DatabaseName $dbName
        Write-Host "OK: imported database-full.sql (clean snapshot restored)" -ForegroundColor Green
        Write-Host "Tip: app.demo.sync-accounts-on-startup=false keeps SQL demo data unchanged" -ForegroundColor Yellow
        Write-Host "Demo login: student 202225220101 / 123456 , admin admin / admin123" -ForegroundColor Yellow
        return
    }

    foreach ($f in @($initSql, $schemaSql, $dataSql)) {
        if (-not (Test-Path $f)) {
            Write-Host "ERR: missing $f - run export-database-for-git.ps1 on a machine with data first." -ForegroundColor Red
            exit 1
        }
    }

    if ($CleanDatabase) {
        Reset-Database
    } else {
        Invoke-SqlFile -Path $initSql
    }
    Invoke-SqlFile -Path $schemaSql -DatabaseName $dbName
    Invoke-SqlFile -Path $dataSql -DatabaseName $dbName
    Write-Host "OK: init + schema + data imported" -ForegroundColor Green
    Write-Host "Demo login: student 202225220101 / 123456 , admin admin / admin123" -ForegroundColor Yellow
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
