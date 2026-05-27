# One-click after git clone: import DB snapshot + start app
# Usage: .\scripts\setup-after-clone.ps1 [-MySqlPassword pwd] [-SkipImport] [-SkipStart]

param(
    [string]$MySqlPassword = "",
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [switch]$SkipImport,
    [switch]$SkipStart,
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host ">> $msg" -ForegroundColor Cyan
}

function Require-Cmd([string]$name, [string]$hint) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        Write-Host "ERR: $name not found. $hint" -ForegroundColor Red
        exit 1
    }
}

function Get-DeployConfigDir([string]$projectRoot) {
    $cfgDir = Join-Path $projectRoot "docs\06-部署配置"
    if (Test-Path -LiteralPath $cfgDir) { return $cfgDir }
    $docsDir = Join-Path $projectRoot "docs"
    if (Test-Path -LiteralPath $docsDir) {
        $found = Get-ChildItem -LiteralPath $docsDir -Directory |
            Where-Object { $_.Name -match '^06-' } |
            Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $cfgDir
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " CSRRM setup after clone" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Step "1/6 Check Java and MySQL client"
Require-Cmd "java" "Install JDK 21+"
Require-Cmd "mysql" "Install MySQL 8 and add to PATH"

$staticIndex = Join-Path $root "src\main\resources\static\index.html"
if (-not (Test-Path $staticIndex)) {
    Write-Host "static/ missing, building frontend..." -ForegroundColor Yellow
    if (Get-Command npm -ErrorAction SilentlyContinue) {
        & (Join-Path $PSScriptRoot "build-frontend.ps1")
    } else {
        Write-Host "ERR: No Node.js and no prebuilt static." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[OK] frontend static found" -ForegroundColor Green
}

Write-Step "2/6 application-local.properties"
$localProps = Join-Path $root "src\main\resources\application-local.properties"
$example = Join-Path $root "src\main\resources\application-local.properties.example"
if (-not (Test-Path $localProps)) {
    Copy-Item $example $localProps
}

if (-not $MySqlPassword) {
    foreach ($line in Get-Content $localProps) {
        if ($line -like "spring.datasource.password=*") {
            $val = $line.Substring("spring.datasource.password=".Length).Trim()
            if ($val.Length -gt 0) { $MySqlPassword = $val }
        }
    }
}
if (-not $MySqlPassword) {
    $prompt = "Enter MySQL password for " + $User + " (stored locally only): "
    $sec = Read-Host $prompt -AsSecureString
    $MySqlPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}

$uploadDir = Join-Path $root "uploads"
$outLines = New-Object System.Collections.Generic.List[string]
$hasPwd = $false
$hasUpload = $false
$hasDemoSync = $false
foreach ($line in Get-Content $localProps) {
    if ($line -like "spring.datasource.password=*") {
        [void]$outLines.Add("spring.datasource.password=" + $MySqlPassword)
        $hasPwd = $true
    } elseif ($line -like "app.upload.dir=*") {
        [void]$outLines.Add("app.upload.dir=" + $uploadDir)
        $hasUpload = $true
    } elseif ($line -like "app.demo.sync-accounts-on-startup=*") {
        [void]$outLines.Add("app.demo.sync-accounts-on-startup=false")
        $hasDemoSync = $true
    } else {
        [void]$outLines.Add($line)
    }
}
if (-not $hasPwd) { [void]$outLines.Add("spring.datasource.password=" + $MySqlPassword) }
if (-not $hasUpload) { [void]$outLines.Add("app.upload.dir=" + $uploadDir) }
if (-not $hasDemoSync) { [void]$outLines.Add("app.demo.sync-accounts-on-startup=false") }
$outLines | Set-Content -Path $localProps -Encoding UTF8
Write-Host "[OK] local config ready (demo sync off = keep SQL data)" -ForegroundColor Green

if (-not $SkipImport) {
    Write-Step "3/6 Import database-full.sql"
    $cfgDir = Get-DeployConfigDir $root
    $fullSql = Join-Path $cfgDir "database-full.sql"
    if (-not (Test-Path -LiteralPath $fullSql)) {
        Write-Host "ERR: missing database-full.sql" -ForegroundColor Red
        Write-Host "      expected: $fullSql" -ForegroundColor Yellow
        Write-Host "      Re-download full ZIP from GitHub." -ForegroundColor Yellow
        exit 1
    }
    & (Join-Path $PSScriptRoot "import-database-local.ps1") -Password $MySqlPassword -User $User -DbHost $DbHost -Port $Port -UseFullDump
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Step "3/6 Skip import"
}

Write-Step "4/6 Check uploads"
if (-not (Test-Path $uploadDir)) { New-Item -ItemType Directory -Path $uploadDir | Out-Null }
$cnt = 0
Get-ChildItem $uploadDir -Recurse -File | ForEach-Object {
    if ($_.Name -ne "README.md" -and $_.Name -ne ".gitkeep") { $cnt++ }
}
if ($cnt -eq 0) {
    Write-Host "WARN: uploads empty" -ForegroundColor Yellow
} else {
    Write-Host "[OK] uploads: $cnt files" -ForegroundColor Green
}

Write-Step "5/6 MySQL port"
$tcp = Test-NetConnection -ComputerName $DbHost -Port $Port -WarningAction SilentlyContinue
if (-not $tcp.TcpTestSucceeded) {
    Write-Host "ERR: MySQL not reachable on port $Port" -ForegroundColor Red
    exit 1
}

if ($SkipStart) {
    Write-Step "6/6 Done (-SkipStart). Run: .\mvnw.cmd spring-boot:run"
    exit 0
}

Write-Step "6/6 Start Spring Boot"
Write-Host "http://localhost:8080" -ForegroundColor Yellow

if (-not $NoBrowser) {
    Start-Job -ScriptBlock {
        Start-Sleep -Seconds 25
        Start-Process "http://localhost:8080"
    } | Out-Null
}

$mvnw = Join-Path $root "mvnw.cmd"
& $mvnw spring-boot:run
