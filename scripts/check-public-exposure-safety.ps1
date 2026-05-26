# Security check before exposing web via public tunnel
# Run from project root: .\scripts\check-public-exposure-safety.ps1

$ErrorActionPreference = "Continue"
Write-Host "=== CSRRM public exposure safety check ===" -ForegroundColor Cyan
Write-Host ""

$issues = 0

# 1) MySQL must NOT be reachable from LAN if using public tunnel (best: 127.0.0.1 only)
$mysqlListeners = netstat -ano | Select-String "LISTENING" | Select-String ":3306"
$mysqlPublic = $mysqlListeners | Where-Object { $_ -match "0\.0\.0\.0:3306|\[::\]:3306" }
if ($mysqlPublic) {
    Write-Host "WARN: MySQL listens on all interfaces (0.0.0.0:3306)." -ForegroundColor Yellow
    Write-Host "      For public tunnel demos, bind MySQL to 127.0.0.1 only." -ForegroundColor Yellow
    Write-Host "      Do NOT expose port 3306 on router firewall." -ForegroundColor Yellow
    $issues++
} else {
    Write-Host "OK: MySQL is not on 0.0.0.0:3306 (or not running)." -ForegroundColor Green
}

# 2) Only tunnel HTTP 8080, never RDP/3389 etc.
Write-Host "OK: Only use tunnel to http://127.0.0.1:8080 (not whole PC)." -ForegroundColor Green

# 3) Demo passwords warning
Write-Host ""
Write-Host "REMINDER before public URL:" -ForegroundColor Yellow
Write-Host "  - Demo accounts reset on each backend restart (123456 / admin123)."
Write-Host "  - Anyone with the link can try to log in; use only for course demo."
Write-Host "  - Stop tunnel when demo ends (Ctrl+C)."
Write-Host "  - Never tunnel MySQL (3306) or Windows Remote Desktop."

if ($issues -gt 0) {
    Write-Host ""
    Write-Host "Fix warnings above, then start public tunnel." -ForegroundColor Yellow
    exit 1
}
Write-Host ""
Write-Host "Safety check passed. You may start a public tunnel." -ForegroundColor Green
