# Pre-push verify: MySQL import + Spring Boot + homepage 200
param(
    [string]$MySqlPassword = "123456",
    [int]$WaitSeconds = 90
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "=== CSRRM leader startup verify ===" -ForegroundColor Cyan

$env:MYSQL_PW = $MySqlPassword
& (Join-Path $PSScriptRoot "write-leader-pwd.ps1")
if ($LASTEXITCODE -ne 0) { exit 1 }

& (Join-Path $PSScriptRoot "test-mysql-login.ps1") -Password $MySqlPassword
if ($LASTEXITCODE -ne 0) { exit 1 }

& (Join-Path $PSScriptRoot "leader-run-setup.ps1") -PwdFile (Join-Path $env:TEMP "csrrm_mysql_pwd.txt")
if ($LASTEXITCODE -ne 0) { exit 1 }

$localProps = Join-Path $root "src\main\resources\application-local.properties"
$raw = Get-Content -LiteralPath $localProps -Raw
if ($raw -match 'app\.upload\.dir=[^\r\n]*\\') {
    Write-Host "ERR: app.upload.dir must use forward slashes /" -ForegroundColor Red
    exit 1
}

Write-Host "Starting Spring Boot (max $WaitSeconds s)..." -ForegroundColor Yellow
$proc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "mvnw.cmd -q -DskipTests spring-boot:run" `
    -WorkingDirectory $root -PassThru -WindowStyle Hidden

$ok = $false
try {
    for ($i = 0; $i -lt $WaitSeconds; $i++) {
        Start-Sleep -Seconds 1
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:8080/" -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) { $ok = $true; break }
        } catch { }
    }
} finally {
    if (-not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
}

if (-not $ok) {
    Write-Host "ERR: http://localhost:8080 not reachable" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] http://localhost:8080 returned 200" -ForegroundColor Green
exit 0
