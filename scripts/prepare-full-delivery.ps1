# Prepare full delivery: export MySQL snapshot + checklist before git push
# Usage: .\scripts\prepare-full-delivery.ps1

param(
    [switch]$SkipBuild,
    [switch]$SkipExport
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Full delivery prepare (DB + uploads)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not $SkipExport) {
    if (-not (Get-Command mysqldump -ErrorAction SilentlyContinue)) {
        Write-Host "ERR: mysqldump not in PATH. Install MySQL 8." -ForegroundColor Red
        exit 1
    }
    & (Join-Path $PSScriptRoot "export-database-for-git.ps1")
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if (-not $SkipBuild) {
    $staticIndex = Join-Path $root "src\main\resources\static\index.html"
    if (-not (Test-Path $staticIndex)) {
        Write-Host "Building frontend..." -ForegroundColor Yellow
        & (Join-Path $PSScriptRoot "build-frontend.ps1")
    } else {
        Write-Host "[OK] static: $staticIndex" -ForegroundColor Green
    }
}

$cfgDir = Join-Path $root "docs\06-部署配置"
if (-not (Test-Path $cfgDir)) {
    $cfgDir = (Get-ChildItem (Join-Path $root "docs") -Directory | Where-Object { $_.Name -match '^06-' } | Select-Object -First 1).FullName
}
$guideDir = Join-Path $root "docs\01-使用指南"
if (-not (Test-Path $guideDir)) {
    $guideDir = (Get-ChildItem (Join-Path $root "docs") -Directory | Where-Object { $_.Name -match '^01-' } | Select-Object -First 1).FullName
}
$files = @(
    (Join-Path $cfgDir "schema.sql"),
    (Join-Path $cfgDir "data.sql"),
    (Join-Path $cfgDir "database-full.sql"),
    (Join-Path $cfgDir "init-shared-mysql.sql"),
    (Join-Path $guideDir "09-全量交付与组长一键验收.md"),
    (Join-Path $root "scripts\setup-after-clone.ps1"),
    (Join-Path $root "scripts\prepare-full-delivery.ps1")
)

Write-Host ""
Write-Host "=== Files to commit ===" -ForegroundColor Yellow
foreach ($p in $files) {
    if (Test-Path $p) {
        $size = (Get-Item $p).Length
        $rel = $p.Substring($root.Length + 1)
        Write-Host "  [OK] $rel ($([math]::Round($size/1KB, 1)) KB)" -ForegroundColor Green
    } else {
        Write-Host "  [!!] missing $p" -ForegroundColor Red
    }
}

$uploadRoot = Join-Path $root "uploads"
$uploadFiles = @(Get-ChildItem $uploadRoot -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -ne "README.md" -and $_.Name -ne ".gitkeep" })
Write-Host ""
Write-Host "uploads files: $($uploadFiles.Count)" -ForegroundColor Cyan
foreach ($f in $uploadFiles) {
    $rel = $f.FullName.Substring($uploadRoot.Length + 1)
    Write-Host "  uploads\$rel"
}

$manifest = @{
    generatedAt = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    database    = "study_room_reservation"
    sqlFiles    = @("schema.sql", "data.sql", "database-full.sql")
    uploadCount = $uploadFiles.Count
    leaderScript = "scripts/setup-after-clone.ps1"
}
$manifestPath = Join-Path $cfgDir "delivery-manifest.json"
$manifest | ConvertTo-Json -Depth 3 | Set-Content -Path $manifestPath -Encoding UTF8
Write-Host ""
Write-Host "[OK] wrote $manifestPath" -ForegroundColor Green

Write-Host ""
Write-Host "=== Suggested git commands ===" -ForegroundColor Yellow
Write-Host "git add docs/06-部署配置/*.sql docs/06-部署配置/delivery-manifest.json"
Write-Host "git add docs/01-使用指南/09-全量交付与组长一键验收.md"
Write-Host "git add scripts/setup-after-clone.ps1 scripts/prepare-full-delivery.ps1"
Write-Host "git add uploads/material uploads/layout uploads/common"
Write-Host "git add src/main/resources/static"
Write-Host "git commit -m `"chore: full delivery snapshot`""
Write-Host "git push"
Write-Host ""
Write-Host "Doc: docs\01-使用指南\09-全量交付与组长一键验收.md" -ForegroundColor Green
